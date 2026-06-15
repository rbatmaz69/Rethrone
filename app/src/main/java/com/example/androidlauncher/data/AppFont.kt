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
    // ── Standard (System, immer offline) ─────────────────────────
    SYSTEM_DEFAULT("System Standard", FontFamily.Default, "Standard"),
    SANS_SERIF("Sans Serif", FontFamily.SansSerif, "Standard"),
    SERIF("Serif", FontFamily.Serif, "Standard"),
    MONOSPACE("Monospace", FontFamily.Monospace, "Standard"),

    // ── Sans-Serif (Google Fonts) ────────────────────────────────
    INTER("Inter", googleFontFamily("Inter"), "Sans-Serif"),
    POPPINS("Poppins", googleFontFamily("Poppins"), "Sans-Serif"),
    MONTSERRAT("Montserrat", googleFontFamily("Montserrat"), "Sans-Serif"),
    NUNITO("Nunito", googleFontFamily("Nunito"), "Sans-Serif"),
    RUBIK("Rubik", googleFontFamily("Rubik"), "Sans-Serif"),
    QUICKSAND("Quicksand", googleFontFamily("Quicksand"), "Sans-Serif"),
    WORK_SANS("Work Sans", googleFontFamily("Work Sans"), "Sans-Serif"),
    COMFORTAA("Comfortaa", googleFontFamily("Comfortaa"), "Sans-Serif"),
    MANROPE("Manrope", googleFontFamily("Manrope"), "Sans-Serif"),
    MULISH("Mulish", googleFontFamily("Mulish"), "Sans-Serif"),
    JOSEFIN_SANS("Josefin Sans", googleFontFamily("Josefin Sans"), "Sans-Serif"),
    LEXEND("Lexend", googleFontFamily("Lexend"), "Sans-Serif"),

    // ── Serif (Google Fonts) ─────────────────────────────────────
    MERRIWEATHER("Merriweather", googleFontFamily("Merriweather"), "Serif"),
    PLAYFAIR_DISPLAY("Playfair Display", googleFontFamily("Playfair Display"), "Serif"),
    LORA("Lora", googleFontFamily("Lora"), "Serif"),
    BITTER("Bitter", googleFontFamily("Bitter"), "Serif"),
    EB_GARAMOND("EB Garamond", googleFontFamily("EB Garamond"), "Serif"),
    PT_SERIF("PT Serif", googleFontFamily("PT Serif"), "Serif"),
    CORMORANT("Cormorant", googleFontFamily("Cormorant"), "Serif"),

    // ── Monospace (Google Fonts) ─────────────────────────────────
    ROBOTO_MONO("Roboto Mono", googleFontFamily("Roboto Mono"), "Monospace"),
    JETBRAINS_MONO("JetBrains Mono", googleFontFamily("JetBrains Mono"), "Monospace"),
    FIRA_CODE("Fira Code", googleFontFamily("Fira Code"), "Monospace"),
    SPACE_MONO("Space Mono", googleFontFamily("Space Mono"), "Monospace"),
    SOURCE_CODE_PRO("Source Code Pro", googleFontFamily("Source Code Pro"), "Monospace"),
    IBM_PLEX_MONO("IBM Plex Mono", googleFontFamily("IBM Plex Mono"), "Monospace"),

    // ── Display / Markant ────────────────────────────────────────
    OSWALD("Oswald", googleFontFamily("Oswald"), "Display"),
    BEBAS_NEUE("Bebas Neue", googleFontFamily("Bebas Neue"), "Display"),
    ANTON("Anton", googleFontFamily("Anton"), "Display"),
    ABRIL_FATFACE("Abril Fatface", googleFontFamily("Abril Fatface"), "Display"),
    RIGHTEOUS("Righteous", googleFontFamily("Righteous"), "Display"),
    ARCHIVO_BLACK("Archivo Black", googleFontFamily("Archivo Black"), "Display"),
    TITAN_ONE("Titan One", googleFontFamily("Titan One"), "Display"),

    // ── Handschrift / Dekorativ (Google Fonts) ───────────────────
    LOBSTER("Lobster", googleFontFamily("Lobster"), "Dekorativ"),
    PACIFICO("Pacifico", googleFontFamily("Pacifico"), "Dekorativ"),
    CAVEAT("Caveat", googleFontFamily("Caveat"), "Dekorativ"),
    DANCING_SCRIPT("Dancing Script", googleFontFamily("Dancing Script"), "Dekorativ"),
    SATISFY("Satisfy", googleFontFamily("Satisfy"), "Dekorativ"),
    PERMANENT_MARKER("Permanent Marker", googleFontFamily("Permanent Marker"), "Dekorativ"),
    SHADOWS_INTO_LIGHT("Shadows Into Light", googleFontFamily("Shadows Into Light"), "Dekorativ"),
    INDIE_FLOWER("Indie Flower", googleFontFamily("Indie Flower"), "Dekorativ"),
    SACRAMENTO("Sacramento", googleFontFamily("Sacramento"), "Dekorativ");

    companion object {
        /** Alle verfügbaren Kategorien in Anzeigereihenfolge. */
        val categories: List<String>
            get() = entries.map { it.category }.distinct()
    }
}
