package com.example.androidlauncher.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val MAX_ZOOM = 5f

private enum class CropExitPhase {
    Idle,
    ConfirmPulse,
    FlyOut
}

/**
 * Ein Bildschirm zum Zuschneiden von Hintergrundbildern.
 *
 * Zeigt das ausgewählte Bild an und erlaubt Zoom & Pan.
 * Überlagert wird eine Vorschau des Homescreens (Icons, Uhrzeit etc.),
 * damit der Nutzer genau sehen kann, wie das Ergebnis aussieht.
 *
 * Das Zuschneiden erfolgt exakt basierend auf dem sichtbaren Ausschnitt
 * und der Bildschirmauflösung.
 */
@Composable
fun WallpaperCropScreen(
    sourceUri: Uri,
    onCropFinished: (Uri) -> Unit,
    onCancel: () -> Unit,
    homeScreenPreview: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember(sourceUri) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(sourceUri) { mutableStateOf(true) }
    var isSaving by remember(sourceUri) { mutableStateOf(false) }
    var exitPhase by remember(sourceUri) { mutableStateOf(CropExitPhase.Idle) }
    var pendingCroppedUri by remember(sourceUri) { mutableStateOf<Uri?>(null) }

    val exitTransition = updateTransition(targetState = exitPhase, label = "cropExitTransition")
    val contentScale by exitTransition.animateFloat(
        transitionSpec = {
            when {
                initialState == CropExitPhase.Idle && targetState == CropExitPhase.ConfirmPulse -> {
                    keyframes {
                        durationMillis = 140
                        1.02f at 90
                    }
                }
                else -> tween(durationMillis = 300)
            }
        },
        label = "cropContentScale"
    ) { phase ->
        when (phase) {
            CropExitPhase.Idle -> 1f
            CropExitPhase.ConfirmPulse -> 1.02f
            CropExitPhase.FlyOut -> 0.9f
        }
    }
    val contentAlpha by exitTransition.animateFloat(
        transitionSpec = { tween(durationMillis = 300) },
        label = "cropContentAlpha"
    ) { phase ->
        when (phase) {
            CropExitPhase.Idle -> 1f
            CropExitPhase.ConfirmPulse -> 1f
            CropExitPhase.FlyOut -> 0f
        }
    }
    val contentTranslationY by exitTransition.animateFloat(
        transitionSpec = { tween(durationMillis = 300) },
        label = "cropContentTranslationY"
    ) { phase ->
        when (phase) {
            CropExitPhase.Idle -> 0f
            CropExitPhase.ConfirmPulse -> -8f
            CropExitPhase.FlyOut -> 42f
        }
    }
    val overlayAlpha by exitTransition.animateFloat(
        transitionSpec = { tween(durationMillis = 300) },
        label = "cropOverlayAlpha"
    ) { phase ->
        when (phase) {
            CropExitPhase.Idle -> 0f
            CropExitPhase.ConfirmPulse -> 0.05f
            CropExitPhase.FlyOut -> 0.18f
        }
    }
    val confirmButtonScale by exitTransition.animateFloat(
        transitionSpec = {
            if (targetState == CropExitPhase.ConfirmPulse) {
                keyframes {
                    durationMillis = 160
                    1.14f at 80
                    1f at 160
                }
            } else {
                tween(durationMillis = 180)
            }
        },
        label = "cropConfirmButtonScale"
    ) { phase ->
        when (phase) {
            CropExitPhase.Idle -> 1f
            CropExitPhase.ConfirmPulse -> 1f
            CropExitPhase.FlyOut -> 0.94f
        }
    }

    // Transformations-Zustand
    var scale by remember(sourceUri) { mutableFloatStateOf(1f) }
    var offsetX by remember(sourceUri) { mutableFloatStateOf(0f) }
    var offsetY by remember(sourceUri) { mutableFloatStateOf(0f) }

    // Bounds Check für Pan/Zoom
    var minScale by remember(sourceUri) { mutableFloatStateOf(1f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var userHasInteracted by remember(sourceUri) { mutableStateOf(false) }
    var initializedContainerSize by remember(sourceUri) { mutableStateOf(IntSize.Zero) }

    // Initiales Laden des Bildes
    LaunchedEffect(sourceUri) {
        withContext(Dispatchers.IO) {
            try {
                val original = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }

                withContext(Dispatchers.Main) {
                    if (original == null) {
                        Toast.makeText(context, "Fehler beim Laden des Bildes", Toast.LENGTH_SHORT).show()
                        onCancel()
                        return@withContext
                    }
                    bitmap = original
                    isLoading = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Fehler beim Laden des Bildes", Toast.LENGTH_SHORT).show()
                    onCancel()
                }
            }
        }
    }

    // Initialisierung robust gegen spaete Layout-Groessenaenderungen.
    LaunchedEffect(bitmap, containerSize, userHasInteracted) {
        val bmp = bitmap
        if (bmp != null && containerSize.width > 0 && containerSize.height > 0) {
            val targetMinScale = max(
                containerSize.width.toFloat() / bmp.width.toFloat(),
                containerSize.height.toFloat() / bmp.height.toFloat()
            )

            if (!userHasInteracted) {
                // Vor erster Geste immer auf Cover fitten, auch wenn die Viewport-Groesse nachtraeglich wechselt.
                minScale = targetMinScale
                scale = targetMinScale
                val scaledWidth = bmp.width * scale
                val scaledHeight = bmp.height * scale
                offsetX = (containerSize.width - scaledWidth) / 2f
                offsetY = (containerSize.height - scaledHeight) / 2f
            } else {
                // Nach erster Geste Position beibehalten, nur an neue Mindest-Skalierung/Grenzen anpassen.
                minScale = targetMinScale
                val adjustedScale = scale.coerceAtLeast(targetMinScale)
                val boundedOffset = clampOffsetToViewport(
                    bitmapWidth = bmp.width,
                    bitmapHeight = bmp.height,
                    viewportWidth = containerSize.width,
                    viewportHeight = containerSize.height,
                    scale = adjustedScale,
                    offsetX = offsetX,
                    offsetY = offsetY
                )
                scale = adjustedScale
                offsetX = boundedOffset.first
                offsetY = boundedOffset.second
            }

            initializedContainerSize = containerSize
        }
    }

    LaunchedEffect(pendingCroppedUri) {
        val resultUri = pendingCroppedUri ?: return@LaunchedEffect
        exitPhase = CropExitPhase.ConfirmPulse
        delay(140)
        exitPhase = CropExitPhase.FlyOut
        delay(300)
        onCropFinished(resultUri)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = contentScale
                    scaleY = contentScale
                    alpha = contentAlpha
                    translationY = contentTranslationY
                }
        ) {
            if (isLoading || bitmap == null || initializedContainerSize == IntSize.Zero) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            } else {
                bitmap?.let { bmp ->
                    val imageBitmap = remember(bmp) { bmp.asImageBitmap() }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(bmp, containerSize, minScale, isSaving, exitPhase) {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    if (isSaving || exitPhase != CropExitPhase.Idle) return@detectTransformGestures
                                    if (!userHasInteracted && (zoom != 1f || pan.x != 0f || pan.y != 0f)) {
                                        userHasInteracted = true
                                    }
                                    val newScale = (scale * zoom).coerceIn(minScale, MAX_ZOOM)

                                    // Zoom um den Finger-Schwerpunkt + anschließend Pan.
                                    val zoomFactor = newScale / scale
                                    val rawOffsetX = centroid.x - (centroid.x - offsetX) * zoomFactor + pan.x
                                    val rawOffsetY = centroid.y - (centroid.y - offsetY) * zoomFactor + pan.y

                                    scale = newScale
                                    val boundedOffset = clampOffsetToViewport(
                                        bitmapWidth = bmp.width,
                                        bitmapHeight = bmp.height,
                                        viewportWidth = containerSize.width,
                                        viewportHeight = containerSize.height,
                                        scale = newScale,
                                        offsetX = rawOffsetX,
                                        offsetY = rawOffsetY
                                    )
                                    offsetX = boundedOffset.first
                                    offsetY = boundedOffset.second
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val drawWidth = (bmp.width * scale).roundToInt().coerceAtLeast(1)
                            val drawHeight = (bmp.height * scale).roundToInt().coerceAtLeast(1)
                            drawImage(
                                image = imageBitmap,
                                dstOffset = IntOffset(offsetX.roundToInt(), offsetY.roundToInt()),
                                dstSize = IntSize(drawWidth, drawHeight)
                            )
                        }
                    }
                }
            }

            // Homescreen Vorschau Overlay
            // Wir nutzen eine Box, die Klicks durchlässt an das Bild (da HomeScreen isPreview=true hat und keine gestures fängt)
            // Aber Moment: HomeScreen(isPreview=true) hat interactive Elemente deaktiviert.
            // Das Bild darunter fängt die Gesten via pointerInput des Containers.
            Box(modifier = Modifier.fillMaxSize()) {
                homeScreenPreview()
            }

            // UI Controls (Buttons)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp, start = 32.dp, end = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCancel,
                    enabled = !isSaving && exitPhase == CropExitPhase.Idle,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Abbrechen")
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = {
                        if (bitmap != null && !isLoading && !isSaving && exitPhase == CropExitPhase.Idle) {
                            isSaving = true
                            scope.launch(Dispatchers.IO) {
                                val resultUri = cropAndSaveImage(
                                    context = context,
                                    sourceBitmap = bitmap!!,
                                    containerWidth = containerSize.width,
                                    containerHeight = containerSize.height,
                                    scale = scale,
                                    offsetX = offsetX,
                                    offsetY = offsetY
                                )
                                withContext(Dispatchers.Main) {
                                    isSaving = false
                                    if (resultUri != null) {
                                        pendingCroppedUri = resultUri
                                    } else {
                                        Toast.makeText(context, "Fehler beim Speichern", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isSaving && exitPhase == CropExitPhase.Idle,
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer {
                            scaleX = confirmButtonScale
                            scaleY = confirmButtonScale
                        }
                        .clip(CircleShape)
                        .background(Color.White),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Speichern")
                }
            }
        }

        if (overlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = overlayAlpha))
            )
        }

        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }
    }
}

