package com.example.androidlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.example.androidlauncher.LauncherLogic
import com.example.androidlauncher.NotificationService
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.AutoIconFallbackType
import com.example.androidlauncher.data.IconManager
import com.example.androidlauncher.ui.theme.LocalIconColor
import com.example.androidlauncher.ui.theme.LocalIconSize
import com.example.androidlauncher.ui.theme.LocalNotificationDotsEnabled

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
    val tintColor = LocalIconColor.current

    val dotsEnabled = showBadge && LocalNotificationDotsEnabled.current
    val activeNotifications by if (dotsEnabled) {
        NotificationService.activeNotificationPackages.collectAsState()
    } else {
        remember { mutableStateOf(emptySet<String>()) }
    }
    val hasNotification = LauncherLogic.shouldShowNotificationDot(
        packageName = app.packageName,
        activeNotificationPackages = activeNotifications,
        dotsEnabled = dotsEnabled,
    )

    val manualLucideIcon = resolvedCustomIcons[app.packageName]?.let(::getLucideIconByName)
    val autoFallback = app.autoIconFallback

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
        modifier = Modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = tintColor,
            fontSize = (iconSize.value * 0.6f).sp,
            fontWeight = FontWeight.Medium
        )
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
