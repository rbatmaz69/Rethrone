package com.example.androidlauncher.ui

import com.example.androidlauncher.ForegroundAppResolver
import com.example.androidlauncher.LauncherAccessibilityService
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings2
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Shield
import com.composables.icons.lucide.Hand
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Ban
import com.composables.icons.lucide.BellOff
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Flashlight
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.List
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.PanelRight
import com.composables.icons.lucide.Trash2
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.window.Dialog
import com.example.androidlauncher.LauncherLogic
import com.example.androidlauncher.data.AppAccessMode
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.DesignStyle
import com.example.androidlauncher.data.ShakeAction
import com.example.androidlauncher.ui.LiquidGlass.designSurface
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Konfigurationsmenü für Anpassungen.
 */
@Composable
fun EditConfigMenu(
    onOpenHomeLayoutEdit: () -> Unit,
    onResetHomeLayout: () -> Unit,
    onOpenIconConfig: () -> Unit,
    onOpenUninstallApps: () -> Unit,
    onOpenHiddenApps: () -> Unit,
    onChangeWallpaper: () -> Unit,
    onResetWallpaper: () -> Unit,
    onOpenWallpaperAdjust: () -> Unit,
    isCustomHomeLayoutSet: Boolean,
    isCustomWallpaperSet: Boolean,
    isShakeGesturesEnabled: Boolean,
    onShakeGesturesToggled: (Boolean) -> Unit,
    doubleShakeAction: ShakeAction,
    onDoubleShakeActionChange: (ShakeAction) -> Unit,
    apps: List<AppInfo>,
    shakeOpenAppPackage: String?,
    onShakeOpenAppPackageChange: (String?) -> Unit,
    isSmartSuggestionsEnabled: Boolean,
    onSmartSuggestionsToggled: (Boolean) -> Unit,
    isAnimationsEnabled: Boolean,
    onAnimationsToggled: (Boolean) -> Unit,
    appAccessMode: AppAccessMode,
    onAppAccessModeChange: (AppAccessMode) -> Unit,
    onClearSearchHistory: () -> Unit,
    isHapticFeedbackEnabled: Boolean,
    onHapticFeedbackToggled: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val designStyle = LocalDesignStyle.current
    val surfaceAccent = LocalColorTheme.current.menuSurfaceColor(isDarkTextEnabled)
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val menuListState = rememberLazyListState()

    var isDefaultLauncherSet by remember { mutableStateOf(isDefaultLauncher(context)) }
    var isNotificationEnabled by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var isAccessibilityEnabled by remember { mutableStateOf(LauncherAccessibilityService.isAccessibilityServiceEnabled(context)) }
    var isUsageAccessEnabled by remember { mutableStateOf(ForegroundAppResolver.hasUsageAccess(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isDefaultLauncherSet = isDefaultLauncher(context)
        isNotificationEnabled = isNotificationServiceEnabled(context)
        isAccessibilityEnabled = LauncherAccessibilityService.isAccessibilityServiceEnabled(context)
        isUsageAccessEnabled = ForegroundAppResolver.hasUsageAccess(context)
    }

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
            @Suppress("DEPRECATION")
            Text(
                "Bearbeiten",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = mainTextColor
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = mainTextColor)
            }
        }

        LazyColumn(
            state = menuListState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 32.dp, bottom = 8.dp)
        ) {
            item {
                EditSectionHeader(
                    title = "Allgemein",
                    mainTextColor = mainTextColor
                )
            }

            item {
                EditToggleItem(
                    icon = Lucide.Smartphone,
                    label = "Haptisches Feedback",
                    description = "Vibration bei Interaktionen wie Favoritenbearbeitung",
                    checked = isHapticFeedbackEnabled,
                    onCheckedChange = { onHapticFeedbackToggled(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "haptic_feedback_switch"
                )
            }

            item {
                EditToggleItem(
                    icon = Lucide.Smartphone, // Temporarily using Smartphone or something else, but we need an icon for animation
                    label = "Animationen",
                    description = "App-Start und sonstige Übergänge",
                    checked = isAnimationsEnabled,
                    onCheckedChange = { onAnimationsToggled(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "animations_switch"
                )
            }

            item {
                EditAppAccessSelectorItem(
                    label = "App-Zugriff",
                    selectedMode = appAccessMode,
                    onModeSelected = onAppAccessModeChange,
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "app_access_mode_selector"
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Pencil,
                    label = "Startbildschirm-Layout anpassen",
                    onClick = onOpenHomeLayoutEdit,
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "edit_home_layout_item",
                    trailingContent = {
                        if (isCustomHomeLayoutSet) {
                            IconButton(
                                onClick = onResetHomeLayout,
                                modifier = Modifier.testTag("edit_home_layout_reset")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Reset Home Layout",
                                    tint = mainTextColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = mainTextColor.copy(alpha = 0.4f)
                            )
                        }
                    }
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Settings2,
                    label = "App-Icons anpassen",
                    onClick = onOpenIconConfig,
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Image,
                    label = "Wallpaper ändern",
                    onClick = onChangeWallpaper,
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    trailingContent = {
                        if (isCustomWallpaperSet) {
                            IconButton(onClick = onResetWallpaper) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove Wallpaper",
                                    tint = mainTextColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = mainTextColor.copy(alpha = 0.4f)
                            )
                        }
                    }
                )
            }

            if (isCustomWallpaperSet) {
                item {
                    EditMenuItem(
                        icon = Lucide.Settings2,
                        label = "Hintergrund anpassen",
                        onClick = onOpenWallpaperAdjust,
                        mainTextColor = mainTextColor,
                        designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                        isDarkTextEnabled = isDarkTextEnabled
                    )
                }
            }

            item {
                EditSectionHeader(
                    title = "Apps",
                    mainTextColor = mainTextColor
                )
            }

            item {
                EditMenuItem(
                    icon = Icons.Rounded.VisibilityOff,
                    label = "Apps ausblenden",
                    onClick = onOpenHiddenApps,
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "hidden_apps_item"
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Trash2,
                    label = "Apps deinstallieren",
                    onClick = onOpenUninstallApps,
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "uninstall_apps_item"
                )
            }

            item {
                EditSectionHeader(
                    title = "Suche",
                    mainTextColor = mainTextColor
                )
            }

            item {
                EditToggleItem(
                    icon = Icons.Rounded.Search,
                    label = "Intelligente Suchvorschläge",
                    description = "Lernt aus App-Starts und Websuchen. Alles bleibt lokal auf dem Gerät.",
                    checked = isSmartSuggestionsEnabled,
                    onCheckedChange = onSmartSuggestionsToggled,
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "smart_search_switch"
                )
            }

            item {
                EditMenuItem(
                    icon = Icons.Rounded.Search,
                    label = "Suchverlauf löschen",
                    onClick = onClearSearchHistory,
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled
                )
            }

            item {
                EditSectionHeader(
                    title = "Gesten",
                    mainTextColor = mainTextColor
                )
            }

            item {
                EditToggleItem(
                    icon = Lucide.Smartphone,
                    label = "Shake-Gesten",
                    description = "2× Schütteln, um die gewählte Aktion auszulösen.",
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
                        label = "Bei 2× Schütteln",
                        selectedAction = doubleShakeAction,
                        onActionSelected = onDoubleShakeActionChange,
                        mainTextColor = mainTextColor,
                        designStyle = designStyle,
                        surfaceAccent = surfaceAccent,
                        isDarkTextEnabled = isDarkTextEnabled,
                        testTag = "double_shake_action_selector"
                    )
                }

                if (doubleShakeAction == ShakeAction.OPEN_APP) {
                    item {
                        var showAppPicker by remember { mutableStateOf(false) }
                        val selectedAppLabel = apps.firstOrNull { it.packageName == shakeOpenAppPackage }?.label

                        EditMenuItem(
                            icon = Lucide.LayoutGrid,
                            label = "App wählen",
                            onClick = { showAppPicker = true },
                            statusLabel = selectedAppLabel ?: "Keine",
                            mainTextColor = mainTextColor,
                            designStyle = designStyle,
                            surfaceAccent = surfaceAccent,
                            isDarkTextEnabled = isDarkTextEnabled,
                            testTag = "shake_open_app_picker"
                        )

                        if (showAppPicker) {
                            ShakeAppPickerDialog(
                                apps = apps,
                                selectedPackage = shakeOpenAppPackage,
                                onAppSelected = {
                                    onShakeOpenAppPackageChange(it)
                                    showAppPicker = false
                                },
                                onDismiss = { showAppPicker = false },
                                mainTextColor = mainTextColor,
                                designStyle = designStyle,
                                surfaceAccent = surfaceAccent,
                                isDarkTextEnabled = isDarkTextEnabled
                            )
                        }
                    }
                }
            }

            item {
                EditSectionHeader(
                    title = "Zugriffe",
                    mainTextColor = mainTextColor
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.House,
                    label = "Standard-Launcher",
                    onClick = {
                        openDefaultLauncherSettings(context)
                    },
                    statusLabel = if (isDefaultLauncherSet) "An" else "Aus",
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "default_launcher_item"
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Bell,
                    label = "Benachrichtigung",
                    onClick = {
                        openNotificationSettings(context)
                    },
                    statusLabel = if (isNotificationEnabled) "An" else "Aus",
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Hand,
                    label = "Bedienungshilfen",
                    onClick = {
                        openAccessibilitySettings(context)
                    },
                    statusLabel = if (isAccessibilityEnabled) "An" else "Aus",
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Shield,
                    label = "Nutzungszugriff",
                    onClick = {
                        ForegroundAppResolver.openUsageAccessSettings(context)
                    },
                    statusLabel = if (isUsageAccessEnabled) "An" else "Aus",
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled
                )
            }
        }
    }
}

