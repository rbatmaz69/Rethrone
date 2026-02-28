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
 * Maps package names to Lucide icon names.
 */
val DEFAULT_ICON_MAPPINGS: Map<String, String> = mapOf(
    "com.android.chrome" to "Chrome",
    "com.android.vending" to "Play",
    "com.google.android.youtube" to "Youtube",
    "com.google.android.apps.youtube.music" to "Music",
    "com.google.android.calendar" to "Calendar",
    "com.android.calendar" to "Calendar",
    
    // Camera
    "com.android.camera" to "Camera",
    "com.google.android.GoogleCamera" to "Camera",
    "com.sec.android.app.camera" to "Camera",
    "com.huawei.camera" to "Camera",
    "com.oppo.camera" to "Camera",
    "com.oneplus.camera" to "Camera",
    "com.sonyericsson.android.camera" to "Camera",
    "com.motorola.cameraone" to "Camera",
    
    // Calculator
    "com.google.android.calculator" to "Calculator",
    "com.android.calculator2" to "Calculator",
    "com.sec.android.app.popupcalculator" to "Calculator",
    "com.miui.calculator" to "Calculator",
    "com.huawei.calculator" to "Calculator",
    
    // Voice Search
    "com.google.android.googlequicksearchbox" to "Mic",
    "com.google.android.voicesearch" to "Mic",
    
    // Files / File Manager
    "com.google.android.apps.nbu.files" to "FolderOpen",
    "com.google.android.files" to "FolderOpen",
    "com.android.documentsui" to "FolderOpen",
    "com.sec.android.app.myfiles" to "FolderOpen",
    "com.mi.android.globalFileexplorer" to "FolderOpen",
    "com.android.fileexplorer" to "FolderOpen",
    "com.android.filemanager" to "FolderOpen",
    "com.huawei.hidisk" to "FolderOpen",
    
    // Rethrone / Launcher
    "com.example.androidlauncher" to "Crown"
)

/**
 * Extension function to find the Activity context from a Context.
 * Traverses ContextWrapper to find the base Activity.
 */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Modifier that adds a bouncy scale animation when clicked.
 * Used for interactive elements like app icons.
 */
fun Modifier.bounceClick(interactionSource: MutableInteractionSource, enabled: Boolean = true) = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounceScale"
    )
    this.scale(scale)
}

/**
 * Composable das ein App-Icon rendert.
 *
 * Unterstützt drei Icon-Typen mit folgender Priorität:
 * 1. Benutzerdefiniertes Lucide-Icon (aus [customIcons] oder [DEFAULT_ICON_MAPPINGS])
 * 2. App-eigenes Bitmap-Icon
 * 3. Platzhalter-Kreis
 *
 * **Energieeffizienz:** Benachrichtigungs-Badges werden nur beobachtet wenn
 * [showBadge] true ist. Die [customIcons]-Map wird idealerweise vom Parent
 * einmal gesammelt und durchgereicht, statt pro Icon einen eigenen
 * DataStore-Listener zu erzeugen.
 *
 * @param app Die darzustellende App.
 * @param modifier Optionaler Modifier.
 * @param showBadge Ob ein Benachrichtigungs-Punkt angezeigt werden soll.
 * @param customIcons Optionale Map benutzerdefinierter Icons (packageName → iconName).
 *   Falls null, wird ein Fallback-IconManager pro Composable erzeugt (vermeiden!).
 */
