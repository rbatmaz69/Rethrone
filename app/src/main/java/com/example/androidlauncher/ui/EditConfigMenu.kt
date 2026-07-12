package com.example.androidlauncher.ui

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.CloudSun
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Hand
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Settings2
import com.composables.icons.lucide.Shield
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Upload
import com.example.androidlauncher.ForegroundAppResolver
import com.example.androidlauncher.LauncherAccessibilityService
import com.example.androidlauncher.R
import com.example.androidlauncher.data.AppAccessMode
import com.example.androidlauncher.data.IslandAnimationStyle
import com.example.androidlauncher.isDefaultLauncher
import com.example.androidlauncher.isNotificationServiceEnabled
import com.example.androidlauncher.openAccessibilitySettings
import com.example.androidlauncher.openNotificationSettings
import com.example.androidlauncher.ui.home.ActiveOverlay
import com.example.androidlauncher.ui.home.EditConfigActions
import com.example.androidlauncher.ui.home.EditConfigViewModel
import com.example.androidlauncher.ui.home.HomeViewModel
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle

/**
 * Konfigurationsmenü für Anpassungen.
 */
@Composable
fun EditConfigMenu(
    actions: EditConfigActions
) {
    val context = LocalContext.current

    // A8-Split: Overlay-Navigation direkt über das HomeViewModel (gleiche Activity-
    // Instanz wie in der MainActivity), Einstellungen über das EditConfigViewModel
    // aus den A1-Settings-Stores. Die abgeleiteten Locals tragen die früheren
    // Parameternamen, damit der Menü-Body unverändert bleibt.
    val homeViewModel: HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val editViewModel: EditConfigViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    val onClose = { homeViewModel.closeOverlay() }
    val onOpenHomeLayoutEdit = {
        homeViewModel.closeOverlay()
        homeViewModel.setHomeEditMode(true)
    }
    val onOpenWidgetPicker = { homeViewModel.openOverlay(ActiveOverlay.WidgetPicker) }
    val onOpenIconConfig = { homeViewModel.openOverlay(ActiveOverlay.IconConfig) }
    val onOpenUninstallApps = { homeViewModel.openOverlay(ActiveOverlay.UninstallApps) }
    val onOpenHiddenApps = { homeViewModel.openOverlay(ActiveOverlay.HiddenApps) }
    val onOpenAppLock = { homeViewModel.openOverlay(ActiveOverlay.AppLock) }
    val onOpenWallpaperAdjust = { homeViewModel.openOverlay(ActiveOverlay.WallpaperConfig) }
    val onOpenGesturesConfig = { homeViewModel.openOverlay(ActiveOverlay.GesturesConfig) }
    val onOpenAnimationsConfig = { homeViewModel.openOverlay(ActiveOverlay.AnimationsConfig) }
    val onOpenEdgeLightingConfig = { homeViewModel.openOverlay(ActiveOverlay.EdgeLightingConfig) }
    val onOpenDefaultLauncher = { actions.openDefaultLauncherPrompt() }
    val onChangeWallpaper = {
        homeViewModel.closeOverlay()
        actions.pickWallpaper()
    }

    val isCustomHomeLayoutSet by editViewModel.isCustomHomeLayoutSet.collectAsState(initial = false)
    val isCustomWallpaperSet by editViewModel.isCustomWallpaperSet.collectAsState(initial = false)
    val isSmartSuggestionsEnabled by editViewModel.isSmartSuggestionsEnabled.collectAsState(initial = true)
    val isAnimationsEnabled by editViewModel.isAnimationsEnabled.collectAsState(initial = true)
    val isWeatherWidgetEnabled by editViewModel.isWeatherWidgetEnabled.collectAsState(initial = true)
    val isClockWidgetEnabled by editViewModel.isClockWidgetEnabled.collectAsState(initial = true)
    val isCalendarWidgetEnabled by editViewModel.isCalendarWidgetEnabled.collectAsState(initial = true)
    val isDynamicIslandEnabled by editViewModel.isDynamicIslandEnabled.collectAsState(initial = true)
    val islandAnimationStyle by editViewModel.islandAnimationStyle
        .collectAsState(initial = IslandAnimationStyle.FROM_NOTCH)
    val isEdgeLightingEnabled by editViewModel.isEdgeLightingEnabled.collectAsState(initial = false)
    val appAccessMode by editViewModel.appAccessMode.collectAsState(initial = AppAccessMode.DRAWER_LIST)
    val isHapticFeedbackEnabled by editViewModel.isHapticFeedbackEnabled.collectAsState(initial = true)

    val onSmartSuggestionsToggled: (Boolean) -> Unit = { editViewModel.setSmartSuggestionsEnabled(it) }
    val onWeatherWidgetToggled: (Boolean) -> Unit = { editViewModel.setWeatherWidgetEnabled(it) }
    val onClockWidgetToggled: (Boolean) -> Unit = { editViewModel.setClockWidgetEnabled(it) }
    val onCalendarWidgetToggled: (Boolean) -> Unit = { editViewModel.setCalendarWidgetEnabled(it) }
    val onDynamicIslandToggled: (Boolean) -> Unit = { editViewModel.setDynamicIslandEnabled(it) }
    val onIslandAnimationStyleChange: (IslandAnimationStyle) -> Unit =
        { editViewModel.setIslandAnimationStyle(it) }
    val onAppAccessModeChange: (AppAccessMode) -> Unit = { editViewModel.setAppAccessMode(it) }
    val onResetHomeLayout = {
        editViewModel.resetHomeLayout()
        Toast.makeText(context, context.getString(R.string.home_layout_reset), Toast.LENGTH_SHORT).show()
    }
    val onResetWallpaper = {
        editViewModel.resetWallpaper()
        Toast.makeText(context, context.getString(R.string.wallpaper_removed), Toast.LENGTH_SHORT).show()
    }
    val onClearSearchHistory = {
        editViewModel.clearSearchHistory()
        Toast.makeText(context, context.getString(R.string.search_history_cleared), Toast.LENGTH_SHORT).show()
    }
    // Haptik schreibt zusätzlich das System-Setting → ohne WRITE_SETTINGS-Berechtigung
    // stattdessen den System-Dialog öffnen (Verhalten wie zuvor in der MainActivity).
    val onHapticFeedbackToggled: (Boolean) -> Unit = { enabled ->
        if (Settings.System.canWrite(context)) {
            editViewModel.setHapticFeedbackEnabled(enabled)
        } else {
            val writeSettingsIntent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = ("package:" + context.packageName).toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(writeSettingsIntent)
            Toast.makeText(
                context,
                context.getString(R.string.write_settings_permission_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }

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
                    icon = Lucide.LayoutGrid,
                    label = stringResource(R.string.add_widget),
                    onClick = onOpenWidgetPicker,
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "add_widget_item"
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
                    title = stringResource(R.string.section_backup),
                    mainTextColor = mainTextColor
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Download,
                    label = stringResource(R.string.backup_export),
                    onClick = { actions.exportBackup() },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "backup_export_item"
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Upload,
                    label = stringResource(R.string.backup_import),
                    onClick = { actions.importBackup() },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "backup_import_item"
                )
            }

            // Klartext-Hinweis: Exporte enthalten u. a. die Liste der ausgeblendeten
            // Apps unverschlüsselt (gerätegebundener Ciphertext wäre im Backup nutzlos).
            item {
                Text(
                    text = stringResource(R.string.backup_plaintext_hint),
                    fontSize = 12.sp,
                    color = mainTextColor.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 4.dp)
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
