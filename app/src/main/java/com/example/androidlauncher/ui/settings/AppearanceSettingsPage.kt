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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ALargeSmall
import com.composables.icons.lucide.Droplet
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Settings2
import com.composables.icons.lucide.Smartphone
import com.example.androidlauncher.R
import com.example.androidlauncher.ui.EditMenuItem
import com.example.androidlauncher.ui.home.ActiveOverlay
import com.example.androidlauncher.ui.home.EditConfigActions
import com.example.androidlauncher.ui.home.EditConfigViewModel
import com.example.androidlauncher.ui.home.HomeViewModel
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle

/**
 * Kategorie-Seite „Aussehen" des Einstellungs-Hubs: bündelt alle visuellen
 * Einstellungen (Themen, Farben, Design-Stil, Schrift, Icons, Wallpaper,
 * Animationen), die vorher über Farben-Menü und „Allgemein" verstreut waren.
 */
@Composable
fun AppearanceSettingsPage(
    actions: EditConfigActions
) {
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val editViewModel: EditConfigViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    val isCustomWallpaperSet by editViewModel.isCustomWallpaperSet.collectAsState(initial = false)
    val isAnimationsEnabled by editViewModel.isAnimationsEnabled.collectAsState(initial = true)

    val onResetWallpaper = {
        editViewModel.resetWallpaper()
        Toast.makeText(context, context.getString(R.string.wallpaper_removed), Toast.LENGTH_SHORT).show()
    }
    val onChangeWallpaper = {
        homeViewModel.closeOverlay()
        actions.pickWallpaper()
    }

    val selectedTheme = LocalColorTheme.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val designStyle = LocalDesignStyle.current
    val surfaceAccent = selectedTheme.menuSurfaceColor(isDarkTextEnabled)
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
                stringResource(R.string.section_appearance),
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
                    icon = Lucide.Palette,
                    label = stringResource(R.string.label_themes),
                    onClick = { homeViewModel.openOverlay(ActiveOverlay.ThemeMenu) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    statusLabel = selectedTheme.themeNameRes?.let { stringResource(it) } ?: selectedTheme.themeName,
                    testTag = "appearance_themes_item"
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Droplet,
                    label = stringResource(R.string.color_config_title),
                    onClick = { homeViewModel.openOverlay(ActiveOverlay.ColorConfig) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "appearance_colors_item"
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Layers,
                    label = stringResource(R.string.label_design_style),
                    onClick = { homeViewModel.openOverlay(ActiveOverlay.DesignMenu) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    statusLabel = stringResource(designStyle.titleRes),
                    testTag = "appearance_design_item"
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.ALargeSmall,
                    label = stringResource(R.string.size_config_title),
                    onClick = { homeViewModel.openOverlay(ActiveOverlay.SizeConfig) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "appearance_font_size_item"
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Settings2,
                    label = stringResource(R.string.edit_app_icons),
                    onClick = { homeViewModel.openOverlay(ActiveOverlay.IconConfig) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "appearance_app_icons_item"
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
                    testTag = "appearance_wallpaper_item",
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
                        onClick = { homeViewModel.openOverlay(ActiveOverlay.WallpaperConfig) },
                        mainTextColor = mainTextColor,
                        designStyle = designStyle,
                        surfaceAccent = surfaceAccent,
                        isDarkTextEnabled = isDarkTextEnabled,
                        testTag = "appearance_adjust_wallpaper_item"
                    )
                }
            }

            item {
                EditMenuItem(
                    icon = Lucide.Smartphone,
                    label = stringResource(R.string.label_animations),
                    onClick = { homeViewModel.openOverlay(ActiveOverlay.AnimationsConfig) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    statusLabel = if (isAnimationsEnabled) null else stringResource(R.string.status_off),
                    testTag = "appearance_animations_item"
                )
            }
        }
    }
}
