package com.example.androidlauncher.data

import androidx.compose.ui.text.font.FontWeight

/**
 * Stufenlos einstellbare Schriftstärke (100–900).
 *
 * @property weightValue Numerischer Stärkewert (Thin=100 … Black=900).
 */
@JvmInline
value class FontWeightLevel(val weightValue: Int) {
    /** Die zugehörige Compose-[FontWeight]. */
    val weight: FontWeight get() = FontWeight(weightValue.coerceIn(MIN, MAX))

    /** Anzeigename, abgeleitet aus dem Stärkewert. */
    val label: String
        get() = when {
            weightValue <= 250 -> "Thin"
            weightValue <= 350 -> "Light"
            weightValue <= 450 -> "Normal"
            weightValue <= 550 -> "Medium"
            weightValue <= 650 -> "SemiBold"
            weightValue <= 750 -> "Bold"
            else -> "Black"
        }

    companion object {
        const val MIN = 100
        const val MAX = 900

        val NORMAL = FontWeightLevel(400)

        /** Erzeugt einen auf [MIN]..[MAX] begrenzten Wert. */
        fun of(value: Int) = FontWeightLevel(value.coerceIn(MIN, MAX))
    }
}
