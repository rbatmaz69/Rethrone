package com.example.androidlauncher

import kotlin.math.sqrt

/**
 * Erkennt konservativ Single- und Double-Shake-Muster auf Basis des Accelerometers.
 *
 * - 1 Shake -> Taschenlampe umschalten
 * - 2 schnelle Shakes -> Kamera öffnen
 */
class LauncherShakeGestureDetector(
    private val shakeThresholdG: Float = 2.35f,
    private val minShakeGapMs: Long = 250L,
    private val doubleShakeWindowMs: Long = 700L,
    private val actionCooldownMs: Long = 1_200L
) {
    companion object {
        private const val EARTH_GRAVITY_MS2 = 9.80665f
    }

    enum class GestureAction {
        TOGGLE_FLASHLIGHT,
        OPEN_CAMERA
    }

    val singleShakeConfirmationDelayMs: Long
        get() = doubleShakeWindowMs

    private var lastShakePeakTimestampMs = 0L
    private var pendingShakeTimestampMs: Long? = null
    private var lastDispatchedTimestampMs = Long.MIN_VALUE / 4

    fun onAccelerationChanged(x: Float, y: Float, z: Float, timestampMs: Long): GestureAction? {
        val gForce = sqrt((x * x + y * y + z * z).toDouble()).toFloat() / EARTH_GRAVITY_MS2
        return onGForceSample(gForce = gForce, timestampMs = timestampMs)
    }

    fun onGForceSample(gForce: Float, timestampMs: Long): GestureAction? {
        flushPending(timestampMs)?.let { return it }

        if (gForce < shakeThresholdG) {
            return null
        }

        if (timestampMs - lastDispatchedTimestampMs < actionCooldownMs) {
            return null
        }

        if (lastShakePeakTimestampMs != 0L && timestampMs - lastShakePeakTimestampMs < minShakeGapMs) {
            return null
        }

        val previousPendingShake = pendingShakeTimestampMs
        lastShakePeakTimestampMs = timestampMs

        return if (previousPendingShake != null && timestampMs - previousPendingShake <= doubleShakeWindowMs) {
            pendingShakeTimestampMs = null
            lastDispatchedTimestampMs = timestampMs
            GestureAction.OPEN_CAMERA
        } else {
            pendingShakeTimestampMs = timestampMs
            null
        }
    }

    fun flushPending(timestampMs: Long): GestureAction? {
        val pendingTimestampMs = pendingShakeTimestampMs ?: return null
        if (timestampMs - pendingTimestampMs < doubleShakeWindowMs) {
            return null
        }

        pendingShakeTimestampMs = null
        if (timestampMs - lastDispatchedTimestampMs < actionCooldownMs) {
            return null
        }

        lastDispatchedTimestampMs = timestampMs
        return GestureAction.TOGGLE_FLASHLIGHT
    }

    fun hasPendingSingleShake(): Boolean = pendingShakeTimestampMs != null

    fun reset() {
        lastShakePeakTimestampMs = 0L
        pendingShakeTimestampMs = null
    }
}

