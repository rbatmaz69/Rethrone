package com.example.androidlauncher.data

/**
 * Art des App-Zugriffs auf der Startseite.
 *
 * Vom Nutzer im Bearbeiten-Menü wählbar und in DataStore persistiert. Die drei Optionen schließen sich
 * gegenseitig aus – es ist immer genau ein Zugriffsweg aktiv.
 * - [DRAWER_GRID]: hochziehbarer App-Drawer im Grid-Layout (Standard).
 * - [DRAWER_LIST]: hochziehbarer App-Drawer als Vollbild-Liste (Niagara-Stil, mit A–Z-Leiste).
 * - [HOME_LIST]: keine hochziehbare Übersicht; stattdessen eine A–Z-Schnellleiste direkt am rechten
 *   Rand der Startseite (Berühren + Gleiten + Loslassen startet die App).
 */
enum class AppAccessMode(val label: String) {
    DRAWER_GRID("App-Drawer: Grid"),
    DRAWER_LIST("App-Drawer: Liste"),
    HOME_LIST("Startseiten-Liste")
}
