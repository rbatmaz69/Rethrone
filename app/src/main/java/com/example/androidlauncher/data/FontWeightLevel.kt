package com.example.androidlauncher.data

import androidx.compose.ui.text.font.FontWeight

/**
 * Enum representing available font weights.
 * @property label Display name of the font weight option.
 * @property weight The actual FontWeight to use.
 */
enum class FontWeightLevel(val label: String, val weight: FontWeight) {
    LIGHT("Dünn", FontWeight.Light),
    NORMAL("Normal", FontWeight.Normal),
    BOLD("Fett", FontWeight.Bold)
}

