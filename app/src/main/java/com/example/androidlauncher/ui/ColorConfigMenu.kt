package com.example.androidlauncher.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.androidlauncher.R
import com.example.androidlauncher.data.DesignStyle
import com.example.androidlauncher.ui.LiquidGlass.designSurface
import com.example.androidlauncher.ui.theme.ColorTheme
import com.example.androidlauncher.ui.theme.LocalAnimationsEnabled
import com.example.androidlauncher.ui.theme.LocalFontWeight
import com.example.androidlauncher.ui.theme.RethroneSprings
import com.example.androidlauncher.ui.theme.seedRevision

/**
 * Menu for configuring the visual theme of the launcher.
 */
@Composable
fun ColorConfigMenu(
    selectedTheme: ColorTheme,
    isDarkTextEnabled: Boolean,
    iconColor: Color,
    onIconColorChange: (Color) -> Unit,
    homeTextColor: Color,
    onHomeTextColorChange: (Color) -> Unit,
    designStyle: DesignStyle,
    onOpenDesignMenu: () -> Unit,
    onOpenThemeMenu: () -> Unit,
    customBackgroundColor: Color,
    onCustomBackgroundChange: (Color) -> Unit,
    customMenuColor: Color,
    onCustomMenuChange: (Color) -> Unit,
    dynamicIslandColor: Color,
    onDynamicIslandColorChange: (Color) -> Unit,
    edgeLightingColor: Color,
    onEdgeLightingColorChange: (Color) -> Unit,
    customWallpaperUri: String? = null,
    onClose: () -> Unit
) {
    val fontWeight = LocalFontWeight.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    // Welcher Farbwähler ist gerade offen: "text", "icon", "bg", "menu" oder null.
    var activePicker by remember { mutableStateOf<String?>(null) }
    // Ob die Wallpaper-Farbpinzette gerade aktiv ist (überdeckt den Picker-Dialog).
    var eyedropperActive by remember { mutableStateOf(false) }
    val dialogBackground = remember(selectedTheme, isDarkTextEnabled) {
        selectedTheme.menuSurfaceColor(isDarkTextEnabled)
    }

    // Hintergrund (Wallpaper + Theme-Verlauf) stellt das gemeinsame MenuOverlay bereit.
    Box(modifier = Modifier.fillMaxSize().testTag("color_config_menu")) {
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
                    stringResource(R.string.color_config_title),
                    fontSize = 28.sp,
                    fontWeight = fontWeight.weight,
                    color = mainTextColor
                )
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = null, tint = mainTextColor)
                }
            }

            Spacer(modifier = Modifier.weight(0.5f).heightIn(min = 8.dp, max = 24.dp))

            Text(
                stringResource(R.string.color_config_title),
                color = mainTextColor.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            ColorPickerRow(
                label = stringResource(R.string.color_label_home_text),
                color = homeTextColor,
                mainTextColor = mainTextColor,
                onClick = { activePicker = "text" }
            )
            Spacer(modifier = Modifier.height(10.dp))
            ColorPickerRow(
                label = stringResource(R.string.color_label_icon),
                color = iconColor,
                mainTextColor = mainTextColor,
                onClick = { activePicker = "icon" }
            )
            Spacer(modifier = Modifier.height(10.dp))
            ColorPickerRow(
                label = stringResource(R.string.dynamic_island_color),
                color = dynamicIslandColor,
                mainTextColor = mainTextColor,
                onClick = { activePicker = "island" }
            )
            Spacer(modifier = Modifier.height(10.dp))
            ColorPickerRow(
                label = stringResource(R.string.edge_lighting_color),
                color = edgeLightingColor,
                mainTextColor = mainTextColor,
                onClick = { activePicker = "edge" }
            )

            // Nur bei „Eigene Farbe": zwei wählbare Flächenfarben.
            if (selectedTheme == ColorTheme.CUSTOM) {
                Spacer(modifier = Modifier.height(10.dp))
                ColorPickerRow(
                    label = stringResource(R.string.color_label_background),
                    color = customBackgroundColor,
                    mainTextColor = mainTextColor,
                    onClick = { activePicker = "bg" }
                )
                Spacer(modifier = Modifier.height(10.dp))
                ColorPickerRow(
                    label = stringResource(R.string.color_label_menu),
                    color = customMenuColor,
                    mainTextColor = mainTextColor,
                    onClick = { activePicker = "menu" }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                stringResource(R.string.color_section_appearance),
                color = mainTextColor.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Antippbare Zeile → Theme-Untermenü.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenThemeMenu() }
                    .background(mainTextColor.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .testTag("theme_selection_row")
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.label_themes), color = mainTextColor, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        selectedTheme.themeNameRes?.let { stringResource(it) } ?: selectedTheme.themeName,
                        color = mainTextColor.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = mainTextColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Antippbare Zeile, die das Design-Untermenü öffnet.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenDesignMenu() }
                    .background(mainTextColor.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .testTag("design_style_row")
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.label_design), color = mainTextColor, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(designStyle.titleRes),
                        color = mainTextColor.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = mainTextColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        if (activePicker != null) {
            val (pickerTitle, pickerColor, pickerOnChange) = when (activePicker) {
                "text" -> Triple(stringResource(R.string.color_label_home_text), homeTextColor, onHomeTextColorChange)
                "icon" -> Triple(stringResource(R.string.color_label_icon), iconColor, onIconColorChange)
                "island" -> Triple(
                    stringResource(R.string.dynamic_island_color),
                    dynamicIslandColor,
                    onDynamicIslandColorChange
                )
                "edge" -> Triple(
                    stringResource(R.string.edge_lighting_color),
                    edgeLightingColor,
                    onEdgeLightingColorChange
                )
                "bg" -> Triple(
                    stringResource(R.string.color_label_background),
                    customBackgroundColor,
                    onCustomBackgroundChange
                )
                else -> Triple(stringResource(R.string.color_label_menu), customMenuColor, onCustomMenuChange)
            }
            if (eyedropperActive) {
                WallpaperEyedropper(
                    customWallpaperUri = customWallpaperUri,
                    onPicked = { c ->
                        pickerOnChange(c)
                        eyedropperActive = false
                    },
                    onCancel = { eyedropperActive = false }
                )
            } else {
                ColorPickerDialog(
                    title = pickerTitle,
                    color = pickerColor,
                    onColorChange = pickerOnChange,
                    backgroundColor = dialogBackground,
                    mainTextColor = mainTextColor,
                    onRequestEyedropper = { eyedropperActive = true },
                    onDismiss = { activePicker = null }
                )
            }
        }
    }
}

