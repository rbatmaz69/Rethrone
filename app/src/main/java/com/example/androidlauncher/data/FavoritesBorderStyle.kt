package com.example.androidlauncher.data

/**
 * Wählbare Umrandung der Favoriten-Box auf der Startseite (reiner Umriss).
 *
 * Wird über `LocalFavoritesBorderStyle` bereitgestellt und in HomeScreen an der
 * Favoriten-Liste als Border gezeichnet. [NONE] entspricht dem bisherigen
 * Verhalten (keine Umrandung).
 */
enum class FavoritesBorderStyle(val displayName: String) {
    NONE("Keine"),
    BLACK("Schwarz"),
    WHITE("Weiß"),
    ACCENT("Akzentfarbe"),
    SUBTLE("Dezent");

    companion object {
        /** Liest einen gespeicherten Schlüssel; fällt bei Unbekanntem auf [NONE] zurück. */
        fun fromKey(key: String?): FavoritesBorderStyle = entries.find { it.name == key } ?: NONE
    }
}
