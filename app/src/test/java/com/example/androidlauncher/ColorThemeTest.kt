package com.example.androidlauncher

import com.example.androidlauncher.ui.theme.ColorTheme
import org.junit.Assert.*
import org.junit.Test

class ColorThemeTest {

    @Test
    fun `all color themes have non-blank themeName`() {
        ColorTheme.entries.forEach { theme ->
            assertTrue(
                "Theme ${theme.name} has blank themeName",
                theme.themeName.isNotBlank()
            )
        }
    }

    @Test
    fun `total number of color themes is 16`() {
        assertEquals(16, ColorTheme.entries.size)
    }

    @Test
    fun `valueOf works for all theme entries`() {
        ColorTheme.entries.forEach { theme ->
            assertEquals(theme, ColorTheme.valueOf(theme.name))
        }
    }

    @Test
    fun `SIGNATURE is the first theme`() {
        assertEquals(ColorTheme.SIGNATURE, ColorTheme.entries.first())
    }

    @Test
    fun `all themes have distinct themeNames`() {
        val names = ColorTheme.entries.map { it.themeName }
        assertEquals(names.size, names.toSet().size)
    }


    @Test
    fun `SUNSET tertiary is Black`() {
        val sunset = ColorTheme.SUNSET
        assertEquals("Sunset", sunset.themeName)
        assertEquals(0xFF000000.toInt(), sunset.tertiary.hashCode())
    }

    @Test
    fun `SUNSHINE tertiary is Black`() {
        assertEquals(0xFF000000.toInt(), ColorTheme.SUNSHINE.tertiary.hashCode())
    }

    @Test
    fun `all drawerBackground colors have full alpha`() {
        ColorTheme.entries.forEach { theme ->
            val alpha = theme.drawerBackground.alpha
            assertEquals(
                "Theme ${theme.name} drawerBackground should be fully opaque",
                1.0f, alpha, 0.001f
            )
        }
    }

    @Test
    fun `all primary colors have full alpha`() {
        ColorTheme.entries.forEach { theme ->
            assertEquals(
                "Theme ${theme.name} primary should be fully opaque",
                1.0f, theme.primary.alpha, 0.001f
            )
        }
    }

    @Test
    fun `all secondary colors have full alpha`() {
        ColorTheme.entries.forEach { theme ->
            assertEquals(
                "Theme ${theme.name} secondary should be fully opaque",
                1.0f, theme.secondary.alpha, 0.001f
            )
        }
    }

    @Test
    fun `all tertiary colors have full alpha`() {
        ColorTheme.entries.forEach { theme ->
            assertEquals(
                "Theme ${theme.name} tertiary should be fully opaque",
                1.0f, theme.tertiary.alpha, 0.001f
            )
        }
    }

    @Test
    fun `valueOf throws for unknown name`() {
        try {
            ColorTheme.valueOf("NONEXISTENT_THEME")
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `expected theme names exist`() {
        val expectedNames = listOf(
            "SIGNATURE", "OCEAN", "FOREST", "SUNSET",
            "LAVENDER", "SAKURA", "NIGHTSKY", "MINT", "SUNSHINE",
            "SKY", "PEACH", "CANDY", "LEMONADE", "BUBBLEGUM",
            "TROPICAL", "SPRING"
        )
        val actualNames = ColorTheme.entries.map { it.name }
        expectedNames.forEach { name ->
            assertTrue("Expected theme $name to exist", name in actualNames)
        }
    }
}
