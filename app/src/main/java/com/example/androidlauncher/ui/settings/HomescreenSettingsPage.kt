package com.example.androidlauncher.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.CloudSun
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Sparkles
import com.example.androidlauncher.R
import com.example.androidlauncher.data.IslandAnimationStyle
import com.example.androidlauncher.ui.EditIslandAnimationSelectorItem
import com.example.androidlauncher.ui.EditMenuItem
import com.example.androidlauncher.ui.EditToggleItem
import com.example.androidlauncher.ui.home.ActiveOverlay
import com.example.androidlauncher.ui.home.EditConfigViewModel
import com.example.androidlauncher.ui.home.HomeViewModel
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle

/**
 * Kategorie-Seite „Startbildschirm" des Einstellungs-Hubs: alles, was direkt auf
 * der Startseite sichtbar ist (Favoriten, Layout, Widgets, Dynamic Island,
 * Edge-Beleuchtung).
 */
@Composable
fun HomescreenSettingsPage() {
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val editViewModel: EditConfigViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    val isCustomHomeLayoutSet by editViewModel.isCustomHomeLayoutSet.collectAsState(initial = false)
    val isClockWidgetEnabled by editViewModel.isClockWidgetEnabled.collectAsState(initial = true)
    val isCalendarWidgetEnabled by editViewModel.isCalendarWidgetEnabled.collectAsState(initial = true)
    val isWeatherWidgetEnabled by editViewModel.isWeatherWidgetEnabled.collectAsState(initial = true)
    val isDynamicIslandEnabled by editViewModel.isDynamicIslandEnabled.collectAsState(initial = true)
    val islandAnimationStyle by editViewModel.islandAnimationStyle
        .collectAsState(initial = IslandAnimationStyle.FROM_NOTCH)
    val isEdgeLightingEnabled by editViewModel.isEdgeLightingEnabled.collectAsState(initial = false)

    // One-Shot-Highlight eines Such-Treffers: die gefundene Zeile pulsiert kurz.
    var highlightKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { highlightKey = homeViewModel.consumePendingSettingsHighlight() }

    val onOpenHomeLayoutEdit = {
        homeViewModel.closeOverlay()
        homeViewModel.setHomeEditMode(true)
    }
    val onResetHomeLayout = {
        editViewModel.resetHomeLayout()
        Toast.makeText(context, context.getString(R.string.home_layout_reset), Toast.LENGTH_SHORT).show()
    }

    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val designStyle = LocalDesignStyle.current
    val surfaceAccent = LocalColorTheme.current.menuSurfaceColor(isDarkTextEnabled)
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White

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
                stringResource(R.string.section_homescreen),
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = mainTextColor
            )
            IconButton(onClick = { homeViewModel.closeOverlay() }) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close), tint = mainTextColor)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 32.dp, bottom = 8.dp)
        ) {
            item {
                EditMenuItem(
                    icon = Icons.Rounded.Star,
                    label = stringResource(R.string.favorites_title),
                    onClick = { homeViewModel.openOverlay(ActiveOverlay.FavoritesConfig) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "homescreen_favorites_item",
                    highlighted = highlightKey == "favorites"
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
                    highlighted = highlightKey == "home_layout",
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
                    onClick = { homeViewModel.openOverlay(ActiveOverlay.WidgetPicker) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "add_widget_item",
                    highlighted = highlightKey == "add_widget"
                )
            }

            item {
                EditToggleItem(
                    icon = Lucide.Clock,
                    label = stringResource(R.string.clock_widget),
                    description = stringResource(R.string.clock_widget_desc),
                    checked = isClockWidgetEnabled,
                    onCheckedChange = { editViewModel.setClockWidgetEnabled(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "clock_widget_switch",
                    highlighted = highlightKey == "clock_widget"
                )
            }

            item {
                EditToggleItem(
                    icon = Lucide.Calendar,
                    label = stringResource(R.string.calendar_widget),
                    description = stringResource(R.string.calendar_widget_desc),
                    checked = isCalendarWidgetEnabled,
                    onCheckedChange = { editViewModel.setCalendarWidgetEnabled(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "calendar_widget_switch",
                    highlighted = highlightKey == "calendar_widget"
                )
            }

            item {
                EditToggleItem(
                    icon = Lucide.CloudSun,
                    label = stringResource(R.string.weather_widget),
                    description = stringResource(R.string.weather_widget_desc),
                    checked = isWeatherWidgetEnabled,
                    onCheckedChange = { editViewModel.setWeatherWidgetEnabled(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "weather_widget_switch",
                    highlighted = highlightKey == "weather_widget"
                )
            }

            item {
                EditToggleItem(
                    icon = Lucide.Bell,
                    label = stringResource(R.string.dynamic_island),
                    description = stringResource(R.string.dynamic_island_desc),
                    checked = isDynamicIslandEnabled,
                    onCheckedChange = { editViewModel.setDynamicIslandEnabled(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "dynamic_island_switch",
                    highlighted = highlightKey == "dynamic_island"
                )
            }

            if (isDynamicIslandEnabled) {
                item {
                    EditIslandAnimationSelectorItem(
                        label = stringResource(R.string.island_anim_style),
                        selectedStyle = islandAnimationStyle,
                        onStyleSelected = { editViewModel.setIslandAnimationStyle(it) },
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
                    onClick = { homeViewModel.openOverlay(ActiveOverlay.EdgeLightingConfig) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    statusLabel = if (isEdgeLightingEnabled) null else stringResource(R.string.status_off),
                    testTag = "edge_lighting_menu_item",
                    highlighted = highlightKey == "edge_lighting"
                )
            }
        }
    }
}
