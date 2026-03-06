package com.example.androidlauncher.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.androidlauncher.data.AppFont
import com.example.androidlauncher.data.FontSize
import com.example.androidlauncher.data.FontWeightLevel
import com.example.androidlauncher.data.IconSize

/**
 * CompositionLocals for passing theme configuration down the widget tree.
 * Avoids prop drilling for commonly used style properties.
 */
val LocalColorTheme = staticCompositionLocalOf { ColorTheme.SIGNATURE }
val LocalFontSize = staticCompositionLocalOf { FontSize.STANDARD }
val LocalIconSize = staticCompositionLocalOf { IconSize.STANDARD }
val LocalFontWeight = staticCompositionLocalOf { FontWeightLevel.NORMAL }
val LocalDarkTextEnabled = staticCompositionLocalOf { false }
val LocalShowFavoriteLabels = staticCompositionLocalOf { false }
val LocalLiquidGlassEnabled = staticCompositionLocalOf { true }
val LocalAppFont = staticCompositionLocalOf { AppFont.SYSTEM_DEFAULT }
/**
 * CompositionLocal for haptic feedback enabled state.
 */
val LocalHapticFeedbackEnabled = staticCompositionLocalOf { true }

@Composable
fun AndroidLauncherTheme(
    colorTheme: ColorTheme = ColorTheme.SIGNATURE,
    fontSize: FontSize = FontSize.STANDARD,
    iconSize: IconSize = IconSize.STANDARD,
    fontWeight: FontWeightLevel = FontWeightLevel.NORMAL,
    darkTextEnabled: Boolean = false,
    showFavoriteLabels: Boolean = false,
    liquidGlassEnabled: Boolean = true,
    appFont: AppFont = AppFont.SYSTEM_DEFAULT,
    hapticFeedbackEnabled: Boolean = true,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalView.current.context
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            val backgroundColor = colorTheme.backgroundColor(darkTextEnabled)
            val menuSurfaceColor = colorTheme.menuSurfaceColor(darkTextEnabled)
            val searchSurfaceColor = colorTheme.searchSurfaceColor(darkTextEnabled)
            val accentColor = colorTheme.accentColor(darkTextEnabled)
            val highlightColor = colorTheme.highlightColor(darkTextEnabled)
            val outlineColor = colorTheme.borderColor(darkTextEnabled)

            darkColorScheme(
                primary = colorTheme.primary,
                secondary = accentColor,
                tertiary = highlightColor,
                background = backgroundColor,
                surface = menuSurfaceColor,
                surfaceVariant = searchSurfaceColor,
                secondaryContainer = searchSurfaceColor,
                tertiaryContainer = highlightColor.copy(alpha = 0.22f),
                outline = outlineColor
            )
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                window.isNavigationBarContrastEnforced = false
                @Suppress("DEPRECATION")
                window.isStatusBarContrastEnforced = false
            }

            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = darkTextEnabled
            controller.isAppearanceLightNavigationBars = darkTextEnabled
        }
    }

    CompositionLocalProvider(
        LocalColorTheme provides colorTheme,
        LocalFontSize provides fontSize,
        LocalIconSize provides iconSize,
        LocalFontWeight provides fontWeight,
        LocalDarkTextEnabled provides darkTextEnabled,
        LocalShowFavoriteLabels provides showFavoriteLabels,
        LocalLiquidGlassEnabled provides liquidGlassEnabled,
        LocalAppFont provides appFont,
        LocalHapticFeedbackEnabled provides hapticFeedbackEnabled
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = getTypography(appFont.fontFamily, fontWeight),
            content = content
        )
    }
}
