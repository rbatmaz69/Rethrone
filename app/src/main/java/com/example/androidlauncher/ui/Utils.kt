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
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.composables.icons.lucide.Lucide
import com.example.androidlauncher.NotificationService
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.IconManager
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalIconSize

/**
 * Default system-side mapping for app icons to Lucide icons.
 */
val DEFAULT_ICON_MAPPINGS: Map<String, String> = mapOf(
    "com.android.chrome" to "Chrome",
    "com.android.vending" to "Play",
    "com.google.android.youtube" to "Youtube",
    "com.google.android.apps.youtube.music" to "Music",
    "com.google.android.calendar" to "Calendar",
    "com.android.calendar" to "Calendar",
    "com.android.camera" to "Camera",
    "com.google.android.GoogleCamera" to "Camera",
    "com.google.android.calculator" to "Calculator",
    "com.google.android.googlequicksearchbox" to "Mic",
    "com.google.android.apps.nbu.files" to "FolderOpen",
    "com.example.androidlauncher" to "Crown"
)

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

/**
 * Composable das ein App-Icon rendert.
 * Zeigt Lucide-Icons oder App-eigene Bitmap-Icons an.
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

    val customIconName = resolvedCustomIcons[app.packageName] ?: DEFAULT_ICON_MAPPINGS[app.packageName]
    val lucideIcon = if (customIconName != null) getLucideIconByName(customIconName) else app.lucideIcon

    Box(
        modifier = modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        when {
            lucideIcon != null -> {
                Icon(
                    imageVector = lucideIcon,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize * 0.65f),
                    tint = tintColor
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
            else -> Box(modifier = Modifier.size(iconSize).background(tintColor.copy(alpha = 0.05f), CircleShape))
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
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    targetScale: Float = 0.7f
) {
    if (bounds == null || rootSize.width == 0 || rootSize.height == 0) {
        LaunchedEffect(bounds, rootSize) { onFinished() }
        return
    }
    val density = LocalDensity.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(bounds, rootSize) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
        onFinished()
    }
    val centerX = rootSize.width / 2f
    val centerY = rootSize.height / 2f
    val launchTranslation = Offset(bounds.center.x - centerX, bounds.center.y - centerY)
    val targetWidthPx = (bounds.width * targetScale).coerceAtLeast(with(density) { 24.dp.toPx() })
    val targetHeightPx = (bounds.height * targetScale).coerceAtLeast(with(density) { 24.dp.toPx() })
    val translationX = launchTranslation.x * progress.value
    val translationY = launchTranslation.y * progress.value
    val currentWidthPx = rootSize.width - (rootSize.width - targetWidthPx) * progress.value
    val currentHeightPx = rootSize.height - (rootSize.height - targetHeightPx) * progress.value
    val currentCenterX = centerX + translationX
    val currentCenterY = centerY + translationY
    val topLeftX = (currentCenterX - currentWidthPx / 2f).toInt()
    val topLeftY = (currentCenterY - currentHeightPx / 2f).toInt()
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(2000f)
            .graphicsLayer {
                this.transformOrigin = TransformOrigin.Center
                this.alpha = 1f - progress.value
            }
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(topLeftX, topLeftY) }
                .size(
                    width = with(density) { currentWidthPx.toDp() },
                    height = with(density) { currentHeightPx.toDp() }
                )
                .background(background)
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
