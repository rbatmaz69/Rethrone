package com.example.androidlauncher.data.settings

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.androidlauncher.data.EdgeLightingStyle
import com.example.androidlauncher.data.IslandAnimationStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Einstellungen der Dynamic Island (Pille am oberen Rand) und des Edge Lightings
 * (leuchtender Rand bei Benachrichtigungen). Aus dem ThemeManager extrahiert
 * (A1-Split); nutzt dieselbe DataStore-Datei wie zuvor, daher keine Datenmigration.
 */
class IslandAndEdgeSettings(private val dataStore: DataStore<Preferences>) {

    /** Produktiv-Konstruktor: nutzt die geteilte "settings"-DataStore-Datei. */
    constructor(context: Context) : this(context.settingsDataStore)

    companion object {
        private val DYNAMIC_ISLAND_KEY = booleanPreferencesKey("dynamic_island_enabled")
        private val DYNAMIC_ISLAND_OFFSET_KEY = floatPreferencesKey("dynamic_island_offset")

        // Einmalige Migration: Mit der cutout-basierten Auto-Zentrierung wechselt die vertikale
        // Basis von statusBar/2 auf die echte Kamera-Mitte. Ein alter, gegen statusBar/2
        // kompensierter Offset würde sonst doppelt wirken → bis der Nutzer den Offset erstmals
        // neu setzt, alten Wert ignorieren (als 0 behandeln).
        private val DYNAMIC_ISLAND_OFFSET_MIGRATED_V2_KEY = booleanPreferencesKey("dynamic_island_offset_migrated_v2")

        // ARGB-Farbe der Dynamic Island (Pille + geöffnete Karte). Default: nahezu Schwarz.
        private val DYNAMIC_ISLAND_COLOR_KEY = intPreferencesKey("dynamic_island_color")

        private val EDGE_LIGHTING_KEY = booleanPreferencesKey("edge_lighting_enabled")
        private val EDGE_LIGHTING_COLOR_KEY = intPreferencesKey("edge_lighting_color")
        private val EDGE_LIGHTING_SPEED_KEY = floatPreferencesKey("edge_lighting_speed")
        private val EDGE_LIGHTING_LAPS_KEY = intPreferencesKey("edge_lighting_laps")
        private val EDGE_LIGHTING_THICKNESS_KEY = floatPreferencesKey("edge_lighting_thickness")
        private val EDGE_LIGHTING_STYLE_KEY = stringPreferencesKey("edge_lighting_style")
        private val ISLAND_ANIMATION_STYLE_KEY = stringPreferencesKey("island_animation_style")

        /** Erlaubter Bereich des vertikalen Insel-Feinversatzes in dp. */
        const val MIN_OFFSET_DP = -12f
        const val MAX_OFFSET_DP = 40f
    }

    /** Observable flow für die Dynamic Island (Pille am oberen Rand). Default: an. */
    val isDynamicIslandEnabled: Flow<Boolean> = dataStore.data
        .map { it[DYNAMIC_ISLAND_KEY] ?: true }

    /**
     * Manueller vertikaler Feinversatz der Dynamic Island in dp ([MIN_OFFSET_DP]..[MAX_OFFSET_DP]).
     * Default: 0. Seit der cutout-basierten Auto-Zentrierung nur noch optionale Feinjustierung.
     * Alte (gegen statusBar/2 kompensierte) Werte werden einmalig ignoriert, bis der Nutzer den
     * Offset über [setDynamicIslandOffset] erstmals selbst setzt (siehe MIGRATED_V2-Flag).
     */
    val dynamicIslandOffset: Flow<Float> = dataStore.data
        .map { prefs ->
            if (prefs[DYNAMIC_ISLAND_OFFSET_MIGRATED_V2_KEY] == true) {
                prefs[DYNAMIC_ISLAND_OFFSET_KEY] ?: 0f
            } else {
                0f
            }
        }

    /** Frei wählbare Farbe der Dynamic Island (Pille + geöffnete Karte). Default: nahezu Schwarz. */
    val dynamicIslandColor: Flow<Color> = dataStore.data
        .map { Color(it[DYNAMIC_ISLAND_COLOR_KEY] ?: 0xFF0B0B0C.toInt()) }

    /** Observable flow für das Edge Lighting (leuchtender Rand bei Benachrichtigungen). Default: aus. */
    val isEdgeLightingEnabled: Flow<Boolean> = dataStore.data
        .map { it[EDGE_LIGHTING_KEY] ?: false }

    /** Frei wählbare Farbe des Edge Lightings. Default: Akzent-Blau. */
    val edgeLightingColor: Flow<Color> = dataStore.data
        .map { Color(it[EDGE_LIGHTING_COLOR_KEY] ?: 0xFF0A84FF.toInt()) }

