package com.example.androidlauncher.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.androidlauncher.data.GestureAction
import com.example.androidlauncher.data.SwipeDirection
import com.example.androidlauncher.data.SwipeGestureConfig
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
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var settings: GestureSettings

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        testFile = File.createTempFile("gesture_settings_test", ".preferences_pb")
        testDataStore = PreferenceDataStoreFactory.create(
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

    @Test
    fun `swipe defaults match the previously hardcoded behaviour`() = testScope.runTest {
        val config = settings.swipeGestureConfig.first()
        assertEquals(GestureAction.APP_DRAWER, config[SwipeDirection.UP].action)
        assertEquals(GestureAction.NOTIFICATIONS, config[SwipeDirection.DOWN].action)
        assertEquals(GestureAction.NONE, config[SwipeDirection.LEFT].action)
        assertEquals(GestureAction.NONE, config[SwipeDirection.RIGHT].action)
        SwipeDirection.entries.forEach { assertNull(config[it].appPackage) }
        // Leere Config (HomeScreen-Default-Parameter) verhält sich identisch.
        SwipeDirection.entries.forEach { assertEquals(SwipeGestureConfig()[it], config[it]) }
    }

    @Test
    fun `swipe actions roundtrip per direction`() = testScope.runTest {
        settings.setSwipeAction(SwipeDirection.UP, GestureAction.SEARCH)
        settings.setSwipeAction(SwipeDirection.DOWN, GestureAction.LOCK_SCREEN)
        settings.setSwipeAction(SwipeDirection.LEFT, GestureAction.CAMERA)
        settings.setSwipeAction(SwipeDirection.RIGHT, GestureAction.FLASHLIGHT)

        val config = settings.swipeGestureConfig.first()
        assertEquals(GestureAction.SEARCH, config[SwipeDirection.UP].action)
        assertEquals(GestureAction.LOCK_SCREEN, config[SwipeDirection.DOWN].action)
        assertEquals(GestureAction.CAMERA, config[SwipeDirection.LEFT].action)
        assertEquals(GestureAction.FLASHLIGHT, config[SwipeDirection.RIGHT].action)
    }

    @Test
    fun `swipe app packages roundtrip and null clears the choice`() = testScope.runTest {
        settings.setSwipeAppPackage(SwipeDirection.RIGHT, "com.example.mailapp")
        assertEquals("com.example.mailapp", settings.swipeGestureConfig.first()[SwipeDirection.RIGHT].appPackage)

        settings.setSwipeAppPackage(SwipeDirection.RIGHT, null)
        assertNull(settings.swipeGestureConfig.first()[SwipeDirection.RIGHT].appPackage)
    }

    @Test
    fun `invalid stored swipe action name falls back to the direction default`() = testScope.runTest {
        testDataStore.edit { it[stringPreferencesKey("swipe_up_action")] = "NOT_A_REAL_ACTION" }
        assertEquals(GestureAction.APP_DRAWER, settings.swipeGestureConfig.first()[SwipeDirection.UP].action)
    }
}