private fun clampOffsetToViewport(
    bitmapWidth: Int,
    bitmapHeight: Int,
    viewportWidth: Int,
    viewportHeight: Int,
    scale: Float,
    offsetX: Float,
    offsetY: Float
): Pair<Float, Float> {
    val scaledWidth = bitmapWidth * scale
    val scaledHeight = bitmapHeight * scale

    val minOffsetX = if (scaledWidth > viewportWidth) {
        -(scaledWidth - viewportWidth)
    } else {
        (viewportWidth - scaledWidth) / 2f
    }
    val maxOffsetX = if (scaledWidth > viewportWidth) 0f else minOffsetX

    val minOffsetY = if (scaledHeight > viewportHeight) {
        -(scaledHeight - viewportHeight)
    } else {
        (viewportHeight - scaledHeight) / 2f
    }
    val maxOffsetY = if (scaledHeight > viewportHeight) 0f else minOffsetY

    return offsetX.coerceIn(minOffsetX, maxOffsetX) to offsetY.coerceIn(minOffsetY, maxOffsetY)
}

/**
 * Schneidet den sichtbaren Bereich aus dem Bitmap aus und speichert ihn.
 */
private fun cropAndSaveImage(
    context: Context,
    sourceBitmap: Bitmap,
    containerWidth: Int,
    containerHeight: Int,
    scale: Float,
    offsetX: Float,
    offsetY: Float
): Uri? {
    return try {
        // 1. Berechne das Rechteck im Originalbild, das aktuell sichtbar ist.
        // Screen Koordinate (0,0) entspricht Image Koordinate: (-offsetX / scale, -offsetY / scale)

        val visibleLeft = (-offsetX / scale).coerceAtLeast(0f)
        val visibleTop = (-offsetY / scale).coerceAtLeast(0f)

        val visibleWidth = (containerWidth / scale)
        val visibleHeight = (containerHeight / scale)

        // Sicherstellen, dass wir nicht außerhalb des Bitmaps lesen
        val cropX = visibleLeft.roundToInt()
        val cropY = visibleTop.roundToInt()
        val cropWidth = min(visibleWidth.roundToInt(), sourceBitmap.width - cropX)
        val cropHeight = min(visibleHeight.roundToInt(), sourceBitmap.height - cropY)

        if (cropWidth <= 0 || cropHeight <= 0) return null

        val croppedBitmap = Bitmap.createBitmap(sourceBitmap, cropX, cropY, cropWidth, cropHeight)

        // 2. Skaliere das Ergebnis auf die Bildschirmgröße (optional, aber gut für Performance/Qualität)
        // Oder wir speichern in voller Auflösung des Ausschnitts (besser für Wallpaper Quality).
        // User Story sagt: "Speicheroptimierung". Zu große Bitmaps können Launcher verlangsamen.
        // Android empfiehlt oft Bildschirmauflösung oder leicht höher.
        // Wir skalieren auf max 1080p Breite oder Höhe wenn es riesig ist, aber behalten Aspect Ratio des Screens.
        // Da 'cropWidth/cropHeight' das Aspect Ratio des Screens haben sollte (da containerWidth/Height das Ratio vorgeben),
        // können wir es einfach speichern.

        val file = File(context.cacheDir, "cropped_wallpaper_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
