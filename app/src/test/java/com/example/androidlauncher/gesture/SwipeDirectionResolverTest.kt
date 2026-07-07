package com.example.androidlauncher.gesture

import com.example.androidlauncher.data.SwipeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwipeDirectionResolverTest {

    private fun resolver() = SwipeDirectionResolver(
        thresholdPx = 60f,
        bottomDeadZonePx = 100f,
        containerHeightPx = 2000f,
    )

    @Test
    fun `detects all four directions once the threshold is crossed`() {
        val cases = listOf(
            Triple(0f, -70f, SwipeDirection.UP),
            Triple(0f, 70f, SwipeDirection.DOWN),
            Triple(-70f, 0f, SwipeDirection.LEFT),
            Triple(70f, 0f, SwipeDirection.RIGHT),
        )
        for ((dx, dy, expected) in cases) {
            val r = resolver()
            r.onDragStart(startY = 500f)
            assertEquals(expected, r.onDrag(dx, dy))
        }
    }

    @Test
    fun `below threshold nothing fires`() {
        val r = resolver()
        r.onDragStart(startY = 500f)
        assertNull(r.onDrag(0f, -59f))
        assertNull(r.onDrag(30f, 0f))
    }

    @Test
    fun `deltas accumulate across drag events`() {
        val r = resolver()
        r.onDragStart(startY = 500f)
        assertNull(r.onDrag(0f, -40f))
        assertEquals(SwipeDirection.UP, r.onDrag(0f, -30f))
    }

    @Test
    fun `dominant axis wins on diagonal swipes`() {
        val r = resolver()
        r.onDragStart(startY = 500f)
        // Mehr horizontal als vertikal: Links gewinnt, obwohl auch dy über dem Threshold liegt.
        assertEquals(SwipeDirection.LEFT, r.onDrag(-100f, -70f))

        val r2 = resolver()
        r2.onDragStart(startY = 500f)
        // Gleichstand zählt als vertikal (bisheriges Verhalten bevorzugt die vertikale Geste).
        assertEquals(SwipeDirection.UP, r2.onDrag(-80f, -80f))
    }

    @Test
    fun `fires at most once per drag and resets on new drag`() {
        val r = resolver()
        r.onDragStart(startY = 500f)
        assertEquals(SwipeDirection.UP, r.onDrag(0f, -70f))
        assertNull(r.onDrag(0f, -500f))
        assertNull(r.onDrag(500f, 0f))

        r.onDragStart(startY = 500f)
        assertEquals(SwipeDirection.DOWN, r.onDrag(0f, 70f))
    }

    @Test
    fun `drags starting in the bottom dead zone never fire`() {
        val r = resolver()
        r.onDragStart(startY = 1950f)
        assertNull(r.onDrag(0f, -500f))
        assertNull(r.onDrag(500f, 0f))

        // Start knapp oberhalb der Zone feuert normal.
        r.onDragStart(startY = 1899f)
        assertEquals(SwipeDirection.UP, r.onDrag(0f, -70f))
    }
}
