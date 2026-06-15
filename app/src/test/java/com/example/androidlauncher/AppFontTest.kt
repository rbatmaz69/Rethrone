package com.example.androidlauncher

import com.example.androidlauncher.data.AppFont
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFontTest {

    @Test
    fun `there are many fonts available`() {
        assertTrue("Erwartet viele Schriftarten", AppFont.entries.size > 30)
    }

    @Test
    fun `all fonts have non-blank labels`() {
        AppFont.entries.forEach { font ->
            assertTrue("${font.name} hat ein leeres Label", font.label.isNotBlank())
        }
    }

    @Test
    fun `all font labels are distinct`() {
        val labels = AppFont.entries.map { it.label }
        assertTrue("Doppelte Schrift-Labels", labels.size == labels.toSet().size)
    }
}
