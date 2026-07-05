package com.example.androidlauncher.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.androidlauncher.data.AppAccessMode
import com.example.androidlauncher.data.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Privatsphäre-Einstellungen: ausgeblendete und gesperrte Apps, das
 * Sperr-Geheimnis (PIN/Muster) samt Biometrie-Option sowie der App-Zugriffsmodus.
 * Aus dem ThemeManager extrahiert (A1-Split); nutzt dieselbe DataStore-Datei wie
 * zuvor, daher keine Datenmigration. Sensible Werte liegen via [CryptoManager]
 * verschlüsselt im DataStore.
 */
class PrivacySettings(private val dataStore: DataStore<Preferences>) {

    /** Produktiv-Konstruktor: nutzt die geteilte "settings"-DataStore-Datei. */
    constructor(context: Context) : this(context.settingsDataStore)

    companion object {
        // Komma-separierte Paketnamen der ausgeblendeten Apps (verschlüsselt).
        private val HIDDEN_APPS_KEY = stringPreferencesKey("hidden_apps")

        // App-Sperre: Komma-separierte Paketnamen der gesperrten Apps (verschlüsselt).
        private val LOCKED_APPS_KEY = stringPreferencesKey("locked_apps")

        // Typ des Sperr-Geheimnisses: "pin", "pattern" oder "none".
        private val LOCK_TYPE_KEY = stringPreferencesKey("lock_type")

        // Gesalzener Hash des PIN/Musters (Format salt:hash, verschlüsselt). Nie das Geheimnis selbst.
        private val LOCK_SECRET_KEY = stringPreferencesKey("lock_secret")

        // Ob zusätzlich per Biometrie/Geräte-Credential entsperrt werden darf.
        private val LOCK_BIOMETRIC_KEY = booleanPreferencesKey("lock_biometric_enabled")

        private val APP_ACCESS_MODE_KEY = stringPreferencesKey("app_access_mode")
    }

    // Ausgeblendete Apps (Paketnamen). Werden überall aus der Anzeige gefiltert.
    val hiddenApps: Flow<Set<String>> = dataStore.data
        .map { prefs ->
            // Wert ist verschlüsselt abgelegt; decryptOrLegacy migriert alten Klartext transparent.
            val raw = CryptoManager.decryptOrLegacy(prefs[HIDDEN_APPS_KEY])
            if (raw.isEmpty()) emptySet() else raw.split(",").filter { it.isNotEmpty() }.toSet()
        }

    // Gesperrte Apps (Paketnamen). Vor dem Öffnen muss authentifiziert werden.
    val lockedApps: Flow<Set<String>> = dataStore.data
        .map { prefs ->
            val raw = CryptoManager.decryptOrLegacy(prefs[LOCKED_APPS_KEY])
            if (raw.isEmpty()) emptySet() else raw.split(",").filter { it.isNotEmpty() }.toSet()
        }

    // Art des hinterlegten Sperr-Geheimnisses ("pin", "pattern", "none").
    val lockType: Flow<String> = dataStore.data
        .map { it[LOCK_TYPE_KEY] ?: "none" }

    // Roh-Token des gespeicherten Geheimnisses (salt:hash, entschlüsselt). Leer = kein Code gesetzt.
    val lockSecret: Flow<String> = dataStore.data
        .map { CryptoManager.decryptOrLegacy(it[LOCK_SECRET_KEY]) }

    val isLockBiometricEnabled: Flow<Boolean> = dataStore.data
        .map { it[LOCK_BIOMETRIC_KEY] ?: false }

    /** Gewählte Art des App-Zugriffs. Default ist die Drawer-Liste (Niagara-Stil). */
    val appAccessMode: Flow<AppAccessMode> = dataStore.data
        .map { preferences ->
            val name = preferences[APP_ACCESS_MODE_KEY] ?: AppAccessMode.DRAWER_LIST.name
            runCatching { AppAccessMode.valueOf(name) }.getOrDefault(AppAccessMode.DRAWER_LIST)
        }

    suspend fun setHiddenApps(packages: Set<String>) {
        dataStore.edit { it[HIDDEN_APPS_KEY] = CryptoManager.encrypt(packages.joinToString(",")) }
    }

    suspend fun setLockedApps(packages: Set<String>) {
        dataStore.edit { it[LOCKED_APPS_KEY] = CryptoManager.encrypt(packages.joinToString(",")) }
    }

    /** Speichert Typ ("pin"/"pattern") und gesalzenen Hash-Token des Geheimnisses. */
    suspend fun setLockSecret(type: String, secretToken: String) {
        dataStore.edit {
            it[LOCK_TYPE_KEY] = type
            it[LOCK_SECRET_KEY] = CryptoManager.encrypt(secretToken)
        }
    }

    /** Entfernt den hinterlegten Code (Typ zurück auf "none"). */
    suspend fun clearLockSecret() {
        dataStore.edit {
            it[LOCK_TYPE_KEY] = "none"
            it.remove(LOCK_SECRET_KEY)
        }
    }

    suspend fun setLockBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[LOCK_BIOMETRIC_KEY] = enabled }
    }

    /** Setzt die gewählte Art des App-Zugriffs. */
    suspend fun setAppAccessMode(mode: AppAccessMode) {
        dataStore.edit { it[APP_ACCESS_MODE_KEY] = mode.name }
    }
}
