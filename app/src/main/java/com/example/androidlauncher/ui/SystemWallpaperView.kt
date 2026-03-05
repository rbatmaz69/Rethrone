package com.example.androidlauncher.ui

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.example.androidlauncher.ui.theme.LocalColorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Zeigt den Hintergrund des Launchers an.
 *
 * Unterstützt drei Modi:
 * 1. **Benutzerdefiniertes Wallpaper** – Geladen aus einer URI (z.B. nach Zuschneiden).
 * 2. **System-Wallpaper** – Das aktuelle Gerätehintergrundbild.
 * 3. **Gradient-Fallback** – Farbverlauf aus dem aktuellen Theme, wenn kein Wallpaper verfügbar.
 *
 * Über die Parameter [blurLevel], [dimLevel] und [zoomLevel] kann das Erscheinungsbild
 * des Hintergrunds feinjustiert werden.
 *
 * Das Wallpaper wird asynchron auf dem IO-Dispatcher geladen, um den Main-Thread
 * nicht zu blockieren.
 *
 * @param customWallpaperUri URI eines benutzerdefinierten Hintergrundbilds (optional).
 * @param blurLevel Unschärfe in dp (0 = scharf, 25 = maximal unscharf).
 * @param dimLevel Abdunklung (0.0 = keine, 1.0 = vollständig schwarz).
 * @param zoomLevel Zoom-Faktor (1.0 = Originalgröße, 2.0 = doppelt vergrößert).
 */
@SuppressLint("MissingPermission")
@Composable
fun SystemWallpaperView(
    customWallpaperUri: String? = null,
    blurLevel: Float = 0f,
    dimLevel: Float = 0.1f,
    zoomLevel: Float = 1.0f
) {
    val context = LocalContext.current
    val colorTheme = LocalColorTheme.current
    val wallpaperManager = WallpaperManager.getInstance(context)
    var wallpaperBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // Wallpaper asynchron laden – reagiert auf URI-Änderungen
    LaunchedEffect(customWallpaperUri) {
        // Beim expliziten Entfernen sofort leeren, damit kein altes Custom-Wallpaper stehen bleibt.
        if (customWallpaperUri.isNullOrEmpty()) {
            wallpaperBitmap = null
        }

        withContext(Dispatchers.IO) {
            try {
                if (!customWallpaperUri.isNullOrEmpty()) {
                    val uri = customWallpaperUri.toUri()
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val b = BitmapFactory.decodeStream(stream)
                        val ib = b?.asImageBitmap()
                        if (ib != null) {
                            withContext(Dispatchers.Main) { wallpaperBitmap = ib }
                        }
                    }
                } else {
                    val drawable = wallpaperManager.drawable
                    val ib = drawable?.toBitmap()?.asImageBitmap()
                    if (ib != null) {
                        withContext(Dispatchers.Main) { wallpaperBitmap = ib }
                    }
                }
            } catch (_: Exception) {
                // Fallback: System-Wallpaper laden bei URI-Fehler
                try {
                    val drawable = wallpaperManager.drawable
                    val ib = drawable?.toBitmap()?.asImageBitmap()
                    if (ib != null) {
                        withContext(Dispatchers.Main) { wallpaperBitmap = ib }
                    }
                } catch (_: Exception) {
                    // Kein Wallpaper verfügbar – beim Reset bleibt dann korrekt der Gradient-Fallback sichtbar.
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        wallpaperBitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoomLevel
                        scaleY = zoomLevel
                    }
                    .blur(blurLevel.dp),
                contentScale = ContentScale.Crop
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(colorTheme.primary, colorTheme.secondary)
                    )
                )
        )

        // Dim-Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimLevel))
        )
    }
}
