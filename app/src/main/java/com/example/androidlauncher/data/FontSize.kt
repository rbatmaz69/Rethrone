package com.example.androidlauncher.data

/**
 * Enum representing available font size scalings.
 * @property label Display name of the size option.
 * @property scale The multiplier factor applied to base font sizes.
 */
enum class FontSize(val label: String, val scale: Float) {
    SMALL("Klein", 0.85f),
    STANDARD("Standard", 1.0f),
    LARGE("Groß", 1.2f)
}
