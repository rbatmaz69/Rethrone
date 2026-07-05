package com.example.androidlauncher.data.settings

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.androidlauncher.data.FavoritesBorderStyle
import com.example.androidlauncher.data.HomeLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Startbildschirm-Einstellungen: Widget-Toggles (Wetter/Uhr/Kalender), die
 * Darstellung der Favoriten-Leiste (Labels, Notification-Dots, Umrandung), die
 * Positionen aller verschiebbaren Elemente sowie Onboarding-Status und smarte
 * Suchvorschläge. Aus dem ThemeManager extrahiert (A1-Split); nutzt dieselbe
 * DataStore-Datei wie zuvor, daher keine Datenmigration.
 */
class HomeLayoutSettings(private val dataStore: DataStore<Preferences>) {

    /** Produktiv-Konstruktor: nutzt die geteilte "settings"-DataStore-Datei. */
    constructor(context: Context) : this(context.settingsDataStore)

    companion object {
        private val SHOW_FAVORITE_LABELS_KEY = booleanPreferencesKey("show_favorite_labels")
        private val NOTIFICATION_DOTS_KEY = booleanPreferencesKey("notification_dots_enabled")
        private val FAVORITES_BORDER_KEY = stringPreferencesKey("favorites_border_style")
        private val SMART_SUGGESTIONS_KEY = booleanPreferencesKey("smart_search_enabled")

        // Widget-Toggles (Standard: an).
        private val WEATHER_WIDGET_KEY = booleanPreferencesKey("weather_widget_enabled")
        private val CLOCK_WIDGET_KEY = booleanPreferencesKey("clock_widget_enabled")
        private val CALENDAR_WIDGET_KEY = booleanPreferencesKey("calendar_widget_enabled")

        // Ob das Erststart-Onboarding bereits abgeschlossen wurde (Standard: false).
        private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")

        // Offset for UI elements (Relative to default position)
        // Uhr, Datum, Wetter und Favoriten sind unabhängig auf X/Y verschiebbar.
        // (clock/favorites nutzen die alten Keys weiter – kompatibel zu bereits gespeicherten Layouts.)
        private val FAVORITES_OFFSET_X_KEY = floatPreferencesKey("favorites_offset_x")
        private val FAVORITES_OFFSET_Y_KEY = floatPreferencesKey("favorites_offset_y")
        private val CLOCK_OFFSET_X_KEY = floatPreferencesKey("clock_offset_x")
        private val CLOCK_OFFSET_Y_KEY = floatPreferencesKey("clock_offset_y")
        private val DATE_OFFSET_X_KEY = floatPreferencesKey("date_offset_x")
        private val DATE_OFFSET_Y_KEY = floatPreferencesKey("date_offset_y")
        private val WEATHER_OFFSET_X_KEY = floatPreferencesKey("weather_offset_x")
        private val WEATHER_OFFSET_Y_KEY = floatPreferencesKey("weather_offset_y")
    }

    val showFavoriteLabels: Flow<Boolean> = dataStore.data
        .map { it[SHOW_FAVORITE_LABELS_KEY] ?: false }

    // Default true: Dots waren bisher fest aktiv, Bestandsnutzer behalten das Verhalten.
    val isNotificationDotsEnabled: Flow<Boolean> = dataStore.data
        .map { it[NOTIFICATION_DOTS_KEY] ?: true }

    /** Gewählte Umrandung der Favoriten-Box (Standard: keine). */
    val favoritesBorderStyle: Flow<FavoritesBorderStyle> = dataStore.data
        .map { FavoritesBorderStyle.fromKey(it[FAVORITES_BORDER_KEY]) }

    /** Observable flow for intelligent search suggestions. */
    val isSmartSuggestionsEnabled: Flow<Boolean> = dataStore.data
        .map { it[SMART_SUGGESTIONS_KEY] ?: true }

    /** Wetter-Widget (Symbol + Temperatur unter der Uhr). Default: an. */
    val isWeatherWidgetEnabled: Flow<Boolean> = dataStore.data
        .map { it[WEATHER_WIDGET_KEY] ?: true }

    /** Uhr-Widget. Default: an. */
    val isClockWidgetEnabled: Flow<Boolean> = dataStore.data
        .map { it[CLOCK_WIDGET_KEY] ?: true }

    /** Kalender-/Datum-Widget. Default: an. */
    val isCalendarWidgetEnabled: Flow<Boolean> = dataStore.data
        .map { it[CALENDAR_WIDGET_KEY] ?: true }

    /** Ob das Erststart-Onboarding bereits abgeschlossen wurde. Default: false. */
    val isOnboardingCompleted: Flow<Boolean> = dataStore.data
        .map { it[ONBOARDING_COMPLETED_KEY] ?: false }

    // Positionen aller unabhängig verschiebbaren Startbildschirm-Elemente.
    val homeLayout: Flow<HomeLayout> = dataStore.data
        .map { prefs ->
            HomeLayout(
                clock = Offset(prefs[CLOCK_OFFSET_X_KEY] ?: 0f, prefs[CLOCK_OFFSET_Y_KEY] ?: 0f),
                date = Offset(prefs[DATE_OFFSET_X_KEY] ?: 0f, prefs[DATE_OFFSET_Y_KEY] ?: 0f),
                weather = Offset(prefs[WEATHER_OFFSET_X_KEY] ?: 0f, prefs[WEATHER_OFFSET_Y_KEY] ?: 0f),
                favorites = Offset(prefs[FAVORITES_OFFSET_X_KEY] ?: 0f, prefs[FAVORITES_OFFSET_Y_KEY] ?: 0f),
            )
        }

    suspend fun setShowFavoriteLabels(show: Boolean) {
        dataStore.edit { it[SHOW_FAVORITE_LABELS_KEY] = show }
    }

    suspend fun setNotificationDotsEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATION_DOTS_KEY] = enabled }
    }

    suspend fun setFavoritesBorderStyle(style: FavoritesBorderStyle) {
        dataStore.edit { it[FAVORITES_BORDER_KEY] = style.name }
    }

    suspend fun setSmartSuggestionsEnabled(enabled: Boolean) {
        dataStore.edit { it[SMART_SUGGESTIONS_KEY] = enabled }
    }

    suspend fun setWeatherWidgetEnabled(enabled: Boolean) {
        dataStore.edit { it[WEATHER_WIDGET_KEY] = enabled }
    }

    suspend fun setClockWidgetEnabled(enabled: Boolean) {
        dataStore.edit { it[CLOCK_WIDGET_KEY] = enabled }
    }

    suspend fun setCalendarWidgetEnabled(enabled: Boolean) {
        dataStore.edit { it[CALENDAR_WIDGET_KEY] = enabled }
    }

    /** Markiert das Erststart-Onboarding als abgeschlossen (oder setzt es zurück). */
    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETED_KEY] = completed }
    }

    // Persistiert die Positionen aller verschiebbaren Startbildschirm-Elemente.
    suspend fun setHomeLayout(layout: HomeLayout) {
        dataStore.edit {
            it[CLOCK_OFFSET_X_KEY] = layout.clock.x
            it[CLOCK_OFFSET_Y_KEY] = layout.clock.y
            it[DATE_OFFSET_X_KEY] = layout.date.x
            it[DATE_OFFSET_Y_KEY] = layout.date.y
            it[WEATHER_OFFSET_X_KEY] = layout.weather.x
            it[WEATHER_OFFSET_Y_KEY] = layout.weather.y
            it[FAVORITES_OFFSET_X_KEY] = layout.favorites.x
            it[FAVORITES_OFFSET_Y_KEY] = layout.favorites.y
        }
    }
}
