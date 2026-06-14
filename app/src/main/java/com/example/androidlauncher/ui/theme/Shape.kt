package com.example.androidlauncher.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Zentrale Form-Skala im Material-3-Expressive-Stil.
 *
 * Statt verstreuter, hartkodierter `RoundedCornerShape(...)`-Werte gibt es eine
 * einzige, großzügig gerundete Skala, die universell für ALLE Design-Styles gilt
 * und dem Launcher den weichen, runden Android-15/16-Look verleiht.
 *
 * Verwendung:
 * - `MaterialTheme.shapes.small/medium/large/...` in Composables, oder
 * - die benannten Aliase [RethroneShape] für Sonderfälle (z. B. Pille).
 */
val RethroneShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),  // Chips, kleine Tags
    small = RoundedCornerShape(16.dp),       // kleine Buttons, Zeilen
    medium = RoundedCornerShape(22.dp),      // Karten, Menü-Zeilen
    large = RoundedCornerShape(28.dp),       // Tiles, Container
    extraLarge = RoundedCornerShape(36.dp),  // große Sheets / Modals
)

/**
 * Benannte Sonderformen, die nicht in das Material3-`Shapes`-Schema passen.
 */
object RethroneShape {
    /** Vollständig abgerundete „Pille" für Buttons, Such-/Chip-Leisten. */
    val Pill = RoundedCornerShape(percent = 50)

    /** Sheet-Form: nur oben stark gerundet (für von unten aufsteigende Menüs). */
    val SheetTop = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
}
