package com.example.androidlauncher.ui.theme

import androidx.compose.ui.graphics.Color

enum class ColorTheme(
    val themeName: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val drawerBackground: Color
) {
    SIGNATURE(
        "Signature",
        Color(0xFF1A0B2E),
        Color(0xFF4A148C),
        Color.White,
        Color(0xFF0F172A)
    ),
    OCEAN(
        "Ocean",
        Color(0xFF0D47A1),
        Color(0xFF1976D2),
        Color.White,
        Color(0xFF010B1A)
    ),
    FOREST(
        "Forest",
        Color(0xFF1B5E20),
        Color(0xFF388E3C),
        Color.White,
        Color(0xFF0A1A0B)
    ),
    SUNSET(
        "Sunset",
        Color(0xFFF57C00),
        Color(0xFFFFB300),
        Color.Black,
        Color(0xFF1A0F01)
    ),
    LAVENDER(
        "Lavender",
        Color(0xFF311B92),
        Color(0xFF673AB7),
        Color.White,
        Color(0xFF0D011A)
    ),
    SAKURA(
        "Sakura",
        Color(0xFF880E4F),
        Color(0xFFC2185B),
        Color.White,
        Color(0xFF1A010D)
    ),
    NIGHTSKY(
        "Nightsky",
        Color(0xFF263238),
        Color(0xFF455A64),
        Color.White,
        Color(0xFF010405)
    ),
    MINT(
        "Mint",
        Color(0xFF004D40),
        Color(0xFF00796B),
        Color.White,
        Color(0xFF011A16)
    ),
    SUNSHINE(
        "Sunshine",
        Color(0xFFFBC02D),
        Color(0xFFFFF176),
        Color.Black,
        Color(0xFF1A1601)
    ),
    SKY(
        "Sky",
        Color(0xFF0288D1),
        Color(0xFF4FC3F7),
        Color.Black,
        Color(0xFF01121A)
    ),
    PEACH(
        "Peach",
        Color(0xFFE64A19),
        Color(0xFFFF8A65),
        Color.White,
        Color(0xFF1A0801)
    ),
    CANDY(
        "Candy",
        Color(0xFFC2185B),
        Color(0xFFF06292),
        Color.White,
        Color(0xFF1A010D)
    ),
    LEMONADE(
        "Lemonade",
        Color(0xFFCDDC39),
        Color(0xFFEEFF41),
        Color.Black,
        Color(0xFF141A01)
    ),
    BUBBLEGUM(
        "Bubblegum",
        Color(0xFFF06292),
        Color(0xFF81D4FA),
        Color.Black,
        Color(0xFF1A0D15)
    ),
    TROPICAL(
        "Tropical",
        Color(0xFF00BCD4),
        Color(0xFFCDDC39),
        Color.Black,
        Color(0xFF011A1A)
    ),
    SPRING(
        "Spring",
        Color(0xFF8BC34A),
        Color(0xFFDCE775),
        Color.Black,
        Color(0xFF0E1A01)
    );

    // Berechnet einen hellen Hintergrund basierend auf der Primärfarbe des Themes
    val lightBackground: Color
        get() {
            // Intensiviere die Farbe: Mische 95% Primärfarbe mit 5% Weiß
            val red = (primary.red * 0.95f + 0.05f)
            val green = (primary.green * 0.95f + 0.05f)
            val blue = (primary.blue * 0.95f + 0.05f)
            return Color(red, green, blue, 1f)
        }
}
