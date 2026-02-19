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

@Composable
fun SizeConfigMenu(
    currentFontSize: FontSize,
    onFontSizeSelected: (FontSize) -> Unit,
    currentIconSize: IconSize,
    onIconSizeSelected: (IconSize) -> Unit,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        SystemWallpaperView()
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A).copy(alpha = 0.95f)))

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
                        color = Color.White
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Schließen",
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Vorschau", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            // Preview Area
            Row(modifier = Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SizePreviewCard(
                    title = "Startseite", 
                    fontSize = currentFontSize, 
                    iconSize = currentIconSize,
                    isHome = true, 
                    modifier = Modifier.weight(1f)
                )
                SizePreviewCard(
                    title = "App Drawer", 
                    fontSize = currentFontSize, 
                    iconSize = currentIconSize,
                    isHome = false, 
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            
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
                    Surface(
                        onClick = { onFontSizeSelected(size) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.1f),
                        contentColor = if (isSelected) Color(0xFF0F172A) else Color.White,
                        border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = size.label,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

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
                    Surface(
                        onClick = { onIconSizeSelected(size) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.1f),
                        contentColor = if (isSelected) Color(0xFF0F172A) else Color.White,
                        border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = size.label,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                            )
                        }
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
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.6f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auf Standardwerte zurücksetzen", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun SizePreviewCard(title: String, fontSize: FontSize, iconSize: IconSize, isHome: Boolean, modifier: Modifier = Modifier) {
    val colorTheme = LocalColorTheme.current
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                            SolidColor(colorTheme.drawerBackground)
                        }
                    )
            ) {
                if (isHome) {
                    // Simpler Home Screen content with size scaling
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.width(40.dp * fontSize.scale).height(8.dp * fontSize.scale).background(Color.White.copy(alpha = 0.8f), CircleShape))
                        Box(modifier = Modifier.width(30.dp * fontSize.scale).height(4.dp * fontSize.scale).background(Color.White.copy(alpha = 0.4f), CircleShape))
                        Spacer(modifier = Modifier.height(12.dp))
                        repeat(3) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Scale preview dot based on icon size
                                val dotSize = 12.dp * (iconSize.size / 48.dp)
                                Box(modifier = Modifier.size(dotSize).border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.width(40.dp * fontSize.scale).height(4.dp * fontSize.scale).background(Color.White.copy(alpha = 0.2f), CircleShape))
                            }
                        }
                    }
                } else {
                    // Simpler App Drawer content with size scaling
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Box(modifier = Modifier.fillMaxWidth().height(16.dp * fontSize.scale).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
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
                                        Box(modifier = Modifier.size(dotSize).border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape))
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
