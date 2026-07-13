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
import com.example.androidlauncher.data.settings.AnimationSettings
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.androidlauncher.data.AppFont
import com.example.androidlauncher.data.DesignStyle
import com.example.androidlauncher.data.FavoriteSpacing
import com.example.androidlauncher.data.FavoritesBorderStyle
import com.example.androidlauncher.data.FontSize
import com.example.androidlauncher.data.FontWeightLevel
import com.example.androidlauncher.data.IconSize

/**
 * CompositionLocals for passing theme configuration down the widget tree.
 * Avoids prop drilling for commonly used style properties.
 */
val LocalColorTheme = staticCompositionLocalOf { ColorTheme.SIGNATURE }
val LocalFontSize = staticCompositionLocalOf { FontSize.STANDARD }
val LocalIconSize = staticCompositionLocalOf { IconSize.DEFAULT }

/** Vertikaler Abstand zwischen den Favoriten-Icons (Startbildschirm). */
val LocalFavoriteSpacing = staticCompositionLocalOf { FavoriteSpacing.DEFAULT }
val LocalFontWeight = staticCompositionLocalOf { FontWeightLevel.NORMAL }
val LocalDarkTextEnabled = staticCompositionLocalOf { false }

/** Frei wählbare Iconfarbe (gilt überall). */
val LocalIconColor = staticCompositionLocalOf { Color.White }

/** Frei wählbare Schriftfarbe – nur Startbildschirm. */
val LocalHomeTextColor = staticCompositionLocalOf { Color.White }
val LocalShowFavoriteLabels = staticCompositionLocalOf { false }

/** Ob Benachrichtigungs-Dots auf App-Icons (Favoriten/Drawer) angezeigt werden. */
val LocalNotificationDotsEnabled = staticCompositionLocalOf { true }
val LocalDesignStyle = staticCompositionLocalOf { DesignStyle.GLASS }
val LocalFavoritesBorderStyle = staticCompositionLocalOf { FavoritesBorderStyle.NONE }
val LocalAppFont = staticCompositionLocalOf { AppFont.SYSTEM_DEFAULT }

/**
 * CompositionLocal for haptic feedback enabled state.
 */
val LocalHapticFeedbackEnabled = staticCompositionLocalOf { true }

/**
 * CompositionLocal for animations enabled state.
 */
val LocalAnimationsEnabled = staticCompositionLocalOf { true }

/**
 * CompositionLocals für einzelne Animationsarten. Diese sind bereits mit dem
 * Master (LocalAnimationsEnabled) verknüpft: ist der Master aus, sind alle false.
 */
val LocalAppOpenAnimationEnabled = staticCompositionLocalOf { true }
val LocalAppCloseAnimationEnabled = staticCompositionLocalOf { true }
val LocalMenuAnimationEnabled = staticCompositionLocalOf { true }
val LocalFavoritesAnimationEnabled = staticCompositionLocalOf { true }

/**
 * Globaler Tempo-Faktor für Animationen (1f = normal, 2f = doppelt so schnell,
 * 0.5f = halbes Tempo). Wird von appTween/appSpring & den Menü-Übergängen genutzt.
 */
val LocalAnimationSpeed = staticCompositionLocalOf { AnimationSettings.DEFAULT_SPEED }

/**
 * CompositionLocal für das Wetter-Widget (Symbol + Temperatur unter der Uhr).
 */
val LocalWeatherWidgetEnabled = staticCompositionLocalOf { true }

/**
 * CompositionLocal für das Uhr-Widget.
 */
val LocalClockWidgetEnabled = staticCompositionLocalOf { true }

/**
 * CompositionLocal für das Kalender-/Datum-Widget.
 */
val LocalCalendarWidgetEnabled = staticCompositionLocalOf { true }

@Composable
fun AndroidLauncherTheme(
    colorTheme: ColorTheme = ColorTheme.SIGNATURE,
    fontSize: FontSize = FontSize.STANDARD,
    iconSize: IconSize = IconSize.DEFAULT,
    favoriteSpacing: FavoriteSpacing = FavoriteSpacing.DEFAULT,
    fontWeight: FontWeightLevel = FontWeightLevel.NORMAL,
    darkTextEnabled: Boolean = false,
    iconColor: Color = Color.White,
    homeTextColor: Color = Color.White,
    showFavoriteLabels: Boolean = false,
    notificationDotsEnabled: Boolean = true,
    designStyle: DesignStyle = DesignStyle.GLASS,
    favoritesBorderStyle: FavoritesBorderStyle = FavoritesBorderStyle.NONE,
    appFont: AppFont = AppFont.SYSTEM_DEFAULT,
    hapticFeedbackEnabled: Boolean = true,
    animationsEnabled: Boolean = true,
    appOpenAnimationEnabled: Boolean = true,
    appCloseAnimationEnabled: Boolean = true,
    menuAnimationEnabled: Boolean = true,
    favoritesAnimationEnabled: Boolean = true,
    animationSpeed: Float = AnimationSettings.DEFAULT_SPEED,
    weatherWidgetEnabled: Boolean = true,
    clockWidgetEnabled: Boolean = true,
    calendarWidgetEnabled: Boolean = true,
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
                primary = colorTheme.schemePrimary(),
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
        LocalFavoriteSpacing provides favoriteSpacing,
        LocalFontWeight provides fontWeight,
        LocalDarkTextEnabled provides darkTextEnabled,
        LocalIconColor provides iconColor,
        LocalHomeTextColor provides homeTextColor,
        LocalShowFavoriteLabels provides showFavoriteLabels,
        LocalNotificationDotsEnabled provides notificationDotsEnabled,
        LocalDesignStyle provides designStyle,
        LocalFavoritesBorderStyle provides favoritesBorderStyle,
        LocalAppFont provides appFont,
        LocalHapticFeedbackEnabled provides hapticFeedbackEnabled,
        LocalAnimationsEnabled provides animationsEnabled,
        LocalAppOpenAnimationEnabled provides (animationsEnabled && appOpenAnimationEnabled),
        LocalAppCloseAnimationEnabled provides (animationsEnabled && appCloseAnimationEnabled),
        LocalMenuAnimationEnabled provides (animationsEnabled && menuAnimationEnabled),
        LocalFavoritesAnimationEnabled provides (animationsEnabled && favoritesAnimationEnabled),
        LocalAnimationSpeed provides animationSpeed,
        LocalWeatherWidgetEnabled provides weatherWidgetEnabled,
        LocalClockWidgetEnabled provides clockWidgetEnabled,
        LocalCalendarWidgetEnabled provides calendarWidgetEnabled
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = getTypography(appFont.fontFamily, fontWeight),
            shapes = RethroneShapes,
            content = content
        )
    }
}
