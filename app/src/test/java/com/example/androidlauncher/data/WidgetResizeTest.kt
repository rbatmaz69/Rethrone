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

    // --- U3: Pinch-Resize (kumulierter Zoom-Faktor) + Anschlag-Info fuer Haptik/Chip ---

    @Test
    fun `resolveResizeZoom scales both axes from the start size`() {
        val limits = WidgetResizeLimits(100, 400, 60, 300)
        val start = WidgetSizeDp(200, 100)

        val result = resolveResizeZoom(start, 1.5f, limits)

        assertEquals(WidgetSizeDp(300, 150), result.size)
        assertFalse(result.clampedWidth)
        assertFalse(result.clampedHeight)
    }

    @Test
    fun `resolveResizeZoom with factor one keeps the size`() {
        val limits = WidgetResizeLimits(100, 400, 60, 300)
        val start = WidgetSizeDp(180, 110)

        assertEquals(start, resolveResizeZoom(start, 1f, limits).size)
    }

    @Test
    fun `resolveResizeZoom accumulates from the start size without rounding drift`() {
        val limits = WidgetResizeLimits(100, 400, 60, 300)
        val start = WidgetSizeDp(180, 110)

        // Kumulierter Faktor statt Delta-Produkt: 1.001 bewegt (noch) fast nichts …
        assertEquals(WidgetSizeDp(180, 110), resolveResizeZoom(start, 1.001f, limits).size)
        // … 1.1 landet gerundet bei 198×121.
        assertEquals(WidgetSizeDp(198, 121), resolveResizeZoom(start, 1.1f, limits).size)
    }

    @Test
    fun `zooming past the max reports the free axes as clamped`() {
        val limits = WidgetResizeLimits(100, 320, 60, 200)
        val start = WidgetSizeDp(180, 110)

        val result = resolveResizeZoom(start, 3f, limits)

        assertEquals(WidgetSizeDp(320, 200), result.size)
        assertTrue(result.clampedWidth)
        assertTrue(result.clampedHeight)
    }

    @Test
    fun `zooming below the min reports the free axes as clamped`() {
        val limits = WidgetResizeLimits(100, 320, 60, 200)
        val start = WidgetSizeDp(180, 110)

        val result = resolveResizeZoom(start, 0.3f, limits)

        assertEquals(WidgetSizeDp(100, 60), result.size)
        assertTrue(result.clampedWidth)
        assertTrue(result.clampedHeight)
    }

    @Test
    fun `locked axis stays fixed and never reports clamped`() {
        // Sonst wuerde jede Pinch-Geste an einem nur-vertikal skalierbaren
        // Widget dauerhaft die Anschlag-Haptik ausloesen.
        val limits = WidgetResizeLimits(180, 180, 60, 300)
        val start = WidgetSizeDp(180, 110)

        val result = resolveResizeZoom(start, 2f, limits)

        assertEquals(WidgetSizeDp(180, 220), result.size)
        assertFalse(result.clampedWidth)
        assertFalse(result.clampedHeight)
    }

    @Test
    fun `landing exactly on the limit does not count as clamped`() {
        // Erst der Versuch, DARUEBER hinaus zu zoomen, meldet den Anschlag – die
        // Haptik feuert damit genau auf der false-zu-true-Flanke an der Wand.
        val limits = WidgetResizeLimits(100, 400, 60, 220)
        val start = WidgetSizeDp(200, 110)

        val result = resolveResizeZoom(start, 2f, limits)

        assertEquals(WidgetSizeDp(400, 220), result.size)
        assertFalse(result.clampedWidth)
        assertFalse(result.clampedHeight)
    }

    @Test
    fun `degenerate zoom factors are floored instead of collapsing the widget`() {
        val limits = WidgetResizeLimits(100, 400, 60, 300)
        val start = WidgetSizeDp(180, 110)

        val zero = resolveResizeZoom(start, 0f, limits)
        val negative = resolveResizeZoom(start, -1f, limits)

        assertEquals(WidgetSizeDp(100, 60), zero.size)
        assertEquals(zero.size, negative.size)
    }
}
