package com.example.androidlauncher.ui.home

import com.example.androidlauncher.data.FolderInfo
import com.example.androidlauncher.data.GestureAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reducer-Tests für den Navigations-/Overlay-Zustand des Startbildschirms.
 * Deckt insbesondere die Back-Prioritätsreihenfolge ab, die zuvor als
 * 19-Zweige-when in der MainActivity lebte (A2-Split).
 */
class HomeViewModelTest {

    private val vm = HomeViewModel()
    private val state get() = vm.uiState.value
    private val folder = FolderInfo(id = "f1", name = "Tools", appPackageNames = listOf("com.a"))

    // --- Overlay-Wechsel ---

    @Test
    fun `opening an overlay replaces the previous one`() {
        vm.openOverlay(ActiveOverlay.ColorConfig)
        vm.openOverlay(ActiveOverlay.SizeConfig)

        assertEquals(ActiveOverlay.SizeConfig, state.activeOverlay)

        vm.closeOverlay()
        assertEquals(ActiveOverlay.None, state.activeOverlay)
    }

    @Test
    fun `opening a child while a parent overlay is open nests it`() {
        // EditConfig (Eltern) → IconConfig (Kind)
        vm.openOverlay(ActiveOverlay.EditConfig)
        vm.openOverlay(ActiveOverlay.IconConfig)

        assertEquals(ActiveOverlay.IconConfig, state.activeOverlay)
        assertEquals(listOf(ActiveOverlay.EditConfig), state.overlayBackStack)
    }

    @Test
    fun `back from a nested child returns to the parent then home`() {
        vm.openOverlay(ActiveOverlay.EditConfig)
        vm.openOverlay(ActiveOverlay.IconConfig)

        // Erste Zurück-Geste: zurück zum Elternmenü, nicht zur Startseite.
        vm.onBack()
        assertEquals(ActiveOverlay.EditConfig, state.activeOverlay)
        assertTrue(state.overlayBackStack.isEmpty())

        // Zweite Zurück-Geste: jetzt zur Startseite.
        vm.onBack()
        assertEquals(ActiveOverlay.None, state.activeOverlay)
    }

    @Test
    fun `back walks a three level hub chain one page at a time`() {
        // Hub → Kategorie-Seite → Untermenü (z. B. Bearbeiten → Aussehen → Farben).
        vm.openOverlay(ActiveOverlay.EditConfig)
        vm.openOverlay(ActiveOverlay.AppearanceSettings)
        vm.openOverlay(ActiveOverlay.ColorConfig)

        assertEquals(ActiveOverlay.ColorConfig, state.activeOverlay)
        assertEquals(
            listOf(ActiveOverlay.EditConfig, ActiveOverlay.AppearanceSettings),
            state.overlayBackStack
        )

        vm.onBack()
        assertEquals(ActiveOverlay.AppearanceSettings, state.activeOverlay)

        vm.onBack()
        assertEquals(ActiveOverlay.EditConfig, state.activeOverlay)
        assertTrue(state.overlayBackStack.isEmpty())

        vm.onBack()
        assertEquals(ActiveOverlay.None, state.activeOverlay)
    }

    @Test
    fun `close from a three level hub chain goes straight home`() {
        vm.openOverlay(ActiveOverlay.EditConfig)
        vm.openOverlay(ActiveOverlay.HomescreenSettings)
        vm.openOverlay(ActiveOverlay.FavoritesConfig)

        vm.closeOverlay()
        assertEquals(ActiveOverlay.None, state.activeOverlay)
        assertTrue(state.overlayBackStack.isEmpty())
    }

    @Test
    fun `palette deep link into a category child keeps back stack shallow`() {
        // Palette-Shortcut öffnet ColorConfig direkt (leerer Stack): Back geht zur
        // Startseite, nicht zum Hub – der gewohnte Shortcut-Vertrag bleibt erhalten.
        vm.openOverlay(ActiveOverlay.ColorConfig)

        vm.onBack()
        assertEquals(ActiveOverlay.None, state.activeOverlay)
    }

    @Test
    fun `close from a nested child goes straight home clearing the stack`() {
        vm.openOverlay(ActiveOverlay.EditConfig)
        vm.openOverlay(ActiveOverlay.IconConfig)

        // ✕ / Runterwischen schließt die gesamte Kette.
        vm.closeOverlay()
        assertEquals(ActiveOverlay.None, state.activeOverlay)
        assertTrue(state.overlayBackStack.isEmpty())
    }

