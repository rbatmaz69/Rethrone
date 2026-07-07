package com.example.androidlauncher.data.backup

/**
 * Definiert, was ein Settings-Backup (B5) enthält und was bewusst nicht.
 *
 * Reine Konstanten-Sammlung ohne Android-Abhängigkeiten, damit die Regeln
 * unabhängig von DataStore/Krypto testbar sind.
 */
object BackupSpec {

    /** Aktuelle Schema-Version der Backup-Datei. Neuere Dateien werden abgelehnt. */
    const val CURRENT_VERSION = 1

    /** Logische Store-Namen im JSON – entsprechen den DataStore-Dateinamen. */
    const val STORE_SETTINGS = "settings"
    const val STORE_FAVORITES = "favorites"
    const val STORE_FOLDERS = "folders"
    const val STORE_ICON_MAPPINGS = "icon_mappings"

    val STORE_NAMES = listOf(STORE_SETTINGS, STORE_FAVORITES, STORE_FOLDERS, STORE_ICON_MAPPINGS)

    /**
     * Keys der "settings"-Datei, die weder exportiert noch beim Restore
     * überschrieben werden:
     * - App-Lock-Credentials (`lock_*`, `locked_apps`): Geheimnisse gehören nie in
     *   eine Export-Datei; eine Locked-Liste ohne zugehöriges Secret wäre inkonsistent.
     * - `custom_wallpaper_uri`: zeigt auf eine gerätelokale Datei im cacheDir.
     * - `hosted_widgets`: appWidgetIds sind gerätespezifisch; Widgets werden
     *   stattdessen id-los in der eigenen `widgets`-Sektion exportiert.
     */
    val EXCLUDED_SETTINGS_KEYS = setOf(
        "lock_secret",
        "lock_type",
        "lock_biometric_enabled",
        "locked_apps",
        "custom_wallpaper_uri",
        "hosted_widgets",
    )

    /**
     * Keys, deren Werte via CryptoManager gerätegebunden verschlüsselt liegen.
     * Export legt sie im Klartext ab (Ciphertext wäre auf jedem anderen Gerät
     * nutzlos), Import verschlüsselt sie mit dem lokalen Schlüssel neu.
     */
    val ENCRYPTED_SETTINGS_KEYS = setOf(
        "hidden_apps",
        "shake_open_app_package",
        "double_tap_app_package",
        "swipe_up_app_package",
        "swipe_down_app_package",
        "swipe_left_app_package",
        "swipe_right_app_package",
    )
}
