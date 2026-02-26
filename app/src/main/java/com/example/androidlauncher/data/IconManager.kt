package com.example.androidlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.iconDataStore by preferencesDataStore(name = "icon_mappings")

/**
 * Manages custom app icons mapping.
 * packageName -> lucideIconName
 */
class IconManager(private val context: Context) {

    /**
     * Returns a flow of the current icon mappings.
     */
    val customIcons: Flow<Map<String, String>> = context.iconDataStore.data
        .map { preferences ->
            preferences.asMap().mapKeys { it.key.name }.mapValues { it.value as String }
        }

    /**
     * Updates the custom icon for a package.
     * If iconName is null, the custom icon is removed (reverts to default).
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
}
