package com.example.androidlauncher.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.geometry.Rect
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
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalFontSize
import com.composables.icons.lucide.*
import kotlin.math.roundToInt

@Composable
fun AppContextMenu(
    targetBounds: Rect?,
    isFavorite: Boolean,
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
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val density = LocalDensity.current
    val config = LocalConfiguration.current

    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(targetBounds) {
        if (targetBounds != null) {
            isVisible = true
        }
    }

    val transition = updateTransition(targetState = isVisible && targetBounds != null, label = "MenuTransition")
    
    val scale by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
            } else {
                tween(durationMillis = 250, easing = FastOutSlowInEasing)
            }
        },
        label = "Scale"
    ) { if (it) 1f else 0.4f }

    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 200) },
        label = "Alpha"
    ) { if (it) 1f else 0f }

    val dismissAndAnimate = {
        isVisible = false
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            kotlinx.coroutines.delay(250)
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
                    onClick = dismissAndAnimate
                )
        ) {
            targetBounds?.let { bounds ->
                val menuWidth = 240.dp
                val menuWidthPx = with(density) { menuWidth.toPx() }
                
                val estimatedMenuHeightPx = with(density) { 
                    val items = 3 + (if (onMoveToFolder != null) 1 else 0) + (if (onRemoveFromFolder != null) 1 else 0)
                    (items * 48 + 16).dp.toPx() 
                }

                val isLeftHalf = bounds.center.x < screenWidthPx / 2
                
                var offsetX = if (isLeftHalf) {
                    bounds.right + with(density) { 12.dp.toPx() }
                } else {
                    bounds.left - menuWidthPx - with(density) { 12.dp.toPx() }
                }

                // Horizontal boundary checks
                offsetX = offsetX.coerceIn(with(density) { 16.dp.toPx() }, screenWidthPx - menuWidthPx - with(density) { 16.dp.toPx() })
                
                // Vertical positioning: center the menu vertically relative to the icon
                var offsetY = bounds.center.y - (estimatedMenuHeightPx / 2)
                
                // Vertical boundary checks
                offsetY = offsetY.coerceIn(with(density) { 16.dp.toPx() }, screenHeightPx - estimatedMenuHeightPx - with(density) { 16.dp.toPx() })

                Surface(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .width(menuWidth)
                        .graphicsLayer {
                            this.scaleX = scale
                            this.scaleY = scale
                            this.alpha = alpha
                            // Fließen aus dem Icon: TransformOrigin ist an der dem Icon zugewandten Seite
                            this.transformOrigin = TransformOrigin(if (isLeftHalf) 0f else 1f, 0.5f)
                        }
                        .clickable(enabled = false) {},
                    color = colorTheme.drawerBackground.copy(alpha = 0.98f),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, mainTextColor.copy(alpha = 0.12f)),
                    shadowElevation = 24.dp
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        ContextMenuItem(
                            icon = Lucide.Info,
                            text = "App-Info",
                            color = mainTextColor,
                            onClick = { onAppInfo(); dismissAndAnimate() }
                        )

                        Divider(modifier = Modifier.padding(horizontal = 12.dp), color = mainTextColor.copy(alpha = 0.08f))

                        ContextMenuItem(
                            icon = if (isFavorite) Lucide.StarOff else Lucide.Star,
                            text = if (isFavorite) "Vom Home entfernen" else "Zu Favoriten hinzufügen",
                            color = if (isFavorite) Color(0xFFFFB74D) else mainTextColor,
                            onClick = { onToggleFavorite(); dismissAndAnimate() }
                        )

                        if (onMoveToFolder != null) {
                            Divider(modifier = Modifier.padding(horizontal = 12.dp), color = mainTextColor.copy(alpha = 0.08f))
                            ContextMenuItem(
                                icon = Lucide.FolderInput,
                                text = "In Ordner verschieben",
                                color = mainTextColor,
                                onClick = { onMoveToFolder(); dismissAndAnimate() }
                            )
                        }

                        if (onRemoveFromFolder != null) {
                            Divider(modifier = Modifier.padding(horizontal = 12.dp), color = mainTextColor.copy(alpha = 0.08f))
                            ContextMenuItem(
                                icon = Lucide.FolderOutput,
                                text = "Aus Ordner entfernen",
                                color = mainTextColor,
                                onClick = { onRemoveFromFolder(); dismissAndAnimate() }
                            )
                        }

                        Divider(modifier = Modifier.padding(horizontal = 12.dp), color = mainTextColor.copy(alpha = 0.08f))

                        ContextMenuItem(
                            icon = Lucide.Trash2,
                            text = "Deinstallieren",
                            color = Color(0xFFEF5350),
                            onClick = { onUninstall(); dismissAndAnimate() }
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
