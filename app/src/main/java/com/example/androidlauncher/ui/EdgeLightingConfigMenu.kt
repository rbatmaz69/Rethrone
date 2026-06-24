package com.example.androidlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Sparkles
import com.example.androidlauncher.R
import com.example.androidlauncher.data.EdgeLightingStyle
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle
import com.example.androidlauncher.ui.theme.LocalFontWeight
import kotlin.math.roundToInt

/**
 * Untermenü für das Edge Lighting: Master-Schalter, Effekt-Stil und Regler für Geschwindigkeit,
 * Durchläufe und Stärke. Die Farbe wird separat im Farben-Menü gewählt.
 */
@Composable
fun EdgeLightingConfigMenu(
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    style: EdgeLightingStyle,
    onStyleChange: (EdgeLightingStyle) -> Unit,
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    laps: Int,
    onLapsChange: (Int) -> Unit,
    thickness: Float,
    onThicknessChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val designStyle = LocalDesignStyle.current
    val surfaceAccent = LocalColorTheme.current.menuSurfaceColor(isDarkTextEnabled)
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val fontWeight = LocalFontWeight.current
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("edge_lighting_config_menu")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.edge_lighting), fontSize = 28.sp, fontWeight = fontWeight.weight, color = mainTextColor)
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close), tint = mainTextColor)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 32.dp, bottom = 8.dp)
        ) {
            // Master-Schalter.
            item {
                EditToggleItem(
                    icon = Lucide.Sparkles,
                    label = stringResource(R.string.edge_lighting),
                    description = stringResource(R.string.edge_lighting_desc),
                    checked = isEnabled,
                    onCheckedChange = { onEnabledChange(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "edge_lighting_master_switch"
                )
            }

            // Effekt-Stil.
            item {
                EdgeStyleSelector(
                    selected = style,
                    onSelect = onStyleChange,
                    mainTextColor = mainTextColor,
                    enabled = isEnabled
                )
            }

            // Geschwindigkeit.
            item {
                EdgeSlider(
                    label = stringResource(R.string.edge_speed_label, "%.1f".format(speed)),
                    value = speed,
                    valueRange = 0.5f..2f,
                    steps = 14,
                    enabled = isEnabled,
                    mainTextColor = mainTextColor,
                    onValueChange = onSpeedChange,
                    testTag = "edge_speed_slider"
                )
            }

            // Durchläufe (1..5).
            item {
                EdgeSlider(
                    label = stringResource(R.string.edge_laps_label, laps),
                    value = laps.toFloat(),
                    valueRange = 1f..5f,
                    steps = 3,
                    enabled = isEnabled,
                    mainTextColor = mainTextColor,
                    onValueChange = { onLapsChange(it.roundToInt()) },
                    testTag = "edge_laps_slider"
                )
            }

            // Stärke.
            item {
                EdgeSlider(
                    label = stringResource(R.string.edge_thickness_label, "%.1f".format(thickness)),
                    value = thickness,
                    valueRange = 0.5f..2f,
                    steps = 14,
                    enabled = isEnabled,
                    mainTextColor = mainTextColor,
                    onValueChange = onThicknessChange,
                    testTag = "edge_thickness_slider"
                )
            }
        }
    }
}

/** Slider mit Beschriftung + aktuellem Wert; ausgegraut, wenn [enabled] = false. */
@Composable
private fun EdgeSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    enabled: Boolean,
    mainTextColor: Color,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    val contentAlpha = if (enabled) 1f else 0.35f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Text(label, color = mainTextColor.copy(alpha = contentAlpha), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = mainTextColor,
                activeTrackColor = mainTextColor,
                inactiveTrackColor = mainTextColor.copy(alpha = 0.2f)
            )
        )
    }
}

/** Drei auswählbare „Pills" für den Effekt-Stil. */
@Composable
private fun EdgeStyleSelector(
    selected: EdgeLightingStyle,
    onSelect: (EdgeLightingStyle) -> Unit,
    mainTextColor: Color,
    enabled: Boolean
) {
    val contentAlpha = if (enabled) 1f else 0.35f
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
        Text(
            stringResource(R.string.edge_lighting_style),
            color = mainTextColor.copy(alpha = contentAlpha),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EdgeLightingStyle.entries.forEach { item ->
                val isSel = item == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(mainTextColor.copy(alpha = if (isSel) 0.18f else 0.05f))
                        .then(if (enabled) Modifier.clickable { onSelect(item) } else Modifier)
                        .border(
                            BorderStroke(1.5.dp, mainTextColor.copy(alpha = if (isSel) 0.5f else 0f)),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(vertical = 12.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(item.labelRes),
                        color = mainTextColor.copy(alpha = contentAlpha),
                        fontSize = 13.sp,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
