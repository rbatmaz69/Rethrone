package com.example.androidlauncher.ui

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import com.example.androidlauncher.ui.theme.LocalAnimationsEnabled

@Composable
fun <T> appSpring(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMedium,
    visibilityThreshold: T? = null
): FiniteAnimationSpec<T> {
    return if (LocalAnimationsEnabled.current) {
        spring(dampingRatio, stiffness, visibilityThreshold)
    } else {
        snap()
    }
}

@Composable
fun <T> appTween(
    durationMillis: Int = 300,
    delayMillis: Int = 0,
    easing: Easing = FastOutSlowInEasing
): FiniteAnimationSpec<T> {
    return if (LocalAnimationsEnabled.current) {
        tween(durationMillis, delayMillis, easing)
    } else {
        snap(delayMillis)
    }
}

