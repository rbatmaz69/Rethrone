package com.example.androidlauncher.ui

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.composables.icons.lucide.Lucide
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.IconManager
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalIconSize
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Default system-side mapping for app icons to Lucide icons.
 * packageName -> lucideIconName
 */
val DEFAULT_ICON_MAPPINGS = mapOf(
    "com.android.chrome" to "Globe",
    "com.google.android.apps.messaging" to "MessageSquare",
    "com.google.android.gm" to "Mail",
    "com.google.android.calendar" to "Calendar",
    "com.google.android.apps.photos" to "Image",
    "com.android.settings" to "Settings",
    "com.google.android.calculator" to "Calculator",
    "com.google.android.apps.maps" to "MapPin",
    "com.android.vending" to "ShoppingBag", // Play Store
    "com.google.android.youtube" to "Youtube",
    "com.whatsapp" to "Phone",
    "com.instagram.android" to "Instagram",
    "com.facebook.katana" to "Facebook"
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
 * Composable that renders an app icon.
 * Supports Vector icons (Lucide), Resource IDs, and Bitmaps.
 * Adjusts tint based on dark text mode.
 */
@Composable
fun AppIconView(app: AppInfo, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val iconManager = remember { IconManager(context) }
    val customIcons by iconManager.customIcons.collectAsState(initial = emptyMap())
    
    val iconSize = LocalIconSize.current.size
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val tintColor = if (isDarkTextEnabled) Color.Black else Color.White

    // Priority: 1. User choice, 2. System default mapping, 3. App's own lucideIcon (if any)
    val customIconName = customIcons[app.packageName] ?: DEFAULT_ICON_MAPPINGS[app.packageName]
    val lucideIcon = if (customIconName != null) getLucideIconByName(customIconName) else app.lucideIcon

    Box(
        modifier = modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        when {
            lucideIcon != null -> {
                // Skalierung der Lucide Icons auf ca. 55% der Originalgröße.
                // Wir halten den Container (Box) auf iconSize und zentrieren das Icon darin,
                // damit die Positionierung symmetrisch zu den anderen Icons bleibt.
                Icon(
                    imageVector = lucideIcon,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize * 0.55f),
                    tint = tintColor
                )
            }
            app.customIconResId != null -> Icon(painter = painterResource(id = app.customIconResId), contentDescription = null, modifier = Modifier.size(iconSize), tint = tintColor)
            app.iconBitmap != null -> Image(bitmap = app.iconBitmap, contentDescription = null, modifier = Modifier.size(iconSize), colorFilter = ColorFilter.tint(tintColor))
            else -> Box(modifier = Modifier.size(iconSize).background(tintColor.copy(alpha = 0.05f), CircleShape))
        }
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
    } catch (e: Exception) {}
    
    try {
        val methodName = if (name.startsWith("get")) name else "get$name"
        val method = lucideClass.getMethod(methodName)
        return method.invoke(null) as? ImageVector
    } catch (e: Exception) {}

    // 2. Try as a Kotlin extension property (compiled to com.composables.icons.lucide.<Name>Kt)
    try {
        // The naming convention for extension properties is usually IconNameKt
        val className = "com.composables.icons.lucide.${name}Kt"
        val clazz = Class.forName(className)
        // Extension property "val Lucide.IconName" becomes "public static final ImageVector getIconName(Lucide receiver)"
        val getterName = "get$name"
        val method = clazz.getMethod(getterName, Lucide::class.java)
        return method.invoke(null, Lucide) as? ImageVector
    } catch (e: Exception) {}

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
                activity.overridePendingTransition(0, 0)
            }
        }
    } catch (e: Exception) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e2: Exception) {
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
