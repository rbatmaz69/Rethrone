package com.example.androidlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.R
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
    homeScreenPreview: @Composable () -> Unit = {},
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
                stringResource(R.string.adjust_wallpaper),
                fontSize = 28.sp,
                fontWeight = fontWeight.weight,
                color = mainTextColor
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close), tint = mainTextColor)
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

                    // Echte, herunterskalierte Startbildschirm-Vorschau (Layout + Farben live).
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val config = LocalConfiguration.current
                        val screenW = config.screenWidthDp.dp
                        val screenH = config.screenHeightDp.dp
                        val previewScale = minOf(maxWidth / screenW, maxHeight / screenH)
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .requiredSize(screenW, screenH)
                                .graphicsLayer {
                                    scaleX = previewScale
                                    scaleY = previewScale
                                }
                        ) {
                            homeScreenPreview()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.wallpaper_preview_home), fontSize = 12.sp, color = secondaryTextColor)
            }
        }

        Spacer(modifier = Modifier.weight(0.5f).heightIn(min = 8.dp, max = 24.dp))

        // Controls
        ControlSlider(
            label = stringResource(R.string.wallpaper_blur),
            value = blurLevel,
            onValueChange = onBlurChange,
            range = 0f..25f,
            mainTextColor = mainTextColor,
            secondaryTextColor = secondaryTextColor
        )
        ControlSlider(
            label = stringResource(R.string.wallpaper_dim),
            value = dimLevel,
            onValueChange = onDimChange,
            range = 0f..0.8f,
            mainTextColor = mainTextColor,
            secondaryTextColor = secondaryTextColor
        )
        ControlSlider(
            label = stringResource(R.string.wallpaper_zoom),
            value = zoomLevel,
            onValueChange = onZoomChange,
            range = 1.0f..2.0f,
            mainTextColor = mainTextColor,
            secondaryTextColor = secondaryTextColor
        )

        Spacer(modifier = Modifier.weight(0.2f).heightIn(min = 8.dp, max = 16.dp))

        Button(
            onClick = {
                onBlurChange(0f)
                onDimChange(0.1f)
                onZoomChange(1.0f)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = mainTextColor.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.wallpaper_reset_effects), color = mainTextColor)
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
