package com.example.androidlauncher.ui

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Hand
import com.composables.icons.lucide.House
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Settings2
import com.example.androidlauncher.R
import com.example.androidlauncher.ui.home.ActiveOverlay
import com.example.androidlauncher.ui.home.HomeViewModel
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle

/**
 * Einstellungs-Hub: kurze Startseite mit einer Kategorie-Zeile pro Themenbereich
 * (Aussehen, Startbildschirm, Apps, Suche, Gesten, System). Jede Zeile nennt im
 * Untertitel die enthaltenen Einstellungen, damit auf einen Blick klar ist, wo
 * was liegt. Die eigentlichen Einstellungen leben auf den Kategorie-Seiten
 * unter ui/settings/.
 */
@Composable
fun EditConfigMenu() {
    val homeViewModel: HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val designStyle = LocalDesignStyle.current
    val surfaceAccent = LocalColorTheme.current.menuSurfaceColor(isDarkTextEnabled)
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val menuListState = rememberLazyListState()

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
                stringResource(R.string.settings_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = mainTextColor
            )
            IconButton(onClick = { homeViewModel.closeOverlay() }) {
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
                SettingsCategoryItem(
                    icon = Lucide.Palette,
                    title = stringResource(R.string.section_appearance),
                    subtitle = stringResource(R.string.category_sub_appearance),
                    onClick = { homeViewModel.openOverlay(ActiveOverlay.AppearanceSettings) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "category_appearance_item"
                )
            }

            item {
                SettingsCategoryItem(
                    icon = Lucide.House,
                    title = stringResource(R.string.section_homescreen),
                    subtitle = stringResource(R.string.category_sub_homescreen),
                    onClick = { homeViewModel.openOverlay(ActiveOverlay.HomescreenSettings) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "category_homescreen_item"
                )
            }

            item {
                SettingsCategoryItem(
                    icon = Lucide.LayoutGrid,
                    title = stringResource(R.string.section_apps),
                    subtitle = stringResource(R.string.category_sub_apps),
                    onClick = { homeViewModel.openOverlay(ActiveOverlay.AppsSettings) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "category_apps_item"
                )
            }

            item {
                SettingsCategoryItem(
                    icon = Icons.Rounded.Search,
                    title = stringResource(R.string.section_search),
                    subtitle = stringResource(R.string.category_sub_search),
                    onClick = { homeViewModel.openOverlay(ActiveOverlay.SearchSettings) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "category_search_item"
                )
            }

            item {
                SettingsCategoryItem(
                    icon = Lucide.Hand,
                    title = stringResource(R.string.label_gestures),
                    subtitle = stringResource(R.string.category_sub_gestures),
                    onClick = { homeViewModel.openOverlay(ActiveOverlay.GesturesConfig) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "category_gestures_item"
                )
            }

            item {
                SettingsCategoryItem(
                    icon = Lucide.Settings2,
                    title = stringResource(R.string.section_system),
                    subtitle = stringResource(R.string.category_sub_system),
                    onClick = { homeViewModel.openOverlay(ActiveOverlay.SystemSettings) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "category_system_item"
                )
            }
        }
    }
}
