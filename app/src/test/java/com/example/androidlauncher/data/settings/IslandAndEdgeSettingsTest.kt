package com.example.androidlauncher.data.settings

import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.androidlauncher.data.EdgeLightingStyle
import com.example.androidlauncher.data.IslandAnimationStyle
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
class IslandAndEdgeSettingsTest {

    private lateinit var testFile: File
    private lateinit var settings: IslandAndEdgeSettings

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        testFile = File.createTempFile("island_edge_settings_test", ".preferences_pb")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { testFile }
        )
        settings = IslandAndEdgeSettings(testDataStore)
    }

    @After
    fun tearDown() {
        testFile.delete()
    }

    @Test
    fun `defaults are island on and edge lighting off with sweep style`() = testScope.runTest {
        assertTrue(settings.isDynamicIslandEnabled.first())
        assertEquals(0f, settings.dynamicIslandOffset.first())
        assertFalse(settings.isEdgeLightingEnabled.first())
        assertEquals(1f, settings.edgeLightingSpeed.first())
        assertEquals(1, settings.edgeLightingLaps.first())
        assertEquals(1f, settings.edgeLightingThickness.first())
        assertEquals(EdgeLightingStyle.SWEEP, settings.edgeLightingStyle.first())
        assertEquals(IslandAnimationStyle.FROM_NOTCH, settings.islandAnimationStyle.first())
    }

    @Test
    fun `island offset is ignored until first deliberate write completes the v2 migration`() = testScope.runTest {
        // Vor der Migration wird ein Alt-Wert im Store als 0 behandelt – das simulieren
        // wir, indem der Flow vor jedem Setter-Aufruf abgefragt wird.
        assertEquals(0f, settings.dynamicIslandOffset.first())

        settings.setDynamicIslandOffset(8f)
        assertEquals(8f, settings.dynamicIslandOffset.first())
    }

    @Test
    fun `island offset is clamped to the allowed range`() = testScope.runTest {
        settings.setDynamicIslandOffset(100f)
        assertEquals(IslandAndEdgeSettings.MAX_OFFSET_DP, settings.dynamicIslandOffset.first())

        settings.setDynamicIslandOffset(-100f)
        assertEquals(IslandAndEdgeSettings.MIN_OFFSET_DP, settings.dynamicIslandOffset.first())
    }

    @Test
    fun `colors roundtrip as argb`() = testScope.runTest {
        settings.setDynamicIslandColor(Color(0xFF112233))
        settings.setEdgeLightingColor(Color(0xFF445566))

        assertEquals(Color(0xFF112233), settings.dynamicIslandColor.first())
        assertEquals(Color(0xFF445566), settings.edgeLightingColor.first())
    }

    @Test
    fun `edge lighting values are clamped and styles roundtrip`() = testScope.runTest {
        settings.setEdgeLightingEnabled(true)
        settings.setEdgeLightingSpeed(9f)
        settings.setEdgeLightingLaps(99)
        settings.setEdgeLightingThickness(0.1f)
        settings.setEdgeLightingStyle(EdgeLightingStyle.GLOW_PULSE)
        settings.setIslandAnimationStyle(IslandAnimationStyle.BOUNCE)

        assertTrue(settings.isEdgeLightingEnabled.first())
        assertEquals(2f, settings.edgeLightingSpeed.first())
        assertEquals(5, settings.edgeLightingLaps.first())
        assertEquals(0.5f, settings.edgeLightingThickness.first())
        assertEquals(EdgeLightingStyle.GLOW_PULSE, settings.edgeLightingStyle.first())
        assertEquals(IslandAnimationStyle.BOUNCE, settings.islandAnimationStyle.first())
    }
}
