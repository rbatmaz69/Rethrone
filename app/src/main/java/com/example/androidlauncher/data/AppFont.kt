package com.example.androidlauncher.data

import androidx.compose.ui.text.font.FontFamily

/**
 * Enum representing available font families.
 * @property label Display name of the font option.
 * @property fontFamily The Compose FontFamily associated with the selection.
 */
enum class AppFont(val label: String, val fontFamily: FontFamily) {
    SYSTEM_DEFAULT("System Default", FontFamily.Default),
    SANS_SERIF("Sans Serif", FontFamily.SansSerif),
    SERIF("Serif", FontFamily.Serif),
    MONOSPACE("Monospace", FontFamily.Monospace),
    CURSIVE("Cursive", FontFamily.Cursive),
    ROBOTO("Roboto", FontFamily.SansSerif),
    SLAB_SERIF("Slab Serif", FontFamily.Serif),
    DYNAMIC_SANS("Dynamic Sans", FontFamily.SansSerif),
    MODERN_TYPE("Modern Type", FontFamily.Monospace),
    CLASSIC_BOOK("Classic Book", FontFamily.Serif),
    ELEGANT_SCRIPT("Elegant Script", FontFamily.Cursive),
    CLEAN_SANS("Clean Sans", FontFamily.SansSerif),
    BOLD_MONO("Bold Mono", FontFamily.Monospace),
    SOFT_SERIF("Soft Serif", FontFamily.Serif),
    QUICK_SANS("Quick Sans", FontFamily.SansSerif),
    STURDY_TYPE("Sturdy Type", FontFamily.SansSerif),
    LITE_SERIF("Lite Serif", FontFamily.Serif),
    FUTURISTIC("Futuristic", FontFamily.SansSerif),
    RETRO_MONO("Retro Mono", FontFamily.Monospace),
    FANCY_CURSIVE("Fancy Cursive", FontFamily.Cursive),
    GEOMETRIC("Geometric", FontFamily.SansSerif),
    HUMANIST("Humanist", FontFamily.SansSerif),
    OLD_STYLE("Old Style", FontFamily.Serif),
    TRANSITIONAL("Transitional", FontFamily.Serif),
    NEO_GROTESQUE("Neo-Grotesque", FontFamily.SansSerif),
    EGYPTIAN("Egyptian", FontFamily.Serif),
    STENCIL("Stencil", FontFamily.Monospace),
    DECORATIVE("Decorative", FontFamily.Cursive),
    HANDWRITTEN("Handwritten", FontFamily.Cursive)
}
