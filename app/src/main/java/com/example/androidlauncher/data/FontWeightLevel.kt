package com.example.androidlauncher.data

import androidx.annotation.StringRes
import androidx.compose.ui.text.font.FontWeight
import com.example.androidlauncher.R

/**
 * Stufenlos einstellbare Schriftstärke (100–900).
 *
 * @property weightValue Numerischer Stärkewert (Thin=100 … Black=900).
 */
@JvmInline
value class FontWeightLevel(val weightValue: Int) {
    /** Die zugehörige Compose-[FontWeight]. */
    val weight: FontWeight get() = FontWeight(weightValue.coerceIn(MIN, MAX))

    /** Lokalisierte Anzeigename-Ressource, abgeleitet aus dem Stärkewert. */
    @get:StringRes
    val labelRes: Int
        get() = when {
            weightValue <= 250 -> R.string.font_weight_thin
            weightValue <= 350 -> R.string.font_weight_light
            weightValue <= 450 -> R.string.font_weight_normal
            weightValue <= 550 -> R.string.font_weight_medium
            weightValue <= 650 -> R.string.font_weight_semibold
            weightValue <= 750 -> R.string.font_weight_bold
            else -> R.string.font_weight_black
        }

    companion object {
        const val MIN = 100
        const val MAX = 900

        val NORMAL = FontWeightLevel(400)

        /** Erzeugt einen auf [MIN]..[MAX] begrenzten Wert. */
        fun of(value: Int) = FontWeightLevel(value.coerceIn(MIN, MAX))
    }
}
