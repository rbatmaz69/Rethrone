package com.example.androidlauncher.data

import androidx.annotation.StringRes
import com.example.androidlauncher.R

/**
 * Wählbarer Öffnungs-/Schließstil der Dynamic Island.
 *
 * Vom Nutzer im Bearbeiten-Menü wählbar und in DataStore (als Enum-Name) persistiert. Alle Stile sind
 * Tunings derselben parametrischen Animation (Feder + vertikaler Jelly-Squash).
 * - [FROM_NOTCH]: opak aus 0-Breite herauswachsen + dezenter Jelly-Squash (Default).
 * - [SOFT]: reines Fade + leichtes Scale, maximal dezent.
 * - [BOUNCE]: wie [FROM_NOTCH], aber stärker federnd/poppig.
 * - [SNAPPY]: schnell und präzise, ohne Nachfedern.
 */
enum class IslandAnimationStyle(@StringRes val labelRes: Int) {
    FROM_NOTCH(R.string.island_anim_from_notch),
    SOFT(R.string.island_anim_soft),
    BOUNCE(R.string.island_anim_bounce),
    SNAPPY(R.string.island_anim_snappy)
}
