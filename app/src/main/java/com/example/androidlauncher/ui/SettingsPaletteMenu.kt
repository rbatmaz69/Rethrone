package com.example.androidlauncher.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ALargeSmall
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Palette
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalLiquidGlassEnabled
import kotlinx.coroutines.delay
import kotlin.math.*

data class PaletteMenuItem(
    val id: String,
    val icon: ImageVector,
    val label: String,
    val action: () -> Unit
)

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
    val isLiquidGlassEnabled = LocalLiquidGlassEnabled.current
    // Verwendung von #010101 gegen HW-Artefakte
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    
    val settingsItems = remember {
        listOf(
            PaletteMenuItem("themes", Lucide.Palette, "Themes", onOpenColorConfig),
            PaletteMenuItem("size", Lucide.ALargeSmall, "Größe", onOpenSizeConfig),
            PaletteMenuItem("favorites", Icons.Default.Star, "Favorites", onOpenFavoritesConfig),
            PaletteMenuItem("system", Icons.Default.Settings, "System", onOpenSystemSettings),
            PaletteMenuItem("info", Icons.Default.Info, "Info", onOpenInfo),
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

        settingsItems.forEachIndexed { index, _ ->
            LaunchedEffect(isSettingsOpen) {
                if (isSettingsOpen) {
                    delay(index * 50L)
                    animStates[index].animateTo(1f, spring(0.62f, 260f))
                } else {
                    val reverseIndex = total - 1 - index
                    delay(reverseIndex * 30L)
                    animStates[index].animateTo(0f, spring(0.7f, 300f))
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
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
                    label = "PressScale_${item.id}"
                )

                val angle = startAngle + (index.toFloat() / (total - 1)) * (endAngle - startAngle)
                val angleRad = Math.toRadians(angle.toDouble())
                val targetX = (cos(angleRad) * radius).toFloat()
                val targetY = (-sin(angleRad) * radius).toFloat()

                // Styles
                val backgroundModifier = if (isLiquidGlassEnabled) {
                     // Liquid/Glass Style Definition
                    val glassBrush = if (isDarkTextEnabled) {
                        // Light Mode - Kristallklar
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.15f),
                                Color.Black.copy(alpha = 0.05f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    } else {
                        // Dark Mode - Sehr transparentes Glas
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.05f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    }
                    val borderBrush = if (isDarkTextEnabled) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Black.copy(alpha = 0.3f)
                            )
                        )
                    } else {
                        // Hellerer Rand im Dark Mode für Glas-Kante
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.6f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                    Modifier
                        .background(glassBrush, CircleShape)
                        .border(BorderStroke(1.2.dp, borderBrush), CircleShape)
                } else {
                    // Standard Ansicht
                    Modifier
                        .background(mainTextColor.copy(alpha = if (isSettingsOpen) 0.1f else 0.15f), CircleShape)
                        .border(BorderStroke(1.dp, mainTextColor.copy(alpha = 0.25f)), CircleShape)
                }

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
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                enabled = isSettingsOpen,
                                onClick = {
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
