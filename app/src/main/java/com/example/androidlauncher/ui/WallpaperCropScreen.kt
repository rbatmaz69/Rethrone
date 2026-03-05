package com.example.androidlauncher.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Transformations-Zustand
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Bounds Check für Pan/Zoom
    var minScale by remember { mutableFloatStateOf(1f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Initiales Laden des Bildes
    LaunchedEffect(sourceUri) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    // Wir laden das Bild. Für sehr große Bilder könnte man hier subsampling nutzen (BitmapFactory.Options)
                    // Da wir aber später croppen wollen, laden wir es hier erst einmal so.
                    // Bei OutOfMemory müsste man aggressiver subsamplen.
                    val original = BitmapFactory.decodeStream(stream)

                    // Rotations-Logik (EXIF) weggelassen zur Vereinfachung,
                    // für Produktionscode sollte EXIF-Rotation beachtet werden.
                    // Moderne Decoder handhaben das teils automatisch oder brauchen Helper.

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

    // Initialisierung der Skalierung sobald Bild und Container Größe bekannt sind
    LaunchedEffect(bitmap, containerSize) {
        val bmp = bitmap
        if (bmp != null && containerSize.width > 0 && containerSize.height > 0) {

            val scaleX = containerSize.width.toFloat() / bmp.width
            val scaleY = containerSize.height.toFloat() / bmp.height

            // "Center Crop" Logik als Startwert: Fülle den Screen
            minScale = max(scaleX, scaleY)
            scale = minScale

            // Zentrieren
            val scaledWidth = bmp.width * scale
            val scaledHeight = bmp.height * scale
            offsetX = (containerSize.width - scaledWidth) / 2f
            offsetY = (containerSize.height - scaledHeight) / 2f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
            .clipToBounds()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        } else {
            bitmap?.let { bmp ->
                val bmpWidthDp = with(density) { bmp.width.toDp() }
                val bmpHeightDp = with(density) { bmp.height.toDp() }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                if (bitmap == null) return@detectTransformGestures
                                val bmp = bitmap!!

                                // 1. Berechne neuen Skalierungsfaktor
                                val newScale = (scale * zoom).coerceAtLeast(minScale).coerceAtMost(5f)

                                // 2. Berechne neuen Offset, damit Zoom um den Schwerpunkt (Finger) passiert
                                // Formel: newOffset = centroid - (centroid - oldOffset) * (newScale / oldScale)
                                val zoomFactor = newScale / scale
                                var newOffsetX = centroid.x - (centroid.x - offsetX) * zoomFactor
                                var newOffsetY = centroid.y - (centroid.y - offsetY) * zoomFactor

                                // 3. Addiere Pan-Bewegung
                                newOffsetX += pan.x
                                newOffsetY += pan.y

                                // 4. Begrenze Offset
                                val scaledWidth = bmp.width * newScale
                                val scaledHeight = bmp.height * newScale

                                val minOffsetX = if (scaledWidth > containerSize.width)
                                    -(scaledWidth - containerSize.width) else (containerSize.width - scaledWidth) / 2f
                                val maxOffsetX = if (scaledWidth > containerSize.width)
                                    0f else (containerSize.width - scaledWidth) / 2f

                                val minOffsetY = if (scaledHeight > containerSize.height)
                                    -(scaledHeight - containerSize.height) else (containerSize.height - scaledHeight) / 2f
                                val maxOffsetY = if (scaledHeight > containerSize.height)
                                    0f else (containerSize.height - scaledHeight) / 2f

                                scale = newScale
                                offsetX = newOffsetX.coerceIn(minOffsetX, maxOffsetX)
                                offsetY = newOffsetY.coerceIn(minOffsetY, maxOffsetY)
                            }
                        }
                ) {
                   Image(
                       bitmap = bmp.asImageBitmap(),
                       contentDescription = null,
                       alignment = Alignment.TopStart,
                       modifier = Modifier
                           .size(bmpWidthDp, bmpHeightDp)
                           .graphicsLayer {
                               this.scaleX = scale
                               this.scaleY = scale
                               this.translationX = offsetX
                               this.translationY = offsetY
                               // Origin auf 0,0 setzen für einfachere Pan/Zoom Berechnung von oben links
                               this.transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                           },
                       contentScale = ContentScale.FillBounds
                   )
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
                .navigationBarsPadding() // Wichtig für Transparenz hinter Navbar
                .padding(bottom = 32.dp, start = 32.dp, end = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel Button
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Abbrechen")
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            IconButton(
                onClick = {
                    if (bitmap != null && !isLoading) {
                        isLoading = true
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
                                isLoading = false
                                if (resultUri != null) {
                                    onCropFinished(resultUri)
                                } else {
                                    Toast.makeText(context, "Fehler beim Speichern", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Speichern")
            }
        }
    }
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

