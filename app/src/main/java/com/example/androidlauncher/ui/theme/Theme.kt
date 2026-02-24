package com.example.androidlauncher.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.example.androidlauncher.data.FontSize
import com.example.androidlauncher.data.IconSize

val LocalColorTheme = staticCompositionLocalOf { ColorTheme.SIGNATURE }
val LocalFontSize = staticCompositionLocalOf { FontSize.STANDARD }
val LocalIconSize = staticCompositionLocalOf { IconSize.STANDARD }
val LocalDarkTextEnabled = staticCompositionLocalOf { false }
val LocalShowFavoriteLabels = staticCompositionLocalOf { false }
val LocalLiquidGlassEnabled = staticCompositionLocalOf { true }

@Composable
fun AndroidLauncherTheme(
    colorTheme: ColorTheme = ColorTheme.SIGNATURE,
    fontSize: FontSize = FontSize.STANDARD,
    iconSize: IconSize = IconSize.STANDARD,
    darkTextEnabled: Boolean = false,
    showFavoriteLabels: Boolean = false,
    liquidGlassEnabled: Boolean = true,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            val backgroundColor = if (darkTextEnabled) colorTheme.lightBackground else colorTheme.drawerBackground

            darkColorScheme(
                primary = colorTheme.primary,
                secondary = colorTheme.secondary,
                tertiary = colorTheme.tertiary,
                background = backgroundColor,
                surface = backgroundColor.copy(alpha = 0.8f)
            )
        }
    }

    CompositionLocalProvider(
        LocalColorTheme provides colorTheme,
        LocalFontSize provides fontSize,
        LocalIconSize provides iconSize,
        LocalDarkTextEnabled provides darkTextEnabled,
        LocalShowFavoriteLabels provides showFavoriteLabels,
        LocalLiquidGlassEnabled provides liquidGlassEnabled
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
