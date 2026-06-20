package com.example.androidlauncher

import com.example.androidlauncher.LauncherAccessibilityService.Companion.isTransientSystemPackage
import com.example.androidlauncher.LauncherAccessibilityService.Companion.shouldShowLockScreen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests der reinen Enforcement-Entscheidung des [LauncherAccessibilityService] – also wann beim
 * Vordergrundwechsel der Sperrbildschirm erscheinen muss.
 */
class AppLockEnforcementTest {

    private val own = "com.rethrone.launcher"
    private val locked = setOf("com.whatsapp", "com.instagram.android")

    @Test
    fun lockedAppNotUnlocked_showsLockScreen() {
        assertTrue(
            shouldShowLockScreen("com.whatsapp", own, locked, alreadyUnlocked = false, isTransient = false)
        )
    }

    @Test
    fun ownLauncherPackage_neverLocked() {
        assertFalse(
            shouldShowLockScreen(own, own, locked + own, alreadyUnlocked = false, isTransient = false)
        )
    }

    @Test
    fun packageNotInLockedSet_notLocked() {
        assertFalse(
            shouldShowLockScreen("com.android.chrome", own, locked, alreadyUnlocked = false, isTransient = false)
        )
    }

    @Test
    fun alreadyUnlockedInSession_notLockedAgain() {
        assertFalse(
            shouldShowLockScreen("com.whatsapp", own, locked, alreadyUnlocked = true, isTransient = false)
        )
    }

    @Test
    fun transientSystemPackage_notLocked() {
        assertFalse(
            shouldShowLockScreen("com.whatsapp", own, locked, alreadyUnlocked = false, isTransient = true)
        )
    }

    @Test
    fun isTransientSystemPackage_detectsSystemUi() {
        assertTrue(isTransientSystemPackage("com.android.systemui"))
        assertTrue(isTransientSystemPackage("com.samsung.systemui.something"))
    }

    @Test
    fun isTransientSystemPackage_allowsNormalApp() {
        assertFalse(isTransientSystemPackage("com.whatsapp"))
    }
}