/** Eine antippbare Zeile mit Beschriftung und Farb-Swatch, öffnet den Farbwähler. */
@Composable
fun ColorPickerRow(label: String, color: Color, mainTextColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(mainTextColor.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = mainTextColor, fontSize = 16.sp)
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, mainTextColor.copy(alpha = 0.3f), CircleShape)
        )
    }
}

/** Modaler Dialog mit dem HSV-Farbwähler. */
@Composable
fun ColorPickerDialog(
    title: String,
    color: Color,
    onColorChange: (Color) -> Unit,
    backgroundColor: Color,
    mainTextColor: Color,
    onDismiss: () -> Unit,
    onRequestEyedropper: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(backgroundColor)
                .padding(24.dp)
        ) {
            Text(title, color = mainTextColor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(20.dp))
            ColorWheelPicker(
                color = color,
                onColorChange = onColorChange,
                mainTextColor = mainTextColor,
                onEyedropper = onRequestEyedropper
            )
            Spacer(modifier = Modifier.height(20.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.button_done), color = mainTextColor, fontSize = 16.sp)
            }
        }
    }
}

/**
 * Eigenständiges Untermenü mit der Theme-Auswahl (vorher inline im Farben-Menü).
 */
@Composable
fun ThemeSelectionMenu(
    selectedTheme: ColorTheme,
    onThemeSelected: (ColorTheme) -> Unit,
    isDarkTextEnabled: Boolean,
    designStyle: DesignStyle,
    customWallpaperUri: String? = null,
    onClose: () -> Unit
) {
    val fontWeight = LocalFontWeight.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val orderedThemes = remember {
        ColorTheme.entries.sortedWith(
            // "Eigene Farbe" und "Dynamisch" ganz vorne, dann Art-Themes, dann alphabetisch.
            compareByDescending<ColorTheme> { it == ColorTheme.CUSTOM }
                .thenByDescending { it == ColorTheme.DYNAMIC }
                .thenByDescending { it.isArtTheme }
                .thenBy { it.themeName }
        )
    }

    // Hintergrund (Wallpaper + Theme-Verlauf) stellt das gemeinsame MenuOverlay bereit.
    Box(modifier = Modifier.fillMaxSize().testTag("theme_selection_menu")) {
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
                    stringResource(R.string.label_themes),
                    fontSize = 28.sp,
                    fontWeight = fontWeight.weight,
                    color = mainTextColor
                )
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = null, tint = mainTextColor)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(orderedThemes, key = { it.name }) { theme ->
                    ThemeOptionItem(
                        theme = theme,
                        isSelected = theme == selectedTheme,
                        mainTextColor = mainTextColor,
                        designStyle = designStyle,
                        isDarkTextEnabled = isDarkTextEnabled,
                        onClick = { onThemeSelected(theme) }
                    )
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
    designStyle: DesignStyle,
    isDarkTextEnabled: Boolean,
    onClick: () -> Unit
) {
    val haptics = com.example.androidlauncher.ui.theme.rememberAppHaptics()
    // Material-3-Expressive: weicher Auswahl-Übergang statt hartem Umspringen.
    val animationsEnabled = LocalAnimationsEnabled.current
    val alphaSpec = if (animationsEnabled) RethroneSprings.effects<Float>() else snap<Float>()
    val fillAlpha by animateFloatAsState(if (isSelected) 0.15f else 0.05f, alphaSpec, label = "themeFill")
    val borderAlpha by animateFloatAsState(if (isSelected) 0.5f else 0f, alphaSpec, label = "themeBorder")
    val itemShape = RoundedCornerShape(20.dp)
    val itemModifier = Modifier
        .designSurface(
            designStyle,
            itemShape,
            isDarkTextEnabled,
            accent = theme.menuSurfaceColor(isDarkTextEnabled),
            fillAlpha = fillAlpha
        )
        .border(BorderStroke(1.5.dp, mainTextColor.copy(alpha = borderAlpha)), itemShape)

    val previewBrush = remember(theme, isDarkTextEnabled, theme.seedRevision()) {
        theme.menuBrush(isDarkTextEnabled, alpha = if (isDarkTextEnabled) 0.98f else 0.94f)
    }
    val themeBorderColor = remember(theme, isDarkTextEnabled, theme.seedRevision()) {
        theme.borderColor(isDarkTextEnabled)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(itemModifier)
            .clickable(onClick = {
                haptics.select()
                onClick()
            })
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
                    Text(
                        theme.themeNameRes?.let { stringResource(it) } ?: theme.themeName,
                        color = mainTextColor,
                        fontSize = 16.sp
                    )
                    if (theme.isArtTheme) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.theme_badge_art),
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
                    text = when {
                        theme == ColorTheme.CUSTOM -> stringResource(R.string.theme_desc_custom)
                        theme == ColorTheme.DYNAMIC -> stringResource(R.string.theme_desc_dynamic)
                        theme.isArtTheme -> stringResource(R.string.theme_desc_art)
                        else -> stringResource(R.string.theme_desc_classic)
                    },
                    color = mainTextColor.copy(alpha = 0.55f),
                    fontSize = 12.sp
                )
            }

            AnimatedVisibility(
                visible = isSelected,
                enter = if (animationsEnabled) scaleIn(RethroneSprings.spatial(), initialScale = 0.6f) + fadeIn() else EnterTransition.None,
                exit = if (animationsEnabled) scaleOut(RethroneSprings.effects(), targetScale = 0.6f) + fadeOut() else ExitTransition.None
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = mainTextColor)
            }
        }
    }
}
