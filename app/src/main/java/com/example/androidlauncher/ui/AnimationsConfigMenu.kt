package com.example.androidlauncher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.R
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Maximize2
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.Minimize2
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Star
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle
import com.example.androidlauncher.ui.theme.LocalFontWeight
import com.example.androidlauncher.ui.theme.rememberAppHaptics
import kotlin.math.roundToInt

/**
 * Untermenü zum gezielten Ein-/Ausschalten einzelner Animationsarten.
 *
 * Oben steht der Master-Schalter „Alle Animationen". Ist er aus, sind die
 * Einzelschalter ausgegraut und ohne Wirkung (die eigentliche UND-Verknüpfung
 * mit dem Master passiert in AndroidLauncherTheme).
 */
@Composable
fun AnimationsConfigMenu(
    isAnimationsEnabled: Boolean,
    onAnimationsToggled: (Boolean) -> Unit,
    isAppOpenAnimationEnabled: Boolean,
    onAppOpenAnimationToggled: (Boolean) -> Unit,
    isAppCloseAnimationEnabled: Boolean,
    onAppCloseAnimationToggled: (Boolean) -> Unit,
    isMenuAnimationEnabled: Boolean,
    onMenuAnimationToggled: (Boolean) -> Unit,
    isFavoritesAnimationEnabled: Boolean,
    onFavoritesAnimationToggled: (Boolean) -> Unit,
    animationSpeed: Float,
    onAnimationSpeedChanged: (Float) -> Unit,
    onClose: () -> Unit
) {
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val designStyle = LocalDesignStyle.current
    val surfaceAccent = LocalColorTheme.current.menuSurfaceColor(isDarkTextEnabled)
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val fontWeight = LocalFontWeight.current
    val listState = rememberLazyListState()
    val haptics = rememberAppHaptics()
    // Merker der zuletzt „getickten" Tempo-Stufe (0,1er-Schritte).
    var lastSpeedStep by remember { mutableStateOf((animationSpeed * 10).roundToInt()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("animations_config_menu")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.label_animations), fontSize = 28.sp, fontWeight = fontWeight.weight, color = mainTextColor)
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
                    label = stringResource(R.string.anim_all),
                    description = stringResource(R.string.anim_all_desc),
                    checked = isAnimationsEnabled,
                    onCheckedChange = { onAnimationsToggled(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "animations_master_switch"
                )
            }

            // Globaler Tempo-Regler (greift nur, wenn der Master aktiv ist).
            item {
                val sliderEnabled = isAnimationsEnabled
                val contentAlpha = if (sliderEnabled) 1f else 0.35f
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("animation_speed_slider")
                ) {
                    Text(
                        stringResource(R.string.anim_speed_label, "%.1f".format(animationSpeed)),
                        color = mainTextColor.copy(alpha = contentAlpha),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = animationSpeed,
                        onValueChange = {
                            val step = (it * 10).roundToInt()
                            if (sliderEnabled && step != lastSpeedStep) { haptics.select(); lastSpeedStep = step }
                            onAnimationSpeedChanged(it)
                        },
                        valueRange = 0.5f..2f,
                        // 0,5×–2× in 0,1-Schritten → 15 Stufen, 14 Zwischenpunkte.
                        steps = 14,
                        enabled = sliderEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = mainTextColor,
                            activeTrackColor = mainTextColor,
                            inactiveTrackColor = mainTextColor.copy(alpha = 0.2f)
                        )
                    )
                }
            }

            item {
                Text(
                    stringResource(R.string.anim_individual),
                    color = mainTextColor.copy(alpha = if (isAnimationsEnabled) 0.7f else 0.35f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            item {
                EditToggleItem(
                    icon = Lucide.Maximize2,
                    label = stringResource(R.string.anim_app_open),
                    description = stringResource(R.string.anim_app_open_desc),
                    checked = isAppOpenAnimationEnabled,
                    onCheckedChange = { onAppOpenAnimationToggled(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "animation_app_open_switch",
                    enabled = isAnimationsEnabled
                )
            }

            item {
                EditToggleItem(
                    icon = Lucide.Minimize2,
                    label = stringResource(R.string.anim_app_close),
                    description = stringResource(R.string.anim_app_close_desc),
                    checked = isAppCloseAnimationEnabled,
                    onCheckedChange = { onAppCloseAnimationToggled(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "animation_app_close_switch",
                    enabled = isAnimationsEnabled
                )
            }

            item {
                EditToggleItem(
                    icon = Lucide.Menu,
                    label = stringResource(R.string.anim_menus),
                    description = stringResource(R.string.anim_menus_desc),
                    checked = isMenuAnimationEnabled,
                    onCheckedChange = { onMenuAnimationToggled(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "animation_menus_switch",
                    enabled = isAnimationsEnabled
                )
            }

            item {
                EditToggleItem(
                    icon = Lucide.Star,
                    label = stringResource(R.string.anim_favorites),
                    description = stringResource(R.string.anim_favorites_desc),
                    checked = isFavoritesAnimationEnabled,
                    onCheckedChange = { onFavoritesAnimationToggled(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "animation_favorites_switch",
                    enabled = isAnimationsEnabled
                )
            }
        }
    }
}
