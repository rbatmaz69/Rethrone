package com.example.androidlauncher.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.SystemWallpaperView
import com.example.androidlauncher.ui.theme.ColorTheme

@Composable
fun ColorConfigMenu(
    selectedTheme: ColorTheme,
    onThemeSelected: (ColorTheme) -> Unit,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().testTag("color_config_menu")) {
        SystemWallpaperView()
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A).copy(alpha = 0.95f)))

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Farben", fontSize = 24.sp, fontWeight = FontWeight.Light, color = Color.White)
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = null, tint = Color.White) }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Vorschau", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            // Preview Area
            Row(modifier = Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Home Screen Preview
                PreviewCard(title = "Startseite", colorTheme = selectedTheme, isHome = true, modifier = Modifier.weight(1f))
                // App Drawer Preview
                PreviewCard(title = "App Drawer", colorTheme = selectedTheme, isHome = false, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Themen", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                @Suppress("EnumValuesSoftDeprecate")
                items(ColorTheme.values()) { theme ->
                    ThemeOptionItem(
                        theme = theme,
                        isSelected = theme == selectedTheme,
                        onClick = { onThemeSelected(theme) }
                    )
                }
            }
        }
    }
}

@Composable
fun PreviewCard(title: String, colorTheme: ColorTheme, isHome: Boolean, modifier: Modifier = Modifier) {
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
                    // Simpler Home Screen content
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.width(40.dp).height(8.dp).background(Color.White.copy(alpha = 0.8f), CircleShape))
                        Box(modifier = Modifier.width(30.dp).height(4.dp).background(Color.White.copy(alpha = 0.4f), CircleShape))
                        Spacer(modifier = Modifier.height(12.dp))
                        repeat(3) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(12.dp).border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.width(40.dp).height(4.dp).background(Color.White.copy(alpha = 0.2f), CircleShape))
                            }
                        }
                    }
                } else {
                    // Simpler App Drawer content
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Box(modifier = Modifier.fillMaxWidth().height(16.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                            Spacer(modifier = Modifier.height(12.dp))
                            repeat(3) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    repeat(4) {
                                        Box(modifier = Modifier.size(10.dp).border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape))
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

@Composable
fun ThemeOptionItem(theme: ColorTheme, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)) else null,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Row(modifier = Modifier.width(60.dp), horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                Box(modifier = Modifier.size(24.dp).background(theme.primary, CircleShape).border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape))
                Box(modifier = Modifier.size(24.dp).background(theme.secondary, CircleShape).border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(theme.themeName, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
            
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
            }
        }
    }
}
