package com.example.androidlauncher

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests der clientseitigen Positions-Extrapolation der Media-Seek-Leiste
 * (Island Media v2.4): kein Polling – die Position wird aus dem letzten
 * PlaybackState hochgerechnet.
 */
class MediaPlaybackTest {

    private fun progress(
        positionMs: Long = 30_000L,
        durationMs: Long = 180_000L,
        speed: Float = 1f,
        updatedAtMs: Long = 100_000L,
    ) = MediaProgress(positionMs, durationMs, speed, updatedAtMs)

    @Test
    fun `playing position advances with elapsed time`() {
        val p = progress()
        assertEquals(35_000L, extrapolateMediaPositionMs(p, isPlaying = true, nowElapsedMs = 105_000L))
    }

    @Test
    fun `playback speed scales the advance`() {
        val p = progress(speed = 2f)
        assertEquals(40_000L, extrapolateMediaPositionMs(p, isPlaying = true, nowElapsedMs = 105_000L))
    }

    @Test
    fun `paused position stays at the last known value`() {
        val p = progress()
        assertEquals(30_000L, extrapolateMediaPositionMs(p, isPlaying = false, nowElapsedMs = 500_000L))
    }

    @Test
    fun `position is clamped to the duration`() {
        val p = progress(positionMs = 179_000L)
        assertEquals(180_000L, extrapolateMediaPositionMs(p, isPlaying = true, nowElapsedMs = 200_000L))
    }

    @Test
    fun `clock skew backwards does not rewind the position`() {
        val p = progress()
        // now < letzter Update-Zeitpunkt (z. B. Race beim State-Wechsel) → Basisposition halten.
        assertEquals(30_000L, extrapolateMediaPositionMs(p, isPlaying = true, nowElapsedMs = 99_000L))
    }

    @Test
    fun `formatMediaTime renders minutes and hours`() {
        assertEquals("0:07", formatMediaTime(7_000L))
        assertEquals("3:05", formatMediaTime(185_000L))
        assertEquals("1:01:05", formatMediaTime(3_665_000L))
        assertEquals("0:00", formatMediaTime(-500L))
    }
}
