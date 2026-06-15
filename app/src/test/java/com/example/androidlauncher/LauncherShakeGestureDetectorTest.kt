package com.example.androidlauncher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherShakeGestureDetectorTest {

    @Test
    fun `single shake does not trigger a gesture`() {
        val detector = LauncherShakeGestureDetector()

        assertFalse(detector.onGForceSample(gForce = 2.7f, timestampMs = 1_000L))
        // Auch nach Ablauf des Doppel-Schüttel-Fensters bleibt ein einzelner Peak wirkungslos.
        assertFalse(detector.onGForceSample(gForce = 0.5f, timestampMs = 2_000L))
    }

    @Test
    fun `two fast shakes are reported as a double shake`() {
        val detector = LauncherShakeGestureDetector()

        assertFalse(detector.onGForceSample(gForce = 2.8f, timestampMs = 1_000L))

        assertTrue(detector.onGForceSample(gForce = 2.9f, timestampMs = 1_400L))
    }

    @Test
    fun `normal movement below threshold does not trigger a gesture`() {
        val detector = LauncherShakeGestureDetector()

        assertFalse(detector.onGForceSample(gForce = 1.2f, timestampMs = 1_000L))
        assertFalse(detector.onGForceSample(gForce = 1.5f, timestampMs = 1_200L))
    }

    @Test
    fun `extra peaks inside the minimum gap do not create a double shake`() {
        val detector = LauncherShakeGestureDetector()

        assertFalse(detector.onGForceSample(gForce = 2.8f, timestampMs = 1_000L))
        // Peak innerhalb der minimalen Lücke wird ignoriert, löst also kein Doppel-Schütteln aus.
        assertFalse(detector.onGForceSample(gForce = 3.0f, timestampMs = 1_120L))
    }

    @Test
    fun `second peak outside the window starts a new pending shake`() {
        val detector = LauncherShakeGestureDetector()

        assertFalse(detector.onGForceSample(gForce = 2.8f, timestampMs = 1_000L))
        // Zu spät für ein Doppel-Schütteln -> wird neuer erster Peak, kein Trigger.
        assertFalse(detector.onGForceSample(gForce = 2.8f, timestampMs = 2_000L))
        // Ein schneller Folge-Peak bildet nun mit dem vorigen ein gültiges Doppel-Schütteln.
        assertTrue(detector.onGForceSample(gForce = 2.8f, timestampMs = 2_400L))
    }
}
