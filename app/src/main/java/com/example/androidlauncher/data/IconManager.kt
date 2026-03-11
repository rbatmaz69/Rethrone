package com.example.androidlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.iconDataStore by preferencesDataStore(name = "icon_mappings")
private const val AUTO_FALLBACK_PREFIX = "auto_fallback__"

/**
 * Manages custom app icons mapping.
 * packageName -> lucideIconName
 */
class IconManager(private val context: Context) {

    /**
     * Returns a flow of the current manual icon mappings.
     */
    val customIcons: Flow<Map<String, String>> = context.iconDataStore.data
        .map { preferences ->
            preferences.asMap()
                .filterKeys { !it.name.startsWith(AUTO_FALLBACK_PREFIX) }
                .mapKeys { it.key.name }
                .mapValues { it.value as String }
        }

    /**
     * Returns a flow of the automatic fallback analysis results.
     */
    val autoIconFallbacks: Flow<Map<String, AutoIconFallback>> = context.iconDataStore.data
        .map { preferences ->
            preferences.asMap()
                .filterKeys { it.name.startsWith(AUTO_FALLBACK_PREFIX) }
                .mapNotNull { (key, value) ->
                    val packageName = key.name.removePrefix(AUTO_FALLBACK_PREFIX)
                    val fallback = (value as? String)?.let(AutoIconFallback::deserialize)
                    if (packageName.isBlank() || fallback == null) null else packageName to fallback
                }
                .toMap()
        }

    /**
     * Updates the custom icon for a package.
     * If iconName is null, the custom icon is removed (reverts to automatic/default).
     */
    suspend fun setCustomIcon(packageName: String, iconName: String?) {
        context.iconDataStore.edit { preferences ->
            val key = stringPreferencesKey(packageName)
            if (iconName != null) {
                preferences[key] = iconName
            } else {
                preferences.remove(key)
            }
        }
    }

    /**
     * Persists or removes the automatic fallback decision for a package.
     */
    suspend fun setAutoIconFallback(packageName: String, fallback: AutoIconFallback?) {
        context.iconDataStore.edit { preferences ->
            val key = stringPreferencesKey("$AUTO_FALLBACK_PREFIX$packageName")
            if (fallback != null) {
                preferences[key] = fallback.serialize()
            } else {
                preferences.remove(key)
            }
        }
    }

    /**
     * Removes persisted automatic analysis for a package and optionally the user override.
     */
    suspend fun invalidatePackage(packageName: String, removeUserOverride: Boolean = false) {
        context.iconDataStore.edit { preferences ->
            preferences.remove(stringPreferencesKey("$AUTO_FALLBACK_PREFIX$packageName"))
            if (removeUserOverride) {
                preferences.remove(stringPreferencesKey(packageName))
            }
        }
    }
}
