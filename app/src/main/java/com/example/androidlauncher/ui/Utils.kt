package com.example.androidlauncher.ui

import android.app.Activity
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.composables.icons.lucide.Lucide
import com.example.androidlauncher.NotificationService
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.AutoIconFallbackType
import com.example.androidlauncher.data.IconManager
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalIconSize
import kotlin.math.roundToInt

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

fun Modifier.bounceClick(interactionSource: MutableInteractionSource, enabled: Boolean = true) = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bounceScale"
    )
    this.scale(scale)
}

@Composable
fun rememberBottomBoundarySwipeToCloseConnection(
    listState: LazyListState,
    enabled: Boolean = true,
    onClose: () -> Unit
): NestedScrollConnection {
    val density = LocalDensity.current
    val swipeCloseThresholdPx = with(density) { 64.dp.toPx() }
    var swipeDragDistance by remember(listState, enabled) { mutableStateOf(0f) }

    return remember(listState, enabled, swipeCloseThresholdPx, onClose) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!enabled) {
                    swipeDragDistance = 0f
                    return Offset.Zero
                }

                val atBottom = !listState.canScrollForward
                if (source == NestedScrollSource.UserInput && atBottom && available.y < 0f) {
                    swipeDragDistance += -available.y
                    if (swipeDragDistance >= swipeCloseThresholdPx) {
                        swipeDragDistance = 0f
                        onClose()
                    }
                    return Offset(0f, available.y)
                }

                if (!atBottom || available.y > 0f) {
                    swipeDragDistance = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!enabled) {
                    swipeDragDistance = 0f
                    return Velocity.Zero
                }

                val atBottom = !listState.canScrollForward
                if (atBottom && available.y < -1500f) {
                    swipeDragDistance = 0f
                    onClose()
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                swipeDragDistance = 0f
                return Velocity.Zero
            }
        }
    }
}

@Composable
fun StableSearchFieldContent(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    textColor: Color,
    placeholderColor: Color,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    leadingIconTint: Color = placeholderColor,
    leadingIconSize: Dp = 18.dp,
    spacing: Dp = 12.dp,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = leadingIconTint,
            modifier = Modifier.size(leadingIconSize)
        )
        Spacer(modifier = Modifier.width(spacing))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = textStyle.copy(color = placeholderColor),
                    maxLines = 1
                )
            }
            val textFieldModifier = if (focusRequester != null) {
                Modifier.fillMaxWidth().focusRequester(focusRequester)
            } else {
                Modifier.fillMaxWidth()
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = textFieldModifier,
                textStyle = textStyle.copy(color = textColor),
                cursorBrush = SolidColor(textColor),
                singleLine = true,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions
            )
        }
    }
}

/**
 * Composable das ein App-Icon rendert.
 * Priorität: manueller Override > automatischer Lucide-Fallback > automatischer Container > Original.
 */
@Composable
fun AppIconView(
    app: AppInfo,
    modifier: Modifier = Modifier,
    showBadge: Boolean = false,
    customIcons: Map<String, String>? = null
) {
    val resolvedCustomIcons = customIcons ?: run {
        val context = LocalContext.current
        val iconManager = remember { IconManager(context) }
        val icons by iconManager.customIcons.collectAsState(initial = emptyMap())
        icons
    }

    val iconSize = LocalIconSize.current.size
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val tintColor = if (isDarkTextEnabled) Color.Black else Color.White

    val activeNotifications by if (showBadge) {
        NotificationService.activeNotificationPackages.collectAsState()
    } else {
        remember { mutableStateOf(emptySet<String>()) }
    }
    val hasNotification = showBadge && app.packageName in activeNotifications

    val manualLucideIcon = resolvedCustomIcons[app.packageName]?.let(::getLucideIconByName)
    val autoFallback = app.autoIconFallback
    val autoLucideIcon = autoFallback?.lucideIconName?.let(::getLucideIconByName)

    Box(
        modifier = modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        when {
            manualLucideIcon != null -> {
                Icon(
                    imageVector = manualLucideIcon,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize * 0.65f),
                    tint = tintColor
                )
            }
            autoFallback?.type == AutoIconFallbackType.LUCIDE && autoLucideIcon != null -> {
                Icon(
                    imageVector = autoLucideIcon,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize * 0.65f),
                    tint = tintColor
                )
            }
            autoFallback?.type == AutoIconFallbackType.NEUTRAL -> {
                NeutralIconFallback(
                    label = app.label,
                    tintColor = tintColor,
                    iconSize = iconSize
                )
            }
            app.iconBitmap != null -> {
                Image(
                    bitmap = app.iconBitmap,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    colorFilter = ColorFilter.tint(tintColor)
                )
            }
            else -> {
                NeutralIconFallback(
                    label = app.label,
                    tintColor = tintColor,
                    iconSize = iconSize
                )
            }
        }

        if (hasNotification) {
            val dotSize = iconSize * 0.2f
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = iconSize * 0.05f, end = iconSize * 0.05f)
                    .size(dotSize)
                    .background(tintColor, CircleShape)
                    .border(1.dp, Color.Black.copy(alpha = 0.1f), CircleShape)
            )
        }
    }
}

