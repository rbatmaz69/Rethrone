package com.example.androidlauncher.ui.home

import com.example.androidlauncher.data.FolderInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Absicherung des A2-Umbaus: genau ein Overlay-Zustand statt 17 Booleans.
 * Die Tests decken die Zustands-Semantik ab, auf die sich die MainActivity
 * verlässt (Gleichheit von FolderConfig, Unterscheidbarkeit der Objekte).
 */
class ActiveOverlayTest {

    private val folder = FolderInfo(id = "f1", name = "Tools", appPackageNames = listOf("com.a"))

    @Test
    fun `folder config carries its folder and compares by value`() {
        val a = ActiveOverlay.FolderConfig(folder)
        val b = ActiveOverlay.FolderConfig(folder.copy())

        assertEquals(a, b)
        assertEquals(folder, a.folder)
        assertNotEquals(a, ActiveOverlay.FolderConfig(folder.copy(id = "f2")))
    }

    @Test
    fun `overlays are mutually exclusive by construction`() {
        // Ein einzelner Zustand kann immer nur eine Auspraegung haben – die
        // frueheren Bool-Kombinationen (z. B. zwei Menues gleichzeitig) sind
        // per Typsystem ausgeschlossen.
        var state: ActiveOverlay = ActiveOverlay.ColorConfig
        state = ActiveOverlay.SizeConfig

        assertTrue(state is ActiveOverlay.SizeConfig)
        assertNotEquals(ActiveOverlay.ColorConfig as ActiveOverlay, state as ActiveOverlay)
    }

    @Test
    fun `folder extraction mirrors the old selectedFolderForConfig semantics`() {
        val open: ActiveOverlay = ActiveOverlay.FolderConfig(folder)
        val closed: ActiveOverlay = ActiveOverlay.None

        assertEquals(folder, (open as? ActiveOverlay.FolderConfig)?.folder)
        assertNull((closed as? ActiveOverlay.FolderConfig)?.folder)
    }
}
