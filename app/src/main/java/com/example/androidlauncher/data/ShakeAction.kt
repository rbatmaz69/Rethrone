package com.example.androidlauncher.data

/**
 * Aktion, die einer Shake-Geste zugeordnet werden kann.
 *
 * Vom Nutzer pro Geste (1×/2× Schütteln) frei wählbar und in DataStore persistiert.
 * Erweiterbar für zukünftige Shake-Aktionen.
 */
enum class ShakeAction {
    NONE,
    FLASHLIGHT,
    CAMERA
}
