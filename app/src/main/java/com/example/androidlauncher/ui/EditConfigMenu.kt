package com.example.androidlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings2
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Image
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalLiquidGlassEnabled
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@Composable
fun EditConfigMenu(
    onOpenIconConfig: () -> Unit,
    onChangeWallpaper: () -> Unit,
    onResetWallpaper: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val isLiquidGlassEnabled = LocalLiquidGlassEnabled.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White

    var isNotificationEnabled by remember { mutableStateOf(isNotificationServiceEnabled(context)) }

    // Re-check permission when returning to the app
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isNotificationEnabled = isNotificationServiceEnabled(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Bearbeiten",
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                color = mainTextColor
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = mainTextColor)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Menu Point: App Icons anpassen
        EditMenuItem(
            icon = Lucide.Settings2,
            label = "App-Icons anpassen",
            onClick = onOpenIconConfig,
            mainTextColor = mainTextColor,
            isLiquidGlassEnabled = isLiquidGlassEnabled,
            isDarkTextEnabled = isDarkTextEnabled
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Menu Point: Wallpaper ändern
        EditMenuItem(
            icon = Lucide.Image,
            label = "Wallpaper ändern",
            onClick = onChangeWallpaper,
            mainTextColor = mainTextColor,
            isLiquidGlassEnabled = isLiquidGlassEnabled,
            isDarkTextEnabled = isDarkTextEnabled,
            trailingContent = {
                IconButton(onClick = { 
                    onResetWallpaper()
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = mainTextColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Menu Point: Notification Dots
        EditMenuItem(
            icon = Lucide.Bell,
            label = "Benachrichtigungs-Punkte",
            onClick = {
                if (!isNotificationEnabled) {
                    openNotificationSettings(context)
                } else {
                    openNotificationSettings(context)
                }
            },
            statusLabel = if (isNotificationEnabled) "An" else "Aus",
            mainTextColor = mainTextColor,
            isLiquidGlassEnabled = isLiquidGlassEnabled,
            isDarkTextEnabled = isDarkTextEnabled
        )
    }
}

@Composable
fun EditMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    mainTextColor: Color,
    isLiquidGlassEnabled: Boolean,
    isDarkTextEnabled: Boolean,
    statusLabel: String? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val backgroundModifier = if (isLiquidGlassEnabled) {
        val glassBrush = if (isDarkTextEnabled) {
            Brush.linearGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.10f),
                    Color.Black.copy(alpha = 0.03f)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.04f)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        }
        val borderBrush = if (isDarkTextEnabled) {
            Brush.linearGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.2f),
                    Color.Black.copy(alpha = 0.05f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0.05f)
                )
            )
        }
        Modifier
            .background(glassBrush, RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, borderBrush), RoundedCornerShape(16.dp))
    } else {
        Modifier.background(mainTextColor.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(backgroundModifier),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main clickable area
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick() }
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = mainTextColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = label,
                    color = mainTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (statusLabel != null) {
                    Text(
                        text = statusLabel,
                        color = mainTextColor.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            // Trailing content area (separate from main click)
            if (trailingContent != null) {
                Box(modifier = Modifier.padding(end = 8.dp)) {
                    trailingContent()
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = mainTextColor.copy(alpha = 0.4f),
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        }
    }
}
