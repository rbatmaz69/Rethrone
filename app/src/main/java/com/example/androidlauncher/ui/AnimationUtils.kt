package com.example.androidlauncher.ui

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import com.example.androidlauncher.ui.theme.LocalAnimationSpeed
import com.example.androidlauncher.ui.theme.LocalAnimationsEnabled
import kotlin.math.roundToInt

@Composable
fun <T> appSpring(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMedium,
    visibilityThreshold: T? = null
): FiniteAnimationSpec<T> {
    return if (LocalAnimationsEnabled.current) {
        // Höherer Faktor = steifere Feder = schnellere Bewegung.
        spring(dampingRatio, stiffness * LocalAnimationSpeed.current, visibilityThreshold)
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
        // Höherer Faktor = kürzere Dauer = schnellere Animation.
        val speed = LocalAnimationSpeed.current
        tween((durationMillis / speed).roundToInt(), (delayMillis / speed).roundToInt(), easing)
    } else {
        snap(delayMillis)
    }
}

/**
 * Skaliert eine feste Tween-Dauer mit dem globalen Tempo-Faktor. Für Inline-
 * Animationen (z. B. `tween(scaledDurationMs(300))`). Bei deaktivierten
 * Animationen wird 0 (= sofort) zurückgegeben.
 */
@Composable
fun scaledDurationMs(base: Int): Int =
    if (LocalAnimationsEnabled.current) (base / LocalAnimationSpeed.current).roundToInt() else 0

