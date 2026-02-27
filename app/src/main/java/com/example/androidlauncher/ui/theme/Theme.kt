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
import com.example.androidlauncher.data.IconSize

/**
 * CompositionLocals for passing theme configuration down the widget tree.
 * Avoids prop drilling for commonly used style properties.
 */
val LocalColorTheme = staticCompositionLocalOf { ColorTheme.SIGNATURE }
val LocalFontSize = staticCompositionLocalOf { FontSize.STANDARD }
val LocalIconSize = staticCompositionLocalOf { IconSize.STANDARD }
val LocalDarkTextEnabled = staticCompositionLocalOf { false }
val LocalShowFavoriteLabels = staticCompositionLocalOf { false }
/**
 * CompositionLocal for the "Liquid Glass" visual effect.
 */
val LocalLiquidGlassEnabled = staticCompositionLocalOf { true }
val LocalAppFont = staticCompositionLocalOf { AppFont.SYSTEM_DEFAULT }

private val DarkColorScheme = darkColorScheme(
    primary = ColorTheme.SIGNATURE.primary,
    secondary = ColorTheme.SIGNATURE.secondary,
    tertiary = ColorTheme.SIGNATURE.tertiary,
    background = ColorTheme.SIGNATURE.lightBackground,
    surface = ColorTheme.SIGNATURE.lightBackground.copy(alpha = 0.8f)
)

@Composable
fun AndroidLauncherTheme(
    colorTheme: ColorTheme = ColorTheme.SIGNATURE,
    fontSize: FontSize = FontSize.STANDARD,
    iconSize: IconSize = IconSize.STANDARD,
    darkTextEnabled: Boolean = false,
    showFavoriteLabels: Boolean = false,
    liquidGlassEnabled: Boolean = true,
    appFont: AppFont = AppFont.SYSTEM_DEFAULT,
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
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
        LocalDarkTextEnabled provides darkTextEnabled,
        LocalShowFavoriteLabels provides showFavoriteLabels,
        LocalLiquidGlassEnabled provides liquidGlassEnabled,
        LocalAppFont provides appFont
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = getTypography(appFont.fontFamily),
            content = content
        )
    }
}