    @Test
    fun `opening the drawer closes palette search edit mode and overlay`() {
        vm.setSettingsOpen(true)
        vm.openOverlay(ActiveOverlay.ColorConfig)
        vm.setHomeEditMode(true)

        vm.setDrawerOpen(true)

        assertTrue(state.isDrawerOpen)
        assertFalse(state.isSettingsOpen)
        assertFalse(state.isSearchOpen)
        assertFalse(state.isHomeEditMode)
        assertEquals(ActiveOverlay.None, state.activeOverlay)
    }

    @Test
    fun `closing the drawer leaves the rest untouched`() {
        vm.setDrawerOpen(true)
        vm.setDrawerOpen(false)

        assertFalse(state.isDrawerOpen)
    }

    // --- Back-Priorität ---

    @Test
    fun `back closes folder config before everything else`() {
        vm.setHomeEditMode(true)
        vm.openOverlay(ActiveOverlay.FolderConfig(folder))

        vm.onBack()

        assertEquals(ActiveOverlay.None, state.activeOverlay)
        // Edit-Modus bleibt für den nächsten Back-Schritt bestehen.
        assertTrue(state.isHomeEditMode)

        vm.onBack()
        assertFalse(state.isHomeEditMode)
    }

    @Test
    fun `back closes search before inner menus`() {
        vm.setSearchOpen(true)
        vm.openOverlay(ActiveOverlay.FontSelection)

        vm.onBack()
        assertFalse(state.isSearchOpen)
        assertEquals(ActiveOverlay.FontSelection, state.activeOverlay)

        vm.onBack()
        assertEquals(ActiveOverlay.None, state.activeOverlay)
    }

    @Test
    fun `back closes inner menus before the drawer but palette menus after it`() {
        // Inneres Menü (z. B. App-Sperre) hat Vorrang vor dem Drawer …
        vm.setDrawerOpen(true)
        vm.openOverlay(ActiveOverlay.AppLock)
        vm.onBack()
        assertEquals(ActiveOverlay.None, state.activeOverlay)
        assertTrue(state.isDrawerOpen)

        // … Palette-Menüs (z. B. Farb-Konfiguration) kommen erst nach dem Drawer dran.
        vm.openOverlay(ActiveOverlay.ColorConfig)
        vm.onBack()
        assertFalse(state.isDrawerOpen)
        assertEquals(ActiveOverlay.ColorConfig, state.activeOverlay)

        vm.onBack()
        assertEquals(ActiveOverlay.None, state.activeOverlay)
    }

    @Test
    fun `back closes the widget picker before the drawer`() {
        // Der Widget-Picker ist ein inneres Menü (öffnet aus dem Einstellungs-Menü heraus).
        vm.setDrawerOpen(true)
        vm.openOverlay(ActiveOverlay.WidgetPicker)

        vm.onBack()
        assertEquals(ActiveOverlay.None, state.activeOverlay)
        assertTrue(state.isDrawerOpen)
    }

    @Test
    fun `back on empty state changes nothing and modal detection matches`() {
        assertFalse(state.hasModalSurface)
        vm.onBack()
        assertEquals(HomeUiState(), state)

        vm.openOverlay(ActiveOverlay.Info)
        assertTrue(state.hasModalSurface)
    }

    // --- Permission-Prompts ---

    @Test
    fun `usage access prompt shows only once per session`() {
        vm.requestUsageAccessPromptOnce()
        assertTrue(state.showUsageAccessPrompt)

        vm.dismissUsageAccessPrompt()
        assertFalse(state.showUsageAccessPrompt)

        // Zweite Anforderung wird ignoriert (bereits gezeigt).
        vm.requestUsageAccessPromptOnce()
        assertFalse(state.showUsageAccessPrompt)
    }

    @Test
    fun `pending settings highlight is consumed exactly once`() {
        vm.setPendingSettingsHighlight("wallpaper")

        assertEquals("wallpaper", vm.consumePendingSettingsHighlight())
        assertNull(vm.consumePendingSettingsHighlight())
    }

    @Test
    fun `pending shake action is consumed exactly once`() {
        vm.setPendingPermissionShakeAction(GestureAction.FLASHLIGHT)

        assertEquals(GestureAction.FLASHLIGHT, vm.consumePendingPermissionShakeAction())
        assertNull(vm.consumePendingPermissionShakeAction())
        assertNull(state.pendingPermissionShakeAction)
    }
}
