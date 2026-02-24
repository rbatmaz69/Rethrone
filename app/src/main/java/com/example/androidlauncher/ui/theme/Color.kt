package com.example.androidlauncher.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Enum defining the available color themes for the launcher.
 * Each theme provides a set of colors for different UI elements.
 *
 * @property themeName The display name of the theme.
 * @property primary The primary color (e.g., for headers, active states).
 * @property secondary A secondary accent color.
 * @property tertiary Another accent color/contrast color.
 * @property drawerBackground The background color for the App Drawer and darker surfaces.
 * @property lightBackground A calculated lighter background color for "Light Mode" (Dark Text Mode).
 */
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
    ),
    BEIGE(
        "Beige",
        Color(0xFFD7CCC8),
        Color(0xFFEFEBE9),
        Color.Black,
        Color(0xFF1D1B1A)
    ),
    SAND(
        "Sand",
        Color(0xFFC2B280),
        Color(0xFFE6D690),
        Color.Black,
        Color(0xFF1A1811)
    ),
    WARM_GRAY(
        "Warm Gray",
        Color(0xFF9E9E9E),
        Color(0xFFBDBDBD),
        Color.Black,
        Color(0xFF1A1A1A)
    ),
    COOL_GRAY(
        "Cool Gray",
        Color(0xFF78909C),
        Color(0xFFB0BEC5),
        Color.Black,
        Color(0xFF101314)
    ),
    STONE_GRAY(
        "Stone Gray",
        Color(0xFF546E7A),
        Color(0xFF78909C),
        Color.White,
        Color(0xFF0B0E10)
    ),
    LIGHT_BROWN(
        "Light Brown",
        Color(0xFF8D6E63),
        Color(0xFFA1887F),
        Color.White,
        Color(0xFF120E0D)
    ),
    CARAMEL(
        "Caramel",
        Color(0xFFFFB74D),
        Color(0xFFFFE0B2),
        Color.Black,
        Color(0xFF1A1208)
    ),
    TAUPE(
        "Taupe",
        Color(0xFF8E8883),
        Color(0xFFB3ADA8),
        Color.Black,
        Color(0xFF131211)
    ),
    DARK_BEIGE(
        "Dark Beige",
        Color(0xFFA68B6C),
        Color(0xFFC2AA8E),
        Color.Black,
        Color(0xFF16120E)
    ),
    SOFT_BLUE(
        "Soft Blue",
        Color(0xFF90CAF9),
        Color(0xFFBBDEFB),
        Color.Black,
        Color(0xFF0E1419)
    ),
    PASTEL_GREEN(
        "Pastel Green",
        Color(0xFFA5D6A7),
        Color(0xFFC8E6C9),
        Color.Black,
        Color(0xFF111611)
    ),
    LIGHT_RED(
        "Light Red",
        Color(0xFFEF9A9A),
        Color(0xFFFFCDD2),
        Color.Black,
        Color(0xFF1A1111)
    ),
    SOFT_PURPLE(
        "Soft Purple",
        Color(0xFFCE93D8),
        Color(0xFFE1BEE7),
        Color.Black,
        Color(0xFF150F16)
    );

    // Berechnet einen hellen Hintergrund basierend auf der Primärfarbe des Themes
    val lightBackground: Color
        get() {
            // Mische 98% Weiß mit 2% Primärfarbe
            return Color(
                red = primary.red * 0.02f + 0.98f,
                green = primary.green * 0.02f + 0.98f,
                blue = primary.blue * 0.02f + 0.98f,
                alpha = 1f
            )
        }
}
