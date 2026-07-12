package com.example.androidlauncher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class IconManagerTest {

    private lateinit var testFile: File
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var manager: IconManager

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        testFile = File.createTempFile("icon_manager_test", ".preferences_pb")

        testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { testFile }
        )

        manager = IconManager(testDataStore)
    }

    @After
    fun tearDown() {
        testFile.delete()
    }

    @Test
    fun `customIcons empty initially`() = testScope.runTest {
        val icons = manager.customIcons.first()
        assertTrue(icons.isEmpty())
    }

    @Test
    fun `setCustomIcon updates config and flow emits it`() = testScope.runTest {
        manager.setCustomIcon("com.pkg.a", "camera")
        val icons = manager.customIcons.first()
        assertEquals(1, icons.size)
        assertEquals("camera", icons["com.pkg.a"])
    }

    @Test
    fun `selectedIconPack roundtrips and clears`() = testScope.runTest {
        assertNull(manager.selectedIconPack.first())

        manager.setSelectedIconPack("com.pack.a")
        assertEquals("com.pack.a", manager.selectedIconPack.first())

        manager.setSelectedIconPack(null)
        assertNull(manager.selectedIconPack.first())
    }

    @Test
    fun `selected icon pack never leaks into customIcons`() = testScope.runTest {
        manager.setSelectedIconPack("com.pack.a")
        manager.setCustomIcon("com.pkg.a", "camera")

        val icons = manager.customIcons.first()

        assertEquals(mapOf("com.pkg.a" to "camera"), icons)
    }

    @Test
    fun `setCustomIcon with null removes the mapping`() = testScope.runTest {
        manager.setCustomIcon("com.pkg.a", "camera")
        manager.setCustomIcon("com.pkg.a", null)

        val icons = manager.customIcons.first()
        assertTrue(icons.isEmpty())
    }

    @Test
    fun `legacy auto keys never leak into customIcons`() = testScope.runTest {
        // Reste des entfernten Auto-Icon-Systems (Alt-Installation/Backup-Restore).
        testDataStore.edit { preferences ->
            preferences[stringPreferencesKey("auto_fallback__com.pkg.a")] = "type=NEUTRAL"
            preferences[stringPreferencesKey("auto_rule__com.pkg.a")] = "mode=KEEP_ORIGINAL"
        }
        manager.setCustomIcon("com.pkg.a", "camera")

        val icons = manager.customIcons.first()

        assertEquals(mapOf("com.pkg.a" to "camera"), icons)
    }
}
