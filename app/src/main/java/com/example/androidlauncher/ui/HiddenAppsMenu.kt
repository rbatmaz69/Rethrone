package com.example.androidlauncher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.R
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.DesignStyle
import com.example.androidlauncher.ui.LiquidGlass.designSurface
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle
import com.example.androidlauncher.ui.theme.LocalFontWeight

/**
 * Menü zum Aus-/Einblenden von Apps. Ausgeblendete Apps (angehakt) werden überall in der
 * Anzeige (Drawer, Startseiten-Liste, Suche, Favoriten) herausgefiltert – die Liste hier
 * zeigt jedoch ALLE Apps, damit man sie wieder einblenden kann.
 *
 * @param apps Alle bekannten Apps.
 * @param hiddenPackages Aktuell ausgeblendete Paketnamen.
 * @param onToggleHidden Wird beim Antippen mit dem Paketnamen aufgerufen (persistiert sofort).
 */
@Composable
fun HiddenAppsMenu(
    apps: List<AppInfo>,
    hiddenPackages: Set<String>,
    onToggleHidden: (String) -> Unit,
    onClose: () -> Unit
) {
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val designStyle = LocalDesignStyle.current
    val surfaceAccent = LocalColorTheme.current.menuSurfaceColor(isDarkTextEnabled)
    val accentColor = LocalColorTheme.current.accentColor(isDarkTextEnabled)
    val fontWeight = LocalFontWeight.current
    val mainTextColor = LiquidGlass.mainTextColor(isDarkTextEnabled)

    var searchQuery by remember { mutableStateOf("") }
    val sortedApps = remember(apps) { apps.sortedBy { it.label.lowercase() } }
    val filteredApps = remember(sortedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            sortedApps
        } else {
            sortedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("hidden_apps_menu")
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
                stringResource(R.string.hide_apps),
                fontSize = 28.sp,
                fontWeight = fontWeight.weight,
                color = mainTextColor
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close), tint = mainTextColor)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.hidden_apps_desc),
            color = mainTextColor.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        val searchBarModifier = Modifier.designSurface(
            designStyle, RoundedCornerShape(16.dp), isDarkTextEnabled, surfaceAccent,
            fillAlpha = 0.1f, glassStartAlpha = 0.10f, glassEndAlpha = 0.03f,
            borderWidth = 1.dp, borderStartAlpha = 0.2f, borderEndAlpha = 0.05f
        )
        Box(modifier = Modifier.fillMaxWidth().then(searchBarModifier).padding(horizontal = 16.dp, vertical = 12.dp)) {
            StableSearchFieldContent(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = stringResource(R.string.search_apps),
                textStyle = androidx.compose.ui.text.TextStyle(color = mainTextColor, fontSize = 16.sp),
                textColor = mainTextColor,
                placeholderColor = mainTextColor.copy(alpha = 0.4f),
                leadingIconTint = mainTextColor.copy(alpha = 0.4f),
                leadingIconSize = 20.dp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(items = filteredApps, key = { it.packageName }) { app ->
                HiddenAppRow(
                    app = app,
                    isHidden = app.packageName in hiddenPackages,
                    onToggle = { onToggleHidden(app.packageName) },
                    mainTextColor = mainTextColor,
                    accentColor = accentColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled
                )
            }
        }
    }
}

@Composable
private fun HiddenAppRow(
    app: AppInfo,
    isHidden: Boolean,
    onToggle: () -> Unit,
    mainTextColor: Color,
    accentColor: Color,
    designStyle: DesignStyle,
    surfaceAccent: Color,
    isDarkTextEnabled: Boolean
) {
    val itemBackgroundModifier = Modifier.designSurface(
        designStyle, RoundedCornerShape(20.dp), isDarkTextEnabled, surfaceAccent,
        fillAlpha = 0.03f, glassStartAlpha = 0.06f, glassEndAlpha = 0.02f,
        borderWidth = 1.dp, borderStartAlpha = 0.15f, borderEndAlpha = 0.03f
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hidden_item_${app.packageName}")
            .clip(RoundedCornerShape(20.dp))
            .then(itemBackgroundModifier)
            .clickable { onToggle() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isHidden,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("hidden_checkbox_${app.packageName}"),
                colors = CheckboxDefaults.colors(
                    checkedColor = accentColor,
                    uncheckedColor = mainTextColor.copy(alpha = 0.5f),
                    checkmarkColor = Color.White
                )
            )
            AppIconView(app, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                app.label,
                color = if (isHidden) mainTextColor.copy(alpha = 0.55f) else mainTextColor,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
