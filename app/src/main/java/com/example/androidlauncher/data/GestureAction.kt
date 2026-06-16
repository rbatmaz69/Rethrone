package com.example.androidlauncher.data

/**
 * Aktion, die einer Geste (Doppeltippen, Schütteln, …) zugeordnet werden kann.
 *
 * Vom Nutzer frei wählbar und in DataStore als [name] persistiert. Die bisherigen
 * Schüttel-Konstanten behalten ihre Namen, damit gespeicherte Einstellungen gültig
 * bleiben; die ersten drei Werte sind neue Basis-Aktionen.
 */
enum class GestureAction {
    NONE,
    APP_DRAWER,
    SEARCH,
    NOTIFICATIONS,
    FLASHLIGHT,
    CAMERA,
    OPEN_APP,
    LOCK_SCREEN,
    TOGGLE_DND,
    OPEN_SETTINGS
}
