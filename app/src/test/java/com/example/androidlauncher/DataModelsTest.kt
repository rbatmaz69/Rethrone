package com.example.androidlauncher

import com.example.androidlauncher.data.FontSize
import com.example.androidlauncher.data.IconSize
import org.junit.Assert.assertEquals
import org.junit.Test
import androidx.compose.ui.unit.dp

class DataModelsTest {

    @Test
    fun `font sizes expose expected labels and scales`() {
        assertEquals("Klein", FontSize.SMALL.label)
        assertEquals(0.85f, FontSize.SMALL.scale, 0.0001f)

        assertEquals("Standard", FontSize.STANDARD.label)
        assertEquals(1.0f, FontSize.STANDARD.scale, 0.0001f)

        assertEquals("Groß", FontSize.LARGE.label)
        assertEquals(1.2f, FontSize.LARGE.scale, 0.0001f)
    }

    @Test
    fun `icon sizes expose expected labels and values`() {
        assertEquals("Klein", IconSize.SMALL.label)
        assertEquals(40.dp, IconSize.SMALL.size)

        assertEquals("Standard", IconSize.STANDARD.label)
        assertEquals(48.dp, IconSize.STANDARD.size)

        assertEquals("Groß", IconSize.LARGE.label)
        assertEquals(56.dp, IconSize.LARGE.size)
    }
}

