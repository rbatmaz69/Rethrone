package com.example.androidlauncher.data

import android.content.Context
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
    }

    val selectedTheme: Flow<ColorTheme> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[THEME_KEY] ?: ColorTheme.DEFAULT.name
            try {
                ColorTheme.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                ColorTheme.DEFAULT
            }
        }

    suspend fun setTheme(theme: ColorTheme) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }
}
