package com.example.androidlauncher.data

/**
 * Aktion, die einer Shake-Geste zugeordnet werden kann.
 *
 * Vom Nutzer für das doppelte Schütteln frei wählbar und in DataStore persistiert.
 * Erweiterbar für zukünftige Shake-Aktionen.
 */
enum class ShakeAction {
    NONE,
    FLASHLIGHT,
    CAMERA,
    OPEN_APP,
    LOCK_SCREEN,
    TOGGLE_DND,
    OPEN_SETTINGS
}
