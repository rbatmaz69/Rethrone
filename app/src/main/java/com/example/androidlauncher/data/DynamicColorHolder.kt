package com.example.androidlauncher.data

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Aus dem System-Hintergrundbild extrahierte Seed-Farben (Material You).
 */
data class DynamicSeed(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
)

/**
 * Hält die aktuellen Wallpaper-Seed-Farben als Compose-State und speist damit
 * das `ColorTheme.DYNAMIC`-Theme. Quelle: [WallpaperManager.getWallpaperColors]
 * (API 27+). Ohne Wallpaper-Farben (oder Tests) gilt der Fallback im Theme.
 *
 * Da [seed] ein `mutableStateOf` ist, lösen Wallpaper-Wechsel automatisch
 * Recomposition der farbabhängigen Oberflächen aus.
 */
object DynamicColorHolder {

    var seed by mutableStateOf<DynamicSeed?>(null)
        private set

    private var listener: WallpaperManager.OnColorsChangedListener? = null

    /** Einmaliges Auslesen der aktuellen Wallpaper-Farben. */
    fun refresh(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return
        runCatching {
            val wm = WallpaperManager.getInstance(context)
            applyColors(wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM))
        }
    }

    /** Registriert einen Listener, der bei Wallpaper-Wechsel die Seed-Farben aktualisiert. */
    fun register(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1 || listener != null) return
        runCatching {
            val wm = WallpaperManager.getInstance(context)
            val l = WallpaperManager.OnColorsChangedListener { colors, which ->
                if (which and WallpaperManager.FLAG_SYSTEM != 0) applyColors(colors)
            }
            wm.addOnColorsChangedListener(l, Handler(Looper.getMainLooper()))
            listener = l
        }
        refresh(context)
    }

    private fun applyColors(colors: WallpaperColors?) {
        if (colors == null) return
        val p = Color(colors.primaryColor.toArgb())
        val s = colors.secondaryColor?.let { Color(it.toArgb()) } ?: p
        val t = colors.tertiaryColor?.let { Color(it.toArgb()) } ?: s
        seed = DynamicSeed(p, s, t)
    }
}
