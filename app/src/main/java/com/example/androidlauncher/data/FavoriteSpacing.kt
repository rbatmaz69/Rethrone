package com.example.androidlauncher.data

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Vertikaler Abstand zwischen den App-Icons der Favoritenleiste auf dem Startbildschirm.
 *
 * @property label Anzeigename der Option.
 * @property spacing Abstand zwischen zwei Favoriten-Einträgen.
 */
enum class FavoriteSpacing(val label: String, val spacing: Dp) {
    ENG("Eng", 4.dp),
    KOMPAKT("Kompakt", 8.dp),
    STANDARD("Standard", 12.dp),
    LOCKER("Locker", 20.dp),
    WEIT("Weit", 28.dp)
}
