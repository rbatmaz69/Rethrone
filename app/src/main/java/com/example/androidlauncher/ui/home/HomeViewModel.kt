package com.example.androidlauncher.ui.home

import androidx.lifecycle.ViewModel
import com.example.androidlauncher.data.GestureAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * UI-Zustand des Startbildschirms: welches Overlay offen ist, ob Drawer/Suche/
 * Einstellungs-Palette/Edit-Modus aktiv sind sowie die One-Shot-Permission-Prompts.
 * Lebt im [HomeViewModel], damit der Zustand Konfigurationswechsel überlebt.
 */
data class HomeUiState(
    val activeOverlay: ActiveOverlay = ActiveOverlay.None,
    val isDrawerOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isSearchOpen: Boolean = false,
    val isHomeEditMode: Boolean = false,
    /** Einmaliger Hinweis-Dialog für die Nutzungszugriff-Berechtigung. */
    val showUsageAccessPrompt: Boolean = false,
    val hasShownUsageAccessPrompt: Boolean = false,
    /** Geste, die nach erteilter Kamera-Berechtigung ausgeführt werden soll. */
    val pendingPermissionShakeAction: GestureAction? = null,
) {
    /** Liegt irgendeine modale Fläche über dem Startbildschirm? (steuert Back-Handling) */
    val hasModalSurface: Boolean
        get() = isDrawerOpen || activeOverlay != ActiveOverlay.None || isSearchOpen || isHomeEditMode
}

/**
 * Hält den Navigations-/Overlay-Zustand des Startbildschirms (A2-Split aus der
 * MainActivity). Alle Übergänge laufen als Reducer über [HomeUiState] – dadurch
 * ist die Back-Priorität unit-testbar und es gibt genau eine autoritative
 * Quelle statt verstreuter Boolean-Writes.
 */
@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun openOverlay(overlay: ActiveOverlay) {
        _uiState.update { it.copy(activeOverlay = overlay) }
    }

    fun closeOverlay() = openOverlay(ActiveOverlay.None)

    /**
     * Öffnet/schließt den App-Drawer. Beim Öffnen schließen sich Palette, Suche,
     * Edit-Modus und ein offenes Overlay (ehemals ein LaunchedEffect in der Activity).
     */
    fun setDrawerOpen(open: Boolean) {
        _uiState.update {
            if (open) {
                it.copy(
                    isDrawerOpen = true,
                    isSettingsOpen = false,
                    isSearchOpen = false,
                    isHomeEditMode = false,
                    activeOverlay = ActiveOverlay.None,
                )
            } else {
                it.copy(isDrawerOpen = false)
            }
        }
    }

    fun setSettingsOpen(open: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = open) }
    }

    fun toggleSettings() {
        _uiState.update { it.copy(isSettingsOpen = !it.isSettingsOpen) }
    }

    fun setSearchOpen(open: Boolean) {
        _uiState.update { it.copy(isSearchOpen = open) }
    }

    fun setHomeEditMode(enabled: Boolean) {
        _uiState.update { it.copy(isHomeEditMode = enabled) }
    }

    fun toggleHomeEditMode() {
        _uiState.update { it.copy(isHomeEditMode = !it.isHomeEditMode) }
    }

    /**
     * Zurück-Geste: schließt genau eine Fläche pro Aufruf, in der historischen
     * Prioritätsreihenfolge (Ordner-Konfiguration → Edit-Modus → Suche → innere
     * Menüs → Drawer → restliche Overlays).
     */
    fun onBack() {
        _uiState.update { state ->
            val overlay = state.activeOverlay
            when {
                overlay is ActiveOverlay.FolderConfig -> state.copy(activeOverlay = ActiveOverlay.None)
                state.isHomeEditMode -> state.copy(isHomeEditMode = false)
                state.isSearchOpen -> state.copy(isSearchOpen = false)
                overlay in INNER_MENUS -> state.copy(activeOverlay = ActiveOverlay.None)
                state.isDrawerOpen -> state.copy(isDrawerOpen = false)
                overlay != ActiveOverlay.None -> state.copy(activeOverlay = ActiveOverlay.None)
                else -> state
            }
        }
    }

    /** Zeigt den Nutzungszugriff-Hinweis genau einmal pro Prozess-Sitzung. */
    fun requestUsageAccessPromptOnce() {
        _uiState.update {
            if (it.hasShownUsageAccessPrompt) {
                it
            } else {
                it.copy(showUsageAccessPrompt = true, hasShownUsageAccessPrompt = true)
            }
        }
    }

    fun dismissUsageAccessPrompt() {
        _uiState.update { it.copy(showUsageAccessPrompt = false) }
    }

    fun setPendingPermissionShakeAction(action: GestureAction?) {
        _uiState.update { it.copy(pendingPermissionShakeAction = action) }
    }

    /** Liefert die wartende Geste und setzt sie zurück (Aufruf aus dem Permission-Callback). */
    fun consumePendingPermissionShakeAction(): GestureAction? {
        val pending = _uiState.value.pendingPermissionShakeAction
        _uiState.update { it.copy(pendingPermissionShakeAction = null) }
        return pending
    }

    companion object {
        // "Innere" Menüs, die die Zurück-Geste vor dem Drawer schließt
        // (entspricht der früheren when-Reihenfolge in der MainActivity).
        private val INNER_MENUS: Set<ActiveOverlay> = setOf(
            ActiveOverlay.FontSelection,
            ActiveOverlay.WallpaperConfig,
            ActiveOverlay.UninstallApps,
            ActiveOverlay.HiddenApps,
            ActiveOverlay.AppLock,
            ActiveOverlay.IconConfig,
            ActiveOverlay.WidgetPicker,
        )
    }
}
