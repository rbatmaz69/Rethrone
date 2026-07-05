package com.example.androidlauncher.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.example.androidlauncher.ui.theme.LocalAnimationSpeed
import com.example.androidlauncher.ui.theme.RethroneSprings
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

fun Modifier.bounceClick(interactionSource: MutableInteractionSource, enabled: Boolean = true) = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val animationsEnabled = com.example.androidlauncher.ui.theme.LocalAnimationsEnabled.current
    // Universeller, dezenter Tap-Haptik beim Drücken (respektiert die Haptik-Einstellung).
    val haptics = com.example.androidlauncher.ui.theme.rememberAppHaptics()
    LaunchedEffect(isPressed) {
        if (isPressed && enabled) haptics.tap()
    }
    val targetScale = if (!animationsEnabled) 1f else if (isPressed && enabled) 0.93f else 1f
    // Material-3-Expressive: federnderes Tap-Feedback (LowBouncy/StiffnessLow).
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "bounceScale"
    )
    this.scale(scale)
}

/**
 * Ergebnis von [rememberSwipeToCloseRubberBand]: die [NestedScrollConnection]
 * plus der aktuelle, elastisch gedämpfte Versatz [offsetY] (positiv = oberer Rand,
 * negativ = unterer Rand).
 */
class SwipeToCloseState(
    val connection: NestedScrollConnection,
    private val offset: State<Float>
) {
    /** Aktueller Versatz in Pixel. Per `graphicsLayer { translationY = state.offsetY }` anwenden. */
    val offsetY: Float get() = offset.value
}

/**
 * Elastisches Rubber-Band-Feedback an beiden Listenrändern im
 * Material-3-Expressive-Stil. Am **oberen** Rand folgt der Inhalt dem Ziehen mit
 * abnehmender Wirkung und löst [onClose] aus, sobald [closeThreshold] erreicht
 * (oder mit Schwung geflungen) wird. Am **unteren** Rand gibt der Inhalt beim
 * Weiterziehen über das Listenende elastisch nach und federt anschließend via
 * [RethroneSprings] sanft zurück – ohne Close.
 *
 * @param isAtTop liefert, ob die Liste/das Grid ganz oben steht.
 * @param isAtBottom liefert, ob das Listenende erreicht ist (kein Weiterscrollen).
 */
// Dämpfungsfaktor: skaliert die rohe Fling-Geschwindigkeit (px/s) auf die
// Anfangsgeschwindigkeit der Bounce-Feder.
private const val BOUNCE_VELOCITY_FACTOR = 0.16f

@Composable
fun rememberSwipeToCloseRubberBand(
    closeThreshold: Dp = 64.dp,
    maxDrag: Dp = 96.dp,
    isAtTop: () -> Boolean,
    isAtBottom: () -> Boolean = { false },
    onClose: () -> Unit
): SwipeToCloseState {
    val density = LocalDensity.current
    val thresholdPx = with(density) { closeThreshold.toPx() }
    val maxDragPx = with(density) { maxDrag.toPx() }
    // Obergrenze für die Anfangsgeschwindigkeit des Fling-Bounce – begrenzt den
    // Ausschlag bei sehr schnellem Fling, hält den Effekt aber spürbar.
    val maxBounceVelocityPx = with(density) { 340.dp.toPx() }
    val speed = LocalAnimationSpeed.current
    val scope = rememberCoroutineScope()

    val rawDrag = remember { mutableFloatStateOf(0f) }
    val settle = remember { Animatable(0f) }
    val settling = remember { mutableStateOf(false) }

    // Abnehmende Wirkung: nähert sich vorzeichenrichtig asymptotisch maxDragPx an.
    fun elastic(raw: Float): Float {
        val sign = if (raw < 0f) -1f else 1f
        val x = abs(raw)
        return sign * maxDragPx * (1f - exp(-x / maxDragPx))
    }

    val offset = remember {
        derivedStateOf {
            if (settling.value) settle.value else elastic(rawDrag.floatValue)
        }
    }

    // Vorzeichenrichtig zurückfedern (für oberen wie unteren Rand).
    fun springBack() {
        if (rawDrag.floatValue == 0f) return
        val from = elastic(rawDrag.floatValue)
        rawDrag.floatValue = 0f
        settling.value = true
        scope.launch {
            settle.snapTo(from)
            settle.animateTo(0f, RethroneSprings.spatial(speed))
            settling.value = false
        }
    }

    // Dezenter Fling-Bounce: trifft der Schwung ein Listenende, federt der Inhalt
    // mit der Restgeschwindigkeit kurz aus und settlet wieder. [velocity] in px/s,
    // Vorzeichen bestimmt die Richtung (oben positiv, unten negativ).
    fun bounce(velocity: Float) {
        if (velocity == 0f) return
        val initial = (velocity * BOUNCE_VELOCITY_FACTOR)
            .coerceIn(-maxBounceVelocityPx, maxBounceVelocityPx)
        rawDrag.floatValue = 0f
        settling.value = true
        scope.launch {
            settle.snapTo(0f)
            settle.animateTo(0f, RethroneSprings.spatial(speed), initialVelocity = initial)
            settling.value = false
        }
    }

    val connection = remember(thresholdPx, onClose) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                val dy = available.y
                val current = rawDrag.floatValue

                // Oberes Band: aktiv (current>0) oder Start beim Ziehen nach unten am
                // oberen Rand. Folgt dem Finger; beim Nulldurchgang Rest an die Liste.
                if (current > 0f || (current == 0f && isAtTop() && dy > 0f)) {
                    settling.value = false
                    val next = current + dy
                    if (next <= 0f) {
                        rawDrag.floatValue = 0f
                        return Offset(0f, -current)
                    }
                    rawDrag.floatValue = next
                    if (next >= thresholdPx) {
                        rawDrag.floatValue = 0f
                        onClose()
                    }
                    return Offset(0f, dy)
                }

                // Unteres Band: aktiv (current<0) → folgt dem Finger zurück, Rest an
                // die Liste, sobald das Band aufgelöst ist.
                if (current < 0f) {
                    settling.value = false
                    val next = current + dy
                    if (next >= 0f) {
                        rawDrag.floatValue = 0f
                        return Offset(0f, -current)
                    }
                    rawDrag.floatValue = next
                    return Offset(0f, dy)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // Unterer Rand: vom Listenende übrig gebliebenes Ziehen nach oben
                // → elastisches Nachgeben.
                if (source == NestedScrollSource.UserInput && available.y < 0f && isAtBottom()) {
                    settling.value = false
                    rawDrag.floatValue += available.y
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (isAtTop() && available.y > 1500f) {
                    rawDrag.floatValue = 0f
                    settling.value = false
                    settle.snapTo(0f)
                    onClose()
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (rawDrag.floatValue != 0f) {
                    // Aktiver Drag-Versatz → sanft aus der aktuellen Auslenkung zurück.
                    springBack()
                } else {
                    when {
                        // Reiner Schwung in ein Ende (kein vorheriges Ziehen) → dezenter Bounce.
                        isAtTop() && available.y > 0f -> bounce(available.y)
                        isAtBottom() && available.y < 0f -> bounce(available.y)
                    }
                }
                return Velocity.Zero
            }
        }
    }

    return remember(connection, offset) { SwipeToCloseState(connection, offset) }
}
