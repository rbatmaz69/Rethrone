package com.example.androidlauncher.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.example.androidlauncher.data.settings.AnimationSettings
import com.example.androidlauncher.data.settings.AppearanceSettings
import com.example.androidlauncher.data.settings.GestureSettings
import com.example.androidlauncher.data.settings.HomeLayoutSettings
import com.example.androidlauncher.data.settings.IslandAndEdgeSettings
import com.example.androidlauncher.data.settings.PrivacySettings
import com.example.androidlauncher.data.settings.WallpaperSettings
import com.example.androidlauncher.ui.theme.ColorTheme
import kotlinx.coroutines.flow.Flow

/**
 * Fassade über die domänen-spezifischen Settings-Stores unter `data/settings/`
 * (A1-Split abgeschlossen). Alle Keys und ihre Logik liegen in den Stores; diese
 * Klasse delegiert nur noch, damit die vielen bestehenden Aufrufstellen
 * unverändert bleiben. Neue Features sollten direkt den passenden Store
 * injizieren (siehe `DataModule`) statt diese Fassade zu erweitern.
 */
class ThemeManager(context: Context) {

    // Alle Stores teilen sich dieselbe DataStore-Datei "settings" – keine Migration nötig.
    private val appearanceSettings = AppearanceSettings(context)
    private val homeLayoutSettings = HomeLayoutSettings(context)
    private val wallpaperSettings = WallpaperSettings(context)
    private val gestureSettings = GestureSettings(context)
    private val animationSettings = AnimationSettings(context)
    private val islandAndEdgeSettings = IslandAndEdgeSettings(context)
    private val privacySettings = PrivacySettings(context)

    // ── Erscheinungsbild (AppearanceSettings) ────────────────────────
    val selectedTheme: Flow<ColorTheme> = appearanceSettings.selectedTheme
    val selectedFontSize: Flow<FontSize> = appearanceSettings.selectedFontSize
    val selectedFontWeight: Flow<FontWeightLevel> = appearanceSettings.selectedFontWeight
    val selectedIconSize: Flow<IconSize> = appearanceSettings.selectedIconSize
    val selectedFavoriteSpacing: Flow<FavoriteSpacing> = appearanceSettings.selectedFavoriteSpacing
    val isDarkTextEnabled: Flow<Boolean> = appearanceSettings.isDarkTextEnabled
    val iconColor: Flow<Color> = appearanceSettings.iconColor
    val homeTextColor: Flow<Color> = appearanceSettings.homeTextColor
    val customBackgroundColor: Flow<Color> = appearanceSettings.customBackgroundColor
    val customMenuColor: Flow<Color> = appearanceSettings.customMenuColor
    val isLiquidGlassEnabled: Flow<Boolean> = appearanceSettings.isLiquidGlassEnabled
    val designStyle: Flow<DesignStyle> = appearanceSettings.designStyle
    val selectedAppFont: Flow<AppFont> = appearanceSettings.selectedAppFont

    // ── Privatsphäre (PrivacySettings) ───────────────────────────────
    val hiddenApps: Flow<Set<String>> = privacySettings.hiddenApps
    val lockedApps: Flow<Set<String>> = privacySettings.lockedApps
    val lockType: Flow<String> = privacySettings.lockType
    val lockSecret: Flow<String> = privacySettings.lockSecret
    val isLockBiometricEnabled: Flow<Boolean> = privacySettings.isLockBiometricEnabled
    val appAccessMode: Flow<AppAccessMode> = privacySettings.appAccessMode

    // ── Startbildschirm (HomeLayoutSettings) ─────────────────────────
    val showFavoriteLabels: Flow<Boolean> = homeLayoutSettings.showFavoriteLabels
    val isNotificationDotsEnabled: Flow<Boolean> = homeLayoutSettings.isNotificationDotsEnabled
    val favoritesBorderStyle: Flow<FavoritesBorderStyle> = homeLayoutSettings.favoritesBorderStyle
    val isSmartSuggestionsEnabled: Flow<Boolean> = homeLayoutSettings.isSmartSuggestionsEnabled
    val isWeatherWidgetEnabled: Flow<Boolean> = homeLayoutSettings.isWeatherWidgetEnabled
    val isClockWidgetEnabled: Flow<Boolean> = homeLayoutSettings.isClockWidgetEnabled
    val isCalendarWidgetEnabled: Flow<Boolean> = homeLayoutSettings.isCalendarWidgetEnabled
    val isOnboardingCompleted: Flow<Boolean> = homeLayoutSettings.isOnboardingCompleted
    val homeLayout: Flow<HomeLayout> = homeLayoutSettings.homeLayout

