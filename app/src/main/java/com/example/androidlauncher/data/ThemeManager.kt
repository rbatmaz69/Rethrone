package com.example.androidlauncher.data

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.androidlauncher.data.settings.AnimationSettings
import com.example.androidlauncher.data.settings.GestureSettings
import com.example.androidlauncher.data.settings.WallpaperSettings
import com.example.androidlauncher.data.settings.settingsDataStore
import com.example.androidlauncher.ui.theme.ColorTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Geteiltes DataStore-Delegate (Datei "settings") – siehe data/settings/SettingsDataStore.kt.
// Lokaler Alias, damit die bestehenden Zugriffe im ThemeManager unverändert bleiben.
private val Context.dataStore: DataStore<Preferences> get() = settingsDataStore

/**
 * Manages application themes and visual settings using DataStore and System Settings.
 *
 * Wird schrittweise in domänen-spezifische Stores unter `data/settings/` zerlegt
 * (A1-Split); für bereits extrahierte Gruppen (Wallpaper, Gesten, Animationen)
 * delegiert diese Fassade nur noch, damit die Aufrufstellen unverändert bleiben.
 */
class ThemeManager(private val context: Context) {

    // A1-Split: extrahierte Stores; teilen sich die DataStore-Datei mit dieser Fassade.
    private val wallpaperSettings = WallpaperSettings(context)
    private val gestureSettings = GestureSettings(context)
    private val animationSettings = AnimationSettings(context)
    companion object {
        private val THEME_KEY = stringPreferencesKey("selected_theme")

        // Neue stufenlose numerische Keys.
        private val FONT_SIZE_SCALE_KEY = floatPreferencesKey("font_size_scale")
        private val FONT_WEIGHT_VALUE_KEY = intPreferencesKey("font_weight_value")
        private val ICON_SIZE_DP_KEY = floatPreferencesKey("icon_size_dp")
        private val FAVORITE_SPACING_DP_KEY = floatPreferencesKey("favorite_spacing_dp")

        // Alte String-Keys (Enum-Namen) – nur noch zur einmaligen Migration gelesen.
        private val LEGACY_FONT_SIZE_KEY = stringPreferencesKey("font_size")
        private val LEGACY_FONT_WEIGHT_KEY = stringPreferencesKey("font_weight")
        private val LEGACY_ICON_SIZE_KEY = stringPreferencesKey("icon_size")
        private val LEGACY_FAVORITE_SPACING_KEY = stringPreferencesKey("favorite_icon_spacing")
        private val DARK_TEXT_KEY = booleanPreferencesKey("dark_text_enabled")

        // ARGB-Werte für frei wählbare Icon-/Schriftfarbe (Default Weiß).
        private val ICON_COLOR_KEY = intPreferencesKey("icon_color")
        private val HOME_TEXT_COLOR_KEY = intPreferencesKey("home_text_color")

        // ARGB-Flächenfarben für das CUSTOM-Theme ("Eigene Farbe").
        private val CUSTOM_BG_COLOR_KEY = intPreferencesKey("custom_bg_color")
        private val CUSTOM_MENU_COLOR_KEY = intPreferencesKey("custom_menu_color")

        // Komma-separierte Paketnamen der ausgeblendeten Apps.
        private val HIDDEN_APPS_KEY = stringPreferencesKey("hidden_apps")

        // App-Sperre: Komma-separierte Paketnamen der gesperrten Apps (verschlüsselt).
        private val LOCKED_APPS_KEY = stringPreferencesKey("locked_apps")

        // Typ des Sperr-Geheimnisses: "pin", "pattern" oder "none".
        private val LOCK_TYPE_KEY = stringPreferencesKey("lock_type")

        // Gesalzener Hash des PIN/Musters (Format salt:hash, verschlüsselt). Nie das Geheimnis selbst.
        private val LOCK_SECRET_KEY = stringPreferencesKey("lock_secret")

        // Ob zusätzlich per Biometrie/Geräte-Credential entsperrt werden darf.
        private val LOCK_BIOMETRIC_KEY = booleanPreferencesKey("lock_biometric_enabled")
        private val SHOW_FAVORITE_LABELS_KEY = booleanPreferencesKey("show_favorite_labels")
        private val NOTIFICATION_DOTS_KEY = booleanPreferencesKey("notification_dots_enabled")
        private val LIQUID_GLASS_KEY = booleanPreferencesKey("liquid_glass_enabled")
        private val DESIGN_STYLE_KEY = stringPreferencesKey("design_style")
        private val FAVORITES_BORDER_KEY = stringPreferencesKey("favorites_border_style")
        private val APP_FONT_KEY = stringPreferencesKey("app_font")

        // Wallpaper-, Gesten- und Animations-Keys liegen jetzt in den extrahierten
        // Stores unter data/settings/ (A1-Split, gleiche DataStore-Datei).
        private val SMART_SUGGESTIONS_KEY = booleanPreferencesKey("smart_search_enabled")
        private val APP_ACCESS_MODE_KEY = stringPreferencesKey("app_access_mode")

        // Wetter-Widget unter Uhr/Datum (Standard: an).
        private val WEATHER_WIDGET_KEY = booleanPreferencesKey("weather_widget_enabled")

        // Uhr-Widget (Standard: an).
        private val CLOCK_WIDGET_KEY = booleanPreferencesKey("clock_widget_enabled")

        // Kalender-/Datum-Widget (Standard: an).
        private val CALENDAR_WIDGET_KEY = booleanPreferencesKey("calendar_widget_enabled")
        private val DYNAMIC_ISLAND_KEY = booleanPreferencesKey("dynamic_island_enabled")
        private val DYNAMIC_ISLAND_OFFSET_KEY = floatPreferencesKey("dynamic_island_offset")

        // Einmalige Migration: Mit der cutout-basierten Auto-Zentrierung wechselt die vertikale
        // Basis von statusBar/2 auf die echte Kamera-Mitte. Ein alter, gegen statusBar/2
        // kompensierter Offset würde sonst doppelt wirken → bis der Nutzer den Offset erstmals
        // neu setzt, alten Wert ignorieren (als 0 behandeln).
        private val DYNAMIC_ISLAND_OFFSET_MIGRATED_V2_KEY = booleanPreferencesKey("dynamic_island_offset_migrated_v2")

        // ARGB-Farbe der Dynamic Island (Pille + geöffnete Karte). Default: nahezu Schwarz.
        private val DYNAMIC_ISLAND_COLOR_KEY = intPreferencesKey("dynamic_island_color")

        // Edge Lighting (leuchtender Rand bei Benachrichtigungen). Default: aus.
        private val EDGE_LIGHTING_KEY = booleanPreferencesKey("edge_lighting_enabled")

        // ARGB-Farbe des Edge Lightings. Default: Akzent-Blau.
        private val EDGE_LIGHTING_COLOR_KEY = intPreferencesKey("edge_lighting_color")

        // Edge-Lighting-Tempo (höher = schneller). Default: 1.0.
        private val EDGE_LIGHTING_SPEED_KEY = floatPreferencesKey("edge_lighting_speed")

        // Edge-Lighting-Durchläufe pro Benachrichtigung (1..5). Default: 1.
        private val EDGE_LIGHTING_LAPS_KEY = intPreferencesKey("edge_lighting_laps")

        // Edge-Lighting-Stärke (skaliert Strich-/Glow-Breite). Default: 1.0.
        private val EDGE_LIGHTING_THICKNESS_KEY = floatPreferencesKey("edge_lighting_thickness")

        // Edge-Lighting-Stil (EdgeLightingStyle-Name). Default: SWEEP.
        private val EDGE_LIGHTING_STYLE_KEY = stringPreferencesKey("edge_lighting_style")

        // Insel-Öffnungs-/Schließstil (IslandAnimationStyle-Name). Default: FROM_NOTCH.
        private val ISLAND_ANIMATION_STYLE_KEY = stringPreferencesKey("island_animation_style")

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

    val selectedTheme: Flow<ColorTheme> = context.dataStore.data
        .map { preferences ->
            // Standard: warmes Theme "Tulpe" (Amber-Verlauf + schwarze Tulpen-Silhouette) für Neuinstallationen.
            val themeName = preferences[THEME_KEY] ?: ColorTheme.SOFT_SAND.name
            try { ColorTheme.valueOf(themeName) } catch (e: IllegalArgumentException) { ColorTheme.SOFT_SAND }
        }

    // ── Sanfte Migration alter Enum-Namen (String) auf die neuen numerischen Werte ──
    private fun legacyFontSizeScale(name: String?): Float? = when (name) {
        "SMALL" -> 0.85f
        "STANDARD" -> 1.0f
        "LARGE" -> 1.2f
        else -> null
    }
    private fun legacyFontWeightValue(name: String?): Int? = when (name) {
        "LIGHT" -> 300
        "NORMAL" -> 400
        "BOLD" -> 700
        else -> null
    }
    private fun legacyIconSizeDp(name: String?): Float? = when (name) {
        "SMALL" -> 40f
        "STANDARD" -> 48f
        "LARGE" -> 56f
        else -> null
    }
    private fun legacyFavoriteSpacingDp(name: String?): Float? = when (name) {
        "ENG" -> 4f
        "KOMPAKT" -> 8f
        "STANDARD" -> 12f
        "LOCKER" -> 20f
        "WEIT" -> 28f
        else -> null
    }

    val selectedFontSize: Flow<FontSize> = context.dataStore.data
        .map { preferences ->
            val scale = preferences[FONT_SIZE_SCALE_KEY]
                ?: legacyFontSizeScale(preferences[LEGACY_FONT_SIZE_KEY])
                ?: FontSize.STANDARD.scale
            FontSize.of(scale)
        }

    val selectedFontWeight: Flow<FontWeightLevel> = context.dataStore.data
        .map { preferences ->
            val value = preferences[FONT_WEIGHT_VALUE_KEY]
                ?: legacyFontWeightValue(preferences[LEGACY_FONT_WEIGHT_KEY])
                ?: FontWeightLevel.NORMAL.weightValue
            FontWeightLevel.of(value)
        }

    val selectedIconSize: Flow<IconSize> = context.dataStore.data
        .map { preferences ->
            val sizeDp = preferences[ICON_SIZE_DP_KEY]
                ?: legacyIconSizeDp(preferences[LEGACY_ICON_SIZE_KEY])
                ?: IconSize.STANDARD.size.value
            IconSize.of(sizeDp.dp)
        }

    val selectedFavoriteSpacing: Flow<FavoriteSpacing> = context.dataStore.data
        .map { preferences ->
            val spacingDp = preferences[FAVORITE_SPACING_DP_KEY]
                ?: legacyFavoriteSpacingDp(preferences[LEGACY_FAVORITE_SPACING_KEY])
                ?: FavoriteSpacing.STANDARD.spacing.value
            FavoriteSpacing.of(spacingDp.dp)
        }

    // Standard: dunkler Text – passend zum hellen Default-Theme "Tagespapier".
    val isDarkTextEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[DARK_TEXT_KEY] ?: true }

