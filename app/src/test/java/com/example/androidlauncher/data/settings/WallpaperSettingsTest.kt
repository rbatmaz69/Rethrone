package com.example.androidlauncher.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class WallpaperSettingsTest {

    private lateinit var testFile: File
    private lateinit var settings: WallpaperSettings

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        testFile = File.createTempFile("wallpaper_settings_test", ".preferences_pb")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { testFile }
        )
        settings = WallpaperSettings(testDataStore)
    }

    @After
    fun tearDown() {
        testFile.delete()
    }

    @Test
    fun `defaults are system wallpaper without blur and with light dim`() = testScope.runTest {
        assertNull(settings.customWallpaperUri.first())
        assertEquals(0f, settings.wallpaperBlur.first())
        assertEquals(0.1f, settings.wallpaperDim.first())
        assertEquals(1.0f, settings.wallpaperZoom.first())
    }

    @Test
    fun `custom wallpaper uri roundtrips and null removes it`() = testScope.runTest {
        settings.setCustomWallpaperUri("content://media/external/images/42")
        assertEquals("content://media/external/images/42", settings.customWallpaperUri.first())

        settings.setCustomWallpaperUri(null)
        assertNull(settings.customWallpaperUri.first())
    }

    @Test
    fun `blur dim and zoom roundtrip`() = testScope.runTest {
        settings.setWallpaperBlur(0.6f)
        settings.setWallpaperDim(0.3f)
        settings.setWallpaperZoom(1.4f)

        assertEquals(0.6f, settings.wallpaperBlur.first())
        assertEquals(0.3f, settings.wallpaperDim.first())
        assertEquals(1.4f, settings.wallpaperZoom.first())
    }
}
