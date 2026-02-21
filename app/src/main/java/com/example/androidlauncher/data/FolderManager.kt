package com.example.androidlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.folderDataStore by preferencesDataStore(name = "folders")

class FolderManager(private val context: Context) {
    companion object {
        private val FOLDERS_KEY = stringPreferencesKey("app_folders")
    }

    val folders: Flow<List<FolderInfo>> = context.folderDataStore.data
        .map { preferences ->
            val jsonString = preferences[FOLDERS_KEY] ?: "[]"
            FolderSerializer.parseFolders(jsonString)
        }

    suspend fun saveFolders(folders: List<FolderInfo>) {
        context.folderDataStore.edit { preferences ->
            preferences[FOLDERS_KEY] = FolderSerializer.serializeFolders(folders)
        }
    }
}