    // Frei wählbare Iconfarbe (gilt überall). Default: warmes Karamell-Braun,
    // passend zum sonnigen Standard-Theme "Tulpe" (Lucide-Fallback-Glyphen lesbar).
    val iconColor: Flow<Color> = context.dataStore.data
        .map { Color(it[ICON_COLOR_KEY] ?: 0xFF513A14.toInt()) }

    // Frei wählbare Schriftfarbe – nur Startbildschirm. Default: warmes Karamell-Braun,
    // damit Uhr/Datum über dem hellen oberen Bereich des Tulpe-Verlaufs gut lesbar sind.
    val homeTextColor: Flow<Color> = context.dataStore.data
        .map { Color(it[HOME_TEXT_COLOR_KEY] ?: 0xFF513A14.toInt()) }

    // CUSTOM-Theme: frei wählbare Flächenfarben (Default: helles Warmweiß).
    val customBackgroundColor: Flow<Color> = context.dataStore.data
        .map { Color(it[CUSTOM_BG_COLOR_KEY] ?: 0xFFF4EEE2.toInt()) }

    val customMenuColor: Flow<Color> = context.dataStore.data
        .map { Color(it[CUSTOM_MENU_COLOR_KEY] ?: 0xFFFFFFFF.toInt()) }

    // Ausgeblendete Apps (Paketnamen). Werden überall aus der Anzeige gefiltert.
    val hiddenApps: Flow<Set<String>> = context.dataStore.data
        .map { prefs ->
            // Wert ist verschlüsselt abgelegt; decryptOrLegacy migriert alten Klartext transparent.
            val raw = CryptoManager.decryptOrLegacy(prefs[HIDDEN_APPS_KEY])
            if (raw.isEmpty()) emptySet() else raw.split(",").filter { it.isNotEmpty() }.toSet()
        }

