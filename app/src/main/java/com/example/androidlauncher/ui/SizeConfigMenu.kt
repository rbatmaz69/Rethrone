package com.example.androidlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.SystemWallpaperView
import com.example.androidlauncher.data.FontSize
import com.example.androidlauncher.data.IconSize
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalLiquidGlassEnabled

@Composable
fun SizeConfigMenu(
    currentFontSize: FontSize,
    onFontSizeSelected: (FontSize) -> Unit,
    currentIconSize: IconSize,
    onIconSizeSelected: (IconSize) -> Unit,
    onClose: () -> Unit
) {
    val colorTheme = LocalColorTheme.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val isLiquidGlassEnabled = LocalLiquidGlassEnabled.current
    // Nur für primäre Schriften und Symbole
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White

    val backgroundColor = if (isDarkTextEnabled) colorTheme.lightBackground else colorTheme.drawerBackground
    Box(modifier = Modifier.fillMaxSize()) {
        SystemWallpaperView()
        Box(modifier = Modifier.fillMaxSize().background(backgroundColor.copy(alpha = 0.95f)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Größe & Skalierung",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        color = mainTextColor
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Schließen",
                        tint = mainTextColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Bleibt grau
            Text("Vorschau", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            // Preview Area
            Row(modifier = Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SizePreviewCard(
                    title = "Startseite", 
                    fontSize = currentFontSize, 
                    iconSize = currentIconSize,
                    isHome = true, 
                    mainTextColor = mainTextColor,
                    isLiquidGlassEnabled = isLiquidGlassEnabled,
                    isDarkTextEnabled = isDarkTextEnabled,
                    modifier = Modifier.weight(1f)
                )
                SizePreviewCard(
                    title = "App Drawer", 
                    fontSize = currentFontSize, 
                    iconSize = currentIconSize,
                    isHome = false, 
                    mainTextColor = mainTextColor,
                    isLiquidGlassEnabled = isLiquidGlassEnabled,
                    isDarkTextEnabled = isDarkTextEnabled,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // Bleibt grau
            Text(
                text = "Schriftgröße",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FontSize.entries.forEach { size ->
                    val isSelected = size == currentFontSize

                    val buttonModifier = if (isLiquidGlassEnabled && !isSelected) {
                        // Liquid Glass Style for inactive buttons
                        val glassBrush = if (isDarkTextEnabled) {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Black.copy(alpha = 0.05f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.White.copy(alpha = 0.05f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        }

                        val borderBrush = if (isDarkTextEnabled) {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.8f),
                                    Color.Black.copy(alpha = 0.3f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.6f),
                                    Color.White.copy(alpha = 0.1f)
                                )
                            )
                        }

                        Modifier
                            .background(glassBrush, RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.2.dp, borderBrush), RoundedCornerShape(12.dp))
                    } else {
                        // Standard Style or Selected Button (which is solid)
                        val bgColor = if (isSelected) mainTextColor else Color.White.copy(alpha = 0.1f)
                        val border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        Modifier
                            .background(bgColor, RoundedCornerShape(12.dp))
                            .then(if (border != null) Modifier.border(border, RoundedCornerShape(12.dp)) else Modifier)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .then(buttonModifier)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onFontSizeSelected(size) },
                        contentAlignment = Alignment.Center
                    ) {
                        val contentColor = if (isSelected) (if (isDarkTextEnabled) Color.White else Color(0xFF0F172A)) else mainTextColor
                        Text(
                            text = size.label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = contentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Bleibt grau
            Text(
                text = "Icon-Größe",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconSize.entries.forEach { size ->
                    val isSelected = size == currentIconSize

                    val buttonModifier = if (isLiquidGlassEnabled && !isSelected) {
                        // Liquid Glass Style for inactive buttons
                        val glassBrush = if (isDarkTextEnabled) {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Black.copy(alpha = 0.05f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.White.copy(alpha = 0.05f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        }

                        val borderBrush = if (isDarkTextEnabled) {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.8f),
                                    Color.Black.copy(alpha = 0.3f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.6f),
                                    Color.White.copy(alpha = 0.1f)
                                )
                            )
                        }

                        Modifier
                            .background(glassBrush, RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.2.dp, borderBrush), RoundedCornerShape(12.dp))
                    } else {
                        // Standard Style or Selected Button (which is solid)
                        val bgColor = if (isSelected) mainTextColor else Color.White.copy(alpha = 0.1f)
                        val border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        Modifier
                            .background(bgColor, RoundedCornerShape(12.dp))
                            .then(if (border != null) Modifier.border(border, RoundedCornerShape(12.dp)) else Modifier)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .then(buttonModifier)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onIconSizeSelected(size) },
                        contentAlignment = Alignment.Center
                    ) {
                        val contentColor = if (isSelected) (if (isDarkTextEnabled) Color.White else Color(0xFF0F172A)) else mainTextColor
                        Text(
                            text = size.label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = contentColor
                        )
                    }
                }
            }
        }

        // Zurücksetzen Button am unteren Rand
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp, end = 24.dp, start = 24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            TextButton(
                onClick = {
                    onFontSizeSelected(FontSize.STANDARD)
                    onIconSizeSelected(IconSize.STANDARD)
                },
                // Bleibt grau
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.6f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auf Standardwerte zurücksetzen", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun SizePreviewCard(title: String, fontSize: FontSize, iconSize: IconSize, isHome: Boolean, mainTextColor: Color, isLiquidGlassEnabled: Boolean, isDarkTextEnabled: Boolean, modifier: Modifier = Modifier) {
    val colorTheme = LocalColorTheme.current

    val cardModifier = if (isLiquidGlassEnabled) {
        val glassBrush = if (isDarkTextEnabled) {
            Brush.linearGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.15f),
                    Color.Black.copy(alpha = 0.05f)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.05f)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        }

        val borderBrush = if (isDarkTextEnabled) {
            Brush.linearGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.8f),
                    Color.Black.copy(alpha = 0.3f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.6f),
                    Color.White.copy(alpha = 0.1f)
                )
            )
        }

        Modifier
            .background(glassBrush, RoundedCornerShape(16.dp))
            .border(BorderStroke(1.2.dp, borderBrush), RoundedCornerShape(16.dp))
    } else {
        Modifier.background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
    }

    Box(
        modifier = modifier.then(cardModifier)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Karten-Titel bleibt grau
            Text(title, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isHome) {
                            Brush.verticalGradient(listOf(colorTheme.primary.copy(alpha = 0.6f), colorTheme.secondary.copy(alpha = 0.6f)))
                        } else {
                            if (mainTextColor == Color(0xFF010101)) {
                                SolidColor(colorTheme.lightBackground)
                            } else {
                                SolidColor(colorTheme.drawerBackground)
                            }
                        }
                    )
            ) {
                if (isHome) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.width(40.dp * fontSize.scale).height(8.dp * fontSize.scale).background(mainTextColor.copy(alpha = 0.8f), CircleShape))
                        Box(modifier = Modifier.width(30.dp * fontSize.scale).height(4.dp * fontSize.scale).background(mainTextColor.copy(alpha = 0.4f), CircleShape))
                        Spacer(modifier = Modifier.height(12.dp))
                        repeat(3) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val dotSize = 12.dp * (iconSize.size / 48.dp)
                                Box(modifier = Modifier.size(dotSize).border(1.dp, mainTextColor.copy(alpha = 0.5f), CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.width(40.dp * fontSize.scale).height(4.dp * fontSize.scale).background(mainTextColor.copy(alpha = 0.2f), CircleShape))
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Box(modifier = Modifier.fillMaxWidth().height(16.dp * fontSize.scale).background(mainTextColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val columns = when (iconSize) {
                                IconSize.SMALL -> 5
                                IconSize.STANDARD -> 4
                                IconSize.LARGE -> 3
                            }
                            val dotSize = 10.dp * (iconSize.size / 48.dp)
                            
                            repeat(3) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    repeat(columns) {
                                        Box(modifier = Modifier.size(dotSize).border(1.dp, mainTextColor.copy(alpha = 0.7f), CircleShape))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
