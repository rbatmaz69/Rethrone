package com.example.androidlauncher.data

import androidx.annotation.StringRes
import com.example.androidlauncher.R

/**
 * Visueller Stil des Edge Lightings (leuchtender Rand bei Benachrichtigungen).
 *
 * Vom Nutzer im Edge-Lighting-Untermenü wählbar und in DataStore (als Enum-Name) persistiert.
 * - [SWEEP]: ein Lichtpunkt läuft umlaufend einmal/mehrmals um den Rand.
 * - [FROM_CAMERA]: zwei Lichter starten oben an der Kamera, teilen sich und treffen sich unten.
 * - [GLOW_PULSE]: der gesamte Rand leuchtet auf und pulsiert (atmet).
 */
enum class EdgeLightingStyle(@StringRes val labelRes: Int) {
    SWEEP(R.string.edge_style_sweep),
    FROM_CAMERA(R.string.edge_style_from_camera),
    GLOW_PULSE(R.string.edge_style_glow)
}
