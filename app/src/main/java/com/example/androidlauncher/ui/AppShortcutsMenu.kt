package com.example.androidlauncher.ui

import android.content.pm.ShortcutInfo
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalLiquidGlassEnabled
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Schwebendes Shortcuts-Menü für eine App.
 *
 * Wird angezeigt wenn der Nutzer auf dem Homescreen nach rechts wischt.
 * Zeigt bis zu 4 App-Shortcuts (dynamisch + statisch) die das System bereitstellt.
 * Positioniert sich automatisch relativ zum auslösenden Icon.
 *
 * @param packageName Paketname der Ziel-App.
 * @param targetBounds Bildschirm-Position des auslösenden Icons.
 * @param onDismiss Callback zum Schließen des Menüs.
 * @param onShortcutClick Callback beim Klick auf einen Shortcut.
 */
@Composable
fun AppShortcutsMenu(
    packageName: String,
    targetBounds: Rect?,
    onDismiss: () -> Unit,
    onShortcutClick: (ShortcutInfo) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val shortcuts = remember(packageName) {
        getAppShortcuts(context, packageName).take(4)
    }

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
    
    // Wir messen die Größe und Position des Menü-Containers selbst
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var containerPositionInRoot by remember { mutableStateOf(Offset.Zero) }

    var isVisible by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(targetBounds) {
        if (targetBounds != null) {
            isVisible = true
            isDismissing = false
            // Vibrationsfeedback beim Öffnen
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                .onGloballyPositioned { coords ->
                    containerSize = coords.size
                    containerPositionInRoot = coords.positionInRoot()
                }
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

                val screenWidthPx = containerSize.width.toFloat()
                val screenHeightPx = containerSize.height.toFloat()

                if (screenWidthPx <= 0f || screenHeightPx <= 0f) return@let

                // Horizontale Positionierung
                var finalOffsetX = bounds.right + with(density) { 12.dp.toPx() }
                if (finalOffsetX + menuWidthPx > screenWidthPx - with(density) { 16.dp.toPx() }) {
                    finalOffsetX = bounds.left - menuWidthPx - with(density) { 12.dp.toPx() }
                }

                // Vertikale Positionierung: Wenn das Icon im unteren Bereich ist, Menü nach OBEN schieben
                val isNearBottom = bounds.bottom > screenHeightPx * 0.75f
                var finalOffsetY: Float
                
                if (isNearBottom) {
                    // Über dem Icon platzieren
                    finalOffsetY = bounds.top - estimatedMenuHeightPx - with(density) { 8.dp.toPx() }
                } else {
                    // Mittig zum Icon platzieren
                    finalOffsetY = bounds.center.y - (estimatedMenuHeightPx / 2)
                }

                // System Bars berücksichtigen (Padding oben/unten)
                val topInset = with(density) { WindowInsets.systemBars.asPaddingValues().calculateTopPadding().toPx() }
                val bottomInset = with(density) { WindowInsets.systemBars.asPaddingValues().calculateBottomPadding().toPx() }
                
                val minY = topInset + with(density) { 16.dp.toPx() }
                val maxY = screenHeightPx - bottomInset - estimatedMenuHeightPx - with(density) { 16.dp.toPx() }

                finalOffsetX = finalOffsetX.coerceIn(with(density) { 16.dp.toPx() }, screenWidthPx - menuWidthPx - with(density) { 16.dp.toPx() })
                finalOffsetY = finalOffsetY.coerceIn(minY, maxY)

                // Korrektur des Offsets relativ zum Container
                val relativeX = finalOffsetX - containerPositionInRoot.x
                val relativeY = finalOffsetY - containerPositionInRoot.y

                val startOffsetX = bounds.center.x - containerPositionInRoot.x - (menuWidthPx / 2)
                val startOffsetY = bounds.center.y - containerPositionInRoot.y - (estimatedMenuHeightPx / 2)

                val currentOffsetX = startOffsetX + (relativeX - startOffsetX) * progress
                val currentOffsetY = startOffsetY + (relativeY - startOffsetY) * progress
                val scale = 0.05f + (0.95f * progress)

                val menuModifier = if (isLiquidGlassEnabled) {
                    Modifier.border(BorderStroke(1.2.dp, LiquidGlass.borderBrush(isDarkTextEnabled)), RoundedCornerShape(20.dp))
                } else {
                    Modifier.border(BorderStroke(1.dp, mainTextColor.copy(alpha = 0.12f)), RoundedCornerShape(20.dp))
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
        Text(
            text = label.toString(),
            color = textColor,
            fontSize = 15.sp,
            maxLines = 1
        )
    }
}
