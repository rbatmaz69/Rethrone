package com.example.androidlauncher.data

import androidx.compose.ui.geometry.Offset

/**
 * Frei wählbare Positionen der unabhängig verschiebbaren Startbildschirm-Elemente.
 * Jeder Wert ist ein Offset (in Pixeln) relativ zur natürlichen Layout-Position.
 */
data class HomeLayout(
    val clock: Offset = Offset.Zero,
    val date: Offset = Offset.Zero,
    val weather: Offset = Offset.Zero,
    val favorites: Offset = Offset.Zero,
)
