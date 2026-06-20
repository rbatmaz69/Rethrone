package com.example.androidlauncher.data

import androidx.annotation.StringRes
import com.example.androidlauncher.R

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
enum class AppAccessMode(@StringRes val labelRes: Int) {
    DRAWER_GRID(R.string.app_access_drawer_grid),
    DRAWER_LIST(R.string.app_access_drawer_list),
    HOME_LIST(R.string.app_access_home_list)
}
