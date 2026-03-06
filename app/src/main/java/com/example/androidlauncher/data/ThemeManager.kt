package com.example.androidlauncher.data

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.androidlauncher.ui.theme.ColorTheme
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

// DataStore for saving general app settings
private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Manages application themes and visual settings using DataStore and System Settings.
 */
class ThemeManager(private val context: Context) {
    companion object {
        private val THEME_KEY = stringPreferencesKey("selected_theme")
        private val FONT_SIZE_KEY = stringPreferencesKey("font_size")
        private val FONT_WEIGHT_KEY = stringPreferencesKey("font_weight")
        private val ICON_SIZE_KEY = stringPreferencesKey("icon_size")
        private val DARK_TEXT_KEY = booleanPreferencesKey("dark_text_enabled")
        private val SHOW_FAVORITE_LABELS_KEY = booleanPreferencesKey("show_favorite_labels")
        private val LIQUID_GLASS_KEY = booleanPreferencesKey("liquid_glass_enabled")
        private val APP_FONT_KEY = stringPreferencesKey("app_font")
        private val CUSTOM_WALLPAPER_KEY = stringPreferencesKey("custom_wallpaper_uri")
        private val WALLPAPER_BLUR_KEY = floatPreferencesKey("wallpaper_blur")
        private val WALLPAPER_DIM_KEY = floatPreferencesKey("wallpaper_dim")
        private val WALLPAPER_ZOOM_KEY = floatPreferencesKey("wallpaper_zoom")
        private val SHAKE_GESTURES_KEY = booleanPreferencesKey("shake_gestures_enabled")
        private val SMART_SUGGESTIONS_KEY = booleanPreferencesKey("smart_search_enabled")
        private val HAPTIC_FEEDBACK_KEY = booleanPreferencesKey("haptic_feedback_enabled")

        // Offset for UI elements (Relative to default position)
        private val FAVORITES_OFFSET_X_KEY = floatPreferencesKey("favorites_offset_x")
        private val FAVORITES_OFFSET_Y_KEY = floatPreferencesKey("favorites_offset_y")
        private val CLOCK_OFFSET_Y_KEY = floatPreferencesKey("clock_offset_y")
    }

