package com.example.androidlauncher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.composables.icons.lucide.Hand
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Smartphone
import com.example.androidlauncher.R
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.GestureAction
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle
import com.example.androidlauncher.ui.theme.LocalFontWeight

/**
 * Untermenü „Gesten": pro Geste eine frei wählbare Aktion. Enthält aktuell die
 * Doppeltipp-Geste und die (bestehende) Schüttel-Geste.
 */
@Composable
fun GesturesConfigMenu(
    apps: List<AppInfo>,
    doubleTapAction: GestureAction,
    onDoubleTapActionChange: (GestureAction) -> Unit,
    doubleTapAppPackage: String?,
    onDoubleTapAppPackageChange: (String?) -> Unit,
    isShakeGesturesEnabled: Boolean,
    onShakeGesturesToggled: (Boolean) -> Unit,
    doubleShakeAction: GestureAction,
    onDoubleShakeActionChange: (GestureAction) -> Unit,
    shakeOpenAppPackage: String?,
    onShakeOpenAppPackageChange: (String?) -> Unit,
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
            .testTag("gestures_config_menu")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.label_gestures),
                fontSize = 28.sp,
                fontWeight = fontWeight.weight,
                color = mainTextColor
            )
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
            contentPadding = PaddingValues(top = 24.dp, bottom = 8.dp)
        ) {
            // --- Doppeltippen ---
            item { GestureSectionHeader(stringResource(R.string.gestures_section_double_tap), mainTextColor) }
            item {
                EditActionSelectorItem(
                    label = stringResource(R.string.gestures_on_double_tap),
                    selectedAction = doubleTapAction,
                    onActionSelected = onDoubleTapActionChange,
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "double_tap_action_selector"
                )
            }
            if (doubleTapAction == GestureAction.OPEN_APP) {
                item {
                    var showPicker by remember { mutableStateOf(false) }
                    val selectedLabel = apps.firstOrNull { it.packageName == doubleTapAppPackage }?.label
                    EditMenuItem(
                        icon = Lucide.Smartphone,
                        label = stringResource(R.string.choose_app),
                        onClick = { showPicker = true },
                        statusLabel = selectedLabel ?: stringResource(R.string.none),
                        mainTextColor = mainTextColor,
                        designStyle = designStyle,
                        surfaceAccent = surfaceAccent,
                        isDarkTextEnabled = isDarkTextEnabled,
                        testTag = "double_tap_open_app_picker"
                    )
                    if (showPicker) {
                        GestureAppPickerDialog(
                            apps = apps,
                            selectedPackage = doubleTapAppPackage,
                            onAppSelected = {
                                onDoubleTapAppPackageChange(it);
                                showPicker = false
                            },
                            onDismiss = { showPicker = false },
                            mainTextColor = mainTextColor,
                            designStyle = designStyle,
                            surfaceAccent = surfaceAccent,
                            isDarkTextEnabled = isDarkTextEnabled
                        )
                    }
                }
            }

            // --- Schütteln ---
            item { GestureSectionHeader(stringResource(R.string.gestures_section_shake), mainTextColor) }
            item {
                EditToggleItem(
                    icon = Lucide.Hand,
                    label = stringResource(R.string.gestures_shake_toggle),
                    description = stringResource(R.string.gestures_shake_desc),
                    checked = isShakeGesturesEnabled,
                    onCheckedChange = onShakeGesturesToggled,
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "shake_gestures_switch"
                )
            }
            if (isShakeGesturesEnabled) {
                item {
                    EditActionSelectorItem(
                        label = stringResource(R.string.gestures_on_double_shake),
                        selectedAction = doubleShakeAction,
                        onActionSelected = onDoubleShakeActionChange,
                        mainTextColor = mainTextColor,
                        designStyle = designStyle,
                        surfaceAccent = surfaceAccent,
                        isDarkTextEnabled = isDarkTextEnabled,
                        testTag = "double_shake_action_selector"
                    )
                }
                if (doubleShakeAction == GestureAction.OPEN_APP) {
                    item {
                        var showPicker by remember { mutableStateOf(false) }
                        val selectedLabel = apps.firstOrNull { it.packageName == shakeOpenAppPackage }?.label
                        EditMenuItem(
                            icon = Lucide.LayoutGrid,
                            label = stringResource(R.string.choose_app),
                            onClick = { showPicker = true },
                            statusLabel = selectedLabel ?: stringResource(R.string.none),
                            mainTextColor = mainTextColor,
                            designStyle = designStyle,
                            surfaceAccent = surfaceAccent,
                            isDarkTextEnabled = isDarkTextEnabled,
                            testTag = "shake_open_app_picker"
                        )
                        if (showPicker) {
                            GestureAppPickerDialog(
                                apps = apps,
                                selectedPackage = shakeOpenAppPackage,
                                onAppSelected = {
                                    onShakeOpenAppPackageChange(it);
                                    showPicker = false
                                },
                                onDismiss = { showPicker = false },
                                mainTextColor = mainTextColor,
                                designStyle = designStyle,
                                surfaceAccent = surfaceAccent,
                                isDarkTextEnabled = isDarkTextEnabled
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GestureSectionHeader(title: String, mainTextColor: Color) {
    Text(
        title,
        color = mainTextColor.copy(alpha = 0.7f),
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}
