package com.example.androidlauncher.ui

import androidx.compose.animation.*
import androidx.compose.foundation.border
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

/**
 * A floating context menu that appears on long-press of an app icon.
 * Provides options like:
 * - App Info (System Settings)
 * - Uninstall
 * - Add/Remove from Favorites
 * - Move to Folder
 */
@Composable
fun AppContextMenu(
    isFavorite: Boolean,
    targetBounds: Rect?,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onMoveToFolder: (() -> Unit)? = null,
    onRemoveFromFolder: (() -> Unit)? = null
) {
    val colorTheme = LocalColorTheme.current
    val fontSize = LocalFontSize.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val isLiquidGlassEnabled = LocalLiquidGlassEnabled.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White

    // Calculate a light background based on the theme's primary color for the light mode (dark text)
    // Mixing 90% primary color with 10% white to get a clearly visible pastel tint of the theme
    val themedLightBackground = remember(colorTheme.primary) {
        val primary = colorTheme.primary
        Color(
            red = primary.red * 0.90f + 0.10f,
            green = primary.green * 0.90f + 0.10f,
            blue = primary.blue * 0.90f + 0.10f,
            alpha = 1f
        )
    }

    val density = LocalDensity.current
    val config = LocalConfiguration.current

    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }

    var isVisible by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(targetBounds) {
        if (targetBounds != null) {
            isVisible = true
            isDismissing = false
        }
    }

    val transition = updateTransition(targetState = isVisible && targetBounds != null, label = "MenuTransition")

    // Wir nutzen für beide Richtungen eine symmetrische Kurve für den "Fluss"-Effekt
    val progress by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = 350, easing = FastOutSlowInEasing)
        },
        label = "Progress"
    ) { if (it) 1f else 0f }

    val dismissWithAnimation = {
        if (!isDismissing) {
            isDismissing = true
            isVisible = false
        }
    }

    LaunchedEffect(isVisible) {
        if (!isVisible && isDismissing) {
            delay(350)
            onDismiss()
        }
    }

    if (transition.currentState || transition.targetState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(5000f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = dismissWithAnimation
                )
        ) {
            targetBounds?.let { bounds ->
                val menuWidth = 240.dp
                val menuWidthPx = with(density) { menuWidth.toPx() }
                
                val itemsCount = 3 + (if (onMoveToFolder != null) 1 else 0) + (if (onRemoveFromFolder != null) 1 else 0)
                val estimatedMenuHeightPx = with(density) { (itemsCount * 48 + itemsCount + 16).dp.toPx() }

                // Finale Zielposition (unter oder über dem Icon)
                var finalOffsetX = bounds.center.x - (menuWidthPx / 2)
                var finalOffsetY = bounds.bottom + with(density) { 8.dp.toPx() }

                finalOffsetX = finalOffsetX.coerceIn(with(density) { 16.dp.toPx() }, screenWidthPx - menuWidthPx - with(density) { 16.dp.toPx() })
                
                val isBelow = finalOffsetY + estimatedMenuHeightPx < screenHeightPx - with(density) { 16.dp.toPx() }
                if (!isBelow) {
                    finalOffsetY = bounds.top - estimatedMenuHeightPx - with(density) { 8.dp.toPx() }
                }

                // Exakte Startposition (Zentrum des Icons)
                val startOffsetX = bounds.center.x - (menuWidthPx / 2)
                val startOffsetY = bounds.center.y - (estimatedMenuHeightPx / 2)

                // Butterweiche Interpolation der Position
                val currentOffsetX = startOffsetX + (finalOffsetX - startOffsetX) * progress
                val currentOffsetY = startOffsetY + (finalOffsetY - startOffsetY) * progress
                
                // Skalierung von fast 0 auf 1
                val scale = 0.05f + (0.95f * progress)

                val menuModifier = if (isLiquidGlassEnabled) {
                    val borderBrush = if (isDarkTextEnabled) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Black.copy(alpha = 0.3f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.6f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }

                    Modifier.border(androidx.compose.foundation.BorderStroke(1.2.dp, borderBrush), RoundedCornerShape(24.dp))
                } else {
                    Modifier.border(androidx.compose.foundation.BorderStroke(1.dp, mainTextColor.copy(alpha = 0.12f)), RoundedCornerShape(24.dp))
                }

                Surface(
                    modifier = Modifier
                        .offset { IntOffset(currentOffsetX.roundToInt(), currentOffsetY.roundToInt()) }
                        .width(menuWidth)
                        .graphicsLayer {
                            this.scaleX = scale
                            this.scaleY = scale
                            this.alpha = progress.coerceIn(0f, 1f)
                            this.transformOrigin = TransformOrigin.Center
                        }
                        .clickable(enabled = false) {}
                        .then(menuModifier), // Apply custom border here
                    color = if (isDarkTextEnabled) themedLightBackground.copy(alpha = 0.98f) else colorTheme.drawerBackground.copy(alpha = 0.98f),
                    shape = RoundedCornerShape(24.dp),
                    // Remove explicit border parameter as we apply it via modifier
                    shadowElevation = 24.dp
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        ContextMenuItem(
                            icon = Lucide.Info,
                            text = "App-Info",
                            color = mainTextColor,
                            onClick = { onAppInfo(); dismissWithAnimation() }
                        )

                        Divider(modifier = Modifier.padding(horizontal = 12.dp), color = mainTextColor.copy(alpha = 0.08f))

                        ContextMenuItem(
                            icon = if (isFavorite) Lucide.StarOff else Lucide.Star,
                            text = if (isFavorite) "Vom Home entfernen" else "Zu Favoriten hinzufügen",
                            color = if (isFavorite) Color(0xFFFFB74D) else mainTextColor,
                            onClick = { onToggleFavorite(); dismissWithAnimation() }
                        )

                        if (onMoveToFolder != null) {
                            Divider(modifier = Modifier.padding(horizontal = 12.dp), color = mainTextColor.copy(alpha = 0.08f))
                            ContextMenuItem(
                                icon = Lucide.FolderInput,
                                text = "In Ordner verschieben",
                                color = mainTextColor,
                                onClick = { onMoveToFolder(); dismissWithAnimation() }
                            )
                        }

                        if (onRemoveFromFolder != null) {
                            Divider(modifier = Modifier.padding(horizontal = 12.dp), color = mainTextColor.copy(alpha = 0.08f))
                            ContextMenuItem(
                                icon = Lucide.FolderOutput,
                                text = "Aus Ordner entfernen",
                                color = mainTextColor,
                                onClick = { onRemoveFromFolder(); dismissWithAnimation() }
                            )
                        }

                        Divider(modifier = Modifier.padding(horizontal = 12.dp), color = mainTextColor.copy(alpha = 0.08f))

                        ContextMenuItem(
                            icon = Lucide.Trash2,
                            text = "Deinstallieren",
                            color = Color(0xFFEF5350),
                            onClick = { onUninstall(); dismissWithAnimation() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    val fontSize = LocalFontSize.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = text,
            color = color,
            fontSize = 14.sp * fontSize.scale,
            fontWeight = FontWeight.Normal
        )
    }
}
