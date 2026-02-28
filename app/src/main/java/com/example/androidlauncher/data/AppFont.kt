package com.example.androidlauncher.data

import androidx.compose.ui.text.font.FontFamily

/**
 * Verfügbare Schriftarten für den Launcher.
 *
 * Jeder Eintrag repräsentiert eine **tatsächlich visuell unterscheidbare** Schriftart.
 * Zuvor enthielt dieses Enum 28 Einträge, die auf nur 5 System-FontFamilies gemappt
 * waren — viele sahen dadurch identisch aus und verwirrten Nutzer.
 *
 * Die Einträge sind nach typografischer Kategorie gruppiert:
 * - **Sans-Serif**: Moderne, klare Schriften ohne Serifen
 * - **Serif**: Klassische Schriften mit Serifen
 * - **Monospace**: Schriften mit fester Zeichenbreite
 * - **Cursive**: Handschriftliche / dekorative Schriften
 *
 * @property label Anzeigename in der Schriftart-Auswahl.
 * @property fontFamily Die Compose [FontFamily] für die Darstellung.
 * @property category Typografische Kategorie für Gruppierung in der UI.
 */
enum class AppFont(
    val label: String,
    val fontFamily: FontFamily,
    val category: String
) {
    // ── Standard ─────────────────────────────────────────────────
    SYSTEM_DEFAULT("System Standard", FontFamily.Default, "Standard"),

    // ── Sans-Serif ───────────────────────────────────────────────
    SANS_SERIF("Sans Serif", FontFamily.SansSerif, "Sans-Serif"),

    // ── Serif ────────────────────────────────────────────────────
    SERIF("Serif", FontFamily.Serif, "Serif"),

    // ── Monospace ────────────────────────────────────────────────
    MONOSPACE("Monospace", FontFamily.Monospace, "Monospace"),

    // ── Handschrift / Dekorativ ──────────────────────────────────
    CURSIVE("Handschrift", FontFamily.Cursive, "Dekorativ");

    companion object {
        /** Alle verfügbaren Kategorien in Anzeigereihenfolge. */
        val categories: List<String>
            get() = entries.map { it.category }.distinct()
    }
}
