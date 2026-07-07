package com.example.androidlauncher.data.settings

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.androidlauncher.data.CryptoManager
import com.example.androidlauncher.data.GestureAction
import com.example.androidlauncher.data.SwipeBinding
import com.example.androidlauncher.data.SwipeDirection
import com.example.androidlauncher.data.SwipeGestureConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Gesten-Einstellungen: Schüttel- und Doppeltipp-Gesten samt Ziel-Apps sowie das
 * Haptik-Feedback (mit System-Setting synchronisiert). Aus dem ThemeManager
 * extrahiert (A1-Split); nutzt dieselbe DataStore-Datei wie zuvor, daher keine
 * Datenmigration. Ziel-Paketnamen werden via [CryptoManager] verschlüsselt abgelegt.
 */
class GestureSettings(
    private val dataStore: DataStore<Preferences>,
    private val context: Context,
) {

    /** Produktiv-Konstruktor: nutzt die geteilte "settings"-DataStore-Datei. */
    constructor(context: Context) : this(context.settingsDataStore, context)

    companion object {
        private val SHAKE_GESTURES_KEY = booleanPreferencesKey("shake_gestures_enabled")
        private val DOUBLE_SHAKE_ACTION_KEY = stringPreferencesKey("double_shake_action")
        private val SHAKE_OPEN_APP_PACKAGE_KEY = stringPreferencesKey("shake_open_app_package")
        private val DOUBLE_TAP_ACTION_KEY = stringPreferencesKey("double_tap_action")
        private val DOUBLE_TAP_APP_PACKAGE_KEY = stringPreferencesKey("double_tap_app_package")
        private val HAPTIC_FEEDBACK_KEY = booleanPreferencesKey("haptic_feedback_enabled")

        // U1: Keys pro Wisch-Richtung, z. B. "swipe_up_action" / "swipe_up_app_package".
        // Die *_app_package-Keys müssen in BackupSpec.ENCRYPTED_SETTINGS_KEYS gelistet sein.
        private fun swipeActionKey(direction: SwipeDirection) =
            stringPreferencesKey("swipe_${direction.name.lowercase()}_action")

        private fun swipeAppPackageKey(direction: SwipeDirection) =
            stringPreferencesKey("swipe_${direction.name.lowercase()}_app_package")
    }

    /** Observable flow for shake gesture toggle. */
    val isShakeGesturesEnabled: Flow<Boolean> = dataStore.data
        .map { it[SHAKE_GESTURES_KEY] ?: true }

    /** Aktion für doppeltes Schütteln (einzige Shake-Geste). Default Taschenlampe. */
    val doubleShakeAction: Flow<GestureAction> = dataStore.data
        .map { preferences ->
            val name = preferences[DOUBLE_SHAKE_ACTION_KEY] ?: GestureAction.FLASHLIGHT.name
            runCatching { GestureAction.valueOf(name) }.getOrDefault(GestureAction.FLASHLIGHT)
        }

    /** Paketname der App, die bei [GestureAction.OPEN_APP] (Schütteln) gestartet wird (null, wenn keine gewählt). */
    val shakeOpenAppPackage: Flow<String?> = dataStore.data
        .map { preferences ->
            CryptoManager.decryptOrLegacy(
                preferences[SHAKE_OPEN_APP_PACKAGE_KEY]
            ).takeIf { it.isNotBlank() }
        }

    /**
     * Aktion für die Doppeltipp-Geste auf dem Startbildschirm. Default: Bildschirm sperren
     * (entspricht dem bisherigen, fest verdrahteten Verhalten).
     */
    val doubleTapAction: Flow<GestureAction> = dataStore.data
        .map { preferences ->
            val name = preferences[DOUBLE_TAP_ACTION_KEY] ?: GestureAction.LOCK_SCREEN.name
            runCatching { GestureAction.valueOf(name) }.getOrDefault(GestureAction.LOCK_SCREEN)
        }

    /** Paketname der App, die bei [GestureAction.OPEN_APP] (Doppeltippen) gestartet wird (null, wenn keine gewählt). */
    val doubleTapAppPackage: Flow<String?> = dataStore.data
        .map { preferences ->
            CryptoManager.decryptOrLegacy(
                preferences[DOUBLE_TAP_APP_PACKAGE_KEY]
            ).takeIf { it.isNotBlank() }
        }

    /**
     * Komplette Swipe-Gesten-Belegung des Startbildschirms (U1). Ein einzelner
     * `map` über den DataStore statt kombinierter Einzel-Flows – ein Emit pro Änderung.
     */
    val swipeGestureConfig: Flow<SwipeGestureConfig> = dataStore.data
        .map { preferences ->
            SwipeGestureConfig(
                SwipeDirection.entries.associateWith { direction ->
                    val name = preferences[swipeActionKey(direction)]
                    SwipeBinding(
                        action = runCatching { GestureAction.valueOf(name ?: "") }
                            .getOrDefault(SwipeGestureConfig.defaultActionFor(direction)),
                        appPackage = CryptoManager.decryptOrLegacy(
                            preferences[swipeAppPackageKey(direction)]
                        ).takeIf { it.isNotBlank() },
                    )
                }
            )
        }

    /**
     * Observable flow for haptic feedback toggle.
     * Synchronized with system setting HAPTIC_FEEDBACK_ENABLED using a ContentObserver.
     */
    val isHapticFeedbackEnabled: Flow<Boolean> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                val enabled = Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.HAPTIC_FEEDBACK_ENABLED,
                    1
                ) != 0
                trySend(enabled)
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.HAPTIC_FEEDBACK_ENABLED),
            false,
            observer
        )
        // Send initial value
        val initial = Settings.System.getInt(
            context.contentResolver,
            Settings.System.HAPTIC_FEEDBACK_ENABLED,
            1
        ) != 0
        trySend(initial)

        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }.distinctUntilChanged()

    /** Toggles shake gestures. */
    suspend fun setShakeGesturesEnabled(enabled: Boolean) {
        dataStore.edit { it[SHAKE_GESTURES_KEY] = enabled }
    }

    /** Setzt die Aktion für doppeltes Schütteln. */
    suspend fun setDoubleShakeAction(action: GestureAction) {
        dataStore.edit { it[DOUBLE_SHAKE_ACTION_KEY] = action.name }
    }

    /** Setzt das Paket der App, die bei [GestureAction.OPEN_APP] (Schütteln) gestartet wird (null löscht die Wahl). */
    suspend fun setShakeOpenAppPackage(packageName: String?) {
        dataStore.edit { preferences ->
            if (packageName.isNullOrBlank()) {
                preferences.remove(SHAKE_OPEN_APP_PACKAGE_KEY)
            } else {
                preferences[SHAKE_OPEN_APP_PACKAGE_KEY] = CryptoManager.encrypt(packageName)
            }
        }
    }

    /** Setzt die Aktion für die Doppeltipp-Geste. */
    suspend fun setDoubleTapAction(action: GestureAction) {
        dataStore.edit { it[DOUBLE_TAP_ACTION_KEY] = action.name }
    }

    /** Setzt das Paket der App, die bei [GestureAction.OPEN_APP] (Doppeltippen) gestartet wird (null löscht die Wahl). */
    suspend fun setDoubleTapAppPackage(packageName: String?) {
        dataStore.edit { preferences ->
            if (packageName.isNullOrBlank()) {
                preferences.remove(DOUBLE_TAP_APP_PACKAGE_KEY)
            } else {
                preferences[DOUBLE_TAP_APP_PACKAGE_KEY] = CryptoManager.encrypt(packageName)
            }
        }
    }

    /** Setzt die Aktion für eine Wisch-Richtung auf dem Startbildschirm. */
    suspend fun setSwipeAction(direction: SwipeDirection, action: GestureAction) {
        dataStore.edit { it[swipeActionKey(direction)] = action.name }
    }

    /** Setzt das Paket der App für [GestureAction.OPEN_APP] der Wisch-Richtung (null löscht die Wahl). */
    suspend fun setSwipeAppPackage(direction: SwipeDirection, packageName: String?) {
        dataStore.edit { preferences ->
            if (packageName.isNullOrBlank()) {
                preferences.remove(swipeAppPackageKey(direction))
            } else {
                preferences[swipeAppPackageKey(direction)] = CryptoManager.encrypt(packageName)
            }
        }
    }

    /** Toggles haptic feedback both internally and in system settings if permission is granted. */
    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        // Try to update system setting
        try {
            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.HAPTIC_FEEDBACK_ENABLED,
                    if (enabled) 1 else 0
                )
            }
        } catch (_: Exception) {
            // Permission might be missing
        }

        // Also update internally to stay consistent
        dataStore.edit { it[HAPTIC_FEEDBACK_KEY] = enabled }
    }
}
