package com.example.androidlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.composables.icons.lucide.Ban
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.BellOff
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.CloudSun
import com.composables.icons.lucide.Flashlight
import com.composables.icons.lucide.Hand
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.List
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PanelRight
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Settings2
import com.composables.icons.lucide.Shield
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Trash2
import com.example.androidlauncher.ForegroundAppResolver
import com.example.androidlauncher.LauncherAccessibilityService
import com.example.androidlauncher.LauncherLogic
import com.example.androidlauncher.R
import com.example.androidlauncher.data.AppAccessMode
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.DesignStyle
import com.example.androidlauncher.data.GestureAction
import com.example.androidlauncher.data.IslandAnimationStyle
import com.example.androidlauncher.isDefaultLauncher
import com.example.androidlauncher.isNotificationServiceEnabled
import com.example.androidlauncher.openAccessibilitySettings
import com.example.androidlauncher.openNotificationSettings
import com.example.androidlauncher.ui.LiquidGlass.designSurface
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle

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
    onOpenAppLock: () -> Unit,
    onOpenDefaultLauncher: () -> Unit,
    onChangeWallpaper: () -> Unit,
    onResetWallpaper: () -> Unit,
    onOpenWallpaperAdjust: () -> Unit,
    isCustomHomeLayoutSet: Boolean,
    isCustomWallpaperSet: Boolean,
    onOpenGesturesConfig: () -> Unit,
    isSmartSuggestionsEnabled: Boolean,
    onSmartSuggestionsToggled: (Boolean) -> Unit,
    isAnimationsEnabled: Boolean,
    onAnimationsToggled: (Boolean) -> Unit,
    onOpenAnimationsConfig: () -> Unit,
    isWeatherWidgetEnabled: Boolean,
    onWeatherWidgetToggled: (Boolean) -> Unit,
    isClockWidgetEnabled: Boolean,
    onClockWidgetToggled: (Boolean) -> Unit,
    isCalendarWidgetEnabled: Boolean,
    onCalendarWidgetToggled: (Boolean) -> Unit,
    isDynamicIslandEnabled: Boolean,
    onDynamicIslandToggled: (Boolean) -> Unit,
    islandAnimationStyle: IslandAnimationStyle,
    onIslandAnimationStyleChange: (IslandAnimationStyle) -> Unit,
    isEdgeLightingEnabled: Boolean,
    onOpenEdgeLightingConfig: () -> Unit,
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
    var isAccessibilityEnabled by remember {
        mutableStateOf(
            LauncherAccessibilityService.isAccessibilityServiceEnabled(context)
        )
    }
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
                stringResource(R.string.edit_config_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = mainTextColor
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close), tint = mainTextColor)
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
                    title = stringResource(R.string.section_general),
                    mainTextColor = mainTextColor
                )
            }

            item {
                EditToggleItem(
                    icon = Lucide.Smartphone,
                    label = stringResource(R.string.haptic_feedback),
                    description = stringResource(R.string.haptic_feedback_desc),
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
                EditMenuItem(
                    icon = Lucide.Smartphone,
                    label = stringResource(R.string.label_animations),
                    onClick = onOpenAnimationsConfig,
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    statusLabel = if (isAnimationsEnabled) null else stringResource(R.string.status_off),
                    testTag = "animations_menu_item"
                )
            }

            item {
                EditToggleItem(
                    icon = Lucide.Clock,
                    label = stringResource(R.string.clock_widget),
                    description = stringResource(R.string.clock_widget_desc),
                    checked = isClockWidgetEnabled,
                    onCheckedChange = { onClockWidgetToggled(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "clock_widget_switch"
                )
            }

            item {
                EditToggleItem(
                    icon = Lucide.Calendar,
                    label = stringResource(R.string.calendar_widget),
                    description = stringResource(R.string.calendar_widget_desc),
                    checked = isCalendarWidgetEnabled,
                    onCheckedChange = { onCalendarWidgetToggled(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "calendar_widget_switch"
                )
            }

            item {
                EditToggleItem(
                    icon = Lucide.CloudSun,
                    label = stringResource(R.string.weather_widget),
                    description = stringResource(R.string.weather_widget_desc),
                    checked = isWeatherWidgetEnabled,
                    onCheckedChange = { onWeatherWidgetToggled(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "weather_widget_switch"
                )
            }

            item {
                EditToggleItem(
                    icon = Lucide.Bell,
                    label = stringResource(R.string.dynamic_island),
                    description = stringResource(R.string.dynamic_island_desc),
                    checked = isDynamicIslandEnabled,
                    onCheckedChange = { onDynamicIslandToggled(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "dynamic_island_switch"
                )
            }

            if (isDynamicIslandEnabled) {
                item {
                    EditIslandAnimationSelectorItem(
                        label = stringResource(R.string.island_anim_style),
                        selectedStyle = islandAnimationStyle,
                        onStyleSelected = onIslandAnimationStyleChange,
                        mainTextColor = mainTextColor,
                        designStyle = designStyle,
                        surfaceAccent = surfaceAccent,
                        isDarkTextEnabled = isDarkTextEnabled,
                        testTag = "island_animation_selector"
                    )
                }
            }

            item {
                EditMenuItem(
                    icon = Lucide.Sparkles,
                    label = stringResource(R.string.edge_lighting),
                    onClick = onOpenEdgeLightingConfig,
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    statusLabel = if (isEdgeLightingEnabled) null else stringResource(R.string.status_off),
                    testTag = "edge_lighting_menu_item"
                )
            }

            item {
                EditAppAccessSelectorItem(
                    label = stringResource(R.string.app_access_label),
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
                    label = stringResource(R.string.edit_home_layout),
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
                                    contentDescription = stringResource(R.string.cd_reset_home_layout),
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
                    label = stringResource(R.string.edit_app_icons),
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
                    label = stringResource(R.string.change_wallpaper),
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
                                    contentDescription = stringResource(R.string.cd_remove_wallpaper),
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
                        label = stringResource(R.string.adjust_wallpaper),
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
                    title = stringResource(R.string.section_apps),
                    mainTextColor = mainTextColor
                )
            }

            item {
                EditMenuItem(
                    icon = Icons.Rounded.VisibilityOff,
                    label = stringResource(R.string.hide_apps),
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
                    icon = Icons.Rounded.Lock,
                    label = stringResource(R.string.app_lock),
                    onClick = onOpenAppLock,
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "app_lock_item"
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Trash2,
                    label = stringResource(R.string.uninstall_apps),
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
                    title = stringResource(R.string.section_search),
                    mainTextColor = mainTextColor
                )
            }

            item {
                EditToggleItem(
                    icon = Icons.Rounded.Search,
                    label = stringResource(R.string.smart_suggestions),
                    description = stringResource(R.string.smart_suggestions_desc),
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
                    icon = Lucide.Trash2,
                    label = stringResource(R.string.clear_search_history),
                    onClick = onClearSearchHistory,
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled
                )
            }

            item {
                EditSectionHeader(
                    title = stringResource(R.string.label_gestures),
                    mainTextColor = mainTextColor
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Hand,
                    label = stringResource(R.string.label_gestures),
                    onClick = onOpenGesturesConfig,
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "gestures_menu_item"
                )
            }

            item {
                EditSectionHeader(
                    title = stringResource(R.string.section_permissions),
                    mainTextColor = mainTextColor
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.House,
                    label = stringResource(R.string.default_launcher),
                    onClick = onOpenDefaultLauncher,
                    statusLabel = if (isDefaultLauncherSet) {
                        stringResource(
                            R.string.status_on
                        )
                    } else {
                        stringResource(R.string.status_off)
                    },
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
                    label = stringResource(R.string.notifications_label),
                    onClick = {
                        openNotificationSettings(context)
                    },
                    statusLabel = if (isNotificationEnabled) {
                        stringResource(
                            R.string.status_on
                        )
                    } else {
                        stringResource(R.string.status_off)
                    },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Hand,
                    label = stringResource(R.string.accessibility_label),
                    onClick = {
                        openAccessibilitySettings(context)
                    },
                    statusLabel = if (isAccessibilityEnabled) {
                        stringResource(
                            R.string.status_on
                        )
                    } else {
                        stringResource(R.string.status_off)
                    },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Shield,
                    label = stringResource(R.string.usage_access_label),
                    onClick = {
                        ForegroundAppResolver.openUsageAccessSettings(context)
                    },
                    statusLabel = if (isUsageAccessEnabled) {
                        stringResource(
                            R.string.status_on
                        )
                    } else {
                        stringResource(R.string.status_off)
                    },
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
            Text(
                text = label,
                color = mainTextColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (statusLabel != null) {
                Text(
                    text = statusLabel,
                    color = mainTextColor.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = mainTextColor.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun EditToggleItem(
    icon: ImageVector,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    mainTextColor: Color,
    designStyle: DesignStyle,
    surfaceAccent: Color,
    isDarkTextEnabled: Boolean,
    switchTestTag: String,
    enabled: Boolean = true
) {
    val haptics = com.example.androidlauncher.ui.theme.rememberAppHaptics()
    val toggle: (Boolean) -> Unit = {
        haptics.toggle(it)
        onCheckedChange(it)
    }
    // Ausgegraut, wenn deaktiviert (z. B. wenn der Master-Schalter aus ist).
    val contentColor = if (enabled) mainTextColor else mainTextColor.copy(alpha = 0.35f)
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
            .clickable(enabled = enabled) { toggle(!checked) },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 18.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = contentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = contentColor.copy(alpha = if (enabled) 0.6f else 0.35f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                modifier = Modifier.testTag(switchTestTag),
                checked = checked,
                onCheckedChange = toggle,
                enabled = enabled,
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
                text = stringResource(selectedMode.labelRes),
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
                        text = { Text(stringResource(mode.labelRes)) },
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

/** Dropdown-Auswahl des Insel-Öffnungs-/Schließstils ([IslandAnimationStyle]). */
@Composable
private fun EditIslandAnimationSelectorItem(
    label: String,
    selectedStyle: IslandAnimationStyle,
    onStyleSelected: (IslandAnimationStyle) -> Unit,
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
                imageVector = Lucide.Sparkles,
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
                text = stringResource(selectedStyle.labelRes),
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
                IslandAnimationStyle.entries.forEach { style ->
                    DropdownMenuItem(
                        text = { Text(stringResource(style.labelRes)) },
                        onClick = {
                            expanded = false
                            if (style != selectedStyle) {
                                onStyleSelected(style)
                            }
                        }
                    )
                }
            }
        }
    }
}

/** Icon + Anzeigename für eine [GestureAction] (UI-Mapping; das Enum selbst bleibt rein). */
internal fun gestureActionIcon(action: GestureAction): ImageVector = when (action) {
    GestureAction.NONE -> Lucide.Ban
    GestureAction.APP_DRAWER -> Lucide.LayoutGrid
    GestureAction.SEARCH -> Lucide.Search
    GestureAction.NOTIFICATIONS -> Lucide.Bell
    GestureAction.FLASHLIGHT -> Lucide.Flashlight
    GestureAction.CAMERA -> Lucide.Camera
    GestureAction.OPEN_APP -> Lucide.Smartphone
    GestureAction.LOCK_SCREEN -> Lucide.Lock
    GestureAction.TOGGLE_DND -> Lucide.BellOff
    GestureAction.OPEN_SETTINGS -> Lucide.Settings2
}

@Composable
internal fun gestureActionLabel(action: GestureAction): String = stringResource(
    when (action) {
        GestureAction.NONE -> R.string.gesture_action_none
        GestureAction.APP_DRAWER -> R.string.gesture_action_app_drawer
        GestureAction.SEARCH -> R.string.gesture_action_search
        GestureAction.NOTIFICATIONS -> R.string.gesture_action_notifications
        GestureAction.FLASHLIGHT -> R.string.gesture_action_flashlight
        GestureAction.CAMERA -> R.string.gesture_action_camera
        GestureAction.OPEN_APP -> R.string.gesture_action_open_app
        GestureAction.LOCK_SCREEN -> R.string.gesture_action_lock_screen
        GestureAction.TOGGLE_DND -> R.string.gesture_action_toggle_dnd
        GestureAction.OPEN_SETTINGS -> R.string.gesture_action_open_settings
    }
)

/**
 * Zeilen-Eintrag, der die aktuell gewählte Aktion einer Geste anzeigt und bei Klick
 * ein Dropdown mit allen [GestureAction]-Optionen öffnet.
 */
@Composable
internal fun EditActionSelectorItem(
    label: String,
    selectedAction: GestureAction,
    onActionSelected: (GestureAction) -> Unit,
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
                imageVector = gestureActionIcon(selectedAction),
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
                text = gestureActionLabel(selectedAction),
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
                GestureAction.entries.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(gestureActionLabel(action)) },
                        leadingIcon = {
                            Icon(
                                imageVector = gestureActionIcon(action),
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
 * Dialog zum Auswählen der App, die bei [GestureAction.OPEN_APP] gestartet wird.
 * Bietet ein Suchfeld und eine Liste aller installierten Apps (Single-Select).
 */
@Composable
internal fun GestureAppPickerDialog(
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
            color = surfaceAccent
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.choose_app),
                    color = mainTextColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search), color = mainTextColor.copy(alpha = 0.5f)) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = mainTextColor.copy(alpha = 0.6f)
                        )
                    },
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
                                            designStyle,
                                            RoundedCornerShape(16.dp),
                                            isDarkTextEnabled,
                                            surfaceAccent,
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
