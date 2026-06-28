package com.example.androidlauncher.data

/**
 * Stufenlos einstellbarer Schrift-Skalierungsfaktor.
 *
 * @property scale Multiplikator, der auf die Basis-Schriftgrößen angewendet wird.
 */
@JvmInline
value class FontSize(val scale: Float) {
    companion object {
        const val MIN = 0.60f
        const val MAX = 1.60f

        val STANDARD = FontSize(1.0f)

        // Presets für Tests/Defaults und Abwärtskompatibilität.
        val SMALL = FontSize(0.85f)
        val LARGE = FontSize(1.2f)

        /** Erzeugt einen auf [MIN]..[MAX] begrenzten Wert. */
        fun of(scale: Float) = FontSize(scale.coerceIn(MIN, MAX))
    }
}
