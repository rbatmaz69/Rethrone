package com.example.androidlauncher.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Vollbild-Farbpinzette: zeigt das Wallpaper (ohne Dim/Blur für echte Farben) und liest
 * den Pixel unter dem Finger aus. Tippen oder Ziehen wählt; „Übernehmen" liefert die Farbe.
 *
 * Das Screen→Bitmap-Mapping berücksichtigt `ContentScale.Crop` (Skalierung + zentrierter Offset).
 */
@Composable
fun WallpaperEyedropper(
    customWallpaperUri: String?,
    onPicked: (Color) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var pointer by remember { mutableStateOf<Offset?>(null) }
    var picked by remember { mutableStateOf(Color.White) }

    LaunchedEffect(customWallpaperUri) {
        val bmp = withContext(Dispatchers.IO) { loadWallpaperBitmap(context, customWallpaperUri) }
        bitmap = bmp
        imageBitmap = bmp?.asImageBitmap()
    }

    // Zurück bricht die Pinzette ab (statt das ganze Farben-Menü zu schließen).
    BackHandler(enabled = true) { onCancel() }

    fun sampleAt(offset: Offset) {
        val bmp = bitmap ?: return
        if (viewSize.width == 0 || viewSize.height == 0) return
        val vw = viewSize.width.toFloat()
        val vh = viewSize.height.toFloat()
        val bw = bmp.width.toFloat()
        val bh = bmp.height.toFloat()
        val scale = maxOf(vw / bw, vh / bh) // Crop: füllt die Fläche
        val offX = (bw * scale - vw) / 2f
        val offY = (bh * scale - vh) / 2f
        val bx = ((offset.x + offX) / scale).roundToInt().coerceIn(0, bmp.width - 1)
        val by = ((offset.y + offY) / scale).roundToInt().coerceIn(0, bmp.height - 1)
        picked = Color(bmp.getPixel(bx, by))
        pointer = offset
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { viewSize = it }
            .pointerInput(bitmap) { detectTapGestures { sampleAt(it) } }
            .pointerInput(bitmap) { detectDragGestures { change, _ -> sampleAt(change.position) } }
    ) {
        imageBitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Vorschau-Ring über dem Finger
        pointer?.let { p ->
            val ringSize = 56.dp
            val ringPx = with(density) { ringSize.toPx() }
            val liftPx = with(density) { 72.dp.toPx() }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (p.x - ringPx / 2f).roundToInt(),
                            (p.y - liftPx).roundToInt()
                        )
                    }
                    .size(ringSize)
                    .clip(CircleShape)
                    .background(picked)
                    .border(3.dp, Color.White, CircleShape)
            )
        }

        // Hinweis oben
        Text(
            text = stringResource(R.string.eyedropper_hint),
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(24.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )

        // Untere Leiste: Vorschau + Hex + Aktionen
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(24.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(picked)
                    .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
            )
            Text(
                text = "#%06X".format(0xFFFFFF and picked.toArgb()),
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel), color = Color.White)
            }
            Button(
                onClick = { onPicked(picked) },
                colors = ButtonDefaults.buttonColors(containerColor = picked)
            ) {
                Text(
                    stringResource(R.string.apply),
                    color = if (picked.luminance() > 0.5f) Color.Black else Color.White
                )
            }
        }
    }
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
