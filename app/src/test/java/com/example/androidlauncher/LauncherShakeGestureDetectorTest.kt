package com.example.androidlauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherShakeGestureDetectorTest {

    @Test
    fun `single shake becomes flashlight action after confirmation window`() {
        val detector = LauncherShakeGestureDetector()

        assertNull(detector.onGForceSample(gForce = 2.7f, timestampMs = 1_000L))
        assertNull(detector.flushPending(timestampMs = 1_600L))

        val action = detector.flushPending(timestampMs = 1_700L)

        assertEquals(LauncherShakeGestureDetector.GestureAction.TOGGLE_FLASHLIGHT, action)
        assertFalse(detector.hasPendingSingleShake())
    }

    @Test
    fun `two fast shakes open the camera`() {
        val detector = LauncherShakeGestureDetector()

        assertNull(detector.onGForceSample(gForce = 2.8f, timestampMs = 1_000L))

        val action = detector.onGForceSample(gForce = 2.9f, timestampMs = 1_400L)

        assertEquals(LauncherShakeGestureDetector.GestureAction.OPEN_CAMERA, action)
        assertFalse(detector.hasPendingSingleShake())
    }

    @Test
    fun `normal movement below threshold does not trigger a gesture`() {
        val detector = LauncherShakeGestureDetector()

        assertNull(detector.onGForceSample(gForce = 1.2f, timestampMs = 1_000L))
        assertNull(detector.onGForceSample(gForce = 1.5f, timestampMs = 1_200L))
        assertNull(detector.flushPending(timestampMs = 2_000L))
        assertFalse(detector.hasPendingSingleShake())
    }

    @Test
    fun `extra peaks inside the minimum gap do not create a double shake`() {
        val detector = LauncherShakeGestureDetector()

        assertNull(detector.onGForceSample(gForce = 2.8f, timestampMs = 1_000L))
        assertNull(detector.onGForceSample(gForce = 3.0f, timestampMs = 1_120L))

        val action = detector.flushPending(timestampMs = 1_700L)

        assertEquals(LauncherShakeGestureDetector.GestureAction.TOGGLE_FLASHLIGHT, action)
    }
}

