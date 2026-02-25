package com.example.androidlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Datastore instance for storing folder data
private val Context.folderDataStore by preferencesDataStore(name = "folders")

/**
 * Manages the persistence of user folders.
 * Uses DataStore to save the folder structure as a JSON string.
 */
class FolderManager(private val context: Context) {
    companion object {
        // Key for storing the entire folder list as a JSON string
        private val FOLDERS_KEY = stringPreferencesKey("app_folders")
    }

    /**
     * A flow emitting the current list of folders.
     * Automatically updates when the underlying DataStore changes.
     */
    val folders: Flow<List<FolderInfo>> = context.folderDataStore.data
        .map { preferences ->
            val jsonString = preferences[FOLDERS_KEY] ?: "[]"
            FolderSerializer.parseFolders(jsonString)
        }

    /**
     * Saves the list of folders to DataStore.
     * Serializes the list to JSON before saving.
     */
    suspend fun saveFolders(folders: List<FolderInfo>) {
        context.folderDataStore.edit { preferences ->
            preferences[FOLDERS_KEY] = FolderSerializer.serializeFolders(folders)
        }
    }
}
