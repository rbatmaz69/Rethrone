package com.example.androidlauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Eigene DataStore-Instanz für Ordner. */
val Context.folderDataStore: DataStore<Preferences> by preferencesDataStore(name = "folders")

/**
 * Verwaltet die persistente Speicherung der Ordnerstrukturen anhand der App-IDs.
 */
class FolderManager(
    private val dataStore: DataStore<Preferences>,
    private val context: Context,
) {
    // Hilfskonstruktor für einfache Kompatibilität
    constructor(context: Context) : this(context.folderDataStore, context)

    companion object {
        private val FOLDERS_KEY = stringPreferencesKey("folders_list")
    }

    /**
     * Reaktiver Flow der aktuellen Ordner.
     */
    val folders: Flow<List<FolderInfo>> = dataStore.data
        .map { preferences ->
            val jsonString = preferences[FOLDERS_KEY] ?: "[]"
            FolderSerializer.parseFolders(jsonString)
        }

    /**
     * Speichert die gesamte Ordner-Liste.
     */
    suspend fun saveFolders(folders: List<FolderInfo>) {
        dataStore.edit { preferences ->
            preferences[FOLDERS_KEY] = FolderSerializer.serializeFolders(folders)
        }
    }

    /**
     * Erstellt einen neuen Ordner oder aktualisiert einen bestehenden.
     */
    suspend fun createOrUpdateFolder(folder: FolderInfo) {
        dataStore.edit { preferences ->
            val currentFolders = preferences[FOLDERS_KEY]?.let { FolderSerializer.parseFolders(it) } ?: emptyList()
            val updatedFolders = currentFolders.toMutableList()

            // Suche nach bestehendem Ordner mit derselben ID
            val existingFolderIndex = updatedFolders.indexOfFirst { it.id == folder.id }
            if (existingFolderIndex != -1) {
                // Aktualisiere den bestehenden Ordner
                updatedFolders[existingFolderIndex] = folder
            } else {
                // Füge den neuen Ordner hinzu
                updatedFolders.add(folder)
            }

            preferences[FOLDERS_KEY] = FolderSerializer.serializeFolders(updatedFolders)
        }
    }

    /**
     * Löscht einen Ordner anhand seiner ID.
     */
    suspend fun deleteFolder(folderId: String) {
        dataStore.edit { preferences ->
            val currentFolders = preferences[FOLDERS_KEY]?.let { FolderSerializer.parseFolders(it) } ?: emptyList()
            val updatedFolders = currentFolders.filter { it.id != folderId }

            preferences[FOLDERS_KEY] = FolderSerializer.serializeFolders(updatedFolders)
        }
    }

    /**
     * Migriert die Ordnerdaten von den alten SharedPreferences zu DataStore.
     */
    suspend fun migrateFromSharedPreferences(context: Context) {
        val prefs = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
        val existing = prefs.getString("folders_list", null)
        if (existing != null) {
            dataStore.edit { preferences ->
                if (preferences[FOLDERS_KEY] == null) {
                    preferences[FOLDERS_KEY] = existing
                }
            }
            prefs.edit().remove("folders_list").apply()
        }
    }
}
