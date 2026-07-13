package com.example.androidlauncher.data

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Stufenlos einstellbarer vertikaler Abstand zwischen den App-Icons der
 * Favoritenleiste auf dem Startbildschirm.
 *
 * @property spacing Abstand zwischen zwei Favoriten-Einträgen.
 */
@JvmInline
value class FavoriteSpacing(val spacing: Dp) {
    companion object {
        val MIN = 0.dp
        val MAX = 48.dp

        val STANDARD = FavoriteSpacing(12.dp)

        // Auslieferungszustand: enger als das alte 12dp-Standard-Preset, damit die
        // Favoriten ab dem ersten Start kompakter beieinander stehen.
        val DEFAULT = FavoriteSpacing(8.dp)

        /** Erzeugt einen auf [MIN]..[MAX] begrenzten Wert. */
        fun of(spacing: Dp) = FavoriteSpacing(spacing.coerceIn(MIN, MAX))
    }
}
