package com.example.androidlauncher

/**
 * Merkt sich prozessweit, welche Favoriten ihre einmalige Einblend-Animation
 * (gestaffeltes Aufpoppen) bereits gespielt haben. Verhindert, dass die
 * Animation bei jeder Rückkehr aus dem App-Drawer erneut abläuft – dabei
 * verlässt HomeScreen die Composition und betritt sie neu, wodurch sonst der
 * Eingangs-Effekt wieder bei 0 startet.
 *
 * Der Status gilt nur für die aktuelle Prozess-Lebenszeit: Startet das
 * Launcher-Prozess neu (z. B. nach OS-Kill), spielt die Animation wieder einmal.
 */
object FavoritesEntranceTracker {
    private val appeared = mutableSetOf<String>()

    fun hasAppeared(packageName: String): Boolean = appeared.contains(packageName)

    fun markAppeared(packageName: String) { appeared.add(packageName) }
}
