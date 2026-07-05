package com.example.androidlauncher.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Hintergrund-Einstellungen: eigenes Wallpaper (URI) sowie Blur-, Dim- und
 * Zoom-Faktor. Aus dem ThemeManager extrahiert (A1-Split); nutzt dieselbe
 * DataStore-Datei wie zuvor, daher keine Datenmigration.
 */
class WallpaperSettings(private val dataStore: DataStore<Preferences>) {

    /** Produktiv-Konstruktor: nutzt die geteilte "settings"-DataStore-Datei. */
    constructor(context: Context) : this(context.settingsDataStore)

    companion object {
        private val CUSTOM_WALLPAPER_KEY = stringPreferencesKey("custom_wallpaper_uri")
        private val WALLPAPER_BLUR_KEY = floatPreferencesKey("wallpaper_blur")
        private val WALLPAPER_DIM_KEY = floatPreferencesKey("wallpaper_dim")
        private val WALLPAPER_ZOOM_KEY = floatPreferencesKey("wallpaper_zoom")
    }

    /** URI des eigenen Wallpapers oder null, wenn das System-Wallpaper genutzt wird. */
    val customWallpaperUri: Flow<String?> = dataStore.data
        .map { it[CUSTOM_WALLPAPER_KEY] }

    val wallpaperBlur: Flow<Float> = dataStore.data
        .map { it[WALLPAPER_BLUR_KEY] ?: 0f }

    val wallpaperDim: Flow<Float> = dataStore.data
        .map { it[WALLPAPER_DIM_KEY] ?: 0.1f }

    val wallpaperZoom: Flow<Float> = dataStore.data
        .map { it[WALLPAPER_ZOOM_KEY] ?: 1.0f }

    /** Setzt das eigene Wallpaper; null wechselt zurück zum System-Wallpaper. */
    suspend fun setCustomWallpaperUri(uri: String?) {
        dataStore.edit {
            if (uri == null) {
                it.remove(CUSTOM_WALLPAPER_KEY)
            } else {
                it[CUSTOM_WALLPAPER_KEY] = uri
            }
        }
    }

    suspend fun setWallpaperBlur(blur: Float) {
        dataStore.edit { it[WALLPAPER_BLUR_KEY] = blur }
    }

    suspend fun setWallpaperDim(dim: Float) {
        dataStore.edit { it[WALLPAPER_DIM_KEY] = dim }
    }

    suspend fun setWallpaperZoom(zoom: Float) {
        dataStore.edit { it[WALLPAPER_ZOOM_KEY] = zoom }
    }
}
