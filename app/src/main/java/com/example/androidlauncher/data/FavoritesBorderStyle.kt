package com.example.androidlauncher.data

import androidx.annotation.StringRes
import com.example.androidlauncher.R

/**
 * Wählbare Umrandung der Favoriten-Box auf der Startseite (reiner Umriss).
 *
 * Wird über `LocalFavoritesBorderStyle` bereitgestellt und in HomeScreen an der
 * Favoriten-Liste als Border gezeichnet. [NONE] entspricht dem bisherigen
 * Verhalten (keine Umrandung). Der Anzeigename wird lokalisiert über [labelRes].
 */
enum class FavoritesBorderStyle(@StringRes val labelRes: Int) {
    NONE(R.string.border_style_none),
    BLACK(R.string.border_style_black),
    WHITE(R.string.border_style_white),
    ACCENT(R.string.border_style_accent),
    SUBTLE(R.string.border_style_subtle);

    companion object {
        /** Liest einen gespeicherten Schlüssel; fällt bei Unbekanntem auf [NONE] zurück. */
        fun fromKey(key: String?): FavoritesBorderStyle = entries.find { it.name == key } ?: NONE
    }
}
