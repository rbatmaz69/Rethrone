package com.example.androidlauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReturnResumeGuardTest {

    @Test
    fun `resume after launcher screen off is suppressed until user present arrives`() {
        val afterScreenOff = ReturnResumeGuard.onScreenOff(
            state = ReturnResumeGuardState(),
            launcherWasForeground = true
        )

        val firstResume = ReturnResumeGuard.onResume(afterScreenOff)

        assertTrue(firstResume.shouldSuppress)
        assertEquals(afterScreenOff, firstResume.nextState)
    }

    @Test
    fun `resume after user present is suppressed exactly once`() {
        val afterScreenOff = ReturnResumeGuard.onScreenOff(
            state = ReturnResumeGuardState(),
            launcherWasForeground = true
        )
        val afterUnlock = ReturnResumeGuard.onUserPresent(afterScreenOff)

        val firstResume = ReturnResumeGuard.onResume(afterUnlock)
        val secondResume = ReturnResumeGuard.onResume(firstResume.nextState)

        assertTrue(firstResume.shouldSuppress)
        assertFalse(firstResume.nextState.skipNextResume)
        assertFalse(secondResume.shouldSuppress)
    }

    @Test
    fun `screen off outside launcher does not suppress next resume`() {
        val afterScreenOff = ReturnResumeGuard.onScreenOff(
            state = ReturnResumeGuardState(),
            launcherWasForeground = false
        )

        val resume = ReturnResumeGuard.onResume(afterScreenOff)

        assertFalse(resume.shouldSuppress)
        assertEquals(ReturnResumeGuardState(), resume.nextState)
    }

    @Test
    fun `user present without launcher initiated screen off does not arm suppression`() {
        val afterUnlock = ReturnResumeGuard.onUserPresent(ReturnResumeGuardState())
        val resume = ReturnResumeGuard.onResume(afterUnlock)

        assertFalse(resume.shouldSuppress)
        assertEquals(ReturnResumeGuardState(), resume.nextState)
    }
}

