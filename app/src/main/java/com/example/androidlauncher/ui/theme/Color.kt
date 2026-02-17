package com.example.androidlauncher.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

enum class ColorTheme(
    val themeName: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
) {
    DEFAULT(
        "Default",
        Color(0xFF1A0B2E),
        Color(0xFF4A148C),
        Color.White,
    ),
    OCEAN(
        "Ocean",
        Color(0xFF0D47A1),
        Color(0xFF1976D2),
        Color.White
    ),
    FOREST(
        "Forest",
        Color(0xFF1B5E20),
        Color(0xFF388E3C),
        Color.White
    ),
    SUNSET(
        "Sunset",
        Color(0xFFF57C00),
        Color(0xFFFFB300),
        Color.Black
    ),
    LAVENDER(
        "Lavender",
        Color(0xFF311B92),
        Color(0xFF673AB7),
        Color.White
    ),
    SAKURA(
        "Sakura",
        Color(0xFF880E4F),
        Color(0xFFC2185B),
        Color.White
    ),
    NIGHTSKY(
        "Nightsky",
        Color(0xFF263238),
        Color(0xFF455A64),
        Color.White
    ),
    MINT(
        "Mint",
        Color(0xFF004D40),
        Color(0xFF00796B),
        Color.White
    )
}
