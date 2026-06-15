package com.example.androidlauncher.data

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
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
        private val APP_FONT_KEY = stringPreferencesKey("app_font")
        private val CUSTOM_WALLPAPER_KEY = stringPreferencesKey("custom_wallpaper_uri")
        private val WALLPAPER_BLUR_KEY = floatPreferencesKey("wallpaper_blur")
        private val WALLPAPER_DIM_KEY = floatPreferencesKey("wallpaper_dim")
        private val WALLPAPER_ZOOM_KEY = floatPreferencesKey("wallpaper_zoom")
        private val SHAKE_GESTURES_KEY = booleanPreferencesKey("shake_gestures_enabled")
        private val DOUBLE_SHAKE_ACTION_KEY = stringPreferencesKey("double_shake_action")
        // Paketname der App, die bei ShakeAction.OPEN_APP gestartet wird.
        private val SHAKE_OPEN_APP_PACKAGE_KEY = stringPreferencesKey("shake_open_app_package")
        private val SMART_SUGGESTIONS_KEY = booleanPreferencesKey("smart_search_enabled")
        private val HAPTIC_FEEDBACK_KEY = booleanPreferencesKey("haptic_feedback_enabled")
        private val ANIMATIONS_ENABLED_KEY = booleanPreferencesKey("animations_enabled")
        private val APP_ACCESS_MODE_KEY = stringPreferencesKey("app_access_mode")

        // Offset for UI elements (Relative to default position)
        private val FAVORITES_OFFSET_X_KEY = floatPreferencesKey("favorites_offset_x")
        private val FAVORITES_OFFSET_Y_KEY = floatPreferencesKey("favorites_offset_y")
        // Uhrbereich kann als Einheit (Uhr + Datum) auf X/Y verschoben werden.
        private val CLOCK_OFFSET_X_KEY = floatPreferencesKey("clock_offset_x")
        private val CLOCK_OFFSET_Y_KEY = floatPreferencesKey("clock_offset_y")
    }

    val selectedTheme: Flow<ColorTheme> = context.dataStore.data
        .map { preferences ->
            // Standard: helles Papier-Theme "Tagespapier" für Neuinstallationen.
            val themeName = preferences[THEME_KEY] ?: ColorTheme.PAPER_DAYLIGHT.name
            try { ColorTheme.valueOf(themeName) } catch (e: IllegalArgumentException) { ColorTheme.PAPER_DAYLIGHT }
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

    // Standard: dunkler Text – passend zum hellen Default-Theme "Tagespapier".
    val isDarkTextEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[DARK_TEXT_KEY] ?: true }

    // Frei wählbare Iconfarbe (gilt überall). Default Weiß.
    val iconColor: Flow<Color> = context.dataStore.data
        .map { Color(it[ICON_COLOR_KEY] ?: Color.White.toArgb()) }

    // Frei wählbare Schriftfarbe – nur Startbildschirm. Default Weiß.
    val homeTextColor: Flow<Color> = context.dataStore.data
        .map { Color(it[HOME_TEXT_COLOR_KEY] ?: Color.White.toArgb()) }

    // CUSTOM-Theme: frei wählbare Flächenfarben (Default: dunkel, kontraststark zu Weiß).
    val customBackgroundColor: Flow<Color> = context.dataStore.data
        .map { Color(it[CUSTOM_BG_COLOR_KEY] ?: 0xFF12141A.toInt()) }

    val customMenuColor: Flow<Color> = context.dataStore.data
        .map { Color(it[CUSTOM_MENU_COLOR_KEY] ?: 0xFF20242E.toInt()) }

    // Ausgeblendete Apps (Paketnamen). Werden überall aus der Anzeige gefiltert.
    val hiddenApps: Flow<Set<String>> = context.dataStore.data
        .map { prefs ->
            val raw = prefs[HIDDEN_APPS_KEY] ?: ""
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

    // X-Offset für die Uhr-/Datums-Einheit im Home-Edit-Modus.
    val clockOffsetX: Flow<Float> = context.dataStore.data
        .map { it[CLOCK_OFFSET_X_KEY] ?: 0f }

    // Y-Offset für die Uhr-/Datums-Einheit im Home-Edit-Modus.
    val clockOffsetY: Flow<Float> = context.dataStore.data
        .map { it[CLOCK_OFFSET_Y_KEY] ?: 0f }

    /**
     * Observable flow for animations toggle.
     */
    val isAnimationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[ANIMATIONS_ENABLED_KEY] ?: true }

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
    val doubleShakeAction: Flow<ShakeAction> = context.dataStore.data
        .map { preferences ->
            val name = preferences[DOUBLE_SHAKE_ACTION_KEY] ?: ShakeAction.FLASHLIGHT.name
            try { ShakeAction.valueOf(name) } catch (e: IllegalArgumentException) { ShakeAction.FLASHLIGHT }
        }

    /**
     * Paketname der App, die bei [ShakeAction.OPEN_APP] gestartet wird (null, wenn keine gewählt).
     */
    val shakeOpenAppPackage: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[SHAKE_OPEN_APP_PACKAGE_KEY]?.takeIf { it.isNotBlank() } }

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
    suspend fun setDarkTextEnabled(enabled: Boolean) { context.dataStore.edit { it[DARK_TEXT_KEY] = enabled } }
    suspend fun setIconColor(color: Color) { context.dataStore.edit { it[ICON_COLOR_KEY] = color.toArgb() } }
    suspend fun setHomeTextColor(color: Color) { context.dataStore.edit { it[HOME_TEXT_COLOR_KEY] = color.toArgb() } }
    suspend fun setCustomBackgroundColor(color: Color) { context.dataStore.edit { it[CUSTOM_BG_COLOR_KEY] = color.toArgb() } }
    suspend fun setCustomMenuColor(color: Color) { context.dataStore.edit { it[CUSTOM_MENU_COLOR_KEY] = color.toArgb() } }
    suspend fun setHiddenApps(packages: Set<String>) { context.dataStore.edit { it[HIDDEN_APPS_KEY] = packages.joinToString(",") } }
    suspend fun setShowFavoriteLabels(show: Boolean) { context.dataStore.edit { it[SHOW_FAVORITE_LABELS_KEY] = show } }
    suspend fun setLiquidGlassEnabled(enabled: Boolean) { context.dataStore.edit { it[LIQUID_GLASS_KEY] = enabled } }
    suspend fun setDesignStyle(style: DesignStyle) {
        context.dataStore.edit {
            it[DESIGN_STYLE_KEY] = style.name
            // Legacy-Boolean konsistent halten, falls anderer Code es noch liest.
            it[LIQUID_GLASS_KEY] = style.isGlassLike
        }
    }
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

    // Persistiert die Uhr-/Datums-Einheit gemeinsam auf X/Y.
    suspend fun setClockOffset(x: Float, y: Float) {
        context.dataStore.edit {
            it[CLOCK_OFFSET_X_KEY] = x
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
     * Setzt die Aktion für doppeltes Schütteln.
     */
    suspend fun setDoubleShakeAction(action: ShakeAction) {
        context.dataStore.edit { preferences ->
            preferences[DOUBLE_SHAKE_ACTION_KEY] = action.name
        }
    }

    /**
     * Setzt das Paket der App, die bei [ShakeAction.OPEN_APP] gestartet wird (null löscht die Wahl).
     */
    suspend fun setShakeOpenAppPackage(packageName: String?) {
        context.dataStore.edit { preferences ->
            if (packageName.isNullOrBlank()) {
                preferences.remove(SHAKE_OPEN_APP_PACKAGE_KEY)
            } else {
                preferences[SHAKE_OPEN_APP_PACKAGE_KEY] = packageName
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
}