    // Gesperrte Apps (Paketnamen). Vor dem Öffnen muss authentifiziert werden.
    val lockedApps: Flow<Set<String>> = context.dataStore.data
        .map { prefs ->
            val raw = CryptoManager.decryptOrLegacy(prefs[LOCKED_APPS_KEY])
            if (raw.isEmpty()) emptySet() else raw.split(",").filter { it.isNotEmpty() }.toSet()
        }

    // Art des hinterlegten Sperr-Geheimnisses ("pin", "pattern", "none").
    val lockType: Flow<String> = context.dataStore.data
        .map { it[LOCK_TYPE_KEY] ?: "none" }

    // Roh-Token des gespeicherten Geheimnisses (salt:hash, entschlüsselt). Leer = kein Code gesetzt.
    val lockSecret: Flow<String> = context.dataStore.data
        .map { CryptoManager.decryptOrLegacy(it[LOCK_SECRET_KEY]) }

    val isLockBiometricEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[LOCK_BIOMETRIC_KEY] ?: false }

    val showFavoriteLabels: Flow<Boolean> = context.dataStore.data
        .map { it[SHOW_FAVORITE_LABELS_KEY] ?: false }

    // Default true: Dots waren bisher fest aktiv, Bestandsnutzer behalten das Verhalten.
    val isNotificationDotsEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[NOTIFICATION_DOTS_KEY] ?: true }

    val isLiquidGlassEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[LIQUID_GLASS_KEY] ?: true }

    /**
     * Gewählter Oberflächen-Stil. Migriert automatisch aus dem alten
     * `liquid_glass_enabled`-Boolean, falls noch kein `design_style` gesetzt ist
     * (true → GLASS, false → FLAT).
     */
    val designStyle: Flow<DesignStyle> = context.dataStore.data
        .map { preferences ->
            val stored = preferences[DESIGN_STYLE_KEY]
            if (stored != null) {
                DesignStyle.fromKey(stored)
            } else {
                // Neuinstallation → "Standard" (FLAT); Legacy-Glas-Nutzer behalten GLASS.
                when (preferences[LIQUID_GLASS_KEY]) {
                    true -> DesignStyle.GLASS
                    else -> DesignStyle.FLAT
                }
            }
        }

    /** Gewählte Umrandung der Favoriten-Box (Standard: keine). */
    val favoritesBorderStyle: Flow<FavoritesBorderStyle> = context.dataStore.data
        .map { FavoritesBorderStyle.fromKey(it[FAVORITES_BORDER_KEY]) }

    val selectedAppFont: Flow<AppFont> = context.dataStore.data
        .map { preferences ->
            val fontName = preferences[APP_FONT_KEY] ?: AppFont.SYSTEM_DEFAULT.name
            try { AppFont.valueOf(fontName) } catch (e: IllegalArgumentException) { AppFont.SYSTEM_DEFAULT }
        }

    // A1-Split: Wallpaper-Einstellungen liegen im WallpaperSettings-Store.
    val customWallpaperUri: Flow<String?> = wallpaperSettings.customWallpaperUri
    val wallpaperBlur: Flow<Float> = wallpaperSettings.wallpaperBlur
    val wallpaperDim: Flow<Float> = wallpaperSettings.wallpaperDim
    val wallpaperZoom: Flow<Float> = wallpaperSettings.wallpaperZoom

    // Positionen aller unabhängig verschiebbaren Startbildschirm-Elemente.
    val homeLayout: Flow<HomeLayout> = context.dataStore.data
        .map { prefs ->
            HomeLayout(
                clock = Offset(prefs[CLOCK_OFFSET_X_KEY] ?: 0f, prefs[CLOCK_OFFSET_Y_KEY] ?: 0f),
                date = Offset(prefs[DATE_OFFSET_X_KEY] ?: 0f, prefs[DATE_OFFSET_Y_KEY] ?: 0f),
                weather = Offset(prefs[WEATHER_OFFSET_X_KEY] ?: 0f, prefs[WEATHER_OFFSET_Y_KEY] ?: 0f),
                favorites = Offset(prefs[FAVORITES_OFFSET_X_KEY] ?: 0f, prefs[FAVORITES_OFFSET_Y_KEY] ?: 0f),
            )
        }

    /**
     * Observable flow, ob das Erststart-Onboarding bereits abgeschlossen wurde. Default: false.
     */
    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { it[ONBOARDING_COMPLETED_KEY] ?: false }

    // A1-Split: Animations-Einstellungen liegen im AnimationSettings-Store.
    val isAnimationsEnabled: Flow<Boolean> = animationSettings.isAnimationsEnabled
    val isAppOpenAnimationEnabled: Flow<Boolean> = animationSettings.isAppOpenAnimationEnabled
    val isAppCloseAnimationEnabled: Flow<Boolean> = animationSettings.isAppCloseAnimationEnabled
    val isMenuAnimationEnabled: Flow<Boolean> = animationSettings.isMenuAnimationEnabled
    val isFavoritesAnimationEnabled: Flow<Boolean> = animationSettings.isFavoritesAnimationEnabled
    val animationSpeed: Flow<Float> = animationSettings.animationSpeed

    /**
     * Observable flow für das Wetter-Widget (Symbol + Temperatur unter der Uhr). Default: an.
     */
    val isWeatherWidgetEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[WEATHER_WIDGET_KEY] ?: true }

    /**
     * Observable flow für das Uhr-Widget. Default: an.
     */
    val isClockWidgetEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[CLOCK_WIDGET_KEY] ?: true }

    /**
     * Observable flow für das Kalender-/Datum-Widget. Default: an.
     */
    val isCalendarWidgetEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[CALENDAR_WIDGET_KEY] ?: true }

    /**
     * Observable flow für die Dynamic Island (Pille am oberen Rand). Default: an.
     */
    val isDynamicIslandEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[DYNAMIC_ISLAND_KEY] ?: true }

    /**
     * Manueller vertikaler Feinversatz der Dynamic Island in dp (−12..40). Default: 0.
     * Seit der cutout-basierten Auto-Zentrierung nur noch optionale Feinjustierung. Alte
     * (gegen statusBar/2 kompensierte) Werte werden einmalig ignoriert, bis der Nutzer den
     * Offset über [setDynamicIslandOffset] erstmals selbst setzt (siehe MIGRATED_V2-Flag).
     */
    val dynamicIslandOffset: Flow<Float> = context.dataStore.data
        .map { prefs ->
            if (prefs[DYNAMIC_ISLAND_OFFSET_MIGRATED_V2_KEY] == true) {
                prefs[DYNAMIC_ISLAND_OFFSET_KEY] ?: 0f
            } else {
                0f
            }
        }

    /**
     * Frei wählbare Farbe der Dynamic Island (Pille + geöffnete Karte). Default: nahezu Schwarz.
     */
    val dynamicIslandColor: Flow<Color> = context.dataStore.data
        .map { Color(it[DYNAMIC_ISLAND_COLOR_KEY] ?: 0xFF0B0B0C.toInt()) }

    /**
     * Observable flow für das Edge Lighting (leuchtender Rand bei Benachrichtigungen). Default: aus.
     */
    val isEdgeLightingEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[EDGE_LIGHTING_KEY] ?: false }

    /**
     * Frei wählbare Farbe des Edge Lightings. Default: Akzent-Blau.
     */
    val edgeLightingColor: Flow<Color> = context.dataStore.data
        .map { Color(it[EDGE_LIGHTING_COLOR_KEY] ?: 0xFF0A84FF.toInt()) }

    /** Edge-Lighting-Tempo (höher = schneller). Default: 1.0. */
    val edgeLightingSpeed: Flow<Float> = context.dataStore.data
        .map { (it[EDGE_LIGHTING_SPEED_KEY] ?: 1f).coerceIn(0.5f, 2f) }

    /** Edge-Lighting-Durchläufe pro Benachrichtigung (1..5). Default: 1. */
    val edgeLightingLaps: Flow<Int> = context.dataStore.data
        .map { (it[EDGE_LIGHTING_LAPS_KEY] ?: 1).coerceIn(1, 5) }

    /** Edge-Lighting-Stärke (skaliert Strich-/Glow-Breite). Default: 1.0. */
    val edgeLightingThickness: Flow<Float> = context.dataStore.data
        .map { (it[EDGE_LIGHTING_THICKNESS_KEY] ?: 1f).coerceIn(0.5f, 2f) }

    /** Edge-Lighting-Stil. Default: SWEEP. */
    val edgeLightingStyle: Flow<EdgeLightingStyle> = context.dataStore.data
        .map { prefs ->
            prefs[EDGE_LIGHTING_STYLE_KEY]?.let {
                runCatching { EdgeLightingStyle.valueOf(it) }.getOrNull()
            } ?: EdgeLightingStyle.SWEEP
        }

    /** Insel-Öffnungs-/Schließstil. Default: FROM_NOTCH. */
    val islandAnimationStyle: Flow<IslandAnimationStyle> = context.dataStore.data
        .map { prefs ->
            prefs[ISLAND_ANIMATION_STYLE_KEY]?.let {
                runCatching { IslandAnimationStyle.valueOf(it) }.getOrNull()
            } ?: IslandAnimationStyle.FROM_NOTCH
        }

    // A1-Split: Gesten-Einstellungen liegen im GestureSettings-Store.
    val isShakeGesturesEnabled: Flow<Boolean> = gestureSettings.isShakeGesturesEnabled
    val doubleShakeAction: Flow<GestureAction> = gestureSettings.doubleShakeAction
    val shakeOpenAppPackage: Flow<String?> = gestureSettings.shakeOpenAppPackage
    val doubleTapAction: Flow<GestureAction> = gestureSettings.doubleTapAction
    val doubleTapAppPackage: Flow<String?> = gestureSettings.doubleTapAppPackage

    /**
     * Gewählte Art des App-Zugriffs. Default ist die Drawer-Liste (Niagara-Stil).
     */
    val appAccessMode: Flow<AppAccessMode> = context.dataStore.data
        .map { preferences ->
            val name = preferences[APP_ACCESS_MODE_KEY] ?: AppAccessMode.DRAWER_LIST.name
            try { AppAccessMode.valueOf(name) } catch (e: IllegalArgumentException) { AppAccessMode.DRAWER_LIST }
        }

    /**
     * Observable flow for intelligent search suggestions.
     */
    val isSmartSuggestionsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SMART_SUGGESTIONS_KEY] ?: true
        }

    /**
     * Observable flow for haptic feedback toggle (mit System-Setting synchronisiert).
     * A1-Split: Implementierung liegt im GestureSettings-Store.
     */
    val isHapticFeedbackEnabled: Flow<Boolean> = gestureSettings.isHapticFeedbackEnabled

    suspend fun setTheme(theme: ColorTheme) { context.dataStore.edit { it[THEME_KEY] = theme.name } }
    suspend fun setFontSize(scale: Float) {
        context.dataStore.edit { it[FONT_SIZE_SCALE_KEY] = scale.coerceIn(FontSize.MIN, FontSize.MAX) }
    }
    suspend fun setFontWeight(value: Int) {
        context.dataStore.edit { it[FONT_WEIGHT_VALUE_KEY] = value.coerceIn(FontWeightLevel.MIN, FontWeightLevel.MAX) }
    }
    suspend fun setIconSize(size: Dp) {
        context.dataStore.edit { it[ICON_SIZE_DP_KEY] = size.coerceIn(IconSize.MIN, IconSize.MAX).value }
    }
    suspend fun setFavoriteSpacing(spacing: Dp) {
        context.dataStore.edit {
            it[FAVORITE_SPACING_DP_KEY] = spacing.coerceIn(FavoriteSpacing.MIN, FavoriteSpacing.MAX).value
        }
    }
    suspend fun setDarkTextEnabled(enabled: Boolean) { context.dataStore.edit { it[DARK_TEXT_KEY] = enabled } }
    suspend fun setIconColor(color: Color) { context.dataStore.edit { it[ICON_COLOR_KEY] = color.toArgb() } }
    suspend fun setHomeTextColor(color: Color) { context.dataStore.edit { it[HOME_TEXT_COLOR_KEY] = color.toArgb() } }
    suspend fun setCustomBackgroundColor(color: Color) {
        context.dataStore.edit { it[CUSTOM_BG_COLOR_KEY] = color.toArgb() }
    }
    suspend fun setCustomMenuColor(
        color: Color
    ) { context.dataStore.edit { it[CUSTOM_MENU_COLOR_KEY] = color.toArgb() } }
    suspend fun setHiddenApps(packages: Set<String>) {
        context.dataStore.edit { it[HIDDEN_APPS_KEY] = CryptoManager.encrypt(packages.joinToString(",")) }
    }
    suspend fun setLockedApps(packages: Set<String>) {
        context.dataStore.edit { it[LOCKED_APPS_KEY] = CryptoManager.encrypt(packages.joinToString(",")) }
    }

    /** Speichert Typ ("pin"/"pattern") und gesalzenen Hash-Token des Geheimnisses. */
    suspend fun setLockSecret(type: String, secretToken: String) {
        context.dataStore.edit {
            it[LOCK_TYPE_KEY] = type
            it[LOCK_SECRET_KEY] = CryptoManager.encrypt(secretToken)
        }
    }

    /** Entfernt den hinterlegten Code (Typ zurück auf "none"). */
    suspend fun clearLockSecret() {
        context.dataStore.edit {
            it[LOCK_TYPE_KEY] = "none"
            it.remove(LOCK_SECRET_KEY)
        }
    }
    suspend fun setLockBiometricEnabled(
        enabled: Boolean
    ) { context.dataStore.edit { it[LOCK_BIOMETRIC_KEY] = enabled } }
    suspend fun setShowFavoriteLabels(show: Boolean) { context.dataStore.edit { it[SHOW_FAVORITE_LABELS_KEY] = show } }
    suspend fun setNotificationDotsEnabled(
        enabled: Boolean
    ) { context.dataStore.edit { it[NOTIFICATION_DOTS_KEY] = enabled } }
    suspend fun setLiquidGlassEnabled(enabled: Boolean) { context.dataStore.edit { it[LIQUID_GLASS_KEY] = enabled } }
    suspend fun setDesignStyle(style: DesignStyle) {
        context.dataStore.edit {
            it[DESIGN_STYLE_KEY] = style.name
            // Legacy-Boolean konsistent halten, falls anderer Code es noch liest.
            it[LIQUID_GLASS_KEY] = style.isGlassLike
        }
    }
    suspend fun setFavoritesBorderStyle(style: FavoritesBorderStyle) {
        context.dataStore.edit { it[FAVORITES_BORDER_KEY] = style.name }
    }
    suspend fun setAppFont(font: AppFont) { context.dataStore.edit { it[APP_FONT_KEY] = font.name } }
    suspend fun setCustomWallpaperUri(uri: String?) = wallpaperSettings.setCustomWallpaperUri(uri)
    suspend fun setWallpaperBlur(blur: Float) = wallpaperSettings.setWallpaperBlur(blur)
    suspend fun setWallpaperDim(dim: Float) = wallpaperSettings.setWallpaperDim(dim)
    suspend fun setWallpaperZoom(zoom: Float) = wallpaperSettings.setWallpaperZoom(zoom)

    // Persistiert die Positionen aller verschiebbaren Startbildschirm-Elemente.
    suspend fun setHomeLayout(layout: HomeLayout) {
        context.dataStore.edit {
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

    /**
     * Toggles haptic feedback both internally and in system settings if permission is granted.
     */
    suspend fun setHapticFeedbackEnabled(enabled: Boolean) = gestureSettings.setHapticFeedbackEnabled(enabled)

    /**
     * Markiert das Erststart-Onboarding als abgeschlossen (oder setzt es zurück).
     */
    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = completed
        }
    }

    // A1-Split: Gesten-Setter delegieren an den GestureSettings-Store.
    suspend fun setShakeGesturesEnabled(enabled: Boolean) = gestureSettings.setShakeGesturesEnabled(enabled)
    suspend fun setDoubleShakeAction(action: GestureAction) = gestureSettings.setDoubleShakeAction(action)
    suspend fun setShakeOpenAppPackage(packageName: String?) = gestureSettings.setShakeOpenAppPackage(packageName)
    suspend fun setDoubleTapAction(action: GestureAction) = gestureSettings.setDoubleTapAction(action)
    suspend fun setDoubleTapAppPackage(packageName: String?) = gestureSettings.setDoubleTapAppPackage(packageName)

    /**
     * Setzt die gewählte Art des App-Zugriffs.
     */
    suspend fun setAppAccessMode(mode: AppAccessMode) {
        context.dataStore.edit { preferences ->
            preferences[APP_ACCESS_MODE_KEY] = mode.name
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

    // A1-Split: Animations-Setter delegieren an den AnimationSettings-Store.
    suspend fun setAnimationsEnabled(enabled: Boolean) = animationSettings.setAnimationsEnabled(enabled)
    suspend fun setAppOpenAnimationEnabled(enabled: Boolean) = animationSettings.setAppOpenAnimationEnabled(enabled)
    suspend fun setAppCloseAnimationEnabled(enabled: Boolean) = animationSettings.setAppCloseAnimationEnabled(enabled)
    suspend fun setMenuAnimationEnabled(enabled: Boolean) = animationSettings.setMenuAnimationEnabled(enabled)
    suspend fun setFavoritesAnimationEnabled(enabled: Boolean) = animationSettings.setFavoritesAnimationEnabled(enabled)
    suspend fun setAnimationSpeed(speed: Float) = animationSettings.setAnimationSpeed(speed)

    /**
     * Schaltet das Wetter-Widget ein/aus.
     */
    suspend fun setWeatherWidgetEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WEATHER_WIDGET_KEY] = enabled
        }
    }

    /**
     * Schaltet das Uhr-Widget ein/aus.
     */
    suspend fun setClockWidgetEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CLOCK_WIDGET_KEY] = enabled
        }
    }

    /**
     * Schaltet das Kalender-/Datum-Widget ein/aus.
     */
    suspend fun setCalendarWidgetEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CALENDAR_WIDGET_KEY] = enabled
        }
    }

    /**
     * Schaltet die Dynamic Island ein/aus.
     */
    suspend fun setDynamicIslandEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_ISLAND_KEY] = enabled
        }
    }

    /**
     * Setzt den vertikalen Feinversatz der Dynamic Island (auf −16..16 dp begrenzt).
     */
    suspend fun setDynamicIslandOffset(offsetDp: Float) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_ISLAND_OFFSET_KEY] = offsetDp.coerceIn(-12f, 40f)
            // Erste bewusste Justierung schließt die Einmal-Migration ab → ab jetzt wird der
            // gespeicherte Wert wieder verwendet.
            preferences[DYNAMIC_ISLAND_OFFSET_MIGRATED_V2_KEY] = true
        }
    }

    /**
     * Setzt die frei wählbare Farbe der Dynamic Island (Pille + Karte).
     */
    suspend fun setDynamicIslandColor(color: Color) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_ISLAND_COLOR_KEY] = color.toArgb()
        }
    }

    /**
     * Schaltet das Edge Lighting (leuchtender Rand bei Benachrichtigungen) ein/aus.
     */
    suspend fun setEdgeLightingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[EDGE_LIGHTING_KEY] = enabled
        }
    }

    /**
     * Setzt die frei wählbare Farbe des Edge Lightings.
     */
    suspend fun setEdgeLightingColor(color: Color) {
        context.dataStore.edit { preferences ->
            preferences[EDGE_LIGHTING_COLOR_KEY] = color.toArgb()
        }
    }

    /** Setzt das Edge-Lighting-Tempo (0.5..2.0; höher = schneller). */
    suspend fun setEdgeLightingSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[EDGE_LIGHTING_SPEED_KEY] = speed.coerceIn(0.5f, 2f)
        }
    }

    /** Setzt die Edge-Lighting-Durchläufe pro Benachrichtigung (1..5). */
    suspend fun setEdgeLightingLaps(laps: Int) {
        context.dataStore.edit { preferences ->
            preferences[EDGE_LIGHTING_LAPS_KEY] = laps.coerceIn(1, 5)
        }
    }

    /** Setzt die Edge-Lighting-Stärke (0.5..2.0; skaliert Strich-/Glow-Breite). */
    suspend fun setEdgeLightingThickness(thickness: Float) {
        context.dataStore.edit { preferences ->
            preferences[EDGE_LIGHTING_THICKNESS_KEY] = thickness.coerceIn(0.5f, 2f)
        }
    }

    /** Setzt den Edge-Lighting-Stil. */
    suspend fun setEdgeLightingStyle(style: EdgeLightingStyle) {
        context.dataStore.edit { preferences ->
            preferences[EDGE_LIGHTING_STYLE_KEY] = style.name
        }
    }

    /** Setzt den Insel-Öffnungs-/Schließstil. */
    suspend fun setIslandAnimationStyle(style: IslandAnimationStyle) {
        context.dataStore.edit { preferences ->
            preferences[ISLAND_ANIMATION_STYLE_KEY] = style.name
        }
    }
}
