package com.example.androidlauncher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidlauncher.data.AppAccessMode
import com.example.androidlauncher.data.HomeLayout
import com.example.androidlauncher.data.IslandAnimationStyle
import com.example.androidlauncher.data.SearchSuggestionsManager
import com.example.androidlauncher.data.settings.AnimationSettings
import com.example.androidlauncher.data.settings.GestureSettings
import com.example.androidlauncher.data.settings.HomeLayoutSettings
import com.example.androidlauncher.data.settings.IslandAndEdgeSettings
import com.example.androidlauncher.data.settings.PrivacySettings
import com.example.androidlauncher.data.settings.WallpaperSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

/**
 * System-Effekte des Einstellungs-Menüs, die nur die Activity ausführen kann
 * (ActivityResult-Launcher). Bewusst schmal gehalten – Muster: `GestureActionEffects`.
 */
interface EditConfigActions {
    /** Öffnet den System-Dialog „Standard-Launcher festlegen" (ROLE_HOME). */
    fun openDefaultLauncherPrompt()

    /** Öffnet den Foto-Picker für ein eigenes Wallpaper (Ergebnis → Crop-Flow der Activity). */
    fun pickWallpaper()

    /** Öffnet den SAF-Dialog zum Speichern einer Backup-Datei (B5). */
    fun exportBackup()

    /** Öffnet den SAF-Dialog zum Einlesen einer Backup-Datei (B5). */
    fun importBackup()
}

/**
 * Versorgt das Einstellungs-Menü (`EditConfigMenu`) direkt aus den A1-Settings-Stores
 * (A8-Split: ersetzt die frühere 37-Parameter-Callback-Verdrahtung über die MainActivity).
 * Die Overlay-Navigation läuft separat über das [HomeViewModel].
 */
@HiltViewModel
class EditConfigViewModel @Inject constructor(
    private val homeLayoutSettings: HomeLayoutSettings,
    private val wallpaperSettings: WallpaperSettings,
    private val animationSettings: AnimationSettings,
    private val islandAndEdgeSettings: IslandAndEdgeSettings,
    private val gestureSettings: GestureSettings,
    private val privacySettings: PrivacySettings,
    private val searchSuggestionsManager: SearchSuggestionsManager,
) : ViewModel() {

    val isSmartSuggestionsEnabled: Flow<Boolean> = homeLayoutSettings.isSmartSuggestionsEnabled
    val isWeatherWidgetEnabled: Flow<Boolean> = homeLayoutSettings.isWeatherWidgetEnabled
    val isClockWidgetEnabled: Flow<Boolean> = homeLayoutSettings.isClockWidgetEnabled
    val isCalendarWidgetEnabled: Flow<Boolean> = homeLayoutSettings.isCalendarWidgetEnabled
    val isAnimationsEnabled: Flow<Boolean> = animationSettings.isAnimationsEnabled
    val isDynamicIslandEnabled: Flow<Boolean> = islandAndEdgeSettings.isDynamicIslandEnabled
    val islandAnimationStyle: Flow<IslandAnimationStyle> = islandAndEdgeSettings.islandAnimationStyle
    val isEdgeLightingEnabled: Flow<Boolean> = islandAndEdgeSettings.isEdgeLightingEnabled
    val appAccessMode: Flow<AppAccessMode> = privacySettings.appAccessMode
    val isHapticFeedbackEnabled: Flow<Boolean> = gestureSettings.isHapticFeedbackEnabled

    /** Ob mindestens ein Startbildschirm-Element manuell verschoben wurde (→ Reset anbieten). */
    val isCustomHomeLayoutSet: Flow<Boolean> = homeLayoutSettings.homeLayout.map(::hasCustomOffsets)

    /** Ob ein eigenes Wallpaper gesetzt ist (→ Reset anbieten). */
    val isCustomWallpaperSet: Flow<Boolean> = wallpaperSettings.customWallpaperUri.map { it != null }

    fun setSmartSuggestionsEnabled(enabled: Boolean) {
        viewModelScope.launch { homeLayoutSettings.setSmartSuggestionsEnabled(enabled) }
    }

    fun setWeatherWidgetEnabled(enabled: Boolean) {
        viewModelScope.launch { homeLayoutSettings.setWeatherWidgetEnabled(enabled) }
    }

    fun setClockWidgetEnabled(enabled: Boolean) {
        viewModelScope.launch { homeLayoutSettings.setClockWidgetEnabled(enabled) }
    }

    fun setCalendarWidgetEnabled(enabled: Boolean) {
        viewModelScope.launch { homeLayoutSettings.setCalendarWidgetEnabled(enabled) }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch { animationSettings.setAnimationsEnabled(enabled) }
    }

    fun setDynamicIslandEnabled(enabled: Boolean) {
        viewModelScope.launch { islandAndEdgeSettings.setDynamicIslandEnabled(enabled) }
    }

    fun setIslandAnimationStyle(style: IslandAnimationStyle) {
        viewModelScope.launch { islandAndEdgeSettings.setIslandAnimationStyle(style) }
    }

    fun setAppAccessMode(mode: AppAccessMode) {
        viewModelScope.launch { privacySettings.setAppAccessMode(mode) }
    }

    /** Schreibt die Haptik-Einstellung (System-Sync übernimmt der Store, sofern erlaubt). */
    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch { gestureSettings.setHapticFeedbackEnabled(enabled) }
    }

    /** Setzt alle verschobenen Startbildschirm-Elemente auf die Standard-Positionen zurück. */
    fun resetHomeLayout() {
        viewModelScope.launch { homeLayoutSettings.setHomeLayout(HomeLayout()) }
    }

    /** Entfernt das eigene Wallpaper samt Blur/Dim/Zoom (zurück zum System-Wallpaper). */
    fun resetWallpaper() {
        viewModelScope.launch {
            wallpaperSettings.setCustomWallpaperUri(null)
            wallpaperSettings.setWallpaperBlur(0f)
            wallpaperSettings.setWallpaperDim(0.1f)
            wallpaperSettings.setWallpaperZoom(1.0f)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { searchSuggestionsManager.clearWebHistory() }
    }

    companion object {
        // Ab diesem Versatz gilt ein Element als manuell verschoben (Sub-Pixel-Rauschen ignorieren).
        private const val OFFSET_EPSILON = 0.5f

        /** Reine Logik: wurde irgendein Home-Element gegenüber dem Standard verschoben? */
        fun hasCustomOffsets(layout: HomeLayout): Boolean = listOf(
            layout.clock,
            layout.date,
            layout.weather,
            layout.favorites,
        ).any { abs(it.x) > OFFSET_EPSILON || abs(it.y) > OFFSET_EPSILON }
    }
}
