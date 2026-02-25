package com.example.androidlauncher.data

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Enum representing available icon size options.
 * @property label Display name of the size option.
 * @property size The Dp size of the app icon.
 */
enum class IconSize(val label: String, val size: Dp) {
    SMALL("Klein", 40.dp),
    STANDARD("Standard", 48.dp),
    LARGE("Groß", 56.dp)
}
