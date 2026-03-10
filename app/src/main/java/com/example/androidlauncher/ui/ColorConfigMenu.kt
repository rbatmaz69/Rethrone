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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.Sun
import com.composables.icons.lucide.Droplets
import com.composables.icons.lucide.Square
// SystemWallpaperView ist im selben Paket (ui)
import com.example.androidlauncher.ui.theme.ColorTheme
import com.example.androidlauncher.ui.theme.LocalFontWeight


/**
 * Menu for configuring the visual theme of the launcher.
 * Allows selecting:
 * - Color Theme (Yellow, Blue, Red, Green, Purple)
 * - Dark/Light Text Mode
 * - Liquid Glass Effect toggle
 */
@Composable
fun ColorConfigMenu(
    selectedTheme: ColorTheme,
    onThemeSelected: (ColorTheme) -> Unit,
    isDarkTextEnabled: Boolean,
    onDarkTextToggled: (Boolean) -> Unit,
    isLiquidGlassEnabled: Boolean,
    onLiquidGlassToggled: (Boolean) -> Unit,
    customWallpaperUri: String? = null,
    onClose: () -> Unit
) {
    val fontWeight = LocalFontWeight.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val backgroundBrush = remember(selectedTheme, isDarkTextEnabled) {
        selectedTheme.backgroundBrush(isDarkTextEnabled, alpha = 0.95f)
    }
    val orderedThemes = remember {
        ColorTheme.entries.sortedWith(compareByDescending<ColorTheme> { it.isArtTheme }.thenBy { it.themeName })
    }

    Box(modifier = Modifier.fillMaxSize().testTag("color_config_menu")) {
        SystemWallpaperView(customWallpaperUri)
        Box(modifier = Modifier.fillMaxSize().background(backgroundBrush))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Farben", fontSize = 24.sp, fontWeight = fontWeight.weight, color = mainTextColor)
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = mainTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bleibt grau (weiß mit alpha), da es eine sekundäre Info ist
                Text("Vorschau", color = mainTextColor.copy(alpha = 0.5f), fontSize = 14.sp)

                val darkTextSwitchColors = LiquidGlass.switchColors(isDarkTextEnabled, isLiquidGlassEnabled)

                Switch(
                    checked = isDarkTextEnabled,
                    onCheckedChange = onDarkTextToggled,
                    colors = darkTextSwitchColors,
                    thumbContent = {
                        if (isDarkTextEnabled) {
                            Icon(
                                imageVector = Lucide.Moon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
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
                    isLiquidGlassEnabled = isLiquidGlassEnabled,
                    isDarkTextEnabled = isDarkTextEnabled,
                    modifier = Modifier.weight(1f)
                )
                PreviewCard(
                    title = "App Drawer", 
                    colorTheme = selectedTheme, 
                    isHome = false, 
                    mainTextColor = mainTextColor,
                    isLiquidGlassEnabled = isLiquidGlassEnabled,
                    isDarkTextEnabled = isDarkTextEnabled,
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
                Text("Themen", color = mainTextColor.copy(alpha = 0.5f), fontSize = 14.sp)

                val liquidGlassSwitchColors = LiquidGlass.switchColors(isDarkTextEnabled, isLiquidGlassEnabled)

                Switch(
                    checked = isLiquidGlassEnabled,
                    onCheckedChange = onLiquidGlassToggled,
                    colors = liquidGlassSwitchColors,
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
                items(orderedThemes, key = { it.name }) { theme ->
                    ThemeOptionItem(
                        theme = theme,
                        isSelected = theme == selectedTheme,
                        mainTextColor = mainTextColor,
                        isLiquidGlassEnabled = isLiquidGlassEnabled,
                        isDarkTextEnabled = isDarkTextEnabled,
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
    isLiquidGlassEnabled: Boolean,
    isDarkTextEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val cardModifier = if (isLiquidGlassEnabled) {
        Modifier
            .background(LiquidGlass.glassBrush(isDarkTextEnabled), RoundedCornerShape(16.dp))
            .border(BorderStroke(1.2.dp, LiquidGlass.borderBrush(isDarkTextEnabled)), RoundedCornerShape(16.dp))
    } else {
        Modifier.background(mainTextColor.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
    }

    val previewBrush = remember(colorTheme, isHome, isDarkTextEnabled) {
        if (isHome) colorTheme.backgroundBrush(isDarkTextEnabled, alpha = 0.88f)
        else colorTheme.menuBrush(isDarkTextEnabled, alpha = 0.96f)
    }

    Box(
        modifier = modifier.then(cardModifier)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Karten-Titel bleibt grau
            Text(title, color = mainTextColor.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(previewBrush)
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
fun ThemeOptionItem(
    theme: ColorTheme,
    isSelected: Boolean,
    mainTextColor: Color,
    isLiquidGlassEnabled: Boolean,
    isDarkTextEnabled: Boolean,
    onClick: () -> Unit
) {
    val itemModifier = if (isLiquidGlassEnabled) {
        val baseModifier = Modifier
            .background(LiquidGlass.glassBrush(isDarkTextEnabled), RoundedCornerShape(16.dp))
            .border(BorderStroke(1.2.dp, LiquidGlass.borderBrush(isDarkTextEnabled)), RoundedCornerShape(16.dp))

        if (isSelected) {
             baseModifier.border(BorderStroke(1.5.dp, mainTextColor.copy(alpha = 0.5f)), RoundedCornerShape(16.dp))
        } else {
             baseModifier
        }
    } else {
        val bgColor = if (isSelected) mainTextColor.copy(alpha = 0.15f) else mainTextColor.copy(alpha = 0.05f)
        val border = if (isSelected) BorderStroke(1.dp, mainTextColor.copy(alpha = 0.3f)) else null

        Modifier
            .background(bgColor, RoundedCornerShape(16.dp))
            .then(if (border != null) Modifier.border(border, RoundedCornerShape(16.dp)) else Modifier)
    }

    val previewBrush = remember(theme, isDarkTextEnabled) {
        theme.menuBrush(isDarkTextEnabled, alpha = if (isDarkTextEnabled) 0.98f else 0.94f)
    }
    val themeBorderColor = remember(theme, isDarkTextEnabled) {
        theme.borderColor(isDarkTextEnabled)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(itemModifier)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(previewBrush)
                    .border(1.dp, themeBorderColor, RoundedCornerShape(999.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(theme.themeName, color = mainTextColor, fontSize = 16.sp)
                    if (theme.isArtTheme) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ART",
                            color = mainTextColor.copy(alpha = 0.65f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(mainTextColor.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = if (theme.isArtTheme) "Mehrfarbiger Atmosphären-Verlauf" else "Klassische minimalistische Palette",
                    color = mainTextColor.copy(alpha = 0.55f),
                    fontSize = 12.sp
                )
            }

            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = mainTextColor)
            }
        }
    }
}
