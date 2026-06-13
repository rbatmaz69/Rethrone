package com.example.androidlauncher.data

/**
 * Wählbarer Oberflächen-Stil der UI (Karten, Such-/Menüflächen etc.).
 *
 * Ersetzt das frühere Boolean `liquid_glass_enabled` und erlaubt mehrere
 * Designs. Wird über `LocalDesignStyle` bereitgestellt und zentral via
 * `Modifier.designSurface(...)` in [com.example.androidlauncher.ui.LiquidGlass]
 * angewendet.
 */
enum class DesignStyle(val displayName: String, val description: String) {
    GLASS("Liquid Glass", "Frostiges, transluzentes Glas"),
    FLAT("Standard", "Schlichter, halbtransparenter Hintergrund"),
    MINIMAL("Minimal", "Kein Hintergrund – nur Text & Icons"),
    OUTLINE("Outline", "Nur feine Umrandung, innen transparent"),
    SOLID("Solid", "Deckende, eingefärbte Karten"),
    TINTED("Tinted", "Glas im Theme-Farbton getönt");

    /** Ob dieser Stil eine Glas-/Gradient-Optik besitzt (für Switch-/Tint-Farben). */
    val isGlassLike: Boolean get() = this == GLASS || this == TINTED

    companion object {
        /** Liest einen gespeicherten Schlüssel; fällt bei Unbekanntem auf [GLASS] zurück. */
        fun fromKey(key: String?): DesignStyle = entries.find { it.name == key } ?: GLASS
    }
}
