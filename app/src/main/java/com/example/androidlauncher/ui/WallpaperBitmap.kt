package com.example.androidlauncher.ui

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri

/**
 * Lädt das aktuell sichtbare Wallpaper als Bitmap – bevorzugt das benutzerdefinierte
 * Wallpaper (über die URI), sonst das System-Wallpaper.
 *
 * Geteilt von [SystemWallpaperView] (Anzeige) und der Farbpinzette (Pixel-Sampling).
 * Sollte auf einem IO-Dispatcher aufgerufen werden (Bitmap-Dekodierung).
 */
@SuppressLint("MissingPermission")
fun loadWallpaperBitmap(context: Context, customWallpaperUri: String?): Bitmap? {
    if (!customWallpaperUri.isNullOrEmpty()) {
        try {
            context.contentResolver.openInputStream(customWallpaperUri.toUri())?.use { stream ->
                BitmapFactory.decodeStream(stream)?.let { return it }
            }
        } catch (_: Exception) {
            // Fällt unten auf das System-Wallpaper zurück.
        }
    }
    return try {
        WallpaperManager.getInstance(context).drawable?.toBitmap()
    } catch (_: Exception) {
        null
    }
}
