package com.example.androidlauncher.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Paintbrush
import com.composables.icons.lucide.Palette
import com.example.androidlauncher.ui.theme.LocalColorTheme
import kotlinx.coroutines.delay
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

    // Symmetrie-Parameter mit vergrößertem Winkelbereich für mehr Abstand
    val radius = 100f 
    val startAngle = 85f  // Weiter nach "Rechts/Oben" geöffnet
    val endAngle = 185f    // Weiter nach "Unten/Links" geöffnet
    // Mittelpunkt bleibt exakt bei 135° (Diagonale) für perfekte Symmetrie

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        settingsItems.forEachIndexed { index, item ->
            val animProgress = remember { Animatable(0f) }
            
            LaunchedEffect(isSettingsOpen) {
                if (isSettingsOpen) {
                    delay(index * 40L)
                    animProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.65f, 
                            stiffness = 260f
                        )
                    )
                } else {
                    animProgress.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessMedium))
                }
            }

            val progress = animProgress.value
            if (progress > 0.001f || isSettingsOpen) {
                val total = settingsItems.size
                // Winkelberechnung für perfekte Symmetrie um die 135°-Achse
                val angle = startAngle + (index.toFloat() / (total - 1)) * (endAngle - startAngle)
                val angleRad = Math.toRadians(angle.toDouble())
                
                val targetX = (cos(angleRad) * radius).toFloat()
                val targetY = (-sin(angleRad) * radius).toFloat()

                Surface(
                    modifier = Modifier
                        .padding(bottom = 8.dp, end = 8.dp) 
                        .size(56.dp)
                        .offset(
                            x = (targetX * progress).dp,
                            y = (targetY * progress).dp
                        )
                        .scale(progress)
                        .alpha(progress.coerceIn(0f, 1f))
                        .clickable(
                            enabled = isSettingsOpen,
                            onClick = {
                                item.action()
                                onToggleSettings()
                            }
                        ),
                    color = Color.White.copy(alpha = 0.15f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
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
}
