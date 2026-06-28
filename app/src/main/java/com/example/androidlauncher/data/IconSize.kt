package com.example.androidlauncher.data

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Stufenlos einstellbare App-Icon-Größe.
 *
 * @property size Die Dp-Größe des App-Icons.
 * @property scale Relativer Skalierungsfaktor (bezogen auf 48dp) für Vorschauen.
 */
data class IconSize(val size: Dp) {
    val scale: Float get() = size / 48.dp

    companion object {
        val MIN = 28.dp
        val MAX = 72.dp

        val STANDARD = IconSize(48.dp)

        // Presets für Tests/Defaults und Abwärtskompatibilität.
        val SMALL = IconSize(40.dp)
        val LARGE = IconSize(56.dp)

        /** Erzeugt einen auf [MIN]..[MAX] begrenzten Wert. */
        fun of(size: Dp) = IconSize(size.coerceIn(MIN, MAX))
    }
}
