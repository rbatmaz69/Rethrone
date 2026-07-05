package com.example.androidlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.R
// SystemWallpaperView ist im selben Paket (ui)
import com.example.androidlauncher.data.AppFont
import com.example.androidlauncher.data.FavoriteSpacing
import com.example.androidlauncher.data.FontSize
import com.example.androidlauncher.data.FontWeightLevel
import com.example.androidlauncher.data.IconSize
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle
import com.example.androidlauncher.ui.theme.rememberAppHaptics
import com.example.androidlauncher.ui.LiquidGlass.designSurface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import kotlin.math.roundToInt

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
    currentFavoriteSpacing: FavoriteSpacing,
    onFavoriteSpacingSelected: (FavoriteSpacing) -> Unit,
    currentAppFont: AppFont,
    onOpenFontSelection: () -> Unit,
    customWallpaperUri: String? = null,
    onClose: () -> Unit
) {
    val colorTheme = LocalColorTheme.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val designStyle = LocalDesignStyle.current
    val surfaceAccent = colorTheme.menuSurfaceColor(isDarkTextEnabled)
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val secondaryTextColor = if (isDarkTextEnabled) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f)
    val haptics = rememberAppHaptics()

    // Hintergrund (Wallpaper + Theme-Verlauf) stellt das gemeinsame MenuOverlay bereit.
    Box(modifier = Modifier.fillMaxSize()) {
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
                    text = stringResource(R.string.size_config_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    color = mainTextColor
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = mainTextColor
                    )
                }
            }

            // Proportionale Abstände durch Weights in den Spacern
            Spacer(modifier = Modifier.weight(0.5f).heightIn(min = 8.dp, max = 24.dp))

            Text(stringResource(R.string.preview), color = secondaryTextColor, fontSize = 14.sp)
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
                    Text(
                        "12:00",
                        color = mainTextColor,
                        fontSize = (40.sp * currentFontSize.scale),
                        fontWeight = currentFontWeight.weight,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    repeat(2) { index ->
                        if (index > 0) Spacer(modifier = Modifier.height(currentFavoriteSpacing.spacing))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(
                                    32.dp * currentIconSize.scale
                                ).background(mainTextColor.copy(alpha = 0.3f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.preview_favorite),
                                color = mainTextColor,
                                fontSize = (16.sp * currentFontSize.scale),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f).heightIn(min = 8.dp, max = 24.dp))

            Text(
                text = stringResource(R.string.font_label),
                color = secondaryTextColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            val fontButtonModifier = Modifier.designSurface(
                designStyle,
                RoundedCornerShape(16.dp),
                isDarkTextEnabled,
                surfaceAccent,
                fillAlpha = 0.1f
            )

            Box(
                modifier = Modifier.fillMaxWidth().height(
                    56.dp
                ).then(fontButtonModifier).clip(RoundedCornerShape(16.dp)).clickable {
                    onOpenFontSelection()
                }.padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = stringResource(R.string.choose_font), fontSize = 16.sp, color = mainTextColor)
                        Text(
                            text = currentAppFont.label,
                            fontSize = 12.sp,
                            color = mainTextColor.copy(alpha = 0.6f),
                            fontFamily = currentAppFont.fontFamily
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = mainTextColor.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.5f).heightIn(min = 8.dp, max = 24.dp))

            // Slider Bereich – stufenlos/feinstufig wie der Animationsgeschwindigkeits-Slider.
            val sliderColors = SliderDefaults.colors(
                thumbColor = mainTextColor,
                activeTrackColor = mainTextColor,
                inactiveTrackColor = mainTextColor.copy(alpha = 0.2f)
            )

            // Lokale Merker des zuletzt „getickten" Werts – verhindert Doppel-Ticks
            // durch die asynchrone DataStore-Aktualisierung der current*-Werte.
            var lastFontSize by remember { mutableStateOf(currentFontSize) }
            var lastFontWeight by remember { mutableStateOf(currentFontWeight) }
            var lastIconSize by remember { mutableStateOf(currentIconSize) }
            var lastSpacing by remember { mutableStateOf(currentFavoriteSpacing) }

            Text(
                stringResource(R.string.font_size_label, (currentFontSize.scale * 100).roundToInt()),
                color = secondaryTextColor,
                fontSize = 12.sp
            )
            Slider(
                value = currentFontSize.scale,
                onValueChange = {
                    val next = FontSize.of(it)
                    if (next != lastFontSize) {
                        haptics.select()
                        lastFontSize = next
                    }
                    onFontSizeSelected(next)
                },
                valueRange = FontSize.MIN..FontSize.MAX,
                steps = 99, // 0,60–1,60 in 1%-Schritten
                colors = sliderColors
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                stringResource(
                    R.string.font_weight_label,
                    currentFontWeight.weightValue,
                    stringResource(currentFontWeight.labelRes)
                ),
                color = secondaryTextColor,
                fontSize = 12.sp
            )
            Slider(
                value = currentFontWeight.weightValue.toFloat(),
                onValueChange = {
                    val next = FontWeightLevel.of(it.roundToInt())
                    if (next != lastFontWeight) {
                        haptics.select()
                        lastFontWeight = next
                    }
                    onFontWeightSelected(next)
                },
                valueRange = FontWeightLevel.MIN.toFloat()..FontWeightLevel.MAX.toFloat(),
                steps = 31, // 100–900 in 25er-Schritten
                colors = sliderColors
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                stringResource(R.string.icon_size_label, currentIconSize.size.value.roundToInt()),
                color = secondaryTextColor,
                fontSize = 12.sp
            )
            Slider(
                value = currentIconSize.size.value,
                onValueChange = {
                    val next = IconSize.of(it.dp)
                    if (next != lastIconSize) {
                        haptics.select()
                        lastIconSize = next
                    }
                    onIconSizeSelected(next)
                },
                valueRange = IconSize.MIN.value..IconSize.MAX.value,
                steps = 43, // 28–72 dp in 1-dp-Schritten
                colors = sliderColors
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                stringResource(R.string.icon_spacing_label, currentFavoriteSpacing.spacing.value.roundToInt()),
                color = secondaryTextColor,
                fontSize = 12.sp
            )
            Slider(
                value = currentFavoriteSpacing.spacing.value,
                onValueChange = {
                    val next = FavoriteSpacing.of(it.dp)
                    if (next != lastSpacing) {
                        haptics.select()
                        lastSpacing = next
                    }
                    onFavoriteSpacingSelected(next)
                },
                valueRange = FavoriteSpacing.MIN.value..FavoriteSpacing.MAX.value,
                steps = 47, // 0–48 dp in 1-dp-Schritten
                colors = sliderColors
            )
        }
    }
}
