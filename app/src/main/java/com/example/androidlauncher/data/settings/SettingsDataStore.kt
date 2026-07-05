package com.example.androidlauncher.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Geteiltes DataStore-Delegate der allgemeinen App-Einstellungen (Datei "settings").
 *
 * Historisch lag der Zugriff privat im ThemeManager; im Zuge des Splits in
 * domänen-spezifische Stores (siehe `data/settings/`) teilen sich alle Stores und
 * die verbleibende ThemeManager-Fassade **dieselbe** DataStore-Datei – dadurch ist
 * keine Migration der Nutzerdaten nötig. Pro Prozess darf es nur eine
 * DataStore-Instanz pro Datei geben, daher muss jeder Zugriff über dieses
 * Delegate laufen.
 */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
