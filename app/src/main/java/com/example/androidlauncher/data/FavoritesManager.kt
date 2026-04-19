package com.example.androidlauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Eigene DataStore-Instanz für Favoriten. */
val Context.favoritesDataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")

/**
 * Verwaltet die persistente Speicherung der Favoriten-Pakete.
 *
 * Ersetzt die bisherige SharedPreferences-basierte Lösung und verwendet
 * stattdessen DataStore für konsistente, nicht-blockierende Persistenz
 * im gesamten Projekt.
 */
class FavoritesManager(
    private val dataStore: DataStore<Preferences>,
    private val context: Context,
) {

    // Hilfskonstruktor für einfache Kompatibilität
    constructor(context: Context) : this(context.favoritesDataStore, context)

    companion object {
        /** Schlüssel unter dem die komma-separierte Favoritenliste gespeichert wird. */
        private val FAVORITES_KEY = stringPreferencesKey("favorites_list")
    }

    /**
     * Reaktiver Flow der aktuellen Favoritenliste.
     * Emittiert automatisch bei jeder Änderung.
     */
    val favorites: Flow<List<String>> = dataStore.data
        .map { preferences ->
            val raw = preferences[FAVORITES_KEY] ?: ""
            if (raw.isEmpty()) emptyList() else raw.split(",")
        }

    /**
     * Speichert die Favoritenliste.
     * @param favorites Liste der Paket-Namen in gewünschter Reihenfolge.
     */
    suspend fun saveFavorites(favorites: List<String>) {
        dataStore.edit { preferences ->
            preferences[FAVORITES_KEY] = favorites.joinToString(",")
        }
    }

    /**
     * Migriert bestehende SharedPreferences-Daten in den DataStore.
     * Sollte einmalig beim ersten Start nach dem Update aufgerufen werden.
     * Löscht die alten SharedPreferences nach erfolgreicher Migration.
     */
    suspend fun migrateFromSharedPreferences(context: Context) {
        val prefs = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
        val existing = prefs.getString("favorites_list", null)
        if (existing != null) {
            // Nur migrieren wenn DataStore noch leer ist
            dataStore.edit { preferences ->
                if (preferences[FAVORITES_KEY] == null) {
                    preferences[FAVORITES_KEY] = existing
                }
            }
            // Alte Daten entfernen
            prefs.edit().remove("favorites_list").apply()
        }
    }
}
