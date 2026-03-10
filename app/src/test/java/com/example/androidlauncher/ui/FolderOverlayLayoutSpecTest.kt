package com.example.androidlauncher.ui

import com.example.androidlauncher.data.IconSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderOverlayLayoutSpecTest {

    @Test
    fun `compact phones use tighter folder overlay spacing`() {
        val spec = calculateFolderOverlayLayoutSpec(
            screenWidthDp = 340,
            screenHeightDp = 720,
            iconSize = IconSize.STANDARD
        )

        assertEquals(0.94f, spec.widthFraction, 0.001f)
        assertEquals(16, spec.horizontalPaddingDp)
        assertEquals(10, spec.titleGridSpacingDp)
        assertEquals(12, spec.gridSpacingDp)
        assertTrue(spec.gridHeightDp <= 312)
    }

    @Test
    fun `larger icons reserve more room than smaller icons`() {
        val small = calculateFolderOverlayLayoutSpec(
            screenWidthDp = 411,
            screenHeightDp = 891,
            iconSize = IconSize.SMALL
        )
        val large = calculateFolderOverlayLayoutSpec(
            screenWidthDp = 411,
            screenHeightDp = 891,
            iconSize = IconSize.LARGE
        )

        assertTrue(large.gridHeightDp > small.gridHeightDp)
        assertTrue(large.gridSpacingDp > small.gridSpacingDp)
        assertTrue(large.maxWidthDp > small.maxWidthDp)
    }

    @Test
    fun `expanded screens keep overlay bounded but breathable`() {
        val spec = calculateFolderOverlayLayoutSpec(
            screenWidthDp = 800,
            screenHeightDp = 1280,
            iconSize = IconSize.STANDARD
        )

        assertEquals(0.68f, spec.widthFraction, 0.001f)
        assertEquals(320, spec.minWidthDp)
        assertEquals(24, spec.horizontalPaddingDp)
        assertEquals(10, spec.gridHorizontalPaddingDp)
        assertTrue(spec.gridHeightDp in 238..390)
    }
}

