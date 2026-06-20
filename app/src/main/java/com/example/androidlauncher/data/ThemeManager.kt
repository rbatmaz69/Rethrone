package com.example.androidlauncher.data

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
        private val FAVORITE_SPACING_KEY = stringPreferencesKey("favorite_icon_spacing")
        private val DARK_TEXT_KEY = booleanPreferencesKey("dark_text_enabled")
        // ARGB-Werte für frei wählbare Icon-/Schriftfarbe (Default Weiß).
        private val ICON_COLOR_KEY = intPreferencesKey("icon_color")
        private val HOME_TEXT_COLOR_KEY = intPreferencesKey("home_text_color")
        // ARGB-Flächenfarben für das CUSTOM-Theme ("Eigene Farbe").
        private val CUSTOM_BG_COLOR_KEY = intPreferencesKey("custom_bg_color")
        private val CUSTOM_MENU_COLOR_KEY = intPreferencesKey("custom_menu_color")
        // Komma-separierte Paketnamen der ausgeblendeten Apps.
        private val HIDDEN_APPS_KEY = stringPreferencesKey("hidden_apps")
        private val SHOW_FAVORITE_LABELS_KEY = booleanPreferencesKey("show_favorite_labels")
        private val LIQUID_GLASS_KEY = booleanPreferencesKey("liquid_glass_enabled")
        private val DESIGN_STYLE_KEY = stringPreferencesKey("design_style")
        private val FAVORITES_BORDER_KEY = stringPreferencesKey("favorites_border_style")
        private val APP_FONT_KEY = stringPreferencesKey("app_font")
        private val CUSTOM_WALLPAPER_KEY = stringPreferencesKey("custom_wallpaper_uri")
        private val WALLPAPER_BLUR_KEY = floatPreferencesKey("wallpaper_blur")
        private val WALLPAPER_DIM_KEY = floatPreferencesKey("wallpaper_dim")
        private val WALLPAPER_ZOOM_KEY = floatPreferencesKey("wallpaper_zoom")
        private val SHAKE_GESTURES_KEY = booleanPreferencesKey("shake_gestures_enabled")
        private val DOUBLE_SHAKE_ACTION_KEY = stringPreferencesKey("double_shake_action")
        // Paketname der App, die bei GestureAction.OPEN_APP (Schütteln) gestartet wird.
        private val SHAKE_OPEN_APP_PACKAGE_KEY = stringPreferencesKey("shake_open_app_package")
        // Doppeltipp-Geste auf dem Startbildschirm.
        private val DOUBLE_TAP_ACTION_KEY = stringPreferencesKey("double_tap_action")
        private val DOUBLE_TAP_APP_PACKAGE_KEY = stringPreferencesKey("double_tap_app_package")
        private val SMART_SUGGESTIONS_KEY = booleanPreferencesKey("smart_search_enabled")
        private val HAPTIC_FEEDBACK_KEY = booleanPreferencesKey("haptic_feedback_enabled")
        private val ANIMATIONS_ENABLED_KEY = booleanPreferencesKey("animations_enabled")
        // Einzelne Animationsarten (greifen nur, wenn der Master oben aktiv ist; Standard: an).
        private val ANIMATION_APP_OPEN_KEY = booleanPreferencesKey("animation_app_open")
        private val ANIMATION_APP_CLOSE_KEY = booleanPreferencesKey("animation_app_close")
        private val ANIMATION_MENUS_KEY = booleanPreferencesKey("animation_menus")
        private val ANIMATION_FAVORITES_KEY = booleanPreferencesKey("animation_favorites")
        // Globaler Tempo-Faktor für alle Animationen (1.0 = normal, 2.0 = doppelt so schnell,
        // 0.5 = halbes Tempo). Standard: 1.0.
        private val ANIMATION_SPEED_KEY = floatPreferencesKey("animation_speed")
        private val APP_ACCESS_MODE_KEY = stringPreferencesKey("app_access_mode")
        // Wetter-Widget unter Uhr/Datum (Standard: an).
        private val WEATHER_WIDGET_KEY = booleanPreferencesKey("weather_widget_enabled")
        // Uhr-Widget (Standard: an).
        private val CLOCK_WIDGET_KEY = booleanPreferencesKey("clock_widget_enabled")
        // Kalender-/Datum-Widget (Standard: an).
        private val CALENDAR_WIDGET_KEY = booleanPreferencesKey("calendar_widget_enabled")

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

    val selectedFavoriteSpacing: Flow<FavoriteSpacing> = context.dataStore.data
        .map { preferences ->
            val name = preferences[FAVORITE_SPACING_KEY] ?: FavoriteSpacing.STANDARD.name
            try { FavoriteSpacing.valueOf(name) } catch (e: IllegalArgumentException) { FavoriteSpacing.STANDARD }
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

    val showFavoriteLabels: Flow<Boolean> = context.dataStore.data
        .map { it[SHOW_FAVORITE_LABELS_KEY] ?: false }

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

    val customWallpaperUri: Flow<String?> = context.dataStore.data
        .map { it[CUSTOM_WALLPAPER_KEY] }

    val wallpaperBlur: Flow<Float> = context.dataStore.data
        .map { it[WALLPAPER_BLUR_KEY] ?: 0f }

    val wallpaperDim: Flow<Float> = context.dataStore.data
        .map { it[WALLPAPER_DIM_KEY] ?: 0.1f }

    val wallpaperZoom: Flow<Float> = context.dataStore.data
        .map { it[WALLPAPER_ZOOM_KEY] ?: 1.0f }

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
     * Observable flow for animations toggle.
     */
    val isAnimationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[ANIMATIONS_ENABLED_KEY] ?: true }

    /**
     * Observable flow für die App-Öffnen-Animation (Aufzieh-Effekt). Default: an.
     */
    val isAppOpenAnimationEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[ANIMATION_APP_OPEN_KEY] ?: true }

    /**
     * Observable flow für die App-Schließen-/Rückkehr-Animation (Schrumpfen + Bounce). Default: an.
     */
    val isAppCloseAnimationEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[ANIMATION_APP_CLOSE_KEY] ?: true }

    /**
     * Observable flow für Menü-/Einstellungsmenü-Animationen. Default: an.
     */
    val isMenuAnimationEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[ANIMATION_MENUS_KEY] ?: true }

    /**
     * Observable flow für die Favoriten-Leisten-Animation (Vergrößern beim Rüberfahren). Default: an.
     */
    val isFavoritesAnimationEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[ANIMATION_FAVORITES_KEY] ?: true }

    /**
     * Globaler Tempo-Faktor für Animationen (0.5×–2×). Default: 1×.
     */
    val animationSpeed: Flow<Float> = context.dataStore.data
        .map { (it[ANIMATION_SPEED_KEY] ?: 1f).coerceIn(0.5f, 2f) }

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
     * Observable flow for shake gesture toggle.
     */
    val isShakeGesturesEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHAKE_GESTURES_KEY] ?: true
        }

    /**
     * Aktion für doppeltes Schütteln (einzige Shake-Geste). Default Taschenlampe.
     */
    val doubleShakeAction: Flow<GestureAction> = context.dataStore.data
        .map { preferences ->
            val name = preferences[DOUBLE_SHAKE_ACTION_KEY] ?: GestureAction.FLASHLIGHT.name
            try { GestureAction.valueOf(name) } catch (e: IllegalArgumentException) { GestureAction.FLASHLIGHT }
        }

    /**
     * Paketname der App, die bei [GestureAction.OPEN_APP] (Schütteln) gestartet wird (null, wenn keine gewählt).
     */
    val shakeOpenAppPackage: Flow<String?> = context.dataStore.data
        .map { preferences -> CryptoManager.decryptOrLegacy(preferences[SHAKE_OPEN_APP_PACKAGE_KEY]).takeIf { it.isNotBlank() } }

    /**
     * Aktion für die Doppeltipp-Geste auf dem Startbildschirm. Default: Bildschirm sperren
     * (entspricht dem bisherigen, fest verdrahteten Verhalten).
     */
    val doubleTapAction: Flow<GestureAction> = context.dataStore.data
        .map { preferences ->
            val name = preferences[DOUBLE_TAP_ACTION_KEY] ?: GestureAction.LOCK_SCREEN.name
            try { GestureAction.valueOf(name) } catch (e: IllegalArgumentException) { GestureAction.LOCK_SCREEN }
        }

    /**
     * Paketname der App, die bei [GestureAction.OPEN_APP] (Doppeltippen) gestartet wird (null, wenn keine gewählt).
     */
    val doubleTapAppPackage: Flow<String?> = context.dataStore.data
        .map { preferences -> CryptoManager.decryptOrLegacy(preferences[DOUBLE_TAP_APP_PACKAGE_KEY]).takeIf { it.isNotBlank() } }

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
    suspend fun setFavoriteSpacing(spacing: FavoriteSpacing) { context.dataStore.edit { it[FAVORITE_SPACING_KEY] = spacing.name } }
    suspend fun setDarkTextEnabled(enabled: Boolean) { context.dataStore.edit { it[DARK_TEXT_KEY] = enabled } }
    suspend fun setIconColor(color: Color) { context.dataStore.edit { it[ICON_COLOR_KEY] = color.toArgb() } }
    suspend fun setHomeTextColor(color: Color) { context.dataStore.edit { it[HOME_TEXT_COLOR_KEY] = color.toArgb() } }
    suspend fun setCustomBackgroundColor(color: Color) { context.dataStore.edit { it[CUSTOM_BG_COLOR_KEY] = color.toArgb() } }
    suspend fun setCustomMenuColor(color: Color) { context.dataStore.edit { it[CUSTOM_MENU_COLOR_KEY] = color.toArgb() } }
    suspend fun setHiddenApps(packages: Set<String>) { context.dataStore.edit { it[HIDDEN_APPS_KEY] = CryptoManager.encrypt(packages.joinToString(",")) } }
    suspend fun setShowFavoriteLabels(show: Boolean) { context.dataStore.edit { it[SHOW_FAVORITE_LABELS_KEY] = show } }
    suspend fun setLiquidGlassEnabled(enabled: Boolean) { context.dataStore.edit { it[LIQUID_GLASS_KEY] = enabled } }
    suspend fun setDesignStyle(style: DesignStyle) {
        context.dataStore.edit {
            it[DESIGN_STYLE_KEY] = style.name
            // Legacy-Boolean konsistent halten, falls anderer Code es noch liest.
            it[LIQUID_GLASS_KEY] = style.isGlassLike
        }
    }
    suspend fun setFavoritesBorderStyle(style: FavoritesBorderStyle) { context.dataStore.edit { it[FAVORITES_BORDER_KEY] = style.name } }
    suspend fun setAppFont(font: AppFont) { context.dataStore.edit { it[APP_FONT_KEY] = font.name } }
    suspend fun setCustomWallpaperUri(uri: String?) { context.dataStore.edit { if (uri == null) it.remove(CUSTOM_WALLPAPER_KEY) else it[CUSTOM_WALLPAPER_KEY] = uri } }
    suspend fun setWallpaperBlur(blur: Float) { context.dataStore.edit { it[WALLPAPER_BLUR_KEY] = blur } }
    suspend fun setWallpaperDim(dim: Float) { context.dataStore.edit { it[WALLPAPER_DIM_KEY] = dim } }
    suspend fun setWallpaperZoom(zoom: Float) { context.dataStore.edit { it[WALLPAPER_ZOOM_KEY] = zoom } }

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
     * Setzt die Aktion für doppeltes Schütteln.
     */
    suspend fun setDoubleShakeAction(action: GestureAction) {
        context.dataStore.edit { preferences ->
            preferences[DOUBLE_SHAKE_ACTION_KEY] = action.name
        }
    }

    /**
     * Setzt das Paket der App, die bei [GestureAction.OPEN_APP] (Schütteln) gestartet wird (null löscht die Wahl).
     */
    suspend fun setShakeOpenAppPackage(packageName: String?) {
        context.dataStore.edit { preferences ->
            if (packageName.isNullOrBlank()) {
                preferences.remove(SHAKE_OPEN_APP_PACKAGE_KEY)
            } else {
                preferences[SHAKE_OPEN_APP_PACKAGE_KEY] = CryptoManager.encrypt(packageName)
            }
        }
    }

    /**
     * Setzt die Aktion für die Doppeltipp-Geste.
     */
    suspend fun setDoubleTapAction(action: GestureAction) {
        context.dataStore.edit { preferences ->
            preferences[DOUBLE_TAP_ACTION_KEY] = action.name
        }
    }

    /**
     * Setzt das Paket der App, die bei [GestureAction.OPEN_APP] (Doppeltippen) gestartet wird (null löscht die Wahl).
     */
    suspend fun setDoubleTapAppPackage(packageName: String?) {
        context.dataStore.edit { preferences ->
            if (packageName.isNullOrBlank()) {
                preferences.remove(DOUBLE_TAP_APP_PACKAGE_KEY)
            } else {
                preferences[DOUBLE_TAP_APP_PACKAGE_KEY] = CryptoManager.encrypt(packageName)
            }
        }
    }

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

    /**
     * Toggles animations.
     */
    suspend fun setAnimationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ANIMATIONS_ENABLED_KEY] = enabled
        }
    }

    /**
     * Schaltet die App-Öffnen-Animation ein/aus.
     */
    suspend fun setAppOpenAnimationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ANIMATION_APP_OPEN_KEY] = enabled
        }
    }

    /**
     * Schaltet die App-Schließen-/Rückkehr-Animation ein/aus.
     */
    suspend fun setAppCloseAnimationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ANIMATION_APP_CLOSE_KEY] = enabled
        }
    }

    /**
     * Schaltet Menü-/Einstellungsmenü-Animationen ein/aus.
     */
    suspend fun setMenuAnimationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ANIMATION_MENUS_KEY] = enabled
        }
    }

    /**
     * Schaltet die Favoriten-Leisten-Animation (Vergrößern beim Rüberfahren) ein/aus.
     */
    suspend fun setFavoritesAnimationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ANIMATION_FAVORITES_KEY] = enabled
        }
    }

    /**
     * Setzt den globalen Animations-Tempo-Faktor (auf 0.5×–2× begrenzt).
     */
    suspend fun setAnimationSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[ANIMATION_SPEED_KEY] = speed.coerceIn(0.5f, 2f)
        }
    }

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
}
