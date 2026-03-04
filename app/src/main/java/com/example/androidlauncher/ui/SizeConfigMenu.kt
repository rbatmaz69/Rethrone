package com.example.androidlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// SystemWallpaperView ist im selben Paket (ui)
import com.example.androidlauncher.data.AppFont
import com.example.androidlauncher.data.FontSize
import com.example.androidlauncher.data.FontWeightLevel
import com.example.androidlauncher.data.IconSize
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalLiquidGlassEnabled
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable

/**
 * Konfigurationsmenü für Schrift- und Icon-Größen.
 */
@Composable
fun SizeConfigMenu(
    currentFontSize: FontSize,
    onFontSizeSelected: (FontSize) -> Unit,
    currentFontWeight: FontWeightLevel,
    onFontWeightSelected: (FontWeightLevel) -> Unit,
    currentIconSize: IconSize,
    onIconSizeSelected: (IconSize) -> Unit,
    currentAppFont: AppFont,
    onOpenFontSelection: () -> Unit,
    customWallpaperUri: String? = null,
    onClose: () -> Unit
) {
    val colorTheme = LocalColorTheme.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val isLiquidGlassEnabled = LocalLiquidGlassEnabled.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val secondaryTextColor = if (isDarkTextEnabled) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f)

    val backgroundColor = if (isDarkTextEnabled) colorTheme.lightBackground else colorTheme.drawerBackground
    Box(modifier = Modifier.fillMaxSize()) {
        SystemWallpaperView(customWallpaperUri)
        Box(modifier = Modifier.fillMaxSize().background(backgroundColor.copy(alpha = 0.95f)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding() // WICHTIG: Verhindert Überlappung mit System-Nav
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Design & Schriftart",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    color = mainTextColor
                )
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Schließen", tint = mainTextColor)
                }
            }
            
            // Proportionale Abstände durch Weights in den Spacern
            Spacer(modifier = Modifier.weight(0.5f).heightIn(min = 8.dp, max = 24.dp))
            
            Text("Vorschau", color = secondaryTextColor, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Vorschau-Box mit flexibler Höhe aber Fokus auf deinen 240dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 240.dp)
                    .weight(3f, fill = false) // Nimmt Platz ein, aber nicht mehr als nötig
                    .clip(RoundedCornerShape(24.dp))
                    .border(BorderStroke(1.5.dp, mainTextColor.copy(alpha = 0.2f)), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                SystemWallpaperView(customWallpaperUri)
                Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.Start) {
                    Text("12:00", color = mainTextColor, fontSize = (40.sp * currentFontSize.scale), fontWeight = currentFontWeight.weight, letterSpacing = (-1).sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp * currentIconSize.scale).background(mainTextColor.copy(alpha = 0.3f), CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Favorit", color = mainTextColor, fontSize = (16.sp * currentFontSize.scale), fontWeight = FontWeight.Normal)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f).heightIn(min = 8.dp, max = 24.dp))
            
            Text(text = "Schriftart", color = secondaryTextColor, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
            val fontButtonModifier = if (isLiquidGlassEnabled) {
                Modifier.background(LiquidGlass.glassBrush(isDarkTextEnabled), RoundedCornerShape(12.dp)).border(BorderStroke(1.2.dp, LiquidGlass.borderBrush(isDarkTextEnabled)), RoundedCornerShape(12.dp))
            } else { Modifier.background(mainTextColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)) }

            Box(
                modifier = Modifier.fillMaxWidth().height(56.dp).then(fontButtonModifier).clip(RoundedCornerShape(12.dp)).clickable { onOpenFontSelection() }.padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(text = "Schriftart wählen", fontSize = 16.sp, color = mainTextColor)
                        Text(text = currentAppFont.label, fontSize = 12.sp, color = mainTextColor.copy(alpha = 0.6f), fontFamily = currentAppFont.fontFamily)
                    }
                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = mainTextColor.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.weight(0.5f).heightIn(min = 8.dp, max = 24.dp))
            
            // Slider Bereich
            Text("Schriftgröße", color = secondaryTextColor, fontSize = 12.sp)
            val fontSizeOptions = FontSize.entries
            Slider(
                value = fontSizeOptions.indexOf(currentFontSize).toFloat(),
                onValueChange = { onFontSizeSelected(fontSizeOptions[it.toInt()]) },
                valueRange = 0f..(fontSizeOptions.size - 1).toFloat(),
                steps = fontSizeOptions.size - 2,
                colors = SliderDefaults.colors(thumbColor = mainTextColor, activeTrackColor = mainTextColor, inactiveTrackColor = mainTextColor.copy(alpha = 0.2f))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Schriftstärke", color = secondaryTextColor, fontSize = 12.sp)
            val fontWeightOptions = FontWeightLevel.entries
            Slider(
                value = fontWeightOptions.indexOf(currentFontWeight).toFloat(),
                onValueChange = { onFontWeightSelected(fontWeightOptions[it.toInt()]) },
                valueRange = 0f..(fontWeightOptions.size - 1).toFloat(),
                steps = fontWeightOptions.size - 2,
                colors = SliderDefaults.colors(thumbColor = mainTextColor, activeTrackColor = mainTextColor, inactiveTrackColor = mainTextColor.copy(alpha = 0.2f))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Icon-Größe", color = secondaryTextColor, fontSize = 12.sp)
            val iconSizeOptions = IconSize.entries
            Slider(
                value = iconSizeOptions.indexOf(currentIconSize).toFloat(),
                onValueChange = { onIconSizeSelected(iconSizeOptions[it.toInt()]) },
                valueRange = 0f..(iconSizeOptions.size - 1).toFloat(),
                steps = iconSizeOptions.size - 2,
                colors = SliderDefaults.colors(thumbColor = mainTextColor, activeTrackColor = mainTextColor, inactiveTrackColor = mainTextColor.copy(alpha = 0.2f))
            )
        }
    }
}
