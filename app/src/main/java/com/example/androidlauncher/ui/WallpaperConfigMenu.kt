package com.example.androidlauncher.ui

import androidx.compose.foundation.background
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
import com.example.androidlauncher.SystemWallpaperView
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalFontWeight

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

        Spacer(modifier = Modifier.height(16.dp))

        // Preview Section
        Text("Vorschau", color = secondaryTextColor, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.2f))
        ) {
            // Mini Wallpaper Preview
            SystemWallpaperView(
                customWallpaperUri = customWallpaperUri,
                blurLevel = blurLevel,
                dimLevel = dimLevel,
                zoomLevel = zoomLevel
            )
            
            // UI Overlay Mockup
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    "12:00", 
                    color = mainTextColor, 
                    fontSize = 32.sp, 
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                repeat(3) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Box(modifier = Modifier.size(24.dp).background(mainTextColor.copy(alpha = 0.2f), CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.width(60.dp).height(4.dp).background(mainTextColor.copy(alpha = 0.15f), CircleShape))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Blur Control
        Text("Unschärfe", color = secondaryTextColor, fontSize = 14.sp)
        Slider(
            value = blurLevel,
            onValueChange = onBlurChange,
            valueRange = 0f..25f,
            colors = SliderDefaults.colors(
                thumbColor = mainTextColor,
                activeTrackColor = mainTextColor,
                inactiveTrackColor = mainTextColor.copy(alpha = 0.2f)
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Dim Control
        Text("Abdunkelung", color = secondaryTextColor, fontSize = 14.sp)
        Slider(
            value = dimLevel,
            onValueChange = onDimChange,
            valueRange = 0f..0.8f,
            colors = SliderDefaults.colors(
                thumbColor = mainTextColor,
                activeTrackColor = mainTextColor,
                inactiveTrackColor = mainTextColor.copy(alpha = 0.2f)
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Zoom Control
        Text("Zoom", color = secondaryTextColor, fontSize = 14.sp)
        Slider(
            value = zoomLevel,
            onValueChange = onZoomChange,
            valueRange = 1.0f..2.0f,
            colors = SliderDefaults.colors(
                thumbColor = mainTextColor,
                activeTrackColor = mainTextColor,
                inactiveTrackColor = mainTextColor.copy(alpha = 0.2f)
            )
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
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
            Text("Zurücksetzen", color = mainTextColor)
        }
    }
}
