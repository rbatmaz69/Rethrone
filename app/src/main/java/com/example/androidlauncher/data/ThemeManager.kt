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
import com.example.androidlauncher.data.settings.IslandAndEdgeSettings
import com.example.androidlauncher.data.settings.PrivacySettings
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
    private val islandAndEdgeSettings = IslandAndEdgeSettings(context)
    private val privacySettings = PrivacySettings(context)
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

        private val SHOW_FAVORITE_LABELS_KEY = booleanPreferencesKey("show_favorite_labels")
        private val NOTIFICATION_DOTS_KEY = booleanPreferencesKey("notification_dots_enabled")
        private val LIQUID_GLASS_KEY = booleanPreferencesKey("liquid_glass_enabled")
        private val DESIGN_STYLE_KEY = stringPreferencesKey("design_style")
        private val FAVORITES_BORDER_KEY = stringPreferencesKey("favorites_border_style")
        private val APP_FONT_KEY = stringPreferencesKey("app_font")

        // Wallpaper-, Gesten-, Animations-, Island-/Edge- und Privatsphäre-Keys liegen
        // jetzt in den extrahierten Stores unter data/settings/ (A1-Split, gleiche Datei).
        private val SMART_SUGGESTIONS_KEY = booleanPreferencesKey("smart_search_enabled")

        // Wetter-Widget unter Uhr/Datum (Standard: an).
        private val WEATHER_WIDGET_KEY = booleanPreferencesKey("weather_widget_enabled")

        // Uhr-Widget (Standard: an).
        private val CLOCK_WIDGET_KEY = booleanPreferencesKey("clock_widget_enabled")

        // Kalender-/Datum-Widget (Standard: an).
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

    // A1-Split: Privatsphäre-Einstellungen liegen im PrivacySettings-Store.
    val hiddenApps: Flow<Set<String>> = privacySettings.hiddenApps
    val lockedApps: Flow<Set<String>> = privacySettings.lockedApps
    val lockType: Flow<String> = privacySettings.lockType
    val lockSecret: Flow<String> = privacySettings.lockSecret
    val isLockBiometricEnabled: Flow<Boolean> = privacySettings.isLockBiometricEnabled

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

    // A1-Split: Island-/Edge-Lighting-Einstellungen liegen im IslandAndEdgeSettings-Store.
    val isDynamicIslandEnabled: Flow<Boolean> = islandAndEdgeSettings.isDynamicIslandEnabled
    val dynamicIslandOffset: Flow<Float> = islandAndEdgeSettings.dynamicIslandOffset
    val dynamicIslandColor: Flow<Color> = islandAndEdgeSettings.dynamicIslandColor
    val isEdgeLightingEnabled: Flow<Boolean> = islandAndEdgeSettings.isEdgeLightingEnabled
    val edgeLightingColor: Flow<Color> = islandAndEdgeSettings.edgeLightingColor
    val edgeLightingSpeed: Flow<Float> = islandAndEdgeSettings.edgeLightingSpeed
    val edgeLightingLaps: Flow<Int> = islandAndEdgeSettings.edgeLightingLaps
    val edgeLightingThickness: Flow<Float> = islandAndEdgeSettings.edgeLightingThickness
    val edgeLightingStyle: Flow<EdgeLightingStyle> = islandAndEdgeSettings.edgeLightingStyle
    val islandAnimationStyle: Flow<IslandAnimationStyle> = islandAndEdgeSettings.islandAnimationStyle

    // A1-Split: Gesten-Einstellungen liegen im GestureSettings-Store.
    val isShakeGesturesEnabled: Flow<Boolean> = gestureSettings.isShakeGesturesEnabled
    val doubleShakeAction: Flow<GestureAction> = gestureSettings.doubleShakeAction
    val shakeOpenAppPackage: Flow<String?> = gestureSettings.shakeOpenAppPackage
    val doubleTapAction: Flow<GestureAction> = gestureSettings.doubleTapAction
    val doubleTapAppPackage: Flow<String?> = gestureSettings.doubleTapAppPackage

    /**
     * Gewählte Art des App-Zugriffs. Default ist die Drawer-Liste (Niagara-Stil).
     * A1-Split: liegt im PrivacySettings-Store.
     */
    val appAccessMode: Flow<AppAccessMode> = privacySettings.appAccessMode

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

    // A1-Split: Privatsphäre-Setter delegieren an den PrivacySettings-Store.
    suspend fun setHiddenApps(packages: Set<String>) = privacySettings.setHiddenApps(packages)
    suspend fun setLockedApps(packages: Set<String>) = privacySettings.setLockedApps(packages)

    /** Speichert Typ ("pin"/"pattern") und gesalzenen Hash-Token des Geheimnisses. */
    suspend fun setLockSecret(type: String, secretToken: String) = privacySettings.setLockSecret(type, secretToken)

    /** Entfernt den hinterlegten Code (Typ zurück auf "none"). */
    suspend fun clearLockSecret() = privacySettings.clearLockSecret()
    suspend fun setLockBiometricEnabled(enabled: Boolean) = privacySettings.setLockBiometricEnabled(enabled)
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
    suspend fun setAppAccessMode(mode: AppAccessMode) = privacySettings.setAppAccessMode(mode)

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

    // A1-Split: Island-/Edge-Lighting-Setter delegieren an den IslandAndEdgeSettings-Store.
    suspend fun setDynamicIslandEnabled(enabled: Boolean) = islandAndEdgeSettings.setDynamicIslandEnabled(enabled)
    suspend fun setDynamicIslandOffset(offsetDp: Float) = islandAndEdgeSettings.setDynamicIslandOffset(offsetDp)
    suspend fun setDynamicIslandColor(color: Color) = islandAndEdgeSettings.setDynamicIslandColor(color)
    suspend fun setEdgeLightingEnabled(enabled: Boolean) = islandAndEdgeSettings.setEdgeLightingEnabled(enabled)
    suspend fun setEdgeLightingColor(color: Color) = islandAndEdgeSettings.setEdgeLightingColor(color)
    suspend fun setEdgeLightingSpeed(speed: Float) = islandAndEdgeSettings.setEdgeLightingSpeed(speed)
    suspend fun setEdgeLightingLaps(laps: Int) = islandAndEdgeSettings.setEdgeLightingLaps(laps)
    suspend fun setEdgeLightingThickness(thickness: Float) = islandAndEdgeSettings.setEdgeLightingThickness(thickness)
    suspend fun setEdgeLightingStyle(style: EdgeLightingStyle) = islandAndEdgeSettings.setEdgeLightingStyle(style)
    suspend fun setIslandAnimationStyle(style: IslandAnimationStyle) =
        islandAndEdgeSettings.setIslandAnimationStyle(style)
}
