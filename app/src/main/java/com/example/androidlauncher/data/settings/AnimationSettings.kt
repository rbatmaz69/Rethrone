package com.example.androidlauncher.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Animations-Einstellungen: globaler Schalter, Einzel-Toggles pro Animationsart
 * und der Tempo-Faktor. Aus dem ThemeManager extrahiert (A1-Split); nutzt
 * dieselbe DataStore-Datei wie zuvor, daher keine Datenmigration.
 */
class AnimationSettings(private val dataStore: DataStore<Preferences>) {

    /** Produktiv-Konstruktor: nutzt die geteilte "settings"-DataStore-Datei. */
    constructor(context: Context) : this(context.settingsDataStore)

    companion object {
        private val ANIMATIONS_ENABLED_KEY = booleanPreferencesKey("animations_enabled")
        private val ANIMATION_APP_OPEN_KEY = booleanPreferencesKey("animation_app_open")
        private val ANIMATION_APP_CLOSE_KEY = booleanPreferencesKey("animation_app_close")
        private val ANIMATION_MENUS_KEY = booleanPreferencesKey("animation_menus")
        private val ANIMATION_FAVORITES_KEY = booleanPreferencesKey("animation_favorites")
        private val ANIMATION_SPEED_KEY = floatPreferencesKey("animation_speed")

        /** Erlaubter Bereich des Tempo-Faktors. */
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 2f
    }

    /** Globaler Animations-Schalter. Default: an. */
    val isAnimationsEnabled: Flow<Boolean> = dataStore.data
        .map { it[ANIMATIONS_ENABLED_KEY] ?: true }

    /** App-Öffnen-Animation (Aufzieh-Effekt). Default: an. */
    val isAppOpenAnimationEnabled: Flow<Boolean> = dataStore.data
        .map { it[ANIMATION_APP_OPEN_KEY] ?: true }

    /** App-Schließen-/Rückkehr-Animation (Schrumpfen + Bounce). Default: an. */
    val isAppCloseAnimationEnabled: Flow<Boolean> = dataStore.data
        .map { it[ANIMATION_APP_CLOSE_KEY] ?: true }

    /** Menü-/Einstellungsmenü-Animationen. Default: an. */
    val isMenuAnimationEnabled: Flow<Boolean> = dataStore.data
        .map { it[ANIMATION_MENUS_KEY] ?: true }

    /** Favoriten-Leisten-Animation (Vergrößern beim Rüberfahren). Default: an. */
    val isFavoritesAnimationEnabled: Flow<Boolean> = dataStore.data
        .map { it[ANIMATION_FAVORITES_KEY] ?: true }

    /** Globaler Tempo-Faktor für Animationen ([MIN_SPEED]–[MAX_SPEED]). Default: 1×. */
    val animationSpeed: Flow<Float> = dataStore.data
        .map { (it[ANIMATION_SPEED_KEY] ?: 1f).coerceIn(MIN_SPEED, MAX_SPEED) }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        dataStore.edit { it[ANIMATIONS_ENABLED_KEY] = enabled }
    }

    suspend fun setAppOpenAnimationEnabled(enabled: Boolean) {
        dataStore.edit { it[ANIMATION_APP_OPEN_KEY] = enabled }
    }

    suspend fun setAppCloseAnimationEnabled(enabled: Boolean) {
        dataStore.edit { it[ANIMATION_APP_CLOSE_KEY] = enabled }
    }

    suspend fun setMenuAnimationEnabled(enabled: Boolean) {
        dataStore.edit { it[ANIMATION_MENUS_KEY] = enabled }
    }

    suspend fun setFavoritesAnimationEnabled(enabled: Boolean) {
        dataStore.edit { it[ANIMATION_FAVORITES_KEY] = enabled }
    }

    /** Setzt den Tempo-Faktor (auf [MIN_SPEED]–[MAX_SPEED] begrenzt). */
    suspend fun setAnimationSpeed(speed: Float) {
        dataStore.edit { it[ANIMATION_SPEED_KEY] = speed.coerceIn(MIN_SPEED, MAX_SPEED) }
    }
}
