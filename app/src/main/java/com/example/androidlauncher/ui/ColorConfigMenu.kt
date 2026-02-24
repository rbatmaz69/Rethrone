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
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.Sun
import com.composables.icons.lucide.Droplets
import com.composables.icons.lucide.Square
import com.example.androidlauncher.SystemWallpaperView
import com.example.androidlauncher.ui.theme.ColorTheme

@Composable
fun ColorConfigMenu(
    selectedTheme: ColorTheme,
    onThemeSelected: (ColorTheme) -> Unit,
    isDarkTextEnabled: Boolean,
    onDarkTextToggled: (Boolean) -> Unit,
    isLiquidGlassEnabled: Boolean,
    onLiquidGlassToggled: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    // Nur für die primären Schriften und Symbole verwenden
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White

    val backgroundColor = if (isDarkTextEnabled) selectedTheme.lightBackground else selectedTheme.drawerBackground
    Box(modifier = Modifier.fillMaxSize().testTag("color_config_menu")) {
        SystemWallpaperView()
        Box(modifier = Modifier.fillMaxSize().background(backgroundColor.copy(alpha = 0.95f)))

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Farben", fontSize = 24.sp, fontWeight = FontWeight.Light, color = mainTextColor)
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = null, tint = mainTextColor) }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bleibt grau (weiß mit alpha), da es eine sekundäre Info ist
                Text("Vorschau", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                Switch(
                    checked = isDarkTextEnabled,
                    onCheckedChange = onDarkTextToggled,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Color.White.copy(alpha = 0.2f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.2f),
                        checkedThumbColor = Color.White,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.9f),
                        checkedBorderColor = Color.White.copy(alpha = 0.1f),
                        uncheckedBorderColor = Color.White.copy(alpha = 0.1f)
                    ),
                    thumbContent = {
                        if (isDarkTextEnabled) {
                            Icon(
                                imageVector = Lucide.Moon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.Black
                            )
                        } else {
                            Icon(
                                imageVector = Lucide.Sun,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFFFB300)
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Preview Area
            Row(modifier = Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PreviewCard(
                    title = "Startseite", 
                    colorTheme = selectedTheme, 
                    isHome = true, 
                    mainTextColor = mainTextColor,
                    modifier = Modifier.weight(1f)
                )
                PreviewCard(
                    title = "App Drawer", 
                    colorTheme = selectedTheme, 
                    isHome = false, 
                    mainTextColor = mainTextColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bleibt grau
                Text("Themen", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)

                Switch(
                    checked = isLiquidGlassEnabled,
                    onCheckedChange = onLiquidGlassToggled,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Color.White.copy(alpha = 0.2f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.2f),
                        checkedThumbColor = Color.White,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.9f),
                        checkedBorderColor = Color.White.copy(alpha = 0.1f),
                        uncheckedBorderColor = Color.White.copy(alpha = 0.1f)
                    ),
                    thumbContent = {
                        if (isLiquidGlassEnabled) {
                            Icon(
                                imageVector = Lucide.Droplets,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF0EA5E9) // Sky Blue for liquid
                            )
                        } else {
                            Icon(
                                imageVector = Lucide.Square,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.Gray
                            )
                        }
                    }
                )
            }

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
                        mainTextColor = mainTextColor,
                        onClick = { onThemeSelected(theme) }
                    )
                }
            }
        }
    }
}

@Composable
fun PreviewCard(
    title: String, 
    colorTheme: ColorTheme, 
    isHome: Boolean, 
    mainTextColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        // Hintergrund der Karte bleibt grau
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
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
                            // Wenn schwarze Schrift aktiv ist, zeigen wir den hellen Hintergrund
                            if (mainTextColor == Color(0xFF010101)) {
                                SolidColor(colorTheme.lightBackground)
                            } else {
                                SolidColor(colorTheme.drawerBackground)
                            }
                        }
                    )
            ) {
                // In der Vorschau spiegeln wir die Schriftfarbe wider
                if (isHome) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.width(40.dp).height(8.dp).background(mainTextColor.copy(alpha = 0.8f), CircleShape))
                        Box(modifier = Modifier.width(30.dp).height(4.dp).background(mainTextColor.copy(alpha = 0.4f), CircleShape))
                        Spacer(modifier = Modifier.height(12.dp))
                        repeat(3) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(12.dp).border(1.dp, mainTextColor.copy(alpha = 0.5f), CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.width(40.dp).height(4.dp).background(mainTextColor.copy(alpha = 0.2f), CircleShape))
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Box(modifier = Modifier.fillMaxWidth().height(16.dp).background(mainTextColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                            Spacer(modifier = Modifier.height(12.dp))
                            repeat(3) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    repeat(4) {
                                        Box(modifier = Modifier.size(10.dp).border(1.dp, mainTextColor.copy(alpha = 0.7f), CircleShape))
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
fun ThemeOptionItem(theme: ColorTheme, isSelected: Boolean, mainTextColor: Color, onClick: () -> Unit) {
    Surface(
        // Hintergrund der Items bleibt grau (weiß-transparent)
        color = if (isSelected) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        // Rahmen bleibt grau
        border = if (isSelected) BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)) else null,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.width(60.dp), horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                Box(modifier = Modifier.size(24.dp).background(theme.primary, CircleShape).border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape))
                Box(modifier = Modifier.size(24.dp).background(theme.secondary, CircleShape).border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Name des Themes wird schwarz (Schrift)
            Text(theme.themeName, color = mainTextColor, fontSize = 16.sp, modifier = Modifier.weight(1f))
            
            // Haken wird schwarz (Symbol)
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = mainTextColor)
            }
        }
    }
}
