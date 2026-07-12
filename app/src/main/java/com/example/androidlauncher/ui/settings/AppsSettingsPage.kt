package com.example.androidlauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.example.androidlauncher.R
import com.example.androidlauncher.data.AppAccessMode
import com.example.androidlauncher.ui.EditAppAccessSelectorItem
import com.example.androidlauncher.ui.EditMenuItem
import com.example.androidlauncher.ui.home.ActiveOverlay
import com.example.androidlauncher.ui.home.EditConfigViewModel
import com.example.androidlauncher.ui.home.HomeViewModel
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle

/**
 * Kategorie-Seite „Apps" des Einstellungs-Hubs: Zugriffsart auf die App-Liste
 * sowie Ausblenden, Sperren und Deinstallieren einzelner Apps.
 */
@Composable
fun AppsSettingsPage() {
    val homeViewModel: HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val editViewModel: EditConfigViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    val appAccessMode by editViewModel.appAccessMode.collectAsState(initial = AppAccessMode.DRAWER_LIST)

    // One-Shot-Highlight eines Such-Treffers: die gefundene Zeile pulsiert kurz.
    var highlightKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { highlightKey = homeViewModel.consumePendingSettingsHighlight() }

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
                stringResource(R.string.section_apps),
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
                EditAppAccessSelectorItem(
                    label = stringResource(R.string.app_access_label),
                    selectedMode = appAccessMode,
                    onModeSelected = { editViewModel.setAppAccessMode(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "app_access_mode_selector",
                    highlighted = highlightKey == "app_access"
                )
            }

            item {
                EditMenuItem(
                    icon = Icons.Rounded.VisibilityOff,
                    label = stringResource(R.string.hide_apps),
                    onClick = { homeViewModel.openOverlay(ActiveOverlay.HiddenApps) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "hidden_apps_item",
                    highlighted = highlightKey == "hidden_apps"
                )
            }

            item {
                EditMenuItem(
                    icon = Icons.Rounded.Lock,
                    label = stringResource(R.string.app_lock),
                    onClick = { homeViewModel.openOverlay(ActiveOverlay.AppLock) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "app_lock_item",
                    highlighted = highlightKey == "app_lock"
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Trash2,
                    label = stringResource(R.string.uninstall_apps),
                    onClick = { homeViewModel.openOverlay(ActiveOverlay.UninstallApps) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "uninstall_apps_item",
                    highlighted = highlightKey == "uninstall_apps"
                )
            }
        }
    }
}
