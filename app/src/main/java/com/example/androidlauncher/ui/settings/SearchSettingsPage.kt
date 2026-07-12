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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.example.androidlauncher.R
import com.example.androidlauncher.ui.EditMenuItem
import com.example.androidlauncher.ui.EditToggleItem
import com.example.androidlauncher.ui.home.EditConfigViewModel
import com.example.androidlauncher.ui.home.HomeViewModel
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle

/**
 * Kategorie-Seite „Suche" des Einstellungs-Hubs: intelligente Vorschläge und
 * das Löschen des Suchverlaufs.
 */
@Composable
fun SearchSettingsPage() {
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val editViewModel: EditConfigViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    val isSmartSuggestionsEnabled by editViewModel.isSmartSuggestionsEnabled.collectAsState(initial = true)

    // One-Shot-Highlight eines Such-Treffers: die gefundene Zeile pulsiert kurz.
    var highlightKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { highlightKey = homeViewModel.consumePendingSettingsHighlight() }

    val onClearSearchHistory = {
        editViewModel.clearSearchHistory()
        Toast.makeText(context, context.getString(R.string.search_history_cleared), Toast.LENGTH_SHORT).show()
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
                stringResource(R.string.section_search),
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
                EditToggleItem(
                    icon = Icons.Rounded.Search,
                    label = stringResource(R.string.smart_suggestions),
                    description = stringResource(R.string.smart_suggestions_desc),
                    checked = isSmartSuggestionsEnabled,
                    onCheckedChange = { editViewModel.setSmartSuggestionsEnabled(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "smart_search_switch",
                    highlighted = highlightKey == "smart_suggestions"
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
                    isDarkTextEnabled = isDarkTextEnabled,
                    highlighted = highlightKey == "clear_history"
                )
            }
        }
    }
}
