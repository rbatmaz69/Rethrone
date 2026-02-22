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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalIconSize
import kotlin.math.max
import kotlin.math.roundToInt

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

// Verbesserter bounceClick Modifier
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

@Composable
fun AppIconView(app: AppInfo, modifier: Modifier = Modifier) {
    val iconSize = LocalIconSize.current.size
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val tintColor = if (isDarkTextEnabled) Color.Black else Color.White

    when {
        app.lucideIcon != null -> Icon(imageVector = app.lucideIcon, contentDescription = null, modifier = modifier.size(iconSize), tint = tintColor)
        app.customIconResId != null -> Icon(painter = painterResource(id = app.customIconResId), contentDescription = null, modifier = modifier.size(iconSize), tint = tintColor)
        app.iconBitmap != null -> Image(bitmap = app.iconBitmap, contentDescription = null, modifier = modifier.size(iconSize), colorFilter = ColorFilter.tint(tintColor))
        else -> Box(modifier = modifier.size(iconSize).background(tintColor.copy(alpha = 0.05f), CircleShape))
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
