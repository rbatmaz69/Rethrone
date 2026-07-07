package com.example.androidlauncher.gesture

import com.example.androidlauncher.data.SwipeDirection
import kotlin.math.abs

/**
 * Erkennt aus akkumulierten Drag-Deltas die Wisch-Richtung auf dem Startbildschirm.
 * Ersetzt die frühere Inline-Akkumulation in HomeScreen (nur vertikal) und ist
 * pure Kotlin, damit die Schwellwert-/Dead-Zone-Logik unit-testbar bleibt.
 *
 * Regeln:
 * - Es feuert höchstens EINE Richtung pro Drag ([onDragStart] setzt zurück).
 * - Die dominante Achse (größerer Absolutbetrag) entscheidet, damit ein leicht
 *   schräger vertikaler Wisch nicht links/rechts auslöst.
 * - Drags, die in der unteren System-Gestenzone starten, feuern nie (sonst
 *   öffnet ein Home-Wisch von ganz unten versehentlich den Drawer).
 */
class SwipeDirectionResolver(
    private val thresholdPx: Float,
    private val bottomDeadZonePx: Float,
    private val containerHeightPx: Float,
) {
    private var totalDx = 0f
    private var totalDy = 0f
    private var handled = false
    private var startedInDeadZone = false

    fun onDragStart(startY: Float) {
        totalDx = 0f
        totalDy = 0f
        handled = false
        startedInDeadZone = startY > containerHeightPx - bottomDeadZonePx
    }

    /** Liefert genau einmal pro Drag die erkannte Richtung, sonst null. */
    fun onDrag(dx: Float, dy: Float): SwipeDirection? {
        if (handled || startedInDeadZone) return null
        totalDx += dx
        totalDy += dy
        val direction = if (abs(totalDy) >= abs(totalDx)) {
            when {
                totalDy < -thresholdPx -> SwipeDirection.UP
                totalDy > thresholdPx -> SwipeDirection.DOWN
                else -> null
            }
        } else {
            when {
                totalDx < -thresholdPx -> SwipeDirection.LEFT
                totalDx > thresholdPx -> SwipeDirection.RIGHT
                else -> null
            }
        }
        if (direction != null) handled = true
        return direction
    }
}