@Composable
private fun EditSectionHeader(
    title: String,
    mainTextColor: Color
) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = mainTextColor.copy(alpha = 0.7f),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
fun EditMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    mainTextColor: Color,
    designStyle: DesignStyle,
    surfaceAccent: Color,
    isDarkTextEnabled: Boolean,
    statusLabel: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    testTag: String? = null
) {
    val backgroundModifier = Modifier.designSurface(
        designStyle, RoundedCornerShape(20.dp), isDarkTextEnabled, surfaceAccent,
        fillAlpha = 0.05f, glassStartAlpha = 0.10f, glassEndAlpha = 0.03f,
        borderWidth = 1.dp, borderStartAlpha = if (isDarkTextEnabled) 0.2f else 0.25f, borderEndAlpha = 0.05f
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(backgroundModifier)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = mainTextColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, color = mainTextColor, fontSize = 18.sp, fontWeight = FontWeight.Normal, modifier = Modifier.weight(1f))
            if (statusLabel != null) {
                Text(text = statusLabel, color = mainTextColor.copy(alpha = 0.5f), fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp))
            }
            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = mainTextColor.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun EditToggleItem(
    icon: ImageVector,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    mainTextColor: Color,
    designStyle: DesignStyle,
    surfaceAccent: Color,
    isDarkTextEnabled: Boolean,
    switchTestTag: String
) {
    val haptics = com.example.androidlauncher.ui.theme.rememberAppHaptics()
    val toggle: (Boolean) -> Unit = { haptics.toggle(it); onCheckedChange(it) }
    val backgroundModifier = Modifier.designSurface(
        designStyle, RoundedCornerShape(20.dp), isDarkTextEnabled, surfaceAccent,
        fillAlpha = 0.05f, glassStartAlpha = 0.10f, glassEndAlpha = 0.03f,
        borderWidth = 1.dp, borderStartAlpha = if (isDarkTextEnabled) 0.2f else 0.25f, borderEndAlpha = 0.05f
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(backgroundModifier)
            .clickable { toggle(!checked) },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 18.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = mainTextColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = mainTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = mainTextColor.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                modifier = Modifier.testTag(switchTestTag),
                checked = checked,
                onCheckedChange = toggle,
                colors = LiquidGlass.switchColors(isDarkTextEnabled, designStyle.isGlassLike)
            )
        }
    }
}

