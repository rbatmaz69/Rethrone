package com.example.androidlauncher.data.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.androidlauncher.data.CryptoManager
import com.example.androidlauncher.data.WidgetSerializer
import com.example.androidlauncher.data.favoritesDataStore
import com.example.androidlauncher.data.folderDataStore
import com.example.androidlauncher.data.iconDataStore
import com.example.androidlauncher.data.settings.settingsDataStore
import kotlinx.coroutines.flow.first

/** Ergebnis eines Restore-Versuchs. */
enum class RestoreResult {
    APPLIED,

    /** Die Datei stammt aus einer neueren App-Version (backupVersion > CURRENT_VERSION). */
    UNSUPPORTED_VERSION,
}

/**
 * Erstellt und spielt Backups der Launcher-Konfiguration ein (B5).
 *
 * Erfasst die vier Preference-DataStores (settings/favorites/folders/icon_mappings)
 * gemäß [BackupSpec]; der `search_suggestions`-Store bleibt bewusst außen vor
 * (Web-Suchhistorie). Restore ist Overwrite-all pro Store in je einem atomaren
 * Edit; die per [BackupSpec.EXCLUDED_SETTINGS_KEYS] geschützten Werte (App-Lock,
 * Wallpaper-URI, Widget-Bestand) überleben den Import unverändert.
 */
class BackupManager(
    private val settingsStore: DataStore<Preferences>,
    private val favoritesStore: DataStore<Preferences>,
    private val foldersStore: DataStore<Preferences>,
    private val iconStore: DataStore<Preferences>,
) {

    /** Produktiv-Konstruktor: löst die vier geteilten DataStore-Instanzen auf. */
    constructor(context: Context) : this(
        settingsStore = context.settingsDataStore,
        favoritesStore = context.favoritesDataStore,
        foldersStore = context.folderDataStore,
        iconStore = context.iconDataStore,
    )

    /** Liest alle vier Stores und baut den exportierbaren Snapshot. */
    suspend fun createSnapshot(
        appVersionCode: Long,
        nowMs: Long = System.currentTimeMillis(),
    ): BackupSnapshot {
        val settingsPrefs = settingsStore.data.first()
        val stores = mapOf(
            BackupSpec.STORE_SETTINGS to snapshotEntries(
                prefs = settingsPrefs,
                excludedKeys = BackupSpec.EXCLUDED_SETTINGS_KEYS,
                encryptedKeys = BackupSpec.ENCRYPTED_SETTINGS_KEYS,
            ),
            BackupSpec.STORE_FAVORITES to snapshotEntries(favoritesStore.data.first()),
            BackupSpec.STORE_FOLDERS to snapshotEntries(foldersStore.data.first()),
            BackupSpec.STORE_ICON_MAPPINGS to snapshotEntries(iconStore.data.first()),
        )
        return BackupSnapshot(
            backupVersion = BackupSpec.CURRENT_VERSION,
            appVersionCode = appVersionCode,
            exportedAtMs = nowMs,
            stores = stores,
            widgets = snapshotWidgets(settingsPrefs),
        )
    }

    /**
     * Spielt einen Snapshot ein. Kein App-Restart nötig – alle Konsumenten
     * collecten die DataStore-Flows reaktiv.
     */
    suspend fun restore(snapshot: BackupSnapshot): RestoreResult {
        if (snapshot.backupVersion > BackupSpec.CURRENT_VERSION) return RestoreResult.UNSUPPORTED_VERSION

        restoreSettings(snapshot.stores[BackupSpec.STORE_SETTINGS].orEmpty())
        restorePlainStore(favoritesStore, snapshot.stores[BackupSpec.STORE_FAVORITES].orEmpty())
        restorePlainStore(foldersStore, snapshot.stores[BackupSpec.STORE_FOLDERS].orEmpty())
        restorePlainStore(iconStore, snapshot.stores[BackupSpec.STORE_ICON_MAPPINGS].orEmpty())
        return RestoreResult.APPLIED
    }

    private fun snapshotEntries(
        prefs: Preferences,
        excludedKeys: Set<String> = emptySet(),
        encryptedKeys: Set<String> = emptySet(),
    ): List<BackupEntry> = prefs.asMap().mapNotNull { (key, value) ->
        if (key.name in excludedKeys) return@mapNotNull null
        val plainValue = if (key.name in encryptedKeys) {
            CryptoManager.decryptOrLegacy(value as? String)
        } else {
            value
        }
        val type = BackupValueType.fromValue(plainValue) ?: return@mapNotNull null
        BackupEntry(key.name, type, plainValue)
    }

    /** Widgets id-los exportieren – appWidgetIds sind gerätespezifisch (v1: kein Re-Bind beim Import). */
    private fun snapshotWidgets(settingsPrefs: Preferences): List<WidgetBackup> {
        val json = settingsPrefs[stringPreferencesKey(HOSTED_WIDGETS_KEY)] ?: return emptyList()
        return WidgetSerializer.parseWidgets(json).map {
            WidgetBackup(
                provider = it.provider,
                widthDp = it.widthDp,
                heightDp = it.heightDp,
                offsetX = it.offsetX,
                offsetY = it.offsetY,
            )
        }
    }

    private suspend fun restoreSettings(entries: List<BackupEntry>) {
        settingsStore.edit { prefs ->
            val preserved = prefs.asMap().filterKeys { it.name in BackupSpec.EXCLUDED_SETTINGS_KEYS }
            prefs.clear()
            preserved.forEach { (key, value) ->
                @Suppress("UNCHECKED_CAST")
                prefs[key as Preferences.Key<Any>] = value
            }
            entries.forEach { entry ->
                // Geschützte Keys nie aus einer Import-Datei übernehmen (Defense in Depth).
                if (entry.key in BackupSpec.EXCLUDED_SETTINGS_KEYS) return@forEach
                if (entry.key in BackupSpec.ENCRYPTED_SETTINGS_KEYS) {
                    val plain = entry.value as? String ?: return@forEach
                    prefs[stringPreferencesKey(entry.key)] = CryptoManager.encrypt(plain)
                } else {
                    applyEntry(prefs, entry)
                }
            }
        }
    }

    private suspend fun restorePlainStore(store: DataStore<Preferences>, entries: List<BackupEntry>) {
        store.edit { prefs ->
            prefs.clear()
            entries.forEach { applyEntry(prefs, it) }
        }
    }

    /** Rekonstruiert den exakt typisierten Preferences-Key aus dem Typ-Tag. */
    private fun applyEntry(prefs: MutablePreferences, entry: BackupEntry) {
        when (entry.type) {
            BackupValueType.STRING -> prefs[stringPreferencesKey(entry.key)] = entry.value as String
            BackupValueType.BOOLEAN -> prefs[booleanPreferencesKey(entry.key)] = entry.value as Boolean
            BackupValueType.INT -> prefs[intPreferencesKey(entry.key)] = entry.value as Int
            BackupValueType.LONG -> prefs[longPreferencesKey(entry.key)] = entry.value as Long
            BackupValueType.FLOAT -> prefs[floatPreferencesKey(entry.key)] = entry.value as Float
            BackupValueType.DOUBLE -> prefs[doublePreferencesKey(entry.key)] = entry.value as Double
            BackupValueType.STRING_SET ->
                prefs[stringSetPreferencesKey(entry.key)] =
                    (entry.value as Set<*>).map { it.toString() }.toSet()
        }
    }

    private companion object {
        // Muss dem Key in HomeLayoutSettings entsprechen (dort private).
        const val HOSTED_WIDGETS_KEY = "hosted_widgets"
    }
}
