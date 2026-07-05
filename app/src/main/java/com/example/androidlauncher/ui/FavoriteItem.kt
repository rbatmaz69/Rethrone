package com.example.androidlauncher.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.FavoritesEntranceTracker
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.ui.LiquidGlass.designSurface
import com.example.androidlauncher.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Favoriten-Eintrag der Startbildschirm-Leiste + Edit-Steuerknopf
 * (A6-Split aus HomeScreen.kt; gleiches Paket, keine Aufrufer-Aenderungen).
 */

@Composable
internal fun EditControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    tint: Color,
    sizeDp: androidx.compose.ui.unit.Dp = 56.dp,
    containerColor: Color = Color.Transparent,
    testTag: String? = null
) {
    val intSrc = remember { MutableInteractionSource() }
    val designStyle = LocalDesignStyle.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val surfaceAccent = LocalColorTheme.current.menuSurfaceColor(isDarkTextEnabled)

    Box(
        modifier = Modifier
            .size(sizeDp)
            .then(
                if (containerColor == Color.Transparent) {
                    Modifier.designSurface(designStyle, CircleShape, isDarkTextEnabled, surfaceAccent, fillAlpha = 0.1f)
                } else {
                    Modifier.background(containerColor, CircleShape)
                }
            )
            .clip(CircleShape)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .bounceClick(intSrc)
            .clickable(interactionSource = intSrc, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

@Composable
internal fun FavoriteItem(
    app: AppInfo,
    showLabels: Boolean,
    fontSize: Float,
    mainTextColor: Color,
    returnIconPackage: String?,
    index: Int = 0,
    isHovered: Boolean = false,
    onBoundsChanged: (Rect) -> Unit = {},
    onAppLaunchForReturn: (String, Rect?) -> Unit,
    onShortcutRequested: (AppInfo, Rect?) -> Unit,
    isPreview: Boolean = false
) {
    val intSrc = remember { MutableInteractionSource() }
    // Hervorhebung beim Rüberfahren: nur Vergrößerung, kein Hintergrund.
    // Abschaltbar über die Animationseinstellungen (Favoriten-Leiste).
    val favoritesAnimationEnabled = LocalFavoritesAnimationEnabled.current
    val hoverScale by animateFloatAsState(
        targetValue = if (isHovered && favoritesAnimationEnabled) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "FavHoverScale"
    )
    val bounceScale by animateFloatAsState(
        targetValue = if (!LocalAppCloseAnimationEnabled.current) 1f else if (returnIconPackage == app.packageName) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "HomeReturnBounce"
    )
    val itemBounds = remember(app.packageName) { mutableStateOf<Rect?>(null) }
    val iconBounds = remember(app.packageName) { mutableStateOf<Rect?>(null) }
    // Icon-Größe ist (anders als die Icon-Position) unabhängig von der Swipe-Verschiebung
    // und dient als stabiler Anker für das Shortcut-Menü.
    var iconSize by remember(app.packageName) { mutableStateOf(IntSize.Zero) }

    var horizontalOffset by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = if (!LocalAnimationsEnabled.current) 0f else horizontalOffset,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "SwipeOffset"
    )

    // Material-3-Expressive: gestaffelter Eingang der Favoriten (federndes Aufpoppen),
    // einmalig pro Prozess-Lebenszeit beim ersten Erscheinen. Läuft NICHT erneut beim
    // Zurückkommen aus dem App-Drawer (HomeScreen wird dann neu komponiert).
    // Respektiert die Favoriten-Animationseinstellung.
    val playEntrance = favoritesAnimationEnabled && !isPreview &&
        !FavoritesEntranceTracker.hasAppeared(app.packageName)
    val appear = remember(app.packageName) { Animatable(if (playEntrance) 0f else 1f) }
    LaunchedEffect(app.packageName, favoritesAnimationEnabled) {
        if (playEntrance) {
            appear.snapTo(0f)
            delay((index.coerceAtMost(8) * 40).toLong())
            appear.animateTo(1f, RethroneSprings.spatial())
            FavoritesEntranceTracker.markAppeared(app.packageName)
        } else {
            appear.snapTo(1f)
        }
    }

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                val b = coordinates.boundsInRoot()
                itemBounds.value = b
                onBoundsChanged(b)
            }
            .graphicsLayer {
                translationX = animatedOffset
                // Eingang (0.9 → 1.0) mit Hover-Scale kombiniert.
                val entranceScale = 0.9f + 0.1f * appear.value
                scaleX = hoverScale * entranceScale
                scaleY = hoverScale * entranceScale
                alpha = appear.value
                // Linksbündig vergrößern, damit das Icon nicht zur Seite wandert.
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .then(
                if (!isPreview) {
                    Modifier
                        .pointerInput(app.packageName) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (horizontalOffset > 150f) {
                                        // Stabilen Anker nur fürs Icon bauen: Position aus dem
                                        // (unverschobenen) itemBounds, Breite/Höhe aus der Icon-Größe.
                                        // So verdeckt das Menü das Logo nicht – egal ob Labels an sind.
                                        val ib = itemBounds.value
                                        val anchor = if (ib != null && iconSize.width > 0) {
                                            Rect(
                                                left = ib.left,
                                                top = ib.center.y - iconSize.height / 2f,
                                                right = ib.left + iconSize.width,
                                                bottom = ib.center.y + iconSize.height / 2f
                                            )
                                        } else {
                                            ib
                                        }
                                        onShortcutRequested(app, anchor)
                                    }
                                    horizontalOffset = 0f
                                },
                                onDragCancel = { horizontalOffset = 0f },
                                onHorizontalDrag = { _, dragAmount ->
                                    horizontalOffset = (horizontalOffset + dragAmount).coerceIn(0f, 300f)
                                }
                            )
                        }
                        .bounceClick(intSrc)
                        .clickable(interactionSource = intSrc, indication = null) {
                            val targetBounds = iconBounds.value ?: itemBounds.value
                            onAppLaunchForReturn(app.packageName, targetBounds)
                        }
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            // Nur vertikaler Innenabstand, damit die Icons links bündig mit Uhr/Datum stehen.
            modifier = Modifier.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = bounceScale
                        scaleY = bounceScale
                    }
                    .onGloballyPositioned {
                        iconBounds.value = it.boundsInRoot()
                        iconSize = it.size
                    }
            ) {
                AppIconView(app, showBadge = true)
            }
            if (showLabels) {
                Text(
                    text = app.label,
                    color = mainTextColor,
                    fontSize = 18.sp * fontSize,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
