package com.example.androidlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetResizeTest {

    @Suppress("LongParameterList") // spiegelt bewusst die Parameterliste von resolveResizeLimits
    private fun limits(
        resizeMode: Int,
        minResizeWidthDp: Int = 100,
        minResizeHeightDp: Int = 60,
        maxResizeWidthDp: Int = 0,
        maxResizeHeightDp: Int = 0,
        currentWidthDp: Int = 180,
        currentHeightDp: Int = 110,
        screenWidthDp: Int = 400,
        screenHeightDp: Int = 800,
    ) = resolveResizeLimits(
        resizeMode = resizeMode,
        minResizeWidthDp = minResizeWidthDp,
        minResizeHeightDp = minResizeHeightDp,
        maxResizeWidthDp = maxResizeWidthDp,
        maxResizeHeightDp = maxResizeHeightDp,
        currentWidthDp = currentWidthDp,
        currentHeightDp = currentHeightDp,
        screenWidthDp = screenWidthDp,
        screenHeightDp = screenHeightDp,
    )

    @Test
    fun `resize none locks both axes to the current size`() {
        val result = limits(resizeMode = 0)

        assertEquals(WidgetResizeLimits(180, 180, 110, 110), result)
        assertFalse(result.isResizable)
    }

    @Test
    fun `horizontal only locks the vertical axis`() {
        val result = limits(resizeMode = WIDGET_RESIZE_HORIZONTAL)

        assertEquals(100, result.minWidthDp)
        assertEquals(400 - SCREEN_EDGE_MARGIN_DP, result.maxWidthDp)
        assertEquals(110, result.minHeightDp)
        assertEquals(110, result.maxHeightDp)
        assertTrue(result.isResizable)
    }

    @Test
    fun `vertical only locks the horizontal axis`() {
        val result = limits(resizeMode = WIDGET_RESIZE_VERTICAL)

        assertEquals(180, result.minWidthDp)
        assertEquals(180, result.maxWidthDp)
        assertEquals(60, result.minHeightDp)
        assertEquals((800 * MAX_HEIGHT_SCREEN_FRACTION).toInt(), result.maxHeightDp)
    }

    @Test
    fun `both axes use provider max when set and smaller than screen cap`() {
        val result = limits(
            resizeMode = WIDGET_RESIZE_HORIZONTAL or WIDGET_RESIZE_VERTICAL,
            maxResizeWidthDp = 300,
            maxResizeHeightDp = 200,
        )

        assertEquals(WidgetResizeLimits(100, 300, 60, 200), result)
    }

    @Test
    fun `unset provider max of zero falls back to the screen cap`() {
        val result = limits(resizeMode = WIDGET_RESIZE_HORIZONTAL or WIDGET_RESIZE_VERTICAL)

        assertEquals(400 - SCREEN_EDGE_MARGIN_DP, result.maxWidthDp)
        assertEquals((800 * MAX_HEIGHT_SCREEN_FRACTION).toInt(), result.maxHeightDp)
    }

    @Test
    fun `provider min of zero falls back to the app floors`() {
        val result = limits(
            resizeMode = WIDGET_RESIZE_HORIZONTAL or WIDGET_RESIZE_VERTICAL,
            minResizeWidthDp = 0,
            minResizeHeightDp = 0,
        )

        assertEquals(MIN_WIDGET_WIDTH_DP, result.minWidthDp)
        assertEquals(MIN_WIDGET_HEIGHT_DP, result.minHeightDp)
    }

    @Test
    fun `degenerate provider min above screen cap pins the axis to the min`() {
        val result = limits(
            resizeMode = WIDGET_RESIZE_HORIZONTAL,
            minResizeWidthDp = 900,
            screenWidthDp = 400,
        )

        assertEquals(900, result.minWidthDp)
        assertEquals(900, result.maxWidthDp)
    }

    @Test
    fun `applyResizeDrag clamps to the limits`() {
        val limits = WidgetResizeLimits(100, 320, 60, 200)
        val start = WidgetSizeDp(180, 110)

        assertEquals(WidgetSizeDp(320, 60), applyResizeDrag(start, 500f, -500f, limits))
        assertEquals(WidgetSizeDp(100, 200), applyResizeDrag(start, -500f, 500f, limits))
    }

    @Test
    fun `applyResizeDrag accumulates from the start size without rounding drift`() {
        val limits = WidgetResizeLimits(100, 400, 60, 300)
        val start = WidgetSizeDp(180, 110)

        // Kumulierter Drag statt Delta-Summe: 0.4dp bewegt (noch) nichts …
        assertEquals(start, applyResizeDrag(start, 0.4f, 0.4f, limits))
        // … 30.6dp landet gerundet bei +31/+31.
        assertEquals(WidgetSizeDp(211, 141), applyResizeDrag(start, 30.6f, 30.6f, limits))
    }

    @Test
    fun `locked axis is a no-op even with drag input`() {
        val limits = WidgetResizeLimits(180, 180, 60, 300)
        val start = WidgetSizeDp(180, 110)

        assertEquals(WidgetSizeDp(180, 210), applyResizeDrag(start, 999f, 100f, limits))
    }

    // --- U3: Anschlag-Information fuer Haptik/Chip-Puls ---

    @Test
    fun `resolveResizeDrag within the range reports no clamping`() {
        val limits = WidgetResizeLimits(100, 320, 60, 200)
        val start = WidgetSizeDp(180, 110)

        val result = resolveResizeDrag(start, 20f, 20f, limits)

        assertEquals(WidgetSizeDp(200, 130), result.size)
        assertFalse(result.clampedWidth)
        assertFalse(result.clampedHeight)
    }

    @Test
    fun `resolveResizeDrag past max width reports width clamped`() {
        val limits = WidgetResizeLimits(100, 320, 60, 200)
        val start = WidgetSizeDp(180, 110)

        val result = resolveResizeDrag(start, 500f, 0f, limits)

        assertEquals(WidgetSizeDp(320, 110), result.size)
        assertTrue(result.clampedWidth)
        assertFalse(result.clampedHeight)
    }

    @Test
    fun `resolveResizeDrag below min height reports height clamped`() {
        val limits = WidgetResizeLimits(100, 320, 60, 200)
        val start = WidgetSizeDp(180, 110)

        val result = resolveResizeDrag(start, 0f, -500f, limits)

        assertEquals(WidgetSizeDp(180, 60), result.size)
        assertFalse(result.clampedWidth)
        assertTrue(result.clampedHeight)
    }

    @Test
    fun `locked axis never reports clamped even when pushed hard`() {
        // Sonst wuerde jeder Diagonal-Drag an einem nur-vertikal skalierbaren
        // Widget dauerhaft die Anschlag-Haptik ausloesen.
        val limits = WidgetResizeLimits(180, 180, 60, 300)
        val start = WidgetSizeDp(180, 110)

        val result = resolveResizeDrag(start, 999f, 100f, limits)

        assertEquals(WidgetSizeDp(180, 210), result.size)
        assertFalse(result.clampedWidth)
        assertFalse(result.clampedHeight)
    }

    @Test
    fun `landing exactly on the limit does not count as clamped`() {
        // Erst der Versuch, DARUEBER hinaus zu ziehen, meldet den Anschlag – die
        // Haptik feuert damit genau auf der false-zu-true-Flanke an der Wand.
        val limits = WidgetResizeLimits(100, 320, 60, 200)
        val start = WidgetSizeDp(180, 110)

        val result = resolveResizeDrag(start, 140f, 90f, limits)

        assertEquals(WidgetSizeDp(320, 200), result.size)
        assertFalse(result.clampedWidth)
        assertFalse(result.clampedHeight)
    }
}
