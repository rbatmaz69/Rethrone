package com.example.androidlauncher
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
class LauncherShakeManagerTest {
    private lateinit var context: Context
    private lateinit var appContext: Context
    private lateinit var sensorManager: SensorManager
    private lateinit var sensor: Sensor

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        appContext = mockk(relaxed = true)
        sensorManager = mockk(relaxed = true)
        sensor = mockk(relaxed = true)
        every { context.applicationContext } returns appContext
        every { appContext.getSystemService(SensorManager::class.java) } returns sensorManager
        mockkStatic(SystemClock::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testIsAvailable() {
        every { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns sensor
        val manager = LauncherShakeManager(context)
        assertTrue(manager.isAvailable())
    }

    @Test
    fun testIsNotAvailable() {
        every { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns null
        val manager = LauncherShakeManager(context)
        assertFalse(manager.isAvailable())
    }

    @Test
    fun testStartStop() {
        every { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns sensor
        every { sensorManager.registerListener(any(), sensor, any()) } returns true
        val manager = LauncherShakeManager(context)
        val started = manager.start()
        assertTrue(started)
        val listenerSlot = slot<SensorEventListener>()
        verify { sensorManager.registerListener(capture(listenerSlot), sensor, SensorManager.SENSOR_DELAY_GAME) }
        // start again shouldn't re-register
        assertTrue(manager.start())
        verify(exactly = 1) { sensorManager.registerListener(any<SensorEventListener>(), any<Sensor>(), any<Int>()) }
        manager.stop()
        verify { sensorManager.unregisterListener(listenerSlot.captured) }
        // stop again shouldn't unregister twice
        manager.stop()
        verify(exactly = 1) { sensorManager.unregisterListener(any<SensorEventListener>()) }
    }

    @Test
    fun testDoubleShakeTriggersCallback() {
        every { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns sensor
        every { sensorManager.registerListener(any(), sensor, any()) } returns true

        val manager = LauncherShakeManager(context)
        manager.start()

        val listenerSlot = slot<SensorEventListener>()
        verify { sensorManager.registerListener(capture(listenerSlot), sensor, any()) }
        val listener = listenerSlot.captured

        val event = mockk<SensorEvent>()
        try {
            val valuesField = SensorEvent::class.java.getField("values")
            valuesField.isAccessible = true
            valuesField.set(event, floatArrayOf(20f, 20f, 20f))
        } catch (e: Exception) {
            every { event.values } returns floatArrayOf(20f, 20f, 20f)
        }

        var doubleShakeCount = 0
        manager.onDoubleShake = { doubleShakeCount++ }

        // Erster Peak: noch kein Doppel-Schütteln.
        every { SystemClock.elapsedRealtime() } returns 1_000L
        listener.onSensorChanged(event)
        assertEquals(0, doubleShakeCount)

        // Zweiter Peak innerhalb des Fensters: Doppel-Schütteln.
        every { SystemClock.elapsedRealtime() } returns 1_400L
        listener.onSensorChanged(event)
        assertEquals(1, doubleShakeCount)
    }
}