    // ── Wallpaper (WallpaperSettings) ────────────────────────────────
    val customWallpaperUri: Flow<String?> = wallpaperSettings.customWallpaperUri
    val wallpaperBlur: Flow<Float> = wallpaperSettings.wallpaperBlur
    val wallpaperDim: Flow<Float> = wallpaperSettings.wallpaperDim
    val wallpaperZoom: Flow<Float> = wallpaperSettings.wallpaperZoom

    // ── Animationen (AnimationSettings) ──────────────────────────────
    val isAnimationsEnabled: Flow<Boolean> = animationSettings.isAnimationsEnabled
    val isAppOpenAnimationEnabled: Flow<Boolean> = animationSettings.isAppOpenAnimationEnabled
    val isAppCloseAnimationEnabled: Flow<Boolean> = animationSettings.isAppCloseAnimationEnabled
    val isMenuAnimationEnabled: Flow<Boolean> = animationSettings.isMenuAnimationEnabled
    val isFavoritesAnimationEnabled: Flow<Boolean> = animationSettings.isFavoritesAnimationEnabled
    val animationSpeed: Flow<Float> = animationSettings.animationSpeed

    // ── Dynamic Island & Edge Lighting (IslandAndEdgeSettings) ───────
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

    // ── Gesten & Haptik (GestureSettings) ────────────────────────────
    val isShakeGesturesEnabled: Flow<Boolean> = gestureSettings.isShakeGesturesEnabled
    val doubleShakeAction: Flow<GestureAction> = gestureSettings.doubleShakeAction
    val shakeOpenAppPackage: Flow<String?> = gestureSettings.shakeOpenAppPackage
    val doubleTapAction: Flow<GestureAction> = gestureSettings.doubleTapAction
    val doubleTapAppPackage: Flow<String?> = gestureSettings.doubleTapAppPackage
    val isHapticFeedbackEnabled: Flow<Boolean> = gestureSettings.isHapticFeedbackEnabled

    // ── Setter: Erscheinungsbild ─────────────────────────────────────
    suspend fun setTheme(theme: ColorTheme) = appearanceSettings.setTheme(theme)
    suspend fun setFontSize(scale: Float) = appearanceSettings.setFontSize(scale)
    suspend fun setFontWeight(value: Int) = appearanceSettings.setFontWeight(value)
    suspend fun setIconSize(size: Dp) = appearanceSettings.setIconSize(size)
    suspend fun setFavoriteSpacing(spacing: Dp) = appearanceSettings.setFavoriteSpacing(spacing)
    suspend fun setDarkTextEnabled(enabled: Boolean) = appearanceSettings.setDarkTextEnabled(enabled)
    suspend fun setIconColor(color: Color) = appearanceSettings.setIconColor(color)
    suspend fun setHomeTextColor(color: Color) = appearanceSettings.setHomeTextColor(color)
    suspend fun setCustomBackgroundColor(color: Color) = appearanceSettings.setCustomBackgroundColor(color)
    suspend fun setCustomMenuColor(color: Color) = appearanceSettings.setCustomMenuColor(color)
    suspend fun setLiquidGlassEnabled(enabled: Boolean) = appearanceSettings.setLiquidGlassEnabled(enabled)
    suspend fun setDesignStyle(style: DesignStyle) = appearanceSettings.setDesignStyle(style)
    suspend fun setAppFont(font: AppFont) = appearanceSettings.setAppFont(font)

    // ── Setter: Privatsphäre ─────────────────────────────────────────
    suspend fun setHiddenApps(packages: Set<String>) = privacySettings.setHiddenApps(packages)
    suspend fun setLockedApps(packages: Set<String>) = privacySettings.setLockedApps(packages)

    /** Speichert Typ ("pin"/"pattern") und gesalzenen Hash-Token des Geheimnisses. */
    suspend fun setLockSecret(type: String, secretToken: String) = privacySettings.setLockSecret(type, secretToken)

