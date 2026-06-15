package com.example.androidlauncher

import kotlin.math.sqrt

/**
 * Erkennt konservativ ein Doppel-Schüttel-Muster auf Basis des Accelerometers.
 *
 * Meldet ausschließlich, ob ein doppeltes Schütteln (zwei schnelle Peaks) erkannt wurde.
 * Welche Aktion daraus folgt (Taschenlampe, Kamera, …) entscheidet die vom Nutzer
 * konfigurierte Zuordnung.
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

    private var lastShakePeakTimestampMs = 0L
    private var pendingShakeTimestampMs: Long? = null
    private var lastDispatchedTimestampMs = Long.MIN_VALUE / 4

    /** @return true, wenn mit diesem Sample ein doppeltes Schütteln erkannt wurde. */
    fun onAccelerationChanged(x: Float, y: Float, z: Float, timestampMs: Long): Boolean {
        val gForce = sqrt((x * x + y * y + z * z).toDouble()).toFloat() / EARTH_GRAVITY_MS2
        return onGForceSample(gForce = gForce, timestampMs = timestampMs)
    }

    /** @return true, wenn mit diesem Sample ein doppeltes Schütteln erkannt wurde. */
    fun onGForceSample(gForce: Float, timestampMs: Long): Boolean {
        if (gForce < shakeThresholdG) {
            return false
        }

        if (timestampMs - lastDispatchedTimestampMs < actionCooldownMs) {
            return false
        }

        if (lastShakePeakTimestampMs != 0L && timestampMs - lastShakePeakTimestampMs < minShakeGapMs) {
            return false
        }

        val previousPendingShake = pendingShakeTimestampMs
        lastShakePeakTimestampMs = timestampMs

        return if (previousPendingShake != null && timestampMs - previousPendingShake <= doubleShakeWindowMs) {
            pendingShakeTimestampMs = null
            lastDispatchedTimestampMs = timestampMs
            true
        } else {
            // Erster Peak (oder zu spät für ein Doppel-Schütteln): als neuen Startpunkt merken.
            pendingShakeTimestampMs = timestampMs
            false
        }
    }

    fun reset() {
        lastShakePeakTimestampMs = 0L
        pendingShakeTimestampMs = null
    }
}
