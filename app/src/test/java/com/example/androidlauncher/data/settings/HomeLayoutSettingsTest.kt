package com.example.androidlauncher.data.settings

import androidx.compose.ui.geometry.Offset
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.androidlauncher.data.FavoritesBorderStyle
import com.example.androidlauncher.data.HomeLayout
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
class HomeLayoutSettingsTest {

    private lateinit var testFile: File
    private lateinit var settings: HomeLayoutSettings

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        testFile = File.createTempFile("home_layout_settings_test", ".preferences_pb")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { testFile }
        )
        settings = HomeLayoutSettings(testDataStore)
    }

    @After
    fun tearDown() {
        testFile.delete()
    }

    @Test
    fun `defaults are widgets on, labels off, dots on, no border, onboarding open`() = testScope.runTest {
        assertFalse(settings.showFavoriteLabels.first())
        assertTrue(settings.isNotificationDotsEnabled.first())
        assertEquals(FavoritesBorderStyle.NONE, settings.favoritesBorderStyle.first())
        assertTrue(settings.isSmartSuggestionsEnabled.first())
        assertTrue(settings.isWeatherWidgetEnabled.first())
        assertTrue(settings.isClockWidgetEnabled.first())
        assertTrue(settings.isCalendarWidgetEnabled.first())
        assertFalse(settings.isOnboardingCompleted.first())
        assertEquals(HomeLayout(), settings.homeLayout.first())
    }

    @Test
    fun `favorites presentation toggles roundtrip`() = testScope.runTest {
        settings.setShowFavoriteLabels(true)
        settings.setNotificationDotsEnabled(false)
        settings.setFavoritesBorderStyle(FavoritesBorderStyle.entries.last())

        assertTrue(settings.showFavoriteLabels.first())
        assertFalse(settings.isNotificationDotsEnabled.first())
        assertEquals(FavoritesBorderStyle.entries.last(), settings.favoritesBorderStyle.first())
    }

    @Test
    fun `widget toggles and onboarding roundtrip`() = testScope.runTest {
        settings.setWeatherWidgetEnabled(false)
        settings.setClockWidgetEnabled(false)
        settings.setCalendarWidgetEnabled(false)
        settings.setSmartSuggestionsEnabled(false)
        settings.setOnboardingCompleted(true)

        assertFalse(settings.isWeatherWidgetEnabled.first())
        assertFalse(settings.isClockWidgetEnabled.first())
        assertFalse(settings.isCalendarWidgetEnabled.first())
        assertFalse(settings.isSmartSuggestionsEnabled.first())
        assertTrue(settings.isOnboardingCompleted.first())
    }

    @Test
    fun `home layout offsets roundtrip for all four elements`() = testScope.runTest {
        val layout = HomeLayout(
            clock = Offset(10f, -20f),
            date = Offset(0f, 5f),
            weather = Offset(-3f, 7f),
            favorites = Offset(1f, 2f),
        )

        settings.setHomeLayout(layout)

        assertEquals(layout, settings.homeLayout.first())
    }
}