    /** Entfernt den hinterlegten Code (Typ zurück auf "none"). */
    suspend fun clearLockSecret() = privacySettings.clearLockSecret()
    suspend fun setLockBiometricEnabled(enabled: Boolean) = privacySettings.setLockBiometricEnabled(enabled)
    suspend fun setAppAccessMode(mode: AppAccessMode) = privacySettings.setAppAccessMode(mode)

    // ── Setter: Startbildschirm ──────────────────────────────────────
    suspend fun setShowFavoriteLabels(show: Boolean) = homeLayoutSettings.setShowFavoriteLabels(show)
    suspend fun setNotificationDotsEnabled(enabled: Boolean) = homeLayoutSettings.setNotificationDotsEnabled(enabled)
    suspend fun setFavoritesBorderStyle(style: FavoritesBorderStyle) =
        homeLayoutSettings.setFavoritesBorderStyle(style)
    suspend fun setSmartSuggestionsEnabled(enabled: Boolean) = homeLayoutSettings.setSmartSuggestionsEnabled(enabled)
    suspend fun setWeatherWidgetEnabled(enabled: Boolean) = homeLayoutSettings.setWeatherWidgetEnabled(enabled)
    suspend fun setClockWidgetEnabled(enabled: Boolean) = homeLayoutSettings.setClockWidgetEnabled(enabled)
    suspend fun setCalendarWidgetEnabled(enabled: Boolean) = homeLayoutSettings.setCalendarWidgetEnabled(enabled)
    suspend fun setOnboardingCompleted(completed: Boolean) = homeLayoutSettings.setOnboardingCompleted(completed)
    suspend fun setHomeLayout(layout: HomeLayout) = homeLayoutSettings.setHomeLayout(layout)

    // ── Setter: Wallpaper ────────────────────────────────────────────
    suspend fun setCustomWallpaperUri(uri: String?) = wallpaperSettings.setCustomWallpaperUri(uri)
    suspend fun setWallpaperBlur(blur: Float) = wallpaperSettings.setWallpaperBlur(blur)
    suspend fun setWallpaperDim(dim: Float) = wallpaperSettings.setWallpaperDim(dim)
    suspend fun setWallpaperZoom(zoom: Float) = wallpaperSettings.setWallpaperZoom(zoom)

    // ── Setter: Animationen ──────────────────────────────────────────
    suspend fun setAnimationsEnabled(enabled: Boolean) = animationSettings.setAnimationsEnabled(enabled)
    suspend fun setAppOpenAnimationEnabled(enabled: Boolean) = animationSettings.setAppOpenAnimationEnabled(enabled)
    suspend fun setAppCloseAnimationEnabled(enabled: Boolean) = animationSettings.setAppCloseAnimationEnabled(enabled)
    suspend fun setMenuAnimationEnabled(enabled: Boolean) = animationSettings.setMenuAnimationEnabled(enabled)
    suspend fun setFavoritesAnimationEnabled(enabled: Boolean) =
        animationSettings.setFavoritesAnimationEnabled(enabled)
    suspend fun setAnimationSpeed(speed: Float) = animationSettings.setAnimationSpeed(speed)

    // ── Setter: Dynamic Island & Edge Lighting ───────────────────────
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

    // ── Setter: Gesten & Haptik ──────────────────────────────────────
    suspend fun setShakeGesturesEnabled(enabled: Boolean) = gestureSettings.setShakeGesturesEnabled(enabled)
    suspend fun setDoubleShakeAction(action: GestureAction) = gestureSettings.setDoubleShakeAction(action)
    suspend fun setShakeOpenAppPackage(packageName: String?) = gestureSettings.setShakeOpenAppPackage(packageName)
    suspend fun setDoubleTapAction(action: GestureAction) = gestureSettings.setDoubleTapAction(action)
    suspend fun setDoubleTapAppPackage(packageName: String?) = gestureSettings.setDoubleTapAppPackage(packageName)

    /** Toggles haptic feedback both internally and in system settings if permission is granted. */
    suspend fun setHapticFeedbackEnabled(enabled: Boolean) = gestureSettings.setHapticFeedbackEnabled(enabled)
}
