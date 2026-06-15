package com.example.androidlauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReturnAnimationGateTest {

    private val pendingAnimation = ReturnAnimation(
        bounds = null,
        source = LaunchSource.HOME,
        packageName = "com.example.target",
        launchedPackageName = "com.example.target",
        launchedAtMs = System.currentTimeMillis()
    )

    @Test
    fun `returns animation when observation matches launched package and is fresh`() {
        val pendingLaunchStartedAtMs = System.currentTimeMillis() - 2_000L
        val decision = ReturnAnimationGate.resolve(
            pendingReturnAnimation = pendingAnimation,
            pendingLaunchStartedAtMs = pendingLaunchStartedAtMs,
            storedAnimations = emptyMap(),
            observations = listOf(
                ForegroundAppObservation(
                    packageName = "com.example.target",
                    observedAtMs = pendingLaunchStartedAtMs + 500L,
                    source = "accessibility-before-launcher"
                )
            )
        )

        assertEquals("matched-observation", decision.reason)
        assertEquals(pendingAnimation, decision.returnAnimation)
        assertEquals("com.example.target", decision.matchedObservation?.packageName)
    }

    @Test
    fun `does not return animation for stale pending launch session`() {
        val nowMs = System.currentTimeMillis()
        val decision = ReturnAnimationGate.resolve(
            pendingReturnAnimation = pendingAnimation,
            pendingLaunchStartedAtMs = nowMs - (15 * 60 * 1000L) - 1000L,
            storedAnimations = emptyMap(),
            observations = listOf(
                ForegroundAppObservation(
                    packageName = "com.example.target",
                    observedAtMs = nowMs - 1_000L,
                    source = "accessibility-before-launcher"
                )
            )
        )

        assertEquals("stale-animation", decision.reason)
        assertNull(decision.returnAnimation)
        assertEquals("com.example.target", decision.matchedObservation?.packageName)
    }

    @Test
    fun `returns animation when observation matches stored animation instead of pending`() {
        val nowMs = System.currentTimeMillis()
        val storedAnimation = ReturnAnimation(
            bounds = null,
            source = LaunchSource.DRAWER,
            packageName = "com.example.stored",
            launchedPackageName = "com.example.stored",
            launchedAtMs = nowMs - 5_000L
        )

        val decision = ReturnAnimationGate.resolve(
            pendingReturnAnimation = pendingAnimation,
            pendingLaunchStartedAtMs = nowMs - 1_000L,
            storedAnimations = mapOf("com.example.stored" to storedAnimation),
            observations = listOf(
                ForegroundAppObservation(
                    packageName = "com.example.stored",
                    observedAtMs = nowMs - 500L,
                    source = "usage-events"
                )
            )
        )

        assertEquals("matched-observation", decision.reason)
        assertEquals(storedAnimation, decision.returnAnimation)
        assertEquals("com.example.stored", decision.matchedObservation?.packageName)
    }

    @Test
    fun `falls back to fresh pending animation when no observations are available`() {
        val pendingLaunchStartedAtMs = System.currentTimeMillis() - 3_000L
        val decision = ReturnAnimationGate.resolve(
            pendingReturnAnimation = pendingAnimation,
            pendingLaunchStartedAtMs = pendingLaunchStartedAtMs,
            storedAnimations = emptyMap(),
            observations = emptyList()
        )

        assertEquals("pending-fallback-no-observations", decision.reason)
        assertEquals(pendingAnimation, decision.returnAnimation)
        assertNull(decision.matchedObservation)
    }

    @Test
    fun `does not fall back to stale pending animation when no observations are available`() {
        val pendingLaunchStartedAtMs = System.currentTimeMillis() - (5 * 60 * 1000L) - 1_000L
        val decision = ReturnAnimationGate.resolve(
            pendingReturnAnimation = pendingAnimation,
            pendingLaunchStartedAtMs = pendingLaunchStartedAtMs,
            storedAnimations = emptyMap(),
            observations = emptyList()
        )

        assertEquals("no-foreground-observations", decision.reason)
        assertNull(decision.returnAnimation)
    }

    @Test
    fun `does not return animation when only a different package was seen before launcher`() {
        val pendingLaunchStartedAtMs = System.currentTimeMillis() - 1_000L
        val decision = ReturnAnimationGate.resolve(
            pendingReturnAnimation = pendingAnimation,
            pendingLaunchStartedAtMs = pendingLaunchStartedAtMs,
            storedAnimations = emptyMap(),
            observations = listOf(
                ForegroundAppObservation(
                    packageName = "com.example.other",
                    observedAtMs = pendingLaunchStartedAtMs + 200L,
                    source = "usage-events"
                )
            )
        )

        assertEquals("no-matching-animation", decision.reason)
        assertNull(decision.returnAnimation)
        assertEquals("com.example.other", decision.matchedObservation?.packageName)
    }
}
