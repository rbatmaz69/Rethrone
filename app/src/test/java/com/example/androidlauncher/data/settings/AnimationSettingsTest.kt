package com.example.androidlauncher.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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

@OptIn(ExperimentalCoroutinesApi::class)
class AnimationSettingsTest {

    private lateinit var testFile: File
    private lateinit var settings: AnimationSettings

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        testFile = File.createTempFile("animation_settings_test", ".preferences_pb")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { testFile }
        )
        settings = AnimationSettings(testDataStore)
    }

    @After
    fun tearDown() {
        testFile.delete()
    }

    @Test
    fun `all animation toggles default to enabled with normal speed`() = testScope.runTest {
        assertTrue(settings.isAnimationsEnabled.first())
        assertTrue(settings.isAppOpenAnimationEnabled.first())
        assertTrue(settings.isAppCloseAnimationEnabled.first())
        assertTrue(settings.isMenuAnimationEnabled.first())
        assertTrue(settings.isFavoritesAnimationEnabled.first())
        assertEquals(1f, settings.animationSpeed.first())
    }

    @Test
    fun `individual toggles roundtrip independently`() = testScope.runTest {
        settings.setAnimationsEnabled(false)
        settings.setAppOpenAnimationEnabled(false)
        settings.setMenuAnimationEnabled(false)

        assertFalse(settings.isAnimationsEnabled.first())
        assertFalse(settings.isAppOpenAnimationEnabled.first())
        assertFalse(settings.isMenuAnimationEnabled.first())
        // Nicht angefasste Toggles bleiben auf Default an.
        assertTrue(settings.isAppCloseAnimationEnabled.first())
        assertTrue(settings.isFavoritesAnimationEnabled.first())
    }

    @Test
    fun `animation speed is clamped on write and on read`() = testScope.runTest {
        settings.setAnimationSpeed(5f)
        assertEquals(AnimationSettings.MAX_SPEED, settings.animationSpeed.first())

        settings.setAnimationSpeed(0.1f)
        assertEquals(AnimationSettings.MIN_SPEED, settings.animationSpeed.first())

        settings.setAnimationSpeed(1.5f)
        assertEquals(1.5f, settings.animationSpeed.first())
    }
}