/** Icon für einen [AppAccessMode] (UI-Mapping; das Enum selbst bleibt rein). */
private fun appAccessModeIcon(mode: AppAccessMode): ImageVector = when (mode) {
    AppAccessMode.DRAWER_GRID -> Lucide.LayoutGrid
    AppAccessMode.DRAWER_LIST -> Lucide.List
    AppAccessMode.HOME_LIST -> Lucide.PanelRight
}

/**
 * Zeilen-Eintrag, der die aktuell gewählte Art des App-Zugriffs anzeigt und bei Klick
 * ein Dropdown mit allen [AppAccessMode]-Optionen öffnet.
 */
@Composable
private fun EditAppAccessSelectorItem(
    label: String,
    selectedMode: AppAccessMode,
    onModeSelected: (AppAccessMode) -> Unit,
    mainTextColor: Color,
    designStyle: DesignStyle,
    surfaceAccent: Color,
    isDarkTextEnabled: Boolean,
    testTag: String
) {
    var expanded by remember { mutableStateOf(false) }

    val backgroundModifier = Modifier.designSurface(
        designStyle, RoundedCornerShape(20.dp), isDarkTextEnabled, surfaceAccent,
        fillAlpha = 0.05f, glassStartAlpha = 0.10f, glassEndAlpha = 0.03f,
        borderWidth = 1.dp, borderStartAlpha = if (isDarkTextEnabled) 0.2f else 0.25f, borderEndAlpha = 0.05f
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(backgroundModifier)
            .testTag(testTag)
            .clickable { expanded = true },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 18.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = appAccessModeIcon(selectedMode),
                contentDescription = null,
                tint = mainTextColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                color = mainTextColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = selectedMode.label,
                color = mainTextColor.copy(alpha = 0.6f),
                fontSize = 15.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Icon(
                imageVector = Lucide.ChevronDown,
                contentDescription = null,
                tint = mainTextColor.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                AppAccessMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.label) },
                        leadingIcon = {
                            Icon(
                                imageVector = appAccessModeIcon(mode),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            expanded = false
                            if (mode != selectedMode) {
                                onModeSelected(mode)
                            }
                        }
                    )
                }
            }
        }
    }
}

