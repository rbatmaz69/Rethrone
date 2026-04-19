package com.example.androidlauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReturnAnimationGateTest {

    private val pendingAnimation = ReturnAnimation(
        bounds = null,
        source = LaunchSource.HOME,
        packageName = "com.example.target",
        launchedPackageName = "com.example.target"
    )

    @Test
    fun `returns animation when observation matches launched package and is fresh`() {
        val pendingLaunchStartedAtMs = System.currentTimeMillis() - 2_000L
        val decision = ReturnAnimationGate.resolve(
            pendingReturnAnimation = pendingAnimation,
            pendingLaunchStartedAtMs = pendingLaunchStartedAtMs,
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
            pendingLaunchStartedAtMs = nowMs - (15 * 60 * 1000L) - 1L,
            observations = listOf(
                ForegroundAppObservation(
                    packageName = "com.example.target",
                    observedAtMs = nowMs - 1_000L,
                    source = "accessibility-before-launcher"
                )
            )
        )

        assertEquals("stale-pending-animation", decision.reason)
        assertNull(decision.returnAnimation)
        assertNull(decision.matchedObservation)
    }

    @Test
    fun `returns animation when observation happened before the launch session`() {
        val pendingLaunchStartedAtMs = System.currentTimeMillis() - 1_000L
        val decision = ReturnAnimationGate.resolve(
            pendingReturnAnimation = pendingAnimation,
            pendingLaunchStartedAtMs = pendingLaunchStartedAtMs,
            observations = listOf(
                ForegroundAppObservation(
                    packageName = "com.example.target",
                    observedAtMs = pendingLaunchStartedAtMs - 6_000L,
                    source = "usage-events"
                )
            )
        )

        assertEquals("matched-observation-precedes-launch", decision.reason)
        assertEquals(pendingAnimation, decision.returnAnimation)
        assertEquals("com.example.target", decision.matchedObservation?.packageName)
    }

    @Test
    fun `does not return animation when only a different package was seen before launcher`() {
        val pendingLaunchStartedAtMs = System.currentTimeMillis() - 1_000L
        val decision = ReturnAnimationGate.resolve(
            pendingReturnAnimation = pendingAnimation,
            pendingLaunchStartedAtMs = pendingLaunchStartedAtMs,
            observations = listOf(
                ForegroundAppObservation(
                    packageName = "com.example.other",
                    observedAtMs = pendingLaunchStartedAtMs + 200L,
                    source = "usage-events"
                )
            )
        )

        assertEquals("no-matching-observation", decision.reason)
        assertNull(decision.returnAnimation)
        assertNull(decision.matchedObservation)
    }
}
