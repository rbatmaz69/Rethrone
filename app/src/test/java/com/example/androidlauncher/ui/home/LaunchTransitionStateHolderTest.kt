package com.example.androidlauncher.ui.home

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.example.androidlauncher.LaunchSource
import com.example.androidlauncher.ReturnAnimation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Absicherung des A2-Splits: der Holder startet in einem neutralen Zustand
 * (keine laufende Animation) und trägt Mutationen unabhängig voneinander.
 */
class LaunchTransitionStateHolderTest {

    private val holder = LaunchTransitionStateHolder(defaultLaunchBackground = Color.Black)

    @Test
    fun `initial state has no running transitions`() {
        assertNull(holder.pendingReturnAnimation)
        assertEquals(0L, holder.pendingReturnAnimationStartedWallClockMs)
        assertNull(holder.activeReturnAnimation)
        assertNull(holder.returnIconPackage)
        assertEquals(0, holder.searchButtonBounceToken)
        assertEquals(0, holder.returnBounceToken)
        assertNull(holder.returnBounceTargetPackage)
        assertNull(holder.launchIconPackage)
        assertFalse(holder.isSearchLaunching)
        assertFalse(holder.isAppLaunchAnimating)
        assertNull(holder.activeLaunchBounds)
        assertEquals(Color.Black, holder.activeLaunchBackground)
        assertNull(holder.activeLaunchBackgroundBrush)
    }

    @Test
    fun `return animation fields mutate independently`() {
        val animation = ReturnAnimation(
            launchedPackageName = "com.a",
            packageName = "com.a",
            bounds = Rect(0f, 0f, 10f, 10f),
            source = LaunchSource.HOME,
        )

        holder.pendingReturnAnimation = animation
        holder.returnBounceToken += 1
        holder.returnBounceTargetPackage = "com.a"

        assertEquals(animation, holder.pendingReturnAnimation)
        assertEquals(1, holder.returnBounceToken)
        assertEquals("com.a", holder.returnBounceTargetPackage)
        // Unabhängige Felder bleiben unberührt.
        assertNull(holder.activeReturnAnimation)
        assertEquals(0, holder.searchButtonBounceToken)
    }

    @Test
    fun `launch overlay fields mutate independently`() {
        holder.isAppLaunchAnimating = true
        holder.activeLaunchBounds = Rect(1f, 2f, 3f, 4f)
        holder.activeLaunchBackground = Color.White

        assertEquals(Rect(1f, 2f, 3f, 4f), holder.activeLaunchBounds)
        assertEquals(Color.White, holder.activeLaunchBackground)
        assertFalse(holder.isSearchLaunching)
    }
}
