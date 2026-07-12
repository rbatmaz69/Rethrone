package com.example.androidlauncher.ui.home

import com.example.androidlauncher.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests für den Einstellungs-Such-Index: Konsistenz der Registry und das
 * Matching der puren [filterSettings]-Funktion (Label, Synonyme, Kategorie).
 */
class SettingsSearchRegistryTest {

    private val entries = SettingsSearchRegistry.entries

    // Fester Fake-Resolver statt Android-Ressourcen: nur die in den Tests
    // verwendeten Ids brauchen echte Texte, alles andere bekommt Platzhalter.
    private val translations = mapOf(
        R.string.change_wallpaper to "Wallpaper ändern",
        R.string.search_kw_wallpaper to "Hintergrund,Bild,Foto",
        R.string.section_appearance to "Aussehen",
        R.string.clock_widget to "Uhr-Widget",
        R.string.section_system to "System",
        R.string.haptic_feedback to "Haptisches Feedback",
        R.string.search_kw_haptics to "Vibration",
    )

    private fun resolve(res: Int): String = translations[res] ?: "res_$res"

    @Test
    fun `registry ids are unique`() {
        val ids = entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every entry has a non empty overlay path`() {
        assertTrue(entries.all { it.path.isNotEmpty() })
    }

    @Test
    fun `every entry path starts at a hub category or root page`() {
        val validRoots = setOf<ActiveOverlay>(
            ActiveOverlay.AppearanceSettings,
            ActiveOverlay.HomescreenSettings,
            ActiveOverlay.AppsSettings,
            ActiveOverlay.SearchSettings,
            ActiveOverlay.SystemSettings,
            ActiveOverlay.GesturesConfig,
        )
        assertTrue(entries.all { it.path.first() in validRoots })
    }

    @Test
    fun `blank query returns no results`() {
        assertTrue(filterSettings(entries, "", ::resolve).isEmpty())
        assertTrue(filterSettings(entries, "   ", ::resolve).isEmpty())
    }

    @Test
    fun `query matches label case insensitively`() {
        val results = filterSettings(entries, "wALLpaper", ::resolve)
        assertTrue(results.any { it.id == "wallpaper" })
    }

    @Test
    fun `query matches localized keyword synonyms`() {
        // „Hintergrund" steht nur in den Synonymen, nicht im Label „Wallpaper ändern".
        val results = filterSettings(entries, "hintergrund", ::resolve)
        assertTrue(results.any { it.id == "wallpaper" })

        val vibration = filterSettings(entries, "vibration", ::resolve)
        assertTrue(vibration.any { it.id == "haptics" })
    }

    @Test
    fun `query matches the category name`() {
        // Alle System-Einträge tauchen auf, wenn nach der Kategorie gesucht wird.
        val results = filterSettings(entries, "system", ::resolve)
        assertTrue(results.any { it.id == "backup_export" })
        assertTrue(results.any { it.id == "info" })
    }

    @Test
    fun `unrelated query returns nothing`() {
        assertTrue(filterSettings(entries, "zzz-kein-treffer", ::resolve).isEmpty())
    }
}
