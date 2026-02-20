package com.example.androidlauncher.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
        
        // Wir benötigen die Zustände aller Items gleichzeitig für das Clipping
        val animStates = List(total) { index ->
            remember { Animatable(0f) }
        }
        
        settingsItems.forEachIndexed { index, item ->
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

        // Zeichnen der Items: Von unten nach oben
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

                Box(
                    modifier = Modifier
                        .padding(bottom = 8.dp, end = 8.dp) 
                        .size(56.dp)
                        .offset(x = (targetX * progress).dp, y = (targetY * progress).dp)
                        .scale(progress * pressScale)
                        .alpha(progress.coerceIn(0f, 1f))
                        .drawWithContent {
                            // Das Clipping: Wir schneiden alles weg, was von Items ÜBER uns (j < index) verdeckt wird
                            val path = Path()
                            var needsClip = false
                            
                            for (j in 0 until index) {
                                val progressJ = animStates[j].value
                                if (progressJ > 0.001f) {
                                    val angleJ = startAngle + (j.toFloat() / (total - 1)) * (endAngle - startAngle)
                                    val angleRadJ = Math.toRadians(angleJ.toDouble())
                                    val tXj = (cos(angleRadJ) * radius).toFloat()
                                    val tYj = (-sin(angleRadJ) * radius).toFloat()
                                    
                                    // Relativer Versatz des "oberen" Kreises zum aktuellen
                                    val dx = (tXj * progressJ - targetX * progress)
                                    val dy = (tYj * progressJ - targetY * progress)
                                    
                                    with(density) {
                                        val cx = size.width / 2 + dx.dp.toPx()
                                        val cy = size.height / 2 + dy.dp.toPx()
                                        val r = 28.dp.toPx() // Radius der 56.dp Kreise
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
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.White.copy(alpha = 0.15f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
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
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
