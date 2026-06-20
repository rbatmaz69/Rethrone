package com.example.androidlauncher.ui

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
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
    zoomLevel: Float = 1.0f,
    showHero: Boolean = true
) {
    val context = LocalContext.current
    val colorTheme = LocalColorTheme.current
    val wallpaperManager = WallpaperManager.getInstance(context)
    var wallpaperBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // Vom aktuellen Theme mitgeliefertes Hintergrundbild (z.B. „Tulpe").
    val themeBackgroundRes = colorTheme.backgroundRes
    // Das Theme-Bild greift nur, wenn der Nutzer kein eigenes Wallpaper gesetzt hat.
    val useThemeBackground = customWallpaperUri.isNullOrEmpty() && themeBackgroundRes != null

    // Wallpaper asynchron laden – reagiert auf URI-Änderungen und auf das Theme-Bild.
    LaunchedEffect(customWallpaperUri, useThemeBackground) {
        // Beim expliziten Entfernen sofort leeren, damit kein altes Custom-Wallpaper stehen bleibt.
        if (customWallpaperUri.isNullOrEmpty()) {
            wallpaperBitmap = null
        }

        // Theme-Hintergrundbild hat Vorrang vor dem System-Wallpaper – kein Bitmap laden,
        // damit das Geräte-Wallpaper das Theme-Bild nicht überdeckt.
        if (useThemeBackground) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            val ib = loadWallpaperBitmap(context, customWallpaperUri)?.asImageBitmap()
            if (ib != null) {
                withContext(Dispatchers.Main) { wallpaperBitmap = ib }
            }
            // Kein Wallpaper verfügbar – beim Reset bleibt korrekt der Gradient-Fallback sichtbar.
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            wallpaperBitmap != null -> Image(
                bitmap = wallpaperBitmap!!,
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

            useThemeBackground -> Image(
                painter = painterResource(themeBackgroundRes!!),
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

            // Gradient-Fallback: aufgelöste Theme-Hintergrundfarbe (CUSTOM/DYNAMIC reaktiv via cust()/dyn()).
            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorTheme.backgroundBrush(LocalDarkTextEnabled.current))
            )
        }

        // Zentriertes Hero-Motiv (z.B. Tulpen-Silhouette) – nur auf dem Startbildschirm
        // (showHero) und nur ohne eigenes Wallpaper.
        colorTheme.heroImageRes?.let { heroRes ->
            if (showHero && customWallpaperUri.isNullOrEmpty()) {
                Image(
                    painter = painterResource(heroRes),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.55f),
                    contentScale = ContentScale.Fit,
                    colorFilter = colorTheme.heroTint?.let { ColorFilter.tint(it) }
                )
            }
        }

        // Dim-Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimLevel))
        )
    }
}
