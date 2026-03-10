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

    return Typography(
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = regularWeight,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = regularWeight,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = mediumWeight,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        // Add other styles if needed
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = regularWeight,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = mediumWeight,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        )
    )
}

// Default Typography
val Typography = getTypography(FontFamily.Default)
