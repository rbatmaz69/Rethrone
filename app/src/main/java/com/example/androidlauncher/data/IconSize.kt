package com.example.androidlauncher.data

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Enum representing available icon size options.
 * @property label Display name of the size option.
 * @property size The Dp size of the app icon.
 * @property scale Relative scale factor for previews.
 */
enum class IconSize(val label: String, val size: Dp, val scale: Float) {
    SMALL("Klein", 40.dp, 0.83f),
    STANDARD("Standard", 48.dp, 1.0f),
    LARGE("Groß", 56.dp, 1.17f)
}
