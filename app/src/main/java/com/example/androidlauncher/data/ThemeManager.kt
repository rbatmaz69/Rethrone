package com.example.androidlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.androidlauncher.ui.theme.ColorTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore for saving general app settings
private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Manages application themes and visual settings using DataStore.
 * Handles persistence of:
 * - Color Theme
 * - Font Size
 * - Icon Size
 * - Dark Text Mode
 * - Favorite Labels Visibility
 * - Liquid Glass Effect
 * - App Font
 * - Custom Wallpaper URI
 */
class ThemeManager(private val context: Context) {
    companion object {
        // Keys for DataStore
        private val THEME_KEY = stringPreferencesKey("selected_theme")
        private val FONT_SIZE_KEY = stringPreferencesKey("font_size")
        private val FONT_WEIGHT_KEY = stringPreferencesKey("font_weight")
        private val ICON_SIZE_KEY = stringPreferencesKey("icon_size")
        private val DARK_TEXT_KEY = booleanPreferencesKey("dark_text_enabled")
        private val SHOW_FAVORITE_LABELS_KEY = booleanPreferencesKey("show_favorite_labels")
        private val LIQUID_GLASS_KEY = booleanPreferencesKey("liquid_glass_enabled")
        private val APP_FONT_KEY = stringPreferencesKey("app_font")
        private val CUSTOM_WALLPAPER_KEY = stringPreferencesKey("custom_wallpaper_uri")
    }

    /**
     * Observable flow for the currently selected color theme.
     * Defaults to ColorTheme.SIGNATURE if not set or invalid.
     */
    val selectedTheme: Flow<ColorTheme> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[THEME_KEY] ?: ColorTheme.SIGNATURE.name
            try {
                ColorTheme.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                ColorTheme.SIGNATURE
            }
        }

    /**
     * Observable flow for the selected font size.
     */
    val selectedFontSize: Flow<FontSize> = context.dataStore.data
        .map { preferences ->
            val fontSizeName = preferences[FONT_SIZE_KEY] ?: FontSize.STANDARD.name
            try {
                FontSize.valueOf(fontSizeName)
            } catch (e: IllegalArgumentException) {
                FontSize.STANDARD
            }
        }

    /**
     * Observable flow for the selected font weight.
     */
    val selectedFontWeight: Flow<FontWeightLevel> = context.dataStore.data
        .map { preferences ->
            val fontWeightName = preferences[FONT_WEIGHT_KEY] ?: FontWeightLevel.NORMAL.name
            try {
                FontWeightLevel.valueOf(fontWeightName)
            } catch (e: IllegalArgumentException) {
                FontWeightLevel.NORMAL
            }
        }

    /**
     * Observable flow for the selected icon size.
     */
    val selectedIconSize: Flow<IconSize> = context.dataStore.data
        .map { preferences ->
            val iconSizeName = preferences[ICON_SIZE_KEY] ?: IconSize.STANDARD.name
            try {
                IconSize.valueOf(iconSizeName)
            } catch (e: IllegalArgumentException) {
                IconSize.STANDARD
            }
        }

    /**
     * Observable flow for dark text toggle.
     */
    val isDarkTextEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DARK_TEXT_KEY] ?: false
        }

    /**
     * Observable flow for showing labels under favorite icons.
     */
    val showFavoriteLabels: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHOW_FAVORITE_LABELS_KEY] ?: false
        }

    /**
     * Observable flow for liquid glass visual effect.
     */
    val isLiquidGlassEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[LIQUID_GLASS_KEY] ?: true // Default to true as it is the current style
        }

    /**
     * Observable flow for the selected app font.
     */
    val selectedAppFont: Flow<AppFont> = context.dataStore.data
        .map { preferences ->
            val fontName = preferences[APP_FONT_KEY] ?: AppFont.SYSTEM_DEFAULT.name
            try {
                AppFont.valueOf(fontName)
            } catch (e: IllegalArgumentException) {
                AppFont.SYSTEM_DEFAULT
            }
        }

    /**
     * Observable flow for custom wallpaper URI.
     */
    val customWallpaperUri: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[CUSTOM_WALLPAPER_KEY]
        }

    /**
     * Updates the selected theme.
     */
    suspend fun setTheme(theme: ColorTheme) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }

    /**
     * Updates the selected font size.
     */
    suspend fun setFontSize(fontSize: FontSize) {
        context.dataStore.edit { preferences ->
            preferences[FONT_SIZE_KEY] = fontSize.name
        }
    }

    /**
     * Updates the selected font weight.
     */
    suspend fun setFontWeight(fontWeight: FontWeightLevel) {
        context.dataStore.edit { preferences ->
            preferences[FONT_WEIGHT_KEY] = fontWeight.name
        }
    }

    /**
     * Updates the selected icon size.
     */
    suspend fun setIconSize(iconSize: IconSize) {
        context.dataStore.edit { preferences ->
            preferences[ICON_SIZE_KEY] = iconSize.name
        }
    }

    /**
     * Toggles dark text mode.
     */
    suspend fun setDarkTextEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_TEXT_KEY] = enabled
        }
    }

    /**
     * Toggles visibility of favorite app labels.
     */
    suspend fun setShowFavoriteLabels(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_FAVORITE_LABELS_KEY] = show
        }
    }

    /**
     * Toggles liquid glass effect.
     */
    suspend fun setLiquidGlassEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LIQUID_GLASS_KEY] = enabled
        }
    }

    /**
     * Updates the selected app font.
     */
    suspend fun setAppFont(font: AppFont) {
        context.dataStore.edit { preferences ->
            preferences[APP_FONT_KEY] = font.name
        }
    }

    /**
     * Updates the custom wallpaper URI.
     */
    suspend fun setCustomWallpaperUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(CUSTOM_WALLPAPER_KEY)
            } else {
                preferences[CUSTOM_WALLPAPER_KEY] = uri
            }
        }
    }
}
