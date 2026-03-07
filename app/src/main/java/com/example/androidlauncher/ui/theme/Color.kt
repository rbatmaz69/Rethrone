package com.example.androidlauncher.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Enum defining the available color themes for the launcher.
 * Each theme provides semantic colors and subtle multi-stop gradients for artistic UI surfaces.
 */
enum class ColorTheme(
    val themeName: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val drawerBackground: Color,
    val artGradient: List<Color> = listOf(primary, secondary, drawerBackground),
    val menuGradient: List<Color> = artGradient,
    val searchGradient: List<Color> = menuGradient,
    val animationGradient: List<Color> = artGradient,
    val highlight: Color = secondary,
    val isArtTheme: Boolean = false,
    val darkBackgroundBlend: Float = 0.40f,
    val darkMenuBlend: Float = 0.32f,
    val darkSearchBlend: Float = 0.46f,
    val darkAnimationBlend: Float = 0.30f
) {
    SIGNATURE(
        "Signature",
        Color(0xFF1A0B2E),
        Color(0xFF4A148C),
        Color.White,
        Color(0xFF0F172A),
        artGradient = listOf(Color(0xFF120B2D), Color(0xFF3B1C70), Color(0xFF5B21B6), Color(0xFF0F172A)),
        menuGradient = listOf(Color(0xFF0F172A), Color(0xFF241245), Color(0xFF40205F)),
        searchGradient = listOf(Color(0xFF1A1236), Color(0xFF38205E), Color(0xFF5B2FA3)),
        animationGradient = listOf(Color(0xFF0F172A), Color(0xFF312E81), Color(0xFF6D28D9))
    ),
    MOUNTAIN_DUSK(
        "Mountain Dusk",
        Color(0xFF334E68),
        Color(0xFF7E57C2),
        Color.White,
        Color(0xFF121826),
        artGradient = listOf(Color(0xFF1E2A44), Color(0xFF355C7D), Color(0xFF6C5B7B), Color(0xFF1F2937)),
        menuGradient = listOf(Color(0xFF172033), Color(0xFF2A3C59), Color(0xFF5A4D78)),
        searchGradient = listOf(Color(0xFF22314B), Color(0xFF4B5D7D), Color(0xFF7B6D96)),
        animationGradient = listOf(Color(0xFF1A2335), Color(0xFF395878), Color(0xFF7E57C2)),
        highlight = Color(0xFFA7B6C8),
        isArtTheme = true
    ),
    DESERT_GLOW(
        "Desert Glow",
        Color(0xFFC97B4B),
        Color(0xFFE6B17E),
        Color.Black,
        Color(0xFF2A1E18),
        artGradient = listOf(Color(0xFF6E3B2E), Color(0xFFB96848), Color(0xFFE2A66F), Color(0xFFEDD2A4)),
        menuGradient = listOf(Color(0xFF3A271F), Color(0xFF7A4B35), Color(0xFFB87752)),
        searchGradient = listOf(Color(0xFF6D4632), Color(0xFFB77350), Color(0xFFE4BC8A)),
        animationGradient = listOf(Color(0xFF4C2E24), Color(0xFFC97B4B), Color(0xFFF1D3A2)),
        highlight = Color(0xFFF4D7A1),
        isArtTheme = true,
        darkBackgroundBlend = 0.42f,
        darkMenuBlend = 0.34f,
        darkSearchBlend = 0.52f,
        darkAnimationBlend = 0.36f
    ),
    ARCTIC_MIST(
        "Arctic Mist",
        Color(0xFF88AFCB),
        Color(0xFFC9D8E6),
        Color.Black,
        Color(0xFF1A222D),
        artGradient = listOf(Color(0xFF2B3B4F), Color(0xFF6E8BA5), Color(0xFFB4CADB), Color(0xFFE7EEF4)),
        menuGradient = listOf(Color(0xFF253240), Color(0xFF5D748C), Color(0xFFAFC2D1)),
        searchGradient = listOf(Color(0xFF607E99), Color(0xFFAFC3D5), Color(0xFFE8F0F5)),
        animationGradient = listOf(Color(0xFF30465A), Color(0xFF84A9C6), Color(0xFFDCE8F1)),
        highlight = Color(0xFFF5F8FA),
        isArtTheme = true
    ),
    OCEAN_DEPTHS(
        "Ocean Depths",
        Color(0xFF006C84),
        Color(0xFF3A8FB7),
        Color.White,
        Color(0xFF071A24),
        artGradient = listOf(Color(0xFF051923), Color(0xFF0A4F68), Color(0xFF0E7490), Color(0xFF77C5D5)),
        menuGradient = listOf(Color(0xFF081A24), Color(0xFF0C4257), Color(0xFF136F8A)),
        searchGradient = listOf(Color(0xFF0F4C5C), Color(0xFF2F7E9C), Color(0xFF74B9D0)),
        animationGradient = listOf(Color(0xFF082032), Color(0xFF0F6A8A), Color(0xFF63C5DA)),
        highlight = Color(0xFF9CE7F5),
        isArtTheme = true
    ),
    MISTY_VALLEY(
        "Misty Valley",
        Color(0xFF5F7A61),
        Color(0xFF97B49C),
        Color.Black,
        Color(0xFF18211C),
        artGradient = listOf(Color(0xFF243328), Color(0xFF5B7460), Color(0xFF91A899), Color(0xFFD3DED5)),
        menuGradient = listOf(Color(0xFF1D2821), Color(0xFF4B6050), Color(0xFF849B8D)),
        searchGradient = listOf(Color(0xFF536B58), Color(0xFF8CA394), Color(0xFFD3DED7)),
        animationGradient = listOf(Color(0xFF223428), Color(0xFF69876F), Color(0xFFC6D6CC)),
        highlight = Color(0xFFE5ECE7),
        isArtTheme = true
    ),
    AURORA_VEIL(
        "Aurora Veil",
        Color(0xFF28536B),
        Color(0xFF5A5BD4),
        Color.White,
        Color(0xFF0B1220),
        artGradient = listOf(Color(0xFF071A2B), Color(0xFF114B5F), Color(0xFF5A5BD4), Color(0xFF65D6AD)),
        menuGradient = listOf(Color(0xFF0B1220), Color(0xFF15324C), Color(0xFF345D7E), Color(0xFF4B6BC8)),
        searchGradient = listOf(Color(0xFF14314C), Color(0xFF355C7D), Color(0xFF5D81D1), Color(0xFF84D9C0)),
        animationGradient = listOf(Color(0xFF081420), Color(0xFF1D4E89), Color(0xFF6D5EF5), Color(0xFF78E3BE)),
        highlight = Color(0xFF78E3BE),
        isArtTheme = true
    ),
    MOONLIT_LAKE(
        "Moonlit Lake",
        Color(0xFF214E73),
        Color(0xFF6FA3C8),
        Color.White,
        Color(0xFF0B1520),
        artGradient = listOf(Color(0xFF0A1420), Color(0xFF17324A), Color(0xFF285A7A), Color(0xFF90B7D5)),
        menuGradient = listOf(Color(0xFF101B28), Color(0xFF1A344A), Color(0xFF3C6687), Color(0xFF7FA7C4)),
        searchGradient = listOf(Color(0xFF1A2D40), Color(0xFF2C516E), Color(0xFF5F86A4), Color(0xFF9CBBD0)),
        animationGradient = listOf(Color(0xFF0B1623), Color(0xFF244D6C), Color(0xFF4A7EA6), Color(0xFFB7D0E3)),
        highlight = Color(0xFFD5E3EE),
        isArtTheme = true
    ),
    EMBER_RAIN(
        "Ember Rain",
        Color(0xFF7A3A2A),
        Color(0xFFB96A4A),
        Color.White,
        Color(0xFF1A1214),
        artGradient = listOf(Color(0xFF1A1214), Color(0xFF4B2930), Color(0xFF8B4A39), Color(0xFFC88963)),
        menuGradient = listOf(Color(0xFF201719), Color(0xFF4E2C30), Color(0xFF855041), Color(0xFFB97A5B)),
        searchGradient = listOf(Color(0xFF2B1C20), Color(0xFF61343B), Color(0xFF9A5B4C), Color(0xFFD19C78)),
        animationGradient = listOf(Color(0xFF1C1315), Color(0xFF6D372D), Color(0xFFB7694E), Color(0xFFE2B08C)),
        highlight = Color(0xFFE7C2A4),
        isArtTheme = true
    ),
    PINE_DAWN(
        "Pine Dawn",
        Color(0xFF305348),
        Color(0xFF7EA68A),
        Color.Black,
        Color(0xFF122019),
        artGradient = listOf(Color(0xFF122019), Color(0xFF29443A), Color(0xFF4E7564), Color(0xFFB7C9AF)),
        menuGradient = listOf(Color(0xFF18261F), Color(0xFF355346), Color(0xFF62836E), Color(0xFF9FB59B)),
        searchGradient = listOf(Color(0xFF213127), Color(0xFF4A6957), Color(0xFF7E9D85), Color(0xFFC5D0BE)),
        animationGradient = listOf(Color(0xFF14221B), Color(0xFF3A5C4D), Color(0xFF6D9079), Color(0xFFD6DDC6)),
        highlight = Color(0xFFE3E9DA),
        isArtTheme = true
    ),
    ROSE_DUST(
        "Rose Dust",
        Color(0xFF7A5566),
        Color(0xFFB58A9A),
        Color.Black,
        Color(0xFF1C171B),
        artGradient = listOf(Color(0xFF21191E), Color(0xFF5E4250), Color(0xFF9A7381), Color(0xFFD8C2C6)),
        menuGradient = listOf(Color(0xFF261D21), Color(0xFF674954), Color(0xFF9C7784), Color(0xFFC7AAB2)),
        searchGradient = listOf(Color(0xFF33262C), Color(0xFF7B5967), Color(0xFFAF8894), Color(0xFFE0CFD3)),
        animationGradient = listOf(Color(0xFF241B1F), Color(0xFF7D5967), Color(0xFFB78C97), Color(0xFFE8D9DC)),
        highlight = Color(0xFFF0E6E8),
        isArtTheme = true
    ),
    NEON_NOIR(
        "Neon Noir",
        Color(0xFF111827),
        Color(0xFF00C2FF),
        Color.White,
        Color(0xFF070B14),
        artGradient = listOf(Color(0xFF070B14), Color(0xFF182033), Color(0xFF3B1D5E), Color(0xFF00C2FF)),
        menuGradient = listOf(Color(0xFF0B1020), Color(0xFF1A2340), Color(0xFF2E2B5A), Color(0xFF1388C9)),
        searchGradient = listOf(Color(0xFF12182C), Color(0xFF22304D), Color(0xFF3E3C66), Color(0xFF5ED8FF)),
        animationGradient = listOf(Color(0xFF090E19), Color(0xFF1A2B48), Color(0xFF55338A), Color(0xFF24D8FF)),
        highlight = Color(0xFF9CEEFF),
        isArtTheme = true
    ),
    VOLCANIC_GLASS(
        "Volcanic Glass",
        Color(0xFF3A1F22),
        Color(0xFFE2653C),
        Color.White,
        Color(0xFF140D10),
        artGradient = listOf(Color(0xFF140D10), Color(0xFF342125), Color(0xFF6C2F2D), Color(0xFFE2653C)),
        menuGradient = listOf(Color(0xFF1A1214), Color(0xFF3D2327), Color(0xFF7B342F), Color(0xFFC95A3F)),
        searchGradient = listOf(Color(0xFF25171A), Color(0xFF51282B), Color(0xFF924138), Color(0xFFF08A5D)),
        animationGradient = listOf(Color(0xFF171012), Color(0xFF4B2223), Color(0xFFA13D30), Color(0xFFFF8F66)),
        highlight = Color(0xFFFFC1A2),
        isArtTheme = true
    ),
    OXIDE_COPPER(
        "Oxide Copper",
        Color(0xFF5B4A3D),
        Color(0xFF2F8F83),
        Color.Black,
        Color(0xFF151515),
        artGradient = listOf(Color(0xFF171616), Color(0xFF514238), Color(0xFF9E6B43), Color(0xFF2F8F83)),
        menuGradient = listOf(Color(0xFF1A1A19), Color(0xFF4B4037), Color(0xFF8A6649), Color(0xFF3D8D82)),
        searchGradient = listOf(Color(0xFF23211F), Color(0xFF645443), Color(0xFFA97854), Color(0xFF6BB5A4)),
        animationGradient = listOf(Color(0xFF171717), Color(0xFF5A4638), Color(0xFFB36F49), Color(0xFF7BC5BA)),
        highlight = Color(0xFFD9E6E0),
        isArtTheme = true
    ),
    AMETHYST_SMOKE(
        "Amethyst Smoke",
        Color(0xFF4D3A62),
        Color(0xFF8C7AAE),
        Color.White,
        Color(0xFF14111A),
        artGradient = listOf(Color(0xFF18131D), Color(0xFF43324E), Color(0xFF6D5A85), Color(0xFFC7BEDB)),
        menuGradient = listOf(Color(0xFF1A1520), Color(0xFF4A395B), Color(0xFF76648E), Color(0xFFB1A6CA)),
        searchGradient = listOf(Color(0xFF241D2C), Color(0xFF5A4A6F), Color(0xFF8B7AA9), Color(0xFFD8D1E6)),
        animationGradient = listOf(Color(0xFF1A1420), Color(0xFF57406A), Color(0xFF8D73B3), Color(0xFFE4DDF0)),
        highlight = Color(0xFFF0ECF8),
        isArtTheme = true
    ),
    PETROL_DREAM(
        "Petrol Dream",
        Color(0xFF12394A),
        Color(0xFF2E6F7E),
        Color.White,
        Color(0xFF081217),
        artGradient = listOf(Color(0xFF081217), Color(0xFF12394A), Color(0xFF1E5B67), Color(0xFF9FBF8A)),
        menuGradient = listOf(Color(0xFF0D171C), Color(0xFF184252), Color(0xFF29616F), Color(0xFF7FA78A)),
        searchGradient = listOf(Color(0xFF122027), Color(0xFF245060), Color(0xFF3D7682), Color(0xFFC4D7B2)),
        animationGradient = listOf(Color(0xFF0A1418), Color(0xFF1A4655), Color(0xFF337585), Color(0xFFD9E7C5)),
        highlight = Color(0xFFE8F0DE),
        isArtTheme = true
    ),
    FERRARI_RED(
        "Ferrari Red",
        Color(0xFFD20A11),
        Color(0xFFFF3B30),
        Color.White,
        Color(0xFF160607),
        highlight = Color(0xFFFFB1A9)
    ),
    GRAPHITE_MONO(
        "Graphite Mono",
        Color(0xFF3B4048),
        Color(0xFF626973),
        Color.White,
        Color(0xFF0E1115),
        highlight = Color(0xFFD0D4DA)
    ),
    IVORY_INK(
        "Ivory Ink",
        Color(0xFFF2EBDD),
        Color(0xFFD8CEBE),
        Color.Black,
        Color(0xFF201D19),
        highlight = Color(0xFFFFFAF2)
    ),
    OLIVE_MONO(
        "Olive Mono",
        Color(0xFF6E7450),
        Color(0xFF8D9566),
        Color.Black,
        Color(0xFF17180F),
        highlight = Color(0xFFDDE2C8)
    ),
    ONYX_GOLD(
        "Onyx Gold",
        Color(0xFF1C1A19),
        Color(0xFFC9A96A),
        Color.White,
        Color(0xFF090807),
        highlight = Color(0xFFF0E1BC)
    ),
    COBALT_MONO(
        "Cobalt Mono",
        Color(0xFF1346C7),
        Color(0xFF4D74E6),
        Color.White,
        Color(0xFF08101F),
        highlight = Color(0xFFC4D3FF)
    ),
    JADE_MONO(
        "Jade Mono",
        Color(0xFF0C7A58),
        Color(0xFF32A87D),
        Color.White,
        Color(0xFF071810),
        highlight = Color(0xFFCDEEE0)
    ),
    PLUM_MONO(
        "Plum Mono",
        Color(0xFF6A1B7A),
        Color(0xFF9C4DB0),
        Color.White,
        Color(0xFF150A18),
        highlight = Color(0xFFE9D4EF)
    ),
    CITRINE_MONO(
        "Citrine Mono",
        Color(0xFFA87400),
        Color(0xFFC99518),
        Color.Black,
        Color(0xFF181203),
        highlight = Color(0xFFF3E2A1)
    ),
    TEAL_MONO(
        "Teal Mono",
        Color(0xFF007A78),
        Color(0xFF2FA7A3),
        Color.White,
        Color(0xFF071716),
        highlight = Color(0xFFC8F0EE)
    ),
    MAROON_MONO(
        "Maroon Mono",
        Color(0xFF7A1027),
        Color(0xFFA53A50),
        Color.White,
        Color(0xFF17070B),
        highlight = Color(0xFFF1CDD4)
    ),
    SLATE_MONO(
        "Slate Mono",
        Color(0xFF4A5568),
        Color(0xFF718096),
        Color.White,
        Color(0xFF0F141B),
        highlight = Color(0xFFDCE3EC)
    ),
    SANDSTONE_MONO(
        "Sandstone Mono",
        Color(0xFF8C6A43),
        Color(0xFFB28A5C),
        Color.White,
        Color(0xFF17120C),
        highlight = Color(0xFFEAD9C5)
    ),
    BRONZE_MONO(
        "Bronze Mono",
        Color(0xFF8D5A2B),
        Color(0xFFB97A3D),
        Color.White,
        Color(0xFF181008),
        highlight = Color(0xFFF1D4B2)
    ),
    OBSIDIAN_MONO(
        "Obsidian Mono",
        Color(0xFF171717),
        Color(0xFF343434),
        Color.White,
        Color(0xFF050505),
        highlight = Color(0xFFD5D5D5)
    ),
    PLASMA_BURST(
        "Plasma Burst",
        Color(0xFFFF007A),
        Color(0xFF8A2BE2),
        Color.White,
        Color(0xFF140616),
        highlight = Color(0xFFFFC3E0)
    ),
    LASER_LIME(
        "Laser Lime",
        Color(0xFF98FF00),
        Color(0xFFD4FF3F),
        Color.Black,
        Color(0xFF0E1404),
        highlight = Color(0xFFF0FFB8)
    ),
    ELECTRIC_AQUA(
        "Electric Aqua",
        Color(0xFF00E5FF),
        Color(0xFF00FFC6),
        Color.Black,
        Color(0xFF041317),
        highlight = Color(0xFFC8FFF8)
    ),
    ULTRAVIOLET_PULSE(
        "Ultraviolet Pulse",
        Color(0xFF6F00FF),
        Color(0xFFD100FF),
        Color.White,
        Color(0xFF11061A),
        highlight = Color(0xFFF0C6FF)
    ),
    SOLAR_FLARE(
        "Solar Flare",
        Color(0xFFFFC400),
        Color(0xFFFF6A00),
        Color.Black,
        Color(0xFF1A1003),
        highlight = Color(0xFFFFE2A8)
    ),
    HYPER_CORAL(
        "Hyper Coral",
        Color(0xFFFF5A5F),
        Color(0xFFFF2D92),
        Color.White,
        Color(0xFF19080D),
        highlight = Color(0xFFFFC3D8)
    ),
    NITRO_BLUE(
        "Nitro Blue",
        Color(0xFF0057FF),
        Color(0xFF00A6FF),
        Color.White,
        Color(0xFF06101D),
        highlight = Color(0xFFC8E1FF)
    ),
    ACID_MELON(
        "Acid Melon",
        Color(0xFFB8FF00),
        Color(0xFFFF4FD8),
        Color.Black,
        Color(0xFF15110A),
        highlight = Color(0xFFFFD2F2)
    ),
    PIXEL_FIRE(
        "Pixel Fire",
        Color(0xFFFF4D00),
        Color(0xFFFF003C),
        Color.White,
        Color(0xFF19080A),
        highlight = Color(0xFFFFC4B8)
    ),
    RAVE_GRAPE(
        "Rave Grape",
        Color(0xFF7F00FF),
        Color(0xFFFF00AA),
        Color.White,
        Color(0xFF120619),
        highlight = Color(0xFFFFCCEF)
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

    val lightBackground: Color
        get() = menuGradientColors(darkTextEnabled = true).averageColor()

    fun backgroundColor(darkTextEnabled: Boolean): Color =
        backgroundGradientColors(darkTextEnabled).averageColor()

    fun menuSurfaceColor(darkTextEnabled: Boolean): Color =
        menuGradientColors(darkTextEnabled).averageColor()

    fun searchSurfaceColor(darkTextEnabled: Boolean): Color =
        searchGradientColors(darkTextEnabled).averageColor()

    fun accentColor(darkTextEnabled: Boolean): Color =
        if (darkTextEnabled) secondary.blendWith(Color.White, 0.18f) else secondary.blendWith(primary, 0.18f)

    fun highlightColor(darkTextEnabled: Boolean): Color =
        if (darkTextEnabled) highlight.blendWith(Color.Black, 0.08f) else highlight.blendWith(Color.White, 0.08f)

    fun borderColor(darkTextEnabled: Boolean): Color =
        if (darkTextEnabled) primary.blendWith(Color.Black, 0.48f).copy(alpha = 0.22f)
        else secondary.blendWith(Color.White, 0.15f).copy(alpha = 0.28f)

    fun overlayScrimColor(darkTextEnabled: Boolean): Color =
        if (darkTextEnabled) primary.blendWith(Color.White, 0.82f).copy(alpha = 0.52f)
        else drawerBackground.blendWith(Color.Black, 0.18f).copy(alpha = 0.64f)

    fun backgroundBrush(darkTextEnabled: Boolean, alpha: Float = 1f): Brush =
        gradientBrush(backgroundGradientColors(darkTextEnabled).withAlpha(alpha))

    fun menuBrush(darkTextEnabled: Boolean, alpha: Float = 1f): Brush =
        gradientBrush(menuGradientColors(darkTextEnabled).withAlpha(alpha))

    fun searchBrush(darkTextEnabled: Boolean, alpha: Float = 1f): Brush =
        gradientBrush(searchGradientColors(darkTextEnabled).withAlpha(alpha))

    fun animationBrush(darkTextEnabled: Boolean, alpha: Float = 1f): Brush =
        gradientBrush(animationGradientColors(darkTextEnabled).withAlpha(alpha))

    fun passesContrastForMainText(darkTextEnabled: Boolean): Boolean {
        val textColor = if (darkTextEnabled) DarkTextColor else Color.White
        return listOf(
            backgroundColor(darkTextEnabled),
            menuSurfaceColor(darkTextEnabled),
            searchSurfaceColor(darkTextEnabled)
        ).all { contrastRatio(textColor, it) >= MinimumReadableContrast }
    }

    private val lightSurfaceAnchor: Color
        get() = listOf(
            primary.blendWith(Color.White, 0.16f),
            secondary.blendWith(Color.White, 0.24f),
            highlight.blendWith(Color.White, 0.32f)
        ).averageColor().blendWith(Color(0xFFF6F2ED), 0.10f)

    private fun backgroundGradientColors(darkTextEnabled: Boolean): List<Color> =
        if (darkTextEnabled) {
            artGradient
                .map { it.blendWith(lightSurfaceAnchor, 0.42f) }
                .ensureAverageContrastForDarkText()
        } else {
            artGradient.map { it.blendWith(Color.Black, darkBackgroundBlend) }
        }

    private fun menuGradientColors(darkTextEnabled: Boolean): List<Color> =
        if (darkTextEnabled) {
            menuGradient
                .map { it.blendWith(lightSurfaceAnchor, 0.50f) }
                .ensureAverageContrastForDarkText()
        } else {
            menuGradient.map { it.blendWith(drawerBackground, darkMenuBlend) }
        }

    private fun searchGradientColors(darkTextEnabled: Boolean): List<Color> =
        if (darkTextEnabled) {
            searchGradient
                .map { it.blendWith(lightSurfaceAnchor, 0.58f) }
                .ensureAverageContrastForDarkText()
        } else {
            searchGradient.map { it.blendWith(drawerBackground, darkSearchBlend) }
        }

    private fun animationGradientColors(darkTextEnabled: Boolean): List<Color> =
        if (darkTextEnabled) {
            animationGradient
                .map { it.blendWith(lightSurfaceAnchor, 0.46f) }
                .ensureAverageContrastForDarkText()
        } else {
            animationGradient.map { it.blendWith(Color.Black, darkAnimationBlend) }
        }

    companion object {
        val DarkTextColor = Color(0xFF010101)
        const val MinimumReadableContrast = 4.5f
    }
}

private fun gradientBrush(colors: List<Color>): Brush = Brush.linearGradient(
    colors = colors,
    start = Offset.Zero,
    end = Offset(1200f, 1600f)
)

private fun List<Color>.averageColor(): Color {
    if (isEmpty()) return Color.Black
    val size = size.toFloat()
    return Color(
        red = sumOf { it.red.toDouble() }.toFloat() / size,
        green = sumOf { it.green.toDouble() }.toFloat() / size,
        blue = sumOf { it.blue.toDouble() }.toFloat() / size,
        alpha = sumOf { it.alpha.toDouble() }.toFloat() / size
    )
}

private fun List<Color>.withAlpha(alpha: Float): List<Color> = map {
    it.copy(alpha = (it.alpha * alpha).coerceIn(0f, 1f))
}

private fun List<Color>.ensureAverageContrastForDarkText(
    minimumContrast: Float = ColorTheme.MinimumReadableContrast
): List<Color> {
    var adjusted = this
    repeat(10) {
        if (contrastRatio(ColorTheme.DarkTextColor, adjusted.averageColor()) >= minimumContrast) {
            return adjusted
        }
        adjusted = adjusted.map { it.blendWith(Color.White, 0.06f) }
    }
    return adjusted
}

private fun Color.blendWith(other: Color, ratio: Float): Color {
    val normalized = ratio.coerceIn(0f, 1f)
    val inverse = 1f - normalized
    return Color(
        red = (red * inverse) + (other.red * normalized),
        green = (green * inverse) + (other.green * normalized),
        blue = (blue * inverse) + (other.blue * normalized),
        alpha = (alpha * inverse) + (other.alpha * normalized)
    )
}

private fun contrastRatio(foreground: Color, background: Color): Float {
    val lighter = max(foreground.relativeLuminance(), background.relativeLuminance())
    val darker = min(foreground.relativeLuminance(), background.relativeLuminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun Color.relativeLuminance(): Float {
    fun channel(value: Float): Float = if (value <= 0.03928f) value / 12.92f else ((value + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    val r = channel(red)
    val g = channel(green)
    val b = channel(blue)
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}
