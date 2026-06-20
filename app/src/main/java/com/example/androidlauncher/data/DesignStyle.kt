package com.example.androidlauncher.data

import androidx.annotation.StringRes
import com.example.androidlauncher.R

/**
 * Wählbarer Oberflächen-Stil der UI (Karten, Such-/Menüflächen etc.).
 *
 * Ersetzt das frühere Boolean `liquid_glass_enabled` und erlaubt mehrere
 * Designs. Wird über `LocalDesignStyle` bereitgestellt und zentral via
 * `Modifier.designSurface(...)` in [com.example.androidlauncher.ui.LiquidGlass]
 * angewendet. Anzeigetexte werden lokalisiert über [titleRes]/[descRes] aufgelöst.
 */
enum class DesignStyle(@StringRes val titleRes: Int, @StringRes val descRes: Int) {
    GLASS(R.string.design_style_glass_name, R.string.design_style_glass_desc),
    FLAT(R.string.design_style_flat_name, R.string.design_style_flat_desc),
    MINIMAL(R.string.design_style_minimal_name, R.string.design_style_minimal_desc),
    OUTLINE(R.string.design_style_outline_name, R.string.design_style_outline_desc),
    SOLID(R.string.design_style_solid_name, R.string.design_style_solid_desc),
    TINTED(R.string.design_style_tinted_name, R.string.design_style_tinted_desc);

    /** Ob dieser Stil eine Glas-/Gradient-Optik besitzt (für Switch-/Tint-Farben). */
    val isGlassLike: Boolean get() = this == GLASS || this == TINTED

    companion object {
        /** Liest einen gespeicherten Schlüssel; fällt bei Unbekanntem auf [GLASS] zurück. */
        fun fromKey(key: String?): DesignStyle = entries.find { it.name == key } ?: GLASS
    }
}