/** Icon + Anzeigename für eine [ShakeAction] (UI-Mapping; das Enum selbst bleibt rein). */
private fun shakeActionIcon(action: ShakeAction): ImageVector = when (action) {
    ShakeAction.NONE -> Lucide.Ban
    ShakeAction.FLASHLIGHT -> Lucide.Flashlight
    ShakeAction.CAMERA -> Lucide.Camera
    ShakeAction.OPEN_APP -> Lucide.LayoutGrid
    ShakeAction.LOCK_SCREEN -> Lucide.Lock
    ShakeAction.TOGGLE_DND -> Lucide.BellOff
    ShakeAction.OPEN_SETTINGS -> Lucide.Settings2
}

private fun shakeActionLabel(action: ShakeAction): String = when (action) {
    ShakeAction.NONE -> "Aus"
    ShakeAction.FLASHLIGHT -> "Taschenlampe"
    ShakeAction.CAMERA -> "Kamera"
    ShakeAction.OPEN_APP -> "App öffnen"
    ShakeAction.LOCK_SCREEN -> "Bildschirm sperren"
    ShakeAction.TOGGLE_DND -> "Nicht stören"
    ShakeAction.OPEN_SETTINGS -> "Einstellungen"
}

/**
 * Zeilen-Eintrag, der die aktuell gewählte Aktion einer Shake-Geste anzeigt und bei Klick
 * ein Dropdown mit allen [ShakeAction]-Optionen öffnet.
 */
@Composable
private fun EditActionSelectorItem(
    label: String,
    selectedAction: ShakeAction,
    onActionSelected: (ShakeAction) -> Unit,
    mainTextColor: Color,
    designStyle: DesignStyle,
    surfaceAccent: Color,
    isDarkTextEnabled: Boolean,
    testTag: String
) {
    var expanded by remember { mutableStateOf(false) }

    val backgroundModifier = Modifier.designSurface(
        designStyle, RoundedCornerShape(20.dp), isDarkTextEnabled, surfaceAccent,
        fillAlpha = 0.05f, glassStartAlpha = 0.10f, glassEndAlpha = 0.03f,
        borderWidth = 1.dp, borderStartAlpha = if (isDarkTextEnabled) 0.2f else 0.25f, borderEndAlpha = 0.05f
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(backgroundModifier)
            .testTag(testTag)
            .clickable { expanded = true },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 18.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = shakeActionIcon(selectedAction),
                contentDescription = null,
                tint = mainTextColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                color = mainTextColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = shakeActionLabel(selectedAction),
                color = mainTextColor.copy(alpha = 0.6f),
                fontSize = 15.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Icon(
                imageVector = Lucide.ChevronDown,
                contentDescription = null,
                tint = mainTextColor.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ShakeAction.entries.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(shakeActionLabel(action)) },
                        leadingIcon = {
                            Icon(
                                imageVector = shakeActionIcon(action),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            expanded = false
                            if (action != selectedAction) {
                                onActionSelected(action)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Dialog zum Auswählen der App, die bei [ShakeAction.OPEN_APP] gestartet wird.
 * Bietet ein Suchfeld und eine Liste aller installierten Apps (Single-Select).
 */
@Composable
private fun ShakeAppPickerDialog(
    apps: List<AppInfo>,
    selectedPackage: String?,
    onAppSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    mainTextColor: Color,
    designStyle: DesignStyle,
    surfaceAccent: Color,
    isDarkTextEnabled: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(apps, searchQuery) { LauncherLogic.filterApps(apps, searchQuery) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(24.dp)),
            color = if (isDarkTextEnabled) Color(0xFFF2F2F2) else Color(0xFF1C1C1E)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "App wählen",
                    color = mainTextColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Suchen", color = mainTextColor.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = mainTextColor.copy(alpha = 0.6f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = mainTextColor,
                        unfocusedTextColor = mainTextColor,
                        cursorColor = mainTextColor,
                        focusedBorderColor = mainTextColor.copy(alpha = 0.5f),
                        unfocusedBorderColor = mainTextColor.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items = filteredApps, key = { it.packageName }) { app ->
                        val isSelected = app.packageName == selectedPackage
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .then(
                                    if (isSelected) {
                                        Modifier.designSurface(
                                            designStyle, RoundedCornerShape(16.dp), isDarkTextEnabled, surfaceAccent,
                                            fillAlpha = 0.08f
                                        )
                                    } else {
                                        Modifier.background(Color.Transparent)
                                    }
                                )
                                .clickable { onAppSelected(app.packageName) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconView(app)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = app.label,
                                color = mainTextColor,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = mainTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
