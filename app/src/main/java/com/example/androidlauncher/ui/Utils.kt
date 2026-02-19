package com.example.androidlauncher.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.ui.theme.LocalIconSize

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
    when {
        app.lucideIcon != null -> Icon(imageVector = app.lucideIcon, contentDescription = null, modifier = modifier.size(iconSize), tint = Color.White)
        app.customIconResId != null -> Icon(painter = painterResource(id = app.customIconResId), contentDescription = null, modifier = modifier.size(iconSize), tint = Color.White)
        app.iconBitmap != null -> Image(bitmap = app.iconBitmap, contentDescription = null, modifier = modifier.size(iconSize), colorFilter = ColorFilter.tint(Color.White))
        else -> Box(modifier = modifier.size(iconSize).background(Color.White.copy(alpha = 0.05f), CircleShape))
    }
}
