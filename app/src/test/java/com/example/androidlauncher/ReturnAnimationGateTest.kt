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
        val decision = ReturnAnimationGate.resolve(
            pendingReturnAnimation = pendingAnimation,
            pendingLaunchStartedAtMs = 1_000L,
            observations = listOf(
                ForegroundAppObservation(
                    packageName = "com.example.target",
                    observedAtMs = 2_000L,
                    source = "accessibility-before-launcher"
                )
            ),
            nowMs = 2_500L
        )

        assertEquals("confirmed-return", decision.reason)
        assertEquals(pendingAnimation, decision.returnAnimation)
        assertEquals("com.example.target", decision.matchedObservation?.packageName)
    }

    @Test
    fun `does not return animation for stale observation from old recents session`() {
        val decision = ReturnAnimationGate.resolve(
            pendingReturnAnimation = pendingAnimation,
            pendingLaunchStartedAtMs = 1_000L,
            observations = listOf(
                ForegroundAppObservation(
                    packageName = "com.example.target",
                    observedAtMs = 2_000L,
                    source = "accessibility-before-launcher"
                )
            ),
            nowMs = 5_000L
        )

        assertEquals("no-confirmed-return", decision.reason)
        assertNull(decision.returnAnimation)
    }

    @Test
    fun `does not return animation when observation happened before the launch session`() {
        val decision = ReturnAnimationGate.resolve(
            pendingReturnAnimation = pendingAnimation,
            pendingLaunchStartedAtMs = 3_000L,
            observations = listOf(
                ForegroundAppObservation(
                    packageName = "com.example.target",
                    observedAtMs = 2_000L,
                    source = "usage-events"
                )
            ),
            nowMs = 3_100L
        )

        assertEquals("no-confirmed-return", decision.reason)
        assertNull(decision.returnAnimation)
    }

    @Test
    fun `does not return animation when only a different package was seen before launcher`() {
        val decision = ReturnAnimationGate.resolve(
            pendingReturnAnimation = pendingAnimation,
            pendingLaunchStartedAtMs = 1_000L,
            observations = listOf(
                ForegroundAppObservation(
                    packageName = "com.example.other",
                    observedAtMs = 2_000L,
                    source = "usage-events"
                )
            ),
            nowMs = 2_100L
        )

        assertEquals("no-confirmed-return", decision.reason)
        assertNull(decision.returnAnimation)
    }
}

