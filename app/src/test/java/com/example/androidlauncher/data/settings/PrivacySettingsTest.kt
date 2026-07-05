package com.example.androidlauncher.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.androidlauncher.data.AppAccessMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Testet die Privatsphäre-Einstellungen. CryptoManager arbeitet in
 * Plain-JVM-Tests als Klartext-Passthrough, daher sind die verschlüsselten
 * Keys (hidden/locked apps, lock secret) hier direkt testbar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrivacySettingsTest {

    private lateinit var testFile: File
    private lateinit var settings: PrivacySettings

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        testFile = File.createTempFile("privacy_settings_test", ".preferences_pb")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { testFile }
        )
        settings = PrivacySettings(testDataStore)
    }

    @After
    fun tearDown() {
        testFile.delete()
    }

    @Test
    fun `defaults are no hidden or locked apps and no lock configured`() = testScope.runTest {
        assertEquals(emptySet<String>(), settings.hiddenApps.first())
        assertEquals(emptySet<String>(), settings.lockedApps.first())
        assertEquals("none", settings.lockType.first())
        assertEquals("", settings.lockSecret.first())
        assertFalse(settings.isLockBiometricEnabled.first())
        assertEquals(AppAccessMode.DRAWER_LIST, settings.appAccessMode.first())
    }

    @Test
    fun `hidden and locked apps roundtrip as sets`() = testScope.runTest {
        settings.setHiddenApps(setOf("com.a", "com.b"))
        settings.setLockedApps(setOf("com.whatsapp"))

        assertEquals(setOf("com.a", "com.b"), settings.hiddenApps.first())
        assertEquals(setOf("com.whatsapp"), settings.lockedApps.first())

        // Leeren funktioniert ebenfalls (kein Rest-Eintrag durch Join von "").
        settings.setHiddenApps(emptySet())
        assertEquals(emptySet<String>(), settings.hiddenApps.first())
    }

    @Test
    fun `lock secret stores type and token and clear resets to none`() = testScope.runTest {
        settings.setLockSecret("pin", "salt:hash")

        assertEquals("pin", settings.lockType.first())
        assertEquals("salt:hash", settings.lockSecret.first())

        settings.clearLockSecret()

        assertEquals("none", settings.lockType.first())
        assertEquals("", settings.lockSecret.first())
    }

    @Test
    fun `biometric toggle and app access mode roundtrip`() = testScope.runTest {
        settings.setLockBiometricEnabled(true)
        settings.setAppAccessMode(AppAccessMode.DRAWER_GRID)

        assertTrue(settings.isLockBiometricEnabled.first())
        assertEquals(AppAccessMode.DRAWER_GRID, settings.appAccessMode.first())
    }
}
