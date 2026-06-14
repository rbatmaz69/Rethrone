package com.example.androidlauncher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.data.FontWeightLevel

/**
 * Returns a Typography object with the specified [fontFamily] and [baseWeight].
 * This allows dynamic switching of the application's font and its weight.
 */
fun getTypography(fontFamily: FontFamily, baseWeight: FontWeightLevel = FontWeightLevel.NORMAL): Typography {
    // Determine weights based on the selected base weight level
    val (regularWeight, mediumWeight) = when (baseWeight) {
        FontWeightLevel.LIGHT -> Pair(FontWeight.Light, FontWeight.Normal)
        FontWeightLevel.NORMAL -> Pair(FontWeight.Normal, FontWeight.Medium)
        FontWeightLevel.BOLD -> Pair(FontWeight.Medium, FontWeight.Bold)
    }
    // Material-3-Expressive betont „Emphasis": Display/Headline kräftiger als der Body.
    val emphasizedWeight = when (baseWeight) {
        FontWeightLevel.LIGHT -> FontWeight.Medium
        FontWeightLevel.NORMAL -> FontWeight.SemiBold
        FontWeightLevel.BOLD -> FontWeight.Bold
    }

    return Typography(
        // ── Display (große, ausdrucksstarke Titel, z. B. Uhr) ──
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = emphasizedWeight,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = emphasizedWeight,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = emphasizedWeight,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp
        ),
        // ── Headline (Abschnittsüberschriften) ──
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = emphasizedWeight,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = emphasizedWeight,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = mediumWeight,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        // ── Title ──
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = mediumWeight,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = mediumWeight,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = mediumWeight,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        // ── Body ──
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = regularWeight,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = regularWeight,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = regularWeight,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        // ── Label ──
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = mediumWeight,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = mediumWeight,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = mediumWeight,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}

// Default Typography
val Typography = getTypography(FontFamily.Default)