@Composable
private fun NeutralIconFallback(
    label: String,
    tintColor: Color,
    iconSize: androidx.compose.ui.unit.Dp
) {
    val initial = remember(label) {
        label.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "•"
    }

    Box(
        modifier = Modifier
            .size(iconSize * 0.82f)
            .background(tintColor.copy(alpha = 0.04f), CircleShape)
            .border(1.dp, tintColor.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = tintColor.copy(alpha = 0.92f)
        )
    }
}

/**
 * Checks if the notification listener service is enabled for this app.
 */
fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(pkgName)
}

fun openNotificationSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
        val componentName = ComponentName(context, NotificationService::class.java)
        intent.putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, componentName.flattenToString())
        context.startActivity(intent)
    } else {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        context.startActivity(intent)
    }
}

fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    context.startActivity(intent)
}

fun getLucideIconByName(name: String): ImageVector? {
    val lucideClass = Lucide::class.java
    try {
        val field = lucideClass.getField(name)
        return field.get(null) as? ImageVector
    } catch (_: Exception) {}
    try {
        val methodName = if (name.startsWith("get")) name else "get$name"
        val method = lucideClass.getMethod(methodName)
        return method.invoke(null) as? ImageVector
    } catch (_: Exception) {}
    try {
        val className = "com.composables.icons.lucide.${name}Kt"
        val clazz = Class.forName(className)
        val getterName = "get$name"
        val method = clazz.getMethod(getterName, Lucide::class.java)
        return method.invoke(null, Lucide) as? ImageVector
    } catch (_: Exception) {}
    return null
}

fun launchAppWithSourceBoundsAnimation(
    context: Context,
    intent: Intent,
    sourceView: View?,
    sourceBounds: Rect?
) {
    val safeBounds = sourceBounds ?: run {
        launchAppNoTransition(context, Intent(intent))
        return
    }
    val launchWidth = safeBounds.width.roundToInt().coerceAtLeast(1)
    val launchHeight = safeBounds.height.roundToInt().coerceAtLeast(1)
    if (launchWidth <= 1 || launchHeight <= 1 || sourceView == null) {
        launchAppNoTransition(context, Intent(intent))
        return
    }

    val launchIntent = Intent(intent).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        val options = ActivityOptions.makeScaleUpAnimation(
            sourceView,
            safeBounds.left.roundToInt().coerceAtLeast(0),
            safeBounds.top.roundToInt().coerceAtLeast(0),
            launchWidth,
            launchHeight
        )
        context.startActivity(launchIntent, options.toBundle())
    } catch (_: Exception) {
        launchAppNoTransition(context, Intent(intent))
    }
}

