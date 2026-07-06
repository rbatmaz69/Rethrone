package com.example.androidlauncher.data.backup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.androidlauncher.data.HostedWidget
import com.example.androidlauncher.data.WidgetSerializer
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

@OptIn(ExperimentalCoroutinesApi::class)
class BackupManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var files: List<File>
    private lateinit var settingsStore: DataStore<Preferences>
    private lateinit var favoritesStore: DataStore<Preferences>
    private lateinit var foldersStore: DataStore<Preferences>
    private lateinit var iconStore: DataStore<Preferences>
    private lateinit var manager: BackupManager

    private val themeKey = stringPreferencesKey("selected_theme")
    private val dotsKey = booleanPreferencesKey("notification_dots")
    private val blurKey = floatPreferencesKey("wallpaper_blur")
    private val countKey = intPreferencesKey("some_count")
    private val stampKey = longPreferencesKey("some_stamp")
    private val lockSecretKey = stringPreferencesKey("lock_secret")
    private val hostedWidgetsKey = stringPreferencesKey("hosted_widgets")

    // In JVM-Tests ist CryptoManager Klartext-Passthrough; der Key-Name reicht für den Pfad.
    private val hiddenAppsKey = stringPreferencesKey("hidden_apps")
    private val favoritesKey = stringPreferencesKey("favorites_list")
    private val iconKey = stringPreferencesKey("com.example.someapp")

    @Before
    fun setup() {
        fun store(name: String): Pair<File, DataStore<Preferences>> {
            val file = File.createTempFile("backup_manager_test_$name", ".preferences_pb")
            return file to PreferenceDataStoreFactory.create(
                scope = testScope.backgroundScope,
                produceFile = { file },
            )
        }

        val (f1, s1) = store("settings")
        val (f2, s2) = store("favorites")
        val (f3, s3) = store("folders")
        val (f4, s4) = store("icons")
        files = listOf(f1, f2, f3, f4)
        settingsStore = s1
        favoritesStore = s2
        foldersStore = s3
        iconStore = s4
        manager = BackupManager(settingsStore, favoritesStore, foldersStore, iconStore)
    }

    @After
    fun tearDown() {
        files.forEach { it.delete() }
    }

    private suspend fun seedTypicalState() {
        settingsStore.edit {
            it[themeKey] = "SOFT_SAND"
            it[dotsKey] = false
            it[blurKey] = 0.4f
            it[countKey] = 3
            it[stampKey] = 9_876_543_210L
            it[hiddenAppsKey] = "com.hidden.one,com.hidden.two"
            it[lockSecretKey] = "salt:hash"
            it[hostedWidgetsKey] = WidgetSerializer.serializeWidgets(
                listOf(HostedWidget(appWidgetId = 7, provider = "com.x/.Clock", widthDp = 180, heightDp = 110)),
            )
        }
        favoritesStore.edit { it[favoritesKey] = "com.a,com.b" }
        foldersStore.edit { it[stringPreferencesKey("folders_list")] = """[{"name":"Tools"}]""" }
        iconStore.edit { it[iconKey] = "rocket" }
    }

    @Test
    fun `snapshot excludes credentials but exports widgets id-less`() = testScope.runTest {
        seedTypicalState()

        val snapshot = manager.createSnapshot(appVersionCode = 5L, nowMs = 123L)

        val settingsKeys = snapshot.stores.getValue(BackupSpec.STORE_SETTINGS).map { it.key }
        assertFalse("lock_secret" in settingsKeys)
        assertFalse("hosted_widgets" in settingsKeys)
        assertTrue("selected_theme" in settingsKeys)
        assertTrue("hidden_apps" in settingsKeys)
        assertEquals(BackupSpec.CURRENT_VERSION, snapshot.backupVersion)
        assertEquals(5L, snapshot.appVersionCode)
        assertEquals(123L, snapshot.exportedAtMs)
        assertEquals(
            listOf(WidgetBackup("com.x/.Clock", 180, 110, 0f, 0f)),
            snapshot.widgets,
        )
    }

    @Test
    fun `export wipe restore round trip recreates state including types`() = testScope.runTest {
        seedTypicalState()
        val snapshot = manager.createSnapshot(appVersionCode = 5L)

        // Wipe: alle vier Stores leeren (simuliert Neuinstallation).
        listOf(settingsStore, favoritesStore, foldersStore, iconStore).forEach { store ->
            store.edit { it.clear() }
        }

        val result = manager.restore(snapshot)

        assertEquals(RestoreResult.APPLIED, result)
        val prefs = settingsStore.data.first()
        assertEquals("SOFT_SAND", prefs[themeKey])
        assertEquals(false, prefs[dotsKey])
        assertEquals(0.4f, prefs[blurKey])
        assertEquals(3, prefs[countKey])
        assertEquals(9_876_543_210L, prefs[stampKey])
        assertEquals("com.hidden.one,com.hidden.two", prefs[hiddenAppsKey])
        assertEquals("com.a,com.b", favoritesStore.data.first()[favoritesKey])
        assertEquals("rocket", iconStore.data.first()[iconKey])
    }

    @Test
    fun `restore preserves local credentials and widgets, replaces the rest`() = testScope.runTest {
        seedTypicalState()
        val snapshot = manager.createSnapshot(appVersionCode = 5L)

        // Lokaler Zustand nach dem Export: anderes Theme, anderes Secret, anderer Widget-Bestand.
        val localWidgets = WidgetSerializer.serializeWidgets(
            listOf(HostedWidget(appWidgetId = 99, provider = "com.local/.W", widthDp = 100, heightDp = 100)),
        )
        settingsStore.edit {
            it[themeKey] = "MIDNIGHT"
            it[lockSecretKey] = "local-salt:local-hash"
            it[hostedWidgetsKey] = localWidgets
            it[stringPreferencesKey("stale_local_key")] = "soll verschwinden"
        }

        manager.restore(snapshot)

        val prefs = settingsStore.data.first()
        assertEquals("SOFT_SAND", prefs[themeKey])
        assertEquals("local-salt:local-hash", prefs[lockSecretKey])
        assertEquals(localWidgets, prefs[hostedWidgetsKey])
        assertNull(prefs[stringPreferencesKey("stale_local_key")])
    }

    @Test
    fun `restore refuses newer backup versions untouched`() = testScope.runTest {
        seedTypicalState()
        val newer = BackupSnapshot(
            backupVersion = BackupSpec.CURRENT_VERSION + 1,
            appVersionCode = 99L,
            exportedAtMs = 0L,
            stores = mapOf(
                BackupSpec.STORE_SETTINGS to listOf(
                    BackupEntry("selected_theme", BackupValueType.STRING, "FUTURE"),
                ),
            ),
            widgets = emptyList(),
        )

        val result = manager.restore(newer)

        assertEquals(RestoreResult.UNSUPPORTED_VERSION, result)
        assertEquals("SOFT_SAND", settingsStore.data.first()[themeKey])
    }

    @Test
    fun `restore never applies excluded keys from a tampered file`() = testScope.runTest {
        seedTypicalState()
        val tampered = BackupSnapshot(
            backupVersion = BackupSpec.CURRENT_VERSION,
            appVersionCode = 1L,
            exportedAtMs = 0L,
            stores = mapOf(
                BackupSpec.STORE_SETTINGS to listOf(
                    BackupEntry("lock_secret", BackupValueType.STRING, "angreifer:hash"),
                    BackupEntry("selected_theme", BackupValueType.STRING, "OK"),
                ),
            ),
            widgets = emptyList(),
        )

        manager.restore(tampered)

        val prefs = settingsStore.data.first()
        assertEquals("salt:hash", prefs[lockSecretKey])
        assertEquals("OK", prefs[themeKey])
    }

    @Test
    fun `full json round trip through serializer and manager`() = testScope.runTest {
        seedTypicalState()
        val json = BackupSerializer.serialize(manager.createSnapshot(appVersionCode = 5L))

        listOf(settingsStore, favoritesStore, foldersStore, iconStore).forEach { store ->
            store.edit { it.clear() }
        }
        val parsed = BackupSerializer.parse(json)!!
        val result = manager.restore(parsed)

        assertEquals(RestoreResult.APPLIED, result)
        assertEquals("SOFT_SAND", settingsStore.data.first()[themeKey])
        assertEquals("com.a,com.b", favoritesStore.data.first()[favoritesKey])
    }
}
