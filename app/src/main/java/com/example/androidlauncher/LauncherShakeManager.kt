package com.example.androidlauncher

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock

/**
 * Verbindet die Doppel-Schüttel-Erkennung mit dem Android-Accelerometer.
 */
class LauncherShakeManager(context: Context) {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(SensorManager::class.java)
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val detector = LauncherShakeGestureDetector()

    var onDoubleShake: (() -> Unit)? = null

    private var isStarted = false

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val values = event.values
            if (values.size < 3) return

            val isDoubleShake = detector.onAccelerationChanged(
                x = values[0],
                y = values[1],
                z = values[2],
                timestampMs = SystemClock.elapsedRealtime()
            )

            if (isDoubleShake) {
                onDoubleShake?.invoke()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun isAvailable(): Boolean = accelerometer != null

    fun start(): Boolean {
        if (isStarted) return true
        val manager = sensorManager ?: return false
        val sensor = accelerometer ?: return false

        val didRegister = manager.registerListener(
            sensorListener,
            sensor,
            SensorManager.SENSOR_DELAY_GAME
        )

        isStarted = didRegister
        return didRegister
    }

    fun stop() {
        detector.reset()

        if (!isStarted) return
        sensorManager?.unregisterListener(sensorListener)
        isStarted = false
    }
}
