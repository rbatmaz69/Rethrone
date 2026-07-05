package com.example.androidlauncher.data.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.androidlauncher.data.AppFont
import com.example.androidlauncher.data.DesignStyle
import com.example.androidlauncher.data.FontWeightLevel
import com.example.androidlauncher.ui.theme.ColorTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceSettingsTest {

    private lateinit var testFile: File
    private lateinit var settings: AppearanceSettings

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val testDataStore by lazy {
        PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { testFile }
        )
    }

    @Before
    fun setup() {
        testFile = File.createTempFile("appearance_settings_test", ".preferences_pb")
        settings = AppearanceSettings(testDataStore)
    }

    @After
    fun tearDown() {
        testFile.delete()
    }

    @Test
    fun `defaults are soft sand theme with dark text and system font`() = testScope.runTest {
        assertEquals(ColorTheme.SOFT_SAND, settings.selectedTheme.first())
        assertTrue(settings.isDarkTextEnabled.first())
        assertEquals(AppFont.SYSTEM_DEFAULT, settings.selectedAppFont.first())
        assertEquals(FontWeightLevel.NORMAL, settings.selectedFontWeight.first())
    }

    @Test
    fun `theme and design style roundtrip and keep legacy glass boolean in sync`() = testScope.runTest {
        settings.setTheme(ColorTheme.SIGNATURE)
        settings.setDesignStyle(DesignStyle.FLAT)

        assertEquals(ColorTheme.SIGNATURE, settings.selectedTheme.first())
        assertEquals(DesignStyle.FLAT, settings.designStyle.first())
        // setDesignStyle hält das Legacy-Boolean konsistent (FLAT ist nicht glass-like).
        assertEquals(DesignStyle.FLAT.isGlassLike, settings.isLiquidGlassEnabled.first())
    }

    @Test
    fun `design style falls back to legacy liquid glass boolean when unset`() = testScope.runTest {
        // Nur das Legacy-Boolean setzen (kein design_style) → GLASS.
        settings.setLiquidGlassEnabled(true)
        assertEquals(DesignStyle.GLASS, settings.designStyle.first())
    }

    @Test
    fun `colors roundtrip as argb`() = testScope.runTest {
        settings.setIconColor(Color(0xFF102030))
        settings.setHomeTextColor(Color(0xFF405060))
        settings.setCustomBackgroundColor(Color(0xFF708090))
        settings.setCustomMenuColor(Color(0xFFA0B0C0))

        assertEquals(Color(0xFF102030), settings.iconColor.first())
        assertEquals(Color(0xFF405060), settings.homeTextColor.first())
        assertEquals(Color(0xFF708090), settings.customBackgroundColor.first())
        assertEquals(Color(0xFFA0B0C0), settings.customMenuColor.first())
    }

    @Test
    fun `numeric size values roundtrip through their enums`() = testScope.runTest {
        settings.setFontSize(1.2f)
        settings.setFontWeight(700)
        settings.setIconSize(56.dp)
        settings.setFavoriteSpacing(20.dp)

        assertEquals(1.2f, settings.selectedFontSize.first().scale)
        assertEquals(700, settings.selectedFontWeight.first().weightValue)
        assertEquals(56f, settings.selectedIconSize.first().size.value)
        assertEquals(20f, settings.selectedFavoriteSpacing.first().spacing.value)
    }

    @Test
    fun `legacy string keys migrate to numeric values on read`() = testScope.runTest {
        // Alten Enum-String direkt in den Store schreiben (wie von einer Alt-Version hinterlassen).
        testDataStore.edit { it[stringPreferencesKey("font_size")] = "LARGE" }
        testDataStore.edit { it[stringPreferencesKey("font_weight")] = "BOLD" }

        assertEquals(1.2f, settings.selectedFontSize.first().scale)
        assertEquals(700, settings.selectedFontWeight.first().weightValue)
    }
}