    val selectedTheme: Flow<ColorTheme> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[THEME_KEY] ?: ColorTheme.SIGNATURE.name
            try { ColorTheme.valueOf(themeName) } catch (e: IllegalArgumentException) { ColorTheme.SIGNATURE }
        }

    val selectedFontSize: Flow<FontSize> = context.dataStore.data
        .map { preferences ->
            val fontSizeName = preferences[FONT_SIZE_KEY] ?: FontSize.STANDARD.name
            try { FontSize.valueOf(fontSizeName) } catch (e: IllegalArgumentException) { FontSize.STANDARD }
        }

    val selectedFontWeight: Flow<FontWeightLevel> = context.dataStore.data
        .map { preferences ->
            val fontWeightName = preferences[FONT_WEIGHT_KEY] ?: FontWeightLevel.NORMAL.name
            try { FontWeightLevel.valueOf(fontWeightName) } catch (e: IllegalArgumentException) { FontWeightLevel.NORMAL }
        }

    val selectedIconSize: Flow<IconSize> = context.dataStore.data
        .map { preferences ->
            val iconSizeName = preferences[ICON_SIZE_KEY] ?: IconSize.STANDARD.name
            try { IconSize.valueOf(iconSizeName) } catch (e: IllegalArgumentException) { IconSize.STANDARD }
        }

    val isDarkTextEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[DARK_TEXT_KEY] ?: false }

    val showFavoriteLabels: Flow<Boolean> = context.dataStore.data
        .map { it[SHOW_FAVORITE_LABELS_KEY] ?: false }

    val isLiquidGlassEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[LIQUID_GLASS_KEY] ?: true }

    val selectedAppFont: Flow<AppFont> = context.dataStore.data
        .map { preferences ->
            val fontName = preferences[APP_FONT_KEY] ?: AppFont.SYSTEM_DEFAULT.name
            try { AppFont.valueOf(fontName) } catch (e: IllegalArgumentException) { AppFont.SYSTEM_DEFAULT }
        }

    val customWallpaperUri: Flow<String?> = context.dataStore.data
        .map { it[CUSTOM_WALLPAPER_KEY] }

    val wallpaperBlur: Flow<Float> = context.dataStore.data
        .map { it[WALLPAPER_BLUR_KEY] ?: 0f }

    val wallpaperDim: Flow<Float> = context.dataStore.data
        .map { it[WALLPAPER_DIM_KEY] ?: 0.1f }

    val wallpaperZoom: Flow<Float> = context.dataStore.data
        .map { it[WALLPAPER_ZOOM_KEY] ?: 1.0f }

    val favoritesOffsetX: Flow<Float> = context.dataStore.data
        .map { it[FAVORITES_OFFSET_X_KEY] ?: 0f }

    val favoritesOffsetY: Flow<Float> = context.dataStore.data
        .map { it[FAVORITES_OFFSET_Y_KEY] ?: 0f }

    val clockOffsetY: Flow<Float> = context.dataStore.data
        .map { it[CLOCK_OFFSET_Y_KEY] ?: 0f }

    /**
     * Observable flow for shake gesture toggle.
     */
    val isShakeGesturesEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHAKE_GESTURES_KEY] ?: true
        }

    /**
     * Observable flow for intelligent search suggestions.
     */
    val isSmartSuggestionsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SMART_SUGGESTIONS_KEY] ?: true
        }

    /**
     * Observable flow for haptic feedback toggle.
     * Synchronized with system setting HAPTIC_FEEDBACK_ENABLED using a ContentObserver.
     */
    val isHapticFeedbackEnabled: Flow<Boolean> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                val enabled = Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
                trySend(enabled)
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.HAPTIC_FEEDBACK_ENABLED),
            false,
            observer
        )
        // Send initial value
        val initial = Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
        trySend(initial)

        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }.distinctUntilChanged()

    suspend fun setTheme(theme: ColorTheme) { context.dataStore.edit { it[THEME_KEY] = theme.name } }
    suspend fun setFontSize(fontSize: FontSize) { context.dataStore.edit { it[FONT_SIZE_KEY] = fontSize.name } }
    suspend fun setFontWeight(fontWeight: FontWeightLevel) { context.dataStore.edit { it[FONT_WEIGHT_KEY] = fontWeight.name } }
    suspend fun setIconSize(iconSize: IconSize) { context.dataStore.edit { it[ICON_SIZE_KEY] = iconSize.name } }
    suspend fun setDarkTextEnabled(enabled: Boolean) { context.dataStore.edit { it[DARK_TEXT_KEY] = enabled } }
    suspend fun setShowFavoriteLabels(show: Boolean) { context.dataStore.edit { it[SHOW_FAVORITE_LABELS_KEY] = show } }
    suspend fun setLiquidGlassEnabled(enabled: Boolean) { context.dataStore.edit { it[LIQUID_GLASS_KEY] = enabled } }
    suspend fun setAppFont(font: AppFont) { context.dataStore.edit { it[APP_FONT_KEY] = font.name } }
    suspend fun setCustomWallpaperUri(uri: String?) { context.dataStore.edit { if (uri == null) it.remove(CUSTOM_WALLPAPER_KEY) else it[CUSTOM_WALLPAPER_KEY] = uri } }
    suspend fun setWallpaperBlur(blur: Float) { context.dataStore.edit { it[WALLPAPER_BLUR_KEY] = blur } }
    suspend fun setWallpaperDim(dim: Float) { context.dataStore.edit { it[WALLPAPER_DIM_KEY] = dim } }
    suspend fun setWallpaperZoom(zoom: Float) { context.dataStore.edit { it[WALLPAPER_ZOOM_KEY] = zoom } }

    suspend fun setFavoritesOffset(x: Float, y: Float) {
        context.dataStore.edit {
            it[FAVORITES_OFFSET_X_KEY] = x
            it[FAVORITES_OFFSET_Y_KEY] = y
        }
    }

    suspend fun setClockOffset(y: Float) {
        context.dataStore.edit {
            it[CLOCK_OFFSET_Y_KEY] = y
        }
    }

    /**
     * Toggles haptic feedback both internally and in system settings if permission is granted.
     */
    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        // Try to update system setting
        try {
            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, if (enabled) 1 else 0)
            }
        } catch (e: Exception) {
            // Permission might be missing
        }
        
        // Also update internally to stay consistent
        context.dataStore.edit { it[HAPTIC_FEEDBACK_KEY] = enabled }
    }

    /**
     * Toggles shake gestures.
     */
    suspend fun setShakeGesturesEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHAKE_GESTURES_KEY] = enabled
        }
    }

    /**
     * Toggles intelligent search suggestions.
     */
    suspend fun setSmartSuggestionsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SMART_SUGGESTIONS_KEY] = enabled
        }
    }
}
