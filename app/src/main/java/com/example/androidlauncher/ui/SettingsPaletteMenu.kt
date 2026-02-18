package com.example.androidlauncher.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Paintbrush
import com.composables.icons.lucide.Palette
import kotlinx.coroutines.launch
import kotlin.math.*

data class PaletteMenuItem(
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
    onOpenSystemSettings: () -> Unit,
    onOpenInfo: () -> Unit
) {
    val settingsItems = remember {
        listOf(
            PaletteMenuItem(icon = Lucide.Palette, label = "Themes", action = onOpenColorConfig),
            PaletteMenuItem(icon = Lucide.Paintbrush, label = "Colors", action = onOpenColorConfig),
            PaletteMenuItem(icon = Icons.Default.Star, label = "Favorites", action = onOpenFavoritesConfig),
            PaletteMenuItem(icon = Icons.Default.Settings, label = "System", action = onOpenSystemSettings),
            PaletteMenuItem(icon = Icons.Default.Info, label = "Info", action = onOpenInfo),
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val rotationAngle = remember { Animatable(0f) }
    
    // Verringerter Winkel für engere Abstände (vorher 45f)
    val angleStep = 25f 
    val baseAngle = 180f 

    LaunchedEffect(isSettingsOpen) {
        if (isSettingsOpen) {
            rotationAngle.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessLow))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isSettingsOpen) {
                if (!isSettingsOpen) return@pointerInput
                detectDragGestures(
                    onDragEnd = {
                        val currentVal = rotationAngle.value
                        val targetAngle = round(currentVal / angleStep) * angleStep
                        coroutineScope.launch {
                            rotationAngle.animateTo(
                                targetAngle,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            )
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val sensitivity = 0.4f
                        val delta = (dragAmount.x - dragAmount.y) * sensitivity
                        coroutineScope.launch {
                            rotationAngle.snapTo(rotationAngle.value + delta)
                        }
                    }
                )
            }
    ) {
        val radius = 140.dp // Etwas kleinerer Radius für kompakteres Menü
        
        settingsItems.forEachIndexed { index, item ->
            // Stabilere Animation ohne EaseOutBack (um Absturz zu vermeiden)
            val animatedProgress by animateFloatAsState(
                targetValue = if (isSettingsOpen) 1f else 0f,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = LinearOutSlowInEasing
                )
            )

            val currentItemAngle = baseAngle + (index * angleStep) + rotationAngle.value
            val angleRad = Math.toRadians(currentItemAngle.toDouble())
            
            val xOffset = (radius.value * cos(angleRad)).dp * animatedProgress
            val yOffset = (radius.value * sin(angleRad)).dp * animatedProgress

            // Fokusbereich angepasst
            val focusAngle = 225f
            val normalizedAngle = (currentItemAngle % 360 + 360) % 360
            val distanceToFocus = abs(normalizedAngle - focusAngle)
            val isFocused = distanceToFocus < (angleStep / 2) && isSettingsOpen

            val scale by animateFloatAsState(targetValue = if (isFocused) 1.25f else 1.0f)
            val alpha by animateFloatAsState(targetValue = if (isFocused) 1f else 0.7f)

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 32.dp, end = 32.dp)
                    .offset(x = xOffset, y = yOffset)
                    .size(52.dp) // Leicht verkleinerte Symbole für kompakten Look
                    .scale(scale)
                    .alpha(animatedProgress * alpha)
                    .clip(CircleShape)
                    .clickable(enabled = isSettingsOpen) { 
                        item.action()
                        onToggleSettings()
                    },
                color = if (isFocused) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
