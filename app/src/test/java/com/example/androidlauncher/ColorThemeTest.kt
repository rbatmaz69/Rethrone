package com.example.androidlauncher

import com.example.androidlauncher.ui.theme.ColorTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ColorThemeTest {

    @Test
    fun `all color themes have non-blank themeName`() {
        ColorTheme.entries.forEach { theme ->
            assertTrue("Theme ${theme.name} has blank themeName", theme.themeName.isNotBlank())
        }
    }

    @Test
    fun `total number of color themes is 35`() {
        assertEquals(35, ColorTheme.entries.size)
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
    fun `all drawerBackground colors have full alpha`() {
        ColorTheme.entries.forEach { theme ->
            assertEquals("Theme ${theme.name} drawerBackground should be fully opaque", 1.0f, theme.drawerBackground.alpha, 0.001f)
        }
    }

    @Test
    fun `all primary secondary and tertiary colors have full alpha`() {
        ColorTheme.entries.forEach { theme ->
            assertEquals("Theme ${theme.name} primary should be fully opaque", 1.0f, theme.primary.alpha, 0.001f)
            assertEquals("Theme ${theme.name} secondary should be fully opaque", 1.0f, theme.secondary.alpha, 0.001f)
            assertEquals("Theme ${theme.name} tertiary should be fully opaque", 1.0f, theme.tertiary.alpha, 0.001f)
        }
    }

    @Test
    fun `art themes expose at least three gradient stops for all themed brushes`() {
        ColorTheme.entries.filter { it.isArtTheme }.forEach { theme ->
            assertTrue("${theme.name} artGradient should have >= 3 stops", theme.artGradient.size >= 3)
            assertTrue("${theme.name} menuGradient should have >= 3 stops", theme.menuGradient.size >= 3)
            assertTrue("${theme.name} searchGradient should have >= 3 stops", theme.searchGradient.size >= 3)
            assertTrue("${theme.name} animationGradient should have >= 3 stops", theme.animationGradient.size >= 3)
        }
    }

    @Test
    fun `all themes pass contrast checks for dark and light text modes`() {
        ColorTheme.entries.forEach { theme ->
            assertTrue("${theme.name} should keep readable contrast in dark mode", theme.passesContrastForMainText(false))
            assertTrue("${theme.name} should keep readable contrast in light mode", theme.passesContrastForMainText(true))
        }
    }

    @Test
    fun `new artistic theme names exist`() {
        val actualNames = ColorTheme.entries.map { it.name }
        listOf(
            "MOUNTAIN_DUSK",
            "DESERT_GLOW",
            "ARCTIC_MIST",
            "OCEAN_DEPTHS",
            "MISTY_VALLEY",
            "AURORA_VEIL"
        ).forEach { name ->
            assertTrue("Expected theme $name to exist", name in actualNames)
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
}
