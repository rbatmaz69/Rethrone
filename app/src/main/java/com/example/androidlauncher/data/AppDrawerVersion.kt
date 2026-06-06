package com.example.androidlauncher.data

/**
 * Layout-Variante des AppDrawers.
 *
 * Vom Nutzer im Bearbeiten-Menü wählbar und in DataStore persistiert.
 * - [VERSION_1]: klassisches Grid-Layout (Standard).
 * - [VERSION_2]: alphabetische Liste mit A–Z-Schnellnavigation (Niagara-Stil).
 */
enum class AppDrawerVersion(val label: String) {
    VERSION_1("Grid (Standard)"),
    VERSION_2("Liste (Niagara)")
}
