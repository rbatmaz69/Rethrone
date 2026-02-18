package com.example.androidlauncher.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
    onOpenSystemSettings: () -> Unit,
    onOpenInfo: () -> Unit
) {
    val settingsItems = remember {
        listOf(
            PaletteMenuItem("themes", Lucide.Palette, "Themes", onOpenColorConfig),
            PaletteMenuItem("colors", Lucide.Paintbrush, "Colors", onOpenColorConfig),
            PaletteMenuItem("favorites", Icons.Default.Star, "Favorites", onOpenFavoritesConfig),
            PaletteMenuItem("system", Icons.Default.Settings, "System", onOpenSystemSettings),
            PaletteMenuItem("info", Icons.Default.Info, "Info", onOpenInfo),
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val rotationAngle = remember { Animatable(0f) }
    
    // Wir tracken, ob gerade eine Drag-Geste stattfindet, um versehentliche Klicks zu vermeiden
    var isDragging by remember { mutableStateOf(false) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val angleStep = 25f 
    val baseAngle = 180f 

    LaunchedEffect(isSettingsOpen) {
        if (isSettingsOpen) {
            rotationAngle.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessLow))
        } else {
            isDragging = false
            totalDragDistance = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isSettingsOpen) {
                if (!isSettingsOpen) return@pointerInput
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        totalDragDistance = 0f
                    },
                    onDragEnd = {
                        // Snap-to-Position
                        val currentVal = rotationAngle.value
                        val targetAngle = round(currentVal / angleStep) * angleStep
                        coroutineScope.launch {
                            rotationAngle.animateTo(
                                targetAngle,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            )
                        }
                        // Wir setzen isDragging verzögert zurück, damit der Click-Event-Handler
                        // des Items weiß, dass gerade ein Drag zu Ende ging.
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(100)
                            isDragging = false
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val sensitivity = 0.4f
                        val delta = (dragAmount.x - dragAmount.y) * sensitivity
                        totalDragDistance += abs(delta)
                        coroutineScope.launch {
                            rotationAngle.snapTo(rotationAngle.value + delta)
                        }
                    }
                )
            }
    ) {
        val radius = 130.dp 
        
        settingsItems.forEachIndexed { index, item ->
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

            // Fokusbereich (225 Grad ist schräg links oben)
            val focusAngle = 225f
            val normalizedAngle = (currentItemAngle % 360 + 360) % 360
            val distanceToFocus = abs(normalizedAngle - focusAngle)
            val isFocused = distanceToFocus < (angleStep / 0.8f) && isSettingsOpen // Etwas toleranterer Fokus

            // Animationen für das aktive Element
            val scale by animateFloatAsState(
                targetValue = if (isFocused) 1.35f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
            val alpha by animateFloatAsState(targetValue = if (isFocused) 1f else 0.5f)

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 32.dp, end = 32.dp)
                    .offset(x = xOffset, y = yOffset)
                    .size(50.dp) 
                    .scale(scale)
                    .alpha(animatedProgress * alpha)
                    .clip(CircleShape)
                    .clickable(
                        enabled = isSettingsOpen && (!isDragging || totalDragDistance < 5f),
                        onClick = { 
                            item.action()
                            onToggleSettings()
                        }
                    ),
                color = if (isFocused) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.12f),
                shape = CircleShape,
                border = if (isFocused) BorderStroke(2.dp, Color.White.copy(alpha = 0.6f)) else null
            ) {
                Box(contentAlignment = Alignment.Center) {
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
