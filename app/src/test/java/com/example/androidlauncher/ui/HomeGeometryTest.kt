package com.example.androidlauncher.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeGeometryTest {

    private val rect = Rect(left = 10f, top = 20f, right = 110f, bottom = 220f)

    @Test
    fun `rectContains is true for a point inside`() {
        assertTrue(rectContains(rect, Offset(50f, 100f)))
    }

    @Test
    fun `rectContains includes the edges`() {
        assertTrue(rectContains(rect, Offset(10f, 20f)))
        assertTrue(rectContains(rect, Offset(110f, 220f)))
    }

    @Test
    fun `rectContains is false for a point outside`() {
        assertFalse(rectContains(rect, Offset(5f, 100f)))
        assertFalse(rectContains(rect, Offset(50f, 221f)))
    }

    @Test
    fun `intersectionArea computes the overlap area`() {
        val a = Rect(0f, 0f, 100f, 100f)
        val b = Rect(50f, 50f, 150f, 150f)
        // Ueberlappung 50x50 = 2500
        assertEquals(2500f, intersectionArea(a, b), 0.001f)
    }

    @Test
    fun `intersectionArea is zero for disjoint rects`() {
        val a = Rect(0f, 0f, 10f, 10f)
        val b = Rect(20f, 20f, 30f, 30f)
        assertEquals(0f, intersectionArea(a, b), 0.001f)
    }

    @Test
    fun `intersectionArea is zero when rects only touch at an edge`() {
        val a = Rect(0f, 0f, 10f, 10f)
        val b = Rect(10f, 0f, 20f, 10f)
        assertEquals(0f, intersectionArea(a, b), 0.001f)
    }

    // 100x50-Element bei (10,10); Container 300x400, keine Navbar/kein TopLimit.
    private val elementBounds = Rect(left = 10f, top = 10f, right = 110f, bottom = 60f)

    @Test
    fun `clampOffsetToBounds leaves an in-range offset unchanged`() {
        val result = clampOffsetToBounds(elementBounds, 300, 400, 0f, 0f, x = 50f, y = 50f)
        assertEquals(Offset(50f, 50f), result)
    }

    @Test
    fun `clampOffsetToBounds clamps to the right and bottom edges`() {
        val result = clampOffsetToBounds(elementBounds, 300, 400, 0f, 0f, x = 500f, y = 500f)
        assertEquals(190f, result.x, 0.001f) // 300 - 110
        assertEquals(340f, result.y, 0.001f) // 400 - 60
    }

    @Test
    fun `clampOffsetToBounds clamps to the left and top edges`() {
        val result = clampOffsetToBounds(elementBounds, 300, 400, 0f, 0f, x = -100f, y = -100f)
        assertEquals(-10f, result.x, 0.001f)
        assertEquals(-10f, result.y, 0.001f)
    }

    @Test
    fun `clampOffsetToBounds raises the top limit`() {
        val result = clampOffsetToBounds(
            elementBounds,
            rootWidth = 300,
            rootHeight = 400,
            topLimit = 30f,
            navigationBarHeightPx = 0f,
            x = 0f,
            y = 0f,
        )
        assertEquals(20f, result.y, 0.001f) // -10 + 30
    }

    @Test
    fun `clampOffsetToBounds keeps the element above the navigation bar`() {
        val result = clampOffsetToBounds(
            elementBounds,
            rootWidth = 300,
            rootHeight = 400,
            topLimit = 0f,
            navigationBarHeightPx = 100f,
            x = 0f,
            y = 500f,
        )
        assertEquals(240f, result.y, 0.001f) // 400 - 60 - 100
    }

    @Test
    fun `clampOffsetToBounds returns input for an invalid container size`() {
        val result = clampOffsetToBounds(elementBounds, 0, 0, 0f, 0f, x = 777f, y = 888f)
        assertEquals(Offset(777f, 888f), result)
    }

    @Test
    fun `translateRect shifts all edges`() {
        val result = translateRect(Rect(10f, 20f, 30f, 40f), x = 5f, y = -5f)
        assertEquals(Rect(15f, 15f, 35f, 35f), result)
    }

    @Test
    fun `expandRect grows on every side`() {
        val result = expandRect(Rect(10f, 10f, 20f, 20f), padding = 5f)
        assertEquals(Rect(5f, 5f, 25f, 25f), result)
    }

    @Test
    fun `expandRect with negative padding shrinks`() {
        val result = expandRect(Rect(10f, 10f, 30f, 30f), padding = -5f)
        assertEquals(Rect(15f, 15f, 25f, 25f), result)
    }

    @Test
    fun `intersects is true for overlapping rects`() {
        assertTrue(intersects(Rect(0f, 0f, 10f, 10f), Rect(5f, 5f, 15f, 15f)))
    }

    @Test
    fun `intersects is false for disjoint rects`() {
        assertFalse(intersects(Rect(0f, 0f, 10f, 10f), Rect(20f, 20f, 30f, 30f)))
    }

    @Test
    fun `intersects is false when rects only touch at an edge`() {
        assertFalse(intersects(Rect(0f, 0f, 10f, 10f), Rect(10f, 0f, 20f, 10f)))
    }

    @Test
    fun `navigationBarForbiddenZone spans the bottom strip`() {
        val zone = navigationBarForbiddenZone(rootWidth = 300, rootHeight = 400, navigationBarHeightPx = 50f)
        assertEquals(Rect(0f, 350f, 300f, 400f), zone)
    }

    @Test
    fun `navigationBarForbiddenZone is empty for an invalid container size`() {
        assertEquals(Rect(0f, 0f, 0f, 0f), navigationBarForbiddenZone(0, 0, 50f))
    }
}
