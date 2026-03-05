package com.example.androidlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalFontWeight

/**
 * Konfigurationsmenü für Wallpaper-Einstellungen.
 */
@Composable
fun WallpaperConfigMenu(
    blurLevel: Float,
    onBlurChange: (Float) -> Unit,
    dimLevel: Float,
    onDimChange: (Float) -> Unit,
    zoomLevel: Float,
    onZoomChange: (Float) -> Unit,
    customWallpaperUri: String? = null,
    onClose: () -> Unit
) {
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val fontWeight = LocalFontWeight.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val secondaryTextColor = if (isDarkTextEnabled) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f)

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
                "Hintergrund anpassen",
                fontSize = 24.sp,
                fontWeight = fontWeight.weight,
                color = mainTextColor
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Schließen", tint = mainTextColor)
            }
        }

        Spacer(modifier = Modifier.weight(0.5f).heightIn(min = 8.dp, max = 16.dp))

        // Large Centered Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(3f, fill = false)
                .heightIn(min = 200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.85f)
                        .aspectRatio(9f / 19f)
                        .clip(RoundedCornerShape(24.dp))
                        .border(BorderStroke(1.5.dp, mainTextColor.copy(alpha = 0.2f)), RoundedCornerShape(24.dp))
                ) {
                    SystemWallpaperView(
                        customWallpaperUri = customWallpaperUri,
                        blurLevel = blurLevel,
                        dimLevel = dimLevel,
                        zoomLevel = zoomLevel
                    )
                    
                    // Home Screen Mockup
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            "12:00", 
                            color = mainTextColor, 
                            fontSize = 40.sp, 
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        repeat(4) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                                Box(modifier = Modifier.size(28.dp).background(mainTextColor.copy(alpha = 0.3f), CircleShape))
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(modifier = Modifier.width(70.dp).height(5.dp).background(mainTextColor.copy(alpha = 0.15f), CircleShape))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Vorschau Startseite", fontSize = 12.sp, color = secondaryTextColor)
            }
        }

        Spacer(modifier = Modifier.weight(0.5f).heightIn(min = 8.dp, max = 24.dp))

        // Controls
        ControlSlider(label = "Unschärfe", value = blurLevel, onValueChange = onBlurChange, range = 0f..25f, mainTextColor = mainTextColor, secondaryTextColor = secondaryTextColor)
        ControlSlider(label = "Abdunkelung", value = dimLevel, onValueChange = onDimChange, range = 0f..0.8f, mainTextColor = mainTextColor, secondaryTextColor = secondaryTextColor)
        ControlSlider(label = "Zoom", value = zoomLevel, onValueChange = onZoomChange, range = 1.0f..2.0f, mainTextColor = mainTextColor, secondaryTextColor = secondaryTextColor)
        
        Spacer(modifier = Modifier.weight(0.2f).heightIn(min = 8.dp, max = 16.dp))
        
        Button(
            onClick = {
                onBlurChange(0f)
                onDimChange(0.1f)
                onZoomChange(1.0f)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = mainTextColor.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Effekte zurücksetzen", color = mainTextColor)
        }
    }
}

@Composable
fun ControlSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    mainTextColor: Color,
    secondaryTextColor: Color
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, color = secondaryTextColor, fontSize = 12.sp)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = mainTextColor,
                activeTrackColor = mainTextColor,
                inactiveTrackColor = mainTextColor.copy(alpha = 0.2f)
            )
        )
    }
}