fun launchAppNoTransition(context: Context, intent: Intent) {
    val activity = context.findActivity()
    try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val options = ActivityOptions.makeCustomAnimation(context, 0, 0)
        context.startActivity(intent, options.toBundle())
        if (activity != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
            } else {
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(0, 0)
            }
        }
    } catch (_: Exception) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "App konnte nicht gestartet werden", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun ReturnAnimationOverlay(
    bounds: Rect?, 
    rootSize: IntSize, 
    background: Color,
    backgroundBrush: Brush? = null,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    durationMillis: Int = 260,
    targetScale: Float = 0.7f
) {
    if (bounds == null || rootSize.width == 0 || rootSize.height == 0) {
        LaunchedEffect(bounds, rootSize) { onFinished() }
        return
    }
    val density = LocalDensity.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(bounds, rootSize, targetScale, durationMillis) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = durationMillis, easing = LinearOutSlowInEasing))
        onFinished()
    }

    val centerX = rootSize.width / 2f
    val centerY = rootSize.height / 2f
    val launchTranslation = Offset(bounds.center.x - centerX, bounds.center.y - centerY)
    val targetWidthPx = (bounds.width * targetScale).coerceAtLeast(with(density) { 30.dp.toPx() })
    val targetHeightPx = (bounds.height * targetScale).coerceAtLeast(with(density) { 30.dp.toPx() })
    val easedProgress = progress.value
    val translationX = launchTranslation.x * easedProgress
    val translationY = launchTranslation.y * easedProgress
    val currentWidthPx = rootSize.width - (rootSize.width - targetWidthPx) * easedProgress
    val currentHeightPx = rootSize.height - (rootSize.height - targetHeightPx) * easedProgress
    val currentCenterX = centerX + translationX
    val currentCenterY = centerY + translationY
    val topLeftX = (currentCenterX - currentWidthPx / 2f).toInt()
    val topLeftY = (currentCenterY - currentHeightPx / 2f).toInt()
    val radiusProgress = ((easedProgress - 0.18f) / 0.82f).coerceIn(0f, 1f)
    val cornerRadiusDp = with(density) {
        val startRadiusPx = 6.dp.toPx()
        val endRadiusPx = 26.dp.toPx()
        (startRadiusPx + (endRadiusPx - startRadiusPx) * radiusProgress).toDp()
    }
    val animatedShape = RoundedCornerShape(cornerRadiusDp)
    val overlayAlpha = if (easedProgress < 0.82f) 1f else (1f - ((easedProgress - 0.82f) / 0.18f)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(2000f)
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(topLeftX, topLeftY) }
                .size(
                    width = with(density) { currentWidthPx.toDp() },
                    height = with(density) { currentHeightPx.toDp() }
                )
                .graphicsLayer {
                    this.shape = animatedShape
                    this.clip = true
                    this.alpha = overlayAlpha
                }
                .then(
                    if (backgroundBrush != null) Modifier.background(backgroundBrush)
                    else Modifier.background(background)
                )
        )
    }
}

@Composable
fun LaunchAnimationOverlay(
    bounds: Rect?,
    rootSize: IntSize,
    background: Color,
    backgroundBrush: Brush? = null,
    modifier: Modifier = Modifier,
    durationMillis: Int = 320,
    scrimColor: Color = Color.Black.copy(alpha = 0.16f)
) {
    if (bounds == null || rootSize.width == 0 || rootSize.height == 0) return

    val density = LocalDensity.current
    val progress = remember(bounds, rootSize) { Animatable(0f) }
    LaunchedEffect(bounds, rootSize, durationMillis) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = durationMillis, easing = FastOutSlowInEasing))
    }

    val startWidthPx = bounds.width.coerceAtLeast(with(density) { 28.dp.toPx() })
    val startHeightPx = bounds.height.coerceAtLeast(with(density) { 28.dp.toPx() })
    val currentWidthPx = startWidthPx + (rootSize.width - startWidthPx) * progress.value
    val currentHeightPx = startHeightPx + (rootSize.height - startHeightPx) * progress.value
    val currentLeftPx = bounds.left * (1f - progress.value)
    val currentTopPx = bounds.top * (1f - progress.value)
    val radiusProgress = ((progress.value - 0.82f) / 0.18f).coerceIn(0f, 1f)
    val cornerRadiusDp = with(density) {
        val startRadiusPx = 28.dp.toPx()
        val endRadiusPx = 10.dp.toPx()
        (startRadiusPx + (endRadiusPx - startRadiusPx) * radiusProgress).toDp()
    }
    val animatedShape = RoundedCornerShape(cornerRadiusDp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(1900f)
    ) {
        if (scrimColor.alpha > 0f) {
            Box(modifier = Modifier.fillMaxSize().background(scrimColor))
        }
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = currentLeftPx.roundToInt(),
                        y = currentTopPx.roundToInt()
                    )
                }
                .size(
                    width = with(density) { currentWidthPx.toDp() },
                    height = with(density) { currentHeightPx.toDp() }
                )
                .graphicsLayer {
                    this.shape = animatedShape
                    this.clip = true
                }
                .then(
                    if (backgroundBrush != null) Modifier.background(backgroundBrush)
                    else Modifier.background(background)
                )
        )
    }
}

fun getAppShortcuts(context: Context, packageName: String): List<ShortcutInfo> {
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val query = LauncherApps.ShortcutQuery().apply {
        setPackage(packageName)
        setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
    }
    return try {
        launcherApps.getShortcuts(query, Process.myUserHandle()) ?: emptyList()
    } catch (_: SecurityException) {
        emptyList()
    }
}

fun launchShortcut(context: Context, packageName: String, shortcutId: String) {
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    try {
        launcherApps.startShortcut(packageName, shortcutId, null, null, Process.myUserHandle())
    } catch (_: Exception) {
        Toast.makeText(context, "Shortcut konnte nicht geöffnet werden", Toast.LENGTH_SHORT).show()
    }
}