@Composable
fun AppIconView(
    app: AppInfo, 
    modifier: Modifier = Modifier,
    showBadge: Boolean = false,
    customIcons: Map<String, String>? = null
) {
    // Fallback: Nur wenn kein Parent die Icons durchreicht, eigenen Manager erzeugen.
    // Dies sollte langfristig durch durchgereichte Icons ersetzt werden.
    val resolvedCustomIcons = customIcons ?: run {
        val context = LocalContext.current
        val iconManager = remember { IconManager(context) }
        val icons by iconManager.customIcons.collectAsState(initial = emptyMap())
        icons
    }

    val iconSize = LocalIconSize.current.size
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val tintColor = if (isDarkTextEnabled) Color.Black else Color.White

    // Benachrichtigungen nur beobachten wenn Badge-Anzeige gewünscht ist
    val activeNotifications by if (showBadge) {
        NotificationService.activeNotificationPackages.collectAsState()
    } else {
        remember { mutableStateOf(emptySet<String>()) }
    }
    val hasNotification = showBadge && app.packageName in activeNotifications

    // Priorität: 1. User-Wahl, 2. System-Default-Mapping, 3. App-eigenes Icon
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
                    modifier = Modifier.size(iconSize * 0.55f),
                    tint = tintColor
                )
            }
            app.iconBitmap != null -> Image(bitmap = app.iconBitmap, contentDescription = null, modifier = Modifier.size(iconSize), colorFilter = ColorFilter.tint(tintColor))
            else -> Box(modifier = Modifier.size(iconSize).background(tintColor.copy(alpha = 0.05f), CircleShape))
        }

        // Notification Badge (Small Dot top right)
        if (hasNotification) {
            val dotSize = iconSize * 0.2f
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = iconSize * 0.05f, end = iconSize * 0.05f)
                    .size(dotSize)
                    .background(tintColor, CircleShape)
                    .border(1.dp, Color.Black.copy(alpha = 0.1f), CircleShape) // Subtle border for visibility
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

/**
 * Opens the system settings to enable notification access.
 * On Android 11+ (API 30), it attempts to open the specific detail settings for this app
 * so the user doesn't have to find the launcher in a list.
 */
fun openNotificationSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Direct link to this app's notification listener settings (available from Android 11)
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
        val componentName = ComponentName(context, NotificationService::class.java)
        intent.putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, componentName.flattenToString())
        context.startActivity(intent)
    } else {
        // Fallback to the general list of notification listeners for older Android versions
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        context.startActivity(intent)
    }
}

/**
 * Retrieves a Lucide icon by name.
 * Handles both direct members and extension properties (which are compiled to <Name>Kt classes).
 */
fun getLucideIconByName(name: String): ImageVector? {
    val lucideClass = Lucide::class.java
    
    // 1. Try as a direct static member (Field or Method)
    try {
        val field = lucideClass.getField(name)
        return field.get(null) as? ImageVector
    } catch (_: Exception) {}
    
    try {
        val methodName = if (name.startsWith("get")) name else "get$name"
        val method = lucideClass.getMethod(methodName)
        return method.invoke(null) as? ImageVector
    } catch (_: Exception) {}

    // 2. Try as a Kotlin extension property (compiled to com.composables.icons.lucide.<Name>Kt)
    try {
        // The naming convention for extension properties is usually IconNameKt
        val className = "com.composables.icons.lucide.${name}Kt"
        val clazz = Class.forName(className)
        // Extension property "val Lucide.IconName" becomes "public static final ImageVector getIconName(Lucide receiver)"
        val getterName = "get$name"
        val method = clazz.getMethod(getterName, Lucide::class.java)
        return method.invoke(null, Lucide) as? ImageVector
    } catch (_: Exception) {}

    return null
}

/**
 * Launches an app without the default system transition animation.
 * Used to implement custom return animations.
 */
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

/**
 * A sophisticated overlay animation that simulates the app window shrinking back into its icon.
 * Triggered when returning from an app to the launcher.
 */
@Composable
fun ReturnAnimationOverlay(
    bounds: Rect?, // The target bounds (icon position) where the animation ends
    rootSize: IntSize, // Size of the root container
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

    // Shrink the overlay rectangle toward the icon size instead of scaling a full-screen box.
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

/**
 * Ruft dynamische und statische Shortcuts für ein Paket ab.
 * Benötigt Android 7.1 (API 25) oder höher (minSdk 26 erfüllt dies immer).
 */
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

/**
 * Startet einen bestimmten App-Shortcut.
 */
fun launchShortcut(context: Context, packageName: String, shortcutId: String) {
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    try {
        launcherApps.startShortcut(packageName, shortcutId, null, null, Process.myUserHandle())
    } catch (_: Exception) {
        Toast.makeText(context, "Shortcut konnte nicht geöffnet werden", Toast.LENGTH_SHORT).show()
    }
}