    /** Edge-Lighting-Tempo (höher = schneller). Default: 1.0. */
    val edgeLightingSpeed: Flow<Float> = dataStore.data
        .map { (it[EDGE_LIGHTING_SPEED_KEY] ?: 1f).coerceIn(0.5f, 2f) }

    /** Edge-Lighting-Durchläufe pro Benachrichtigung (1..5). Default: 1. */
    val edgeLightingLaps: Flow<Int> = dataStore.data
        .map { (it[EDGE_LIGHTING_LAPS_KEY] ?: 1).coerceIn(1, 5) }

    /** Edge-Lighting-Stärke (skaliert Strich-/Glow-Breite). Default: 1.0. */
    val edgeLightingThickness: Flow<Float> = dataStore.data
        .map { (it[EDGE_LIGHTING_THICKNESS_KEY] ?: 1f).coerceIn(0.5f, 2f) }

    /** Edge-Lighting-Stil. Default: SWEEP. */
    val edgeLightingStyle: Flow<EdgeLightingStyle> = dataStore.data
        .map { prefs ->
            prefs[EDGE_LIGHTING_STYLE_KEY]?.let {
                runCatching { EdgeLightingStyle.valueOf(it) }.getOrNull()
            } ?: EdgeLightingStyle.SWEEP
        }

    /** Insel-Öffnungs-/Schließstil. Default: FROM_NOTCH. */
    val islandAnimationStyle: Flow<IslandAnimationStyle> = dataStore.data
        .map { prefs ->
            prefs[ISLAND_ANIMATION_STYLE_KEY]?.let {
                runCatching { IslandAnimationStyle.valueOf(it) }.getOrNull()
            } ?: IslandAnimationStyle.FROM_NOTCH
        }

    /** Schaltet die Dynamic Island ein/aus. */
    suspend fun setDynamicIslandEnabled(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_ISLAND_KEY] = enabled }
    }

    /** Setzt den vertikalen Feinversatz der Dynamic Island (auf [MIN_OFFSET_DP]..[MAX_OFFSET_DP] begrenzt). */
    suspend fun setDynamicIslandOffset(offsetDp: Float) {
        dataStore.edit { preferences ->
            preferences[DYNAMIC_ISLAND_OFFSET_KEY] = offsetDp.coerceIn(MIN_OFFSET_DP, MAX_OFFSET_DP)
            // Erste bewusste Justierung schließt die Einmal-Migration ab → ab jetzt wird der
            // gespeicherte Wert wieder verwendet.
            preferences[DYNAMIC_ISLAND_OFFSET_MIGRATED_V2_KEY] = true
        }
    }

    /** Setzt die frei wählbare Farbe der Dynamic Island (Pille + Karte). */
    suspend fun setDynamicIslandColor(color: Color) {
        dataStore.edit { it[DYNAMIC_ISLAND_COLOR_KEY] = color.toArgb() }
    }

    /** Schaltet das Edge Lighting (leuchtender Rand bei Benachrichtigungen) ein/aus. */
    suspend fun setEdgeLightingEnabled(enabled: Boolean) {
        dataStore.edit { it[EDGE_LIGHTING_KEY] = enabled }
    }

    /** Setzt die frei wählbare Farbe des Edge Lightings. */
    suspend fun setEdgeLightingColor(color: Color) {
        dataStore.edit { it[EDGE_LIGHTING_COLOR_KEY] = color.toArgb() }
    }

    /** Setzt das Edge-Lighting-Tempo (0.5..2.0; höher = schneller). */
    suspend fun setEdgeLightingSpeed(speed: Float) {
        dataStore.edit { it[EDGE_LIGHTING_SPEED_KEY] = speed.coerceIn(0.5f, 2f) }
    }

    /** Setzt die Edge-Lighting-Durchläufe pro Benachrichtigung (1..5). */
    suspend fun setEdgeLightingLaps(laps: Int) {
        dataStore.edit { it[EDGE_LIGHTING_LAPS_KEY] = laps.coerceIn(1, 5) }
    }

    /** Setzt die Edge-Lighting-Stärke (0.5..2.0; skaliert Strich-/Glow-Breite). */
    suspend fun setEdgeLightingThickness(thickness: Float) {
        dataStore.edit { it[EDGE_LIGHTING_THICKNESS_KEY] = thickness.coerceIn(0.5f, 2f) }
    }

    /** Setzt den Edge-Lighting-Stil. */
    suspend fun setEdgeLightingStyle(style: EdgeLightingStyle) {
        dataStore.edit { it[EDGE_LIGHTING_STYLE_KEY] = style.name }
    }

    /** Setzt den Insel-Öffnungs-/Schließstil. */
    suspend fun setIslandAnimationStyle(style: IslandAnimationStyle) {
        dataStore.edit { it[ISLAND_ANIMATION_STYLE_KEY] = style.name }
    }
}
