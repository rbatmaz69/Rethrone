package com.example.androidlauncher.data.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.androidlauncher.data.GestureAction
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Testet die DataStore-basierten Gesten-Einstellungen. Der Haptik-Flow
 * (ContentObserver auf System-Settings) braucht eine Android-Runtime und ist
 * hier bewusst nicht abgedeckt. CryptoManager arbeitet in Plain-JVM-Tests als
 * Klartext-Passthrough, daher sind auch die verschlüsselten Paket-Keys testbar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GestureSettingsTest {

    private lateinit var testFile: File
    private lateinit var settings: GestureSettings

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        testFile = File.createTempFile("gesture_settings_test", ".preferences_pb")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { testFile }
        )
        settings = GestureSettings(testDataStore, mockk<Context>(relaxed = true))
    }

    @After
    fun tearDown() {
        testFile.delete()
    }

    @Test
    fun `defaults are shake enabled with flashlight and double tap locks screen`() = testScope.runTest {
        assertTrue(settings.isShakeGesturesEnabled.first())
        assertEquals(GestureAction.FLASHLIGHT, settings.doubleShakeAction.first())
        assertEquals(GestureAction.LOCK_SCREEN, settings.doubleTapAction.first())
        assertNull(settings.shakeOpenAppPackage.first())
        assertNull(settings.doubleTapAppPackage.first())
    }

    @Test
    fun `shake toggle and actions roundtrip`() = testScope.runTest {
        settings.setShakeGesturesEnabled(false)
        settings.setDoubleShakeAction(GestureAction.CAMERA)
        settings.setDoubleTapAction(GestureAction.NOTIFICATIONS)

        assertFalse(settings.isShakeGesturesEnabled.first())
        assertEquals(GestureAction.CAMERA, settings.doubleShakeAction.first())
        assertEquals(GestureAction.NOTIFICATIONS, settings.doubleTapAction.first())
    }

    @Test
    fun `open app packages roundtrip and null clears the choice`() = testScope.runTest {
        settings.setShakeOpenAppPackage("com.example.torchapp")
        settings.setDoubleTapAppPackage("com.example.notesapp")
        assertEquals("com.example.torchapp", settings.shakeOpenAppPackage.first())
        assertEquals("com.example.notesapp", settings.doubleTapAppPackage.first())

        settings.setShakeOpenAppPackage(null)
        settings.setDoubleTapAppPackage("")
        assertNull(settings.shakeOpenAppPackage.first())
        assertNull(settings.doubleTapAppPackage.first())
    }
}
