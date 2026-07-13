package com.example.androidlauncher.data.settings

import android.content.Context
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
import com.example.androidlauncher.data.AppFont
import com.example.androidlauncher.data.DesignStyle
import com.example.androidlauncher.data.FavoriteSpacing
import com.example.androidlauncher.data.FontSize
import com.example.androidlauncher.data.FontWeightLevel
import com.example.androidlauncher.data.IconSize
import com.example.androidlauncher.ui.theme.ColorTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Erscheinungsbild-Einstellungen: Farb-Theme, Design-Stil, Schrift (Art/Größe/
 * Gewicht), Icon-Größe, Favoriten-Abstand und frei wählbare Farben. Enthält die
 * sanfte Migration alter Enum-String-Keys auf die neuen numerischen Werte.
 * Aus dem ThemeManager extrahiert (A1-Split); nutzt dieselbe DataStore-Datei
 * wie zuvor, daher keine Datenmigration.
 */
class AppearanceSettings(private val dataStore: DataStore<Preferences>) {

    /** Produktiv-Konstruktor: nutzt die geteilte "settings"-DataStore-Datei. */
    constructor(context: Context) : this(context.settingsDataStore)

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

        private val LIQUID_GLASS_KEY = booleanPreferencesKey("liquid_glass_enabled")
        private val DESIGN_STYLE_KEY = stringPreferencesKey("design_style")
        private val APP_FONT_KEY = stringPreferencesKey("app_font")
    }

    val selectedTheme: Flow<ColorTheme> = dataStore.data
        .map { preferences ->
            // Standard: warmes Theme "Tulpe" (Amber-Verlauf + schwarze Tulpen-Silhouette) für Neuinstallationen.
            val themeName = preferences[THEME_KEY] ?: ColorTheme.SOFT_SAND.name
            runCatching { ColorTheme.valueOf(themeName) }.getOrDefault(ColorTheme.SOFT_SAND)
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

    val selectedFontSize: Flow<FontSize> = dataStore.data
        .map { preferences ->
            val scale = preferences[FONT_SIZE_SCALE_KEY]
                ?: legacyFontSizeScale(preferences[LEGACY_FONT_SIZE_KEY])
                ?: FontSize.STANDARD.scale
            FontSize.of(scale)
        }

    val selectedFontWeight: Flow<FontWeightLevel> = dataStore.data
        .map { preferences ->
            val value = preferences[FONT_WEIGHT_VALUE_KEY]
                ?: legacyFontWeightValue(preferences[LEGACY_FONT_WEIGHT_KEY])
                ?: FontWeightLevel.NORMAL.weightValue
            FontWeightLevel.of(value)
        }

    val selectedIconSize: Flow<IconSize> = dataStore.data
        .map { preferences ->
            val sizeDp = preferences[ICON_SIZE_DP_KEY]
                ?: legacyIconSizeDp(preferences[LEGACY_ICON_SIZE_KEY])
                ?: IconSize.DEFAULT.size.value
            IconSize.of(sizeDp.dp)
        }

    val selectedFavoriteSpacing: Flow<FavoriteSpacing> = dataStore.data
        .map { preferences ->
            val spacingDp = preferences[FAVORITE_SPACING_DP_KEY]
                ?: legacyFavoriteSpacingDp(preferences[LEGACY_FAVORITE_SPACING_KEY])
                ?: FavoriteSpacing.DEFAULT.spacing.value
            FavoriteSpacing.of(spacingDp.dp)
        }

    // Standard: dunkler Text – passend zum hellen Default-Theme "Tagespapier".
    val isDarkTextEnabled: Flow<Boolean> = dataStore.data
        .map { it[DARK_TEXT_KEY] ?: true }

    // Frei wählbare Iconfarbe (gilt überall). Default: warmes Karamell-Braun,
    // passend zum sonnigen Standard-Theme "Tulpe" (Lucide-Fallback-Glyphen lesbar).
    val iconColor: Flow<Color> = dataStore.data
        .map { Color(it[ICON_COLOR_KEY] ?: 0xFF513A14.toInt()) }

    // Frei wählbare Schriftfarbe – nur Startbildschirm. Default: warmes Karamell-Braun,
    // damit Uhr/Datum über dem hellen oberen Bereich des Tulpe-Verlaufs gut lesbar sind.
    val homeTextColor: Flow<Color> = dataStore.data
        .map { Color(it[HOME_TEXT_COLOR_KEY] ?: 0xFF513A14.toInt()) }

    // CUSTOM-Theme: frei wählbare Flächenfarben (Default: helles Warmweiß).
    val customBackgroundColor: Flow<Color> = dataStore.data
        .map { Color(it[CUSTOM_BG_COLOR_KEY] ?: 0xFFF4EEE2.toInt()) }

    val customMenuColor: Flow<Color> = dataStore.data
        .map { Color(it[CUSTOM_MENU_COLOR_KEY] ?: 0xFFFFFFFF.toInt()) }

    val isLiquidGlassEnabled: Flow<Boolean> = dataStore.data
        .map { it[LIQUID_GLASS_KEY] ?: true }

    /**
     * Gewählter Oberflächen-Stil. Migriert automatisch aus dem alten
     * `liquid_glass_enabled`-Boolean, falls noch kein `design_style` gesetzt ist
     * (true → GLASS, false → FLAT).
     */
    val designStyle: Flow<DesignStyle> = dataStore.data
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

    val selectedAppFont: Flow<AppFont> = dataStore.data
        .map { preferences ->
            val fontName = preferences[APP_FONT_KEY] ?: AppFont.SYSTEM_DEFAULT.name
            runCatching { AppFont.valueOf(fontName) }.getOrDefault(AppFont.SYSTEM_DEFAULT)
        }

    suspend fun setTheme(theme: ColorTheme) {
        dataStore.edit { it[THEME_KEY] = theme.name }
    }

    suspend fun setFontSize(scale: Float) {
        dataStore.edit { it[FONT_SIZE_SCALE_KEY] = scale.coerceIn(FontSize.MIN, FontSize.MAX) }
    }

    suspend fun setFontWeight(value: Int) {
        dataStore.edit { it[FONT_WEIGHT_VALUE_KEY] = value.coerceIn(FontWeightLevel.MIN, FontWeightLevel.MAX) }
    }

    suspend fun setIconSize(size: Dp) {
        dataStore.edit { it[ICON_SIZE_DP_KEY] = size.coerceIn(IconSize.MIN, IconSize.MAX).value }
    }

    suspend fun setFavoriteSpacing(spacing: Dp) {
        dataStore.edit {
            it[FAVORITE_SPACING_DP_KEY] = spacing.coerceIn(FavoriteSpacing.MIN, FavoriteSpacing.MAX).value
        }
    }

    suspend fun setDarkTextEnabled(enabled: Boolean) {
        dataStore.edit { it[DARK_TEXT_KEY] = enabled }
    }

    suspend fun setIconColor(color: Color) {
        dataStore.edit { it[ICON_COLOR_KEY] = color.toArgb() }
    }

    suspend fun setHomeTextColor(color: Color) {
        dataStore.edit { it[HOME_TEXT_COLOR_KEY] = color.toArgb() }
    }

    suspend fun setCustomBackgroundColor(color: Color) {
        dataStore.edit { it[CUSTOM_BG_COLOR_KEY] = color.toArgb() }
    }

    suspend fun setCustomMenuColor(color: Color) {
        dataStore.edit { it[CUSTOM_MENU_COLOR_KEY] = color.toArgb() }
    }

    suspend fun setLiquidGlassEnabled(enabled: Boolean) {
        dataStore.edit { it[LIQUID_GLASS_KEY] = enabled }
    }

    suspend fun setDesignStyle(style: DesignStyle) {
        dataStore.edit {
            it[DESIGN_STYLE_KEY] = style.name
            // Legacy-Boolean konsistent halten, falls anderer Code es noch liest.
            it[LIQUID_GLASS_KEY] = style.isGlassLike
        }
    }

    suspend fun setAppFont(font: AppFont) {
        dataStore.edit { it[APP_FONT_KEY] = font.name }
    }
}
