package com.example.androidlauncher.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ALargeSmall
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Pencil
import com.example.androidlauncher.ui.LiquidGlass.designSurface
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalHomeTextColor
import com.example.androidlauncher.ui.theme.LocalDesignStyle
import kotlinx.coroutines.delay
import kotlin.math.*

data class PaletteMenuItem(
    val id: String,
    val icon: ImageVector,
    val label: String,
    val action: () -> Unit
)

/**
 * A circular settings menu that expands when the settings button is clicked.
 * Provides quick access to:
 * - Favorites configuration
 * - Color theme configuration
 * - Size configuration
 * - System Settings / Editing mode
 * - App Info
 */
@Composable
fun SettingsPaletteMenu(
    isSettingsOpen: Boolean,
    onToggleSettings: () -> Unit,
    onOpenFavoritesConfig: () -> Unit,
    onOpenColorConfig: () -> Unit,
    onOpenSizeConfig: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onOpenInfo: () -> Unit
) {
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val designStyle = LocalDesignStyle.current
    val surfaceAccent = LocalColorTheme.current.menuSurfaceColor(isDarkTextEnabled)
    // Icon-Tönung folgt der gewählten Farbe – wie alles andere auf der Startseite.
    val mainTextColor = LocalHomeTextColor.current
    val haptics = com.example.androidlauncher.ui.theme.rememberAppHaptics()
    
    val settingsItems = remember {
        listOf(
            PaletteMenuItem("themes", Lucide.Palette, "Themes", onOpenColorConfig),
            PaletteMenuItem("size", Lucide.ALargeSmall, "Größe", onOpenSizeConfig),
            PaletteMenuItem("favorites", Icons.Rounded.Star, "Favorites", onOpenFavoritesConfig),
            PaletteMenuItem("edit", Lucide.Pencil, "Bearbeiten", onOpenSystemSettings),
            PaletteMenuItem("info", Icons.Rounded.Info, "Info", onOpenInfo),
        )
    }

    val radius = 110f 
    val startAngle = 85f 
    val endAngle = 185f
    val density = LocalDensity.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        val total = settingsItems.size
        val animStates = List(total) { remember { Animatable(0f) } }

        val animationsEnabled = com.example.androidlauncher.ui.theme.LocalMenuAnimationEnabled.current

        settingsItems.forEachIndexed { index, _ ->
            LaunchedEffect(isSettingsOpen) {
                if (isSettingsOpen) {
                    if (animationsEnabled) delay(index * 50L)
                    animStates[index].animateTo(1f, if (animationsEnabled) spring(0.62f, 260f) else snap())
                } else {
                    val reverseIndex = total - 1 - index
                    if (animationsEnabled) delay(reverseIndex * 30L)
                    animStates[index].animateTo(0f, if (animationsEnabled) spring(0.7f, 300f) else snap())
                }
            }
        }

        for (index in (total - 1) downTo 0) {
            val item = settingsItems[index]
            val progress = animStates[index].value

            if (progress > 0.001f || isSettingsOpen) {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val pressScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.92f else 1f,
                    animationSpec = if (animationsEnabled) spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow) else snap(),
                    label = "PressScale_${item.id}"
                )

                val angle = startAngle + (index.toFloat() / (total - 1)) * (endAngle - startAngle)
                val angleRad = Math.toRadians(angle.toDouble())
                val targetX = (cos(angleRad) * radius).toFloat()
                val targetY = (-sin(angleRad) * radius).toFloat()

                // Gleicher innerer Kreis-Hintergrund wie der Einstellungs-Button selbst.
                val backgroundModifier = Modifier.designSurface(
                    designStyle,
                    CircleShape,
                    isDarkTextEnabled,
                    surfaceAccent,
                    fillAlpha = if (isSettingsOpen) 0.1f else 0.15f
                )

                Box(
                    modifier = Modifier
                        .padding(bottom = 8.dp, end = 8.dp)
                        .size(56.dp)
                        .offset(x = (targetX * progress).dp, y = (targetY * progress).dp)
                        .scale(progress * pressScale)
                        .alpha(progress.coerceIn(0f, 1f))
                        .drawWithContent {
                            val path = Path()
                            var needsClip = false

                            for (j in 0 until index) {
                                val progressJ = animStates[j].value
                                if (progressJ > 0.001f) {
                                    val angleJ = startAngle + (j.toFloat() / (total - 1)) * (endAngle - startAngle)
                                    val angleRadJ = Math.toRadians(angleJ.toDouble())
                                    val tXj = (cos(angleRadJ) * radius).toFloat()
                                    val tYj = (-sin(angleRadJ) * radius).toFloat()

                                    val dx = (tXj * progressJ - targetX * progress)
                                    val dy = (tYj * progressJ - targetY * progress)

                                    with(density) {
                                        val cx = size.width / 2 + dx.dp.toPx()
                                        val cy = size.height / 2 + dy.dp.toPx()
                                        val r = 28.dp.toPx()
                                        path.addOval(Rect(cx - r, cy - r, cx + r, cy + r))
                                    }
                                    needsClip = true
                                }
                            }

                            if (needsClip) {
                                clipPath(path, clipOp = ClipOp.Difference) {
                                    this@drawWithContent.drawContent()
                                }
                            } else {
                                drawContent()
                            }
                        }
                ) {
                    // Bubble Content
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(backgroundModifier)
                            .testTag("settings_palette_item_${item.id}")
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                enabled = isSettingsOpen,
                                onClick = {
                                    haptics.confirm()
                                    item.action()
                                    onToggleSettings()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Kleiner extra Glanzpunkt (Specular Highlight) entfernt

                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = mainTextColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
