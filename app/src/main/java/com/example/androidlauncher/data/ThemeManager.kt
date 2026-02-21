package com.example.androidlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.androidlauncher.ui.theme.ColorTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class ThemeManager(private val context: Context) {
    companion object {
        private val THEME_KEY = stringPreferencesKey("selected_theme")
        private val FONT_SIZE_KEY = stringPreferencesKey("font_size")
        private val ICON_SIZE_KEY = stringPreferencesKey("icon_size")
        private val DARK_TEXT_KEY = booleanPreferencesKey("dark_text_enabled")
    }

    val selectedTheme: Flow<ColorTheme> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[THEME_KEY] ?: ColorTheme.LAUNCHER.name
            try {
                ColorTheme.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                ColorTheme.LAUNCHER
            }
        }

    val selectedFontSize: Flow<FontSize> = context.dataStore.data
        .map { preferences ->
            val fontSizeName = preferences[FONT_SIZE_KEY] ?: FontSize.STANDARD.name
            try {
                FontSize.valueOf(fontSizeName)
            } catch (e: IllegalArgumentException) {
                FontSize.STANDARD
            }
        }

    val selectedIconSize: Flow<IconSize> = context.dataStore.data
        .map { preferences ->
            val iconSizeName = preferences[ICON_SIZE_KEY] ?: IconSize.STANDARD.name
            try {
                IconSize.valueOf(iconSizeName)
            } catch (e: IllegalArgumentException) {
                IconSize.STANDARD
            }
        }

    val isDarkTextEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DARK_TEXT_KEY] ?: false
        }

    suspend fun setTheme(theme: ColorTheme) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }

    suspend fun setFontSize(fontSize: FontSize) {
        context.dataStore.edit { preferences ->
            preferences[FONT_SIZE_KEY] = fontSize.name
        }
    }

    suspend fun setIconSize(iconSize: IconSize) {
        context.dataStore.edit { preferences ->
            preferences[ICON_SIZE_KEY] = iconSize.name
        }
    }

    suspend fun setDarkTextEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_TEXT_KEY] = enabled
        }
    }
}
