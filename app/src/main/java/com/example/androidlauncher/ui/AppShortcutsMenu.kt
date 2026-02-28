package com.example.androidlauncher.ui

import android.content.pm.ShortcutInfo
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalLiquidGlassEnabled
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun AppShortcutsMenu(
    packageName: String,
    targetBounds: Rect?,
    onDismiss: () -> Unit,
    onShortcutClick: (ShortcutInfo) -> Unit
) {
    val context = LocalContext.current
    val shortcuts = remember(packageName) { getAppShortcuts(context, packageName) }

    if (shortcuts.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val colorTheme = LocalColorTheme.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val isLiquidGlassEnabled = LocalLiquidGlassEnabled.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White

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

    val transition = updateTransition(targetState = isVisible && targetBounds != null, label = "ShortcutMenuTransition")

    val progress by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 350, easing = FastOutSlowInEasing) },
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
                .zIndex(6000f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = dismissWithAnimation
                )
        ) {
            targetBounds?.let { bounds ->
                val menuWidth = 220.dp
                val menuWidthPx = with(density) { menuWidth.toPx() }

                val itemsCount = shortcuts.size
                val estimatedMenuHeightPx = with(density) { (itemsCount * 48 + 16).dp.toPx() }

                var finalOffsetX = bounds.center.x - (menuWidthPx / 2)
                var finalOffsetY = bounds.bottom + with(density) { 8.dp.toPx() }

                finalOffsetX = finalOffsetX.coerceIn(with(density) { 16.dp.toPx() }, screenWidthPx - menuWidthPx - with(density) { 16.dp.toPx() })

                val isBelow = finalOffsetY + estimatedMenuHeightPx < screenHeightPx - with(density) { 16.dp.toPx() }
                if (!isBelow) {
                    finalOffsetY = bounds.top - estimatedMenuHeightPx - with(density) { 8.dp.toPx() }
                }

                val startOffsetX = bounds.center.x - (menuWidthPx / 2)
                val startOffsetY = bounds.center.y - (estimatedMenuHeightPx / 2)

                val currentOffsetX = startOffsetX + (finalOffsetX - startOffsetX) * progress
                val currentOffsetY = startOffsetY + (finalOffsetY - startOffsetY) * progress
                val scale = 0.05f + (0.95f * progress)

                val menuModifier = if (isLiquidGlassEnabled) {
                    val borderBrush = if (isDarkTextEnabled) {
                        Brush.linearGradient(colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.3f)))
                    } else {
                        Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f)))
                    }
                    Modifier.border(androidx.compose.foundation.BorderStroke(1.2.dp, borderBrush), RoundedCornerShape(20.dp))
                } else {
                    Modifier.border(androidx.compose.foundation.BorderStroke(1.dp, mainTextColor.copy(alpha = 0.12f)), RoundedCornerShape(20.dp))
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
                        .then(menuModifier),
                    color = if (isDarkTextEnabled) themedLightBackground.copy(alpha = 0.98f) else colorTheme.drawerBackground.copy(alpha = 0.98f),
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 20.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        shortcuts.forEach { shortcut ->
                            ShortcutMenuItem(
                                shortcut = shortcut,
                                textColor = mainTextColor,
                                onClick = {
                                    onShortcutClick(shortcut)
                                    dismissWithAnimation()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShortcutMenuItem(
    shortcut: ShortcutInfo,
    textColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val label = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
        shortcut.shortLabel ?: shortcut.longLabel ?: ""
    } else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = textColor.copy(alpha = 0.1f)),
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Here we could add shortcut icons if we load them properly
        Text(
            text = label.toString(),
            color = textColor,
            fontSize = 15.sp,
            maxLines = 1
        )
    }
}

