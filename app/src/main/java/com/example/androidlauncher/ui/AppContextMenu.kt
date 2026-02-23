package com.example.androidlauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalFontSize
import com.composables.icons.lucide.*

@Composable
fun AppContextMenu(
    app: AppInfo,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onMoveToFolder: (() -> Unit)? = null,
    onRemoveFromFolder: (() -> Unit)? = null
) {
    val colorTheme = LocalColorTheme.current
    val fontSize = LocalFontSize.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .width(280.dp)
                    .clickable(enabled = false) {},
                color = colorTheme.drawerBackground.copy(alpha = 0.95f),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, mainTextColor.copy(alpha = 0.1f)),
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(mainTextColor.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(40.dp)) {
                            AppIconView(app)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = app.label,
                            color = mainTextColor,
                            fontSize = 18.sp * fontSize.scale,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Actions
                    ContextMenuItem(
                        icon = Lucide.ExternalLink,
                        text = "Öffnen",
                        color = mainTextColor,
                        onClick = { onOpen(); onDismiss() }
                    )
                    
                    Divider(modifier = Modifier.padding(horizontal = 12.dp), color = mainTextColor.copy(alpha = 0.05f))

                    ContextMenuItem(
                        icon = Lucide.Info,
                        text = "App-Info",
                        color = mainTextColor,
                        onClick = { onAppInfo(); onDismiss() }
                    )

                    Divider(modifier = Modifier.padding(horizontal = 12.dp), color = mainTextColor.copy(alpha = 0.05f))

                    ContextMenuItem(
                        icon = if (isFavorite) Lucide.StarOff else Lucide.Star,
                        text = if (isFavorite) "Vom Home entfernen" else "Zu Favoriten hinzufügen",
                        color = if (isFavorite) Color(0xFFFFB74D) else mainTextColor,
                        onClick = { onToggleFavorite(); onDismiss() }
                    )

                    if (onMoveToFolder != null) {
                        Divider(modifier = Modifier.padding(horizontal = 12.dp), color = mainTextColor.copy(alpha = 0.05f))
                        ContextMenuItem(
                            icon = Lucide.FolderInput,
                            text = "In Ordner verschieben",
                            color = mainTextColor,
                            onClick = { onMoveToFolder(); onDismiss() }
                        )
                    }

                    if (onRemoveFromFolder != null) {
                        Divider(modifier = Modifier.padding(horizontal = 12.dp), color = mainTextColor.copy(alpha = 0.05f))
                        ContextMenuItem(
                            icon = Lucide.FolderOutput,
                            text = "Aus Ordner entfernen",
                            color = mainTextColor,
                            onClick = { onRemoveFromFolder(); onDismiss() }
                        )
                    }

                    Divider(modifier = Modifier.padding(horizontal = 12.dp), color = mainTextColor.copy(alpha = 0.05f))

                    ContextMenuItem(
                        icon = Lucide.Trash2,
                        text = "Deinstallieren",
                        color = Color(0xFFEF5350),
                        onClick = { onUninstall(); onDismiss() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    val fontSize = LocalFontSize.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = color,
            fontSize = 15.sp * fontSize.scale,
            fontWeight = FontWeight.Normal
        )
    }
}
