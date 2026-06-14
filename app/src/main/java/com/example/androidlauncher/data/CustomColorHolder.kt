package com.example.androidlauncher.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Hält die beiden vom Nutzer gewählten Flächenfarben des CUSTOM-Themes
 * („Eigene Farbe") als Compose-State. Wird in `MainActivity` aus den
 * persistierten Flows ([ThemeManager.customBackgroundColor]/[ThemeManager.customMenuColor])
 * befüllt → Recomposition der farbabhängigen Oberflächen bei Änderung.
 *
 * Analog zu [DynamicColorHolder], nur mit zwei festen Seed-Farben statt Wallpaper-Seeds.
 */
object CustomColorHolder {
    var background by mutableStateOf<Color?>(null)
        private set
    var menu by mutableStateOf<Color?>(null)
        private set

    fun set(background: Color, menu: Color) {
        this.background = background
        this.menu = menu
    }
}
