package com.example.androidlauncher.ui

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.androidlauncher.R
import kotlin.math.roundToInt

/**
 * App-Start mit Scale-Up-Animation aus den Quell-Bounds sowie die Overlays
 * für Launch- und Rückkehr-Animation (shrink-to-origin).
 */

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
            Toast.makeText(context, context.getString(R.string.shake_app_not_found), Toast.LENGTH_SHORT).show()
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
        progress.animateTo(1f, tween(durationMillis = durationMillis, easing = FastOutSlowInEasing))
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
    val radiusProgress = easedProgress
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
                    if (backgroundBrush != null) {
                        Modifier.background(backgroundBrush)
                    } else {
                        Modifier.background(background)
                    }
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
    val radiusProgress = progress.value
    val cornerRadiusDp = with(density) {
        val startRadiusPx = 28.dp.toPx()
        val endRadiusPx = 10.dp.toPx()
        (startRadiusPx + (endRadiusPx - startRadiusPx) * radiusProgress).toDp()
    }
    val animatedShape = RoundedCornerShape(cornerRadiusDp)
    val overlayAlpha = (progress.value / 0.18f).coerceIn(0f, 1f)

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
                    this.alpha = overlayAlpha
                }
                .then(
                    if (backgroundBrush != null) {
                        Modifier.background(backgroundBrush)
                    } else {
                        Modifier.background(background)
                    }
                )
        )
    }
}
