package com.example.androidlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// `internal` statt `private`: der BackupManager (B5) erfasst diese DataStore-Datei mit.
internal val Context.iconDataStore by preferencesDataStore(name = "icon_mappings")

// Legacy-Keys des entfernten Auto-Icon-Systems: können in Alt-Installationen und
// Backup-Restores noch vorkommen und werden nur aus customIcons herausgefiltert.
private const val AUTO_FALLBACK_PREFIX = "auto_fallback__"
private const val AUTO_RULE_PREFIX = "auto_rule__"

// Reserved-Key für das global gewählte Icon-Pack (B4) – kollidiert nicht mit
// Package-Namen (doppelte Unterstriche sind in Package-Namen unüblich, und der
// Key wird aus customIcons herausgefiltert).
private val ICON_PACK_KEY = androidx.datastore.preferences.core.stringPreferencesKey("setting__icon_pack")

/**
 * Manages custom app icons mapping.
 * packageName -> lucideIconName
 */
class IconManager(
    private val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
) {
    constructor(context: Context) : this(context.iconDataStore)

    /**
     * Returns a flow of the current manual icon mappings.
     */
    val customIcons: Flow<Map<String, String>> = dataStore.data
        .map { preferences ->
            preferences.asMap()
                .filterKeys {
                    !it.name.startsWith(AUTO_FALLBACK_PREFIX) &&
                        !it.name.startsWith(AUTO_RULE_PREFIX) &&
                        it.name != ICON_PACK_KEY.name
                }
                .mapKeys { it.key.name }
                .mapValues { it.value as String }
        }

    /** Global gewähltes Icon-Pack (Package-Name) oder `null` für System-Icons (B4). */
    val selectedIconPack: Flow<String?> = dataStore.data
        .map { it[ICON_PACK_KEY] }

    /** Setzt oder entfernt (`null`) das global gewählte Icon-Pack. */
    suspend fun setSelectedIconPack(packPackage: String?) {
        dataStore.edit { preferences ->
            if (packPackage != null) {
                preferences[ICON_PACK_KEY] = packPackage
            } else {
                preferences.remove(ICON_PACK_KEY)
            }
        }
    }

    /**
     * Updates the custom icon for a package.
     * If iconName is null, the custom icon is removed (reverts to automatic/default).
     */
    suspend fun setCustomIcon(packageName: String, iconName: String?) {
        dataStore.edit { preferences ->
            val key = stringPreferencesKey(packageName)
            if (iconName != null) {
                preferences[key] = iconName
            } else {
                preferences.remove(key)
            }
        }
    }
}
