package com.example.androidlauncher.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.LauncherLogic
import com.example.androidlauncher.R
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.ui.LiquidGlass.conditionalGlass
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalLiquidGlassEnabled
import kotlinx.coroutines.delay

/**
 * Konfigurationsmenü für Favoriten.
 *
 * Ermöglicht dem Nutzer:
 * - Apps als Favoriten hinzuzufügen/entfernen (max. [LauncherLogic.MAX_FAVORITES])
 * - Die Reihenfolge der Favoriten per Pfeil-Buttons zu ändern
 * - Labels unter Favoriten-Icons ein-/auszuschalten
 * - Apps nach Name zu suchen
 *
 * @param apps Alle verfügbaren Apps.
 * @param initialFavoritePackages Aktuelle Favoritenliste.
 * @param showFavoriteLabels Ob Labels unter Favoriten angezeigt werden.
 * @param onShowLabelsToggled Callback zum Umschalten der Label-Anzeige.
 * @param onConfirm Callback mit der neuen Favoritenliste bei Bestätigung.
 * @param onClose Callback zum Schließen des Menüs.
 */
@Composable
fun FavoritesConfigMenu(
    apps: List<AppInfo>,
    initialFavoritePackages: List<String>,
    showFavoriteLabels: Boolean,
    onShowLabelsToggled: (Boolean) -> Unit,
    onConfirm: (List<String>) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val isLiquidGlassEnabled = LocalLiquidGlassEnabled.current

    val mainTextColor = LiquidGlass.mainTextColor(isDarkTextEnabled)
    val grayTone = LiquidGlass.secondaryTextColor(isDarkTextEnabled)

    var searchQuery by remember { mutableStateOf("") }
    var selectedPackages by remember { mutableStateOf(initialFavoritePackages) }
    val filteredApps = remember(apps, searchQuery) { LauncherLogic.filterApps(apps, searchQuery) }
    val focusRequester = remember { FocusRequester() }
    val favoritesListState = rememberLazyListState()
    val swipeToCloseConnection = rememberBottomBoundarySwipeToCloseConnection(
        listState = favoritesListState,
        onClose = onClose
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    stringResource(R.string.favorites_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    color = mainTextColor
                )
                Text(
                    stringResource(R.string.favorites_count, selectedPackages.size, LauncherLogic.MAX_FAVORITES),
                    fontSize = 14.sp,
                    color = grayTone
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = null, tint = mainTextColor)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Label-Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("App-Titel", color = grayTone, fontSize = 14.sp)

            val symbolColor = if (isDarkTextEnabled) Color.White else Color.Black

            Switch(
                checked = showFavoriteLabels,
                onCheckedChange = onShowLabelsToggled,
                colors = LiquidGlass.switchColors(isDarkTextEnabled, isLiquidGlassEnabled),
                thumbContent = {
                    Box(contentAlignment = Alignment.Center) {
                        if (showFavoriteLabels) {
                            Box(
                                modifier = Modifier
                                    .size(width = 1.5.dp, height = 12.dp)
                                    .background(symbolColor, RoundedCornerShape(0.5.dp))
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(width = 8.dp, height = 12.dp),
                                color = Color.Transparent,
                                border = BorderStroke(1.5.dp, symbolColor),
                                shape = RoundedCornerShape(4.dp)
                            ) {}
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Suchfeld
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .conditionalGlass(RoundedCornerShape(12.dp), isDarkTextEnabled, isLiquidGlassEnabled)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clickable { focusRequester.requestFocus() }
        ) {
            StableSearchFieldContent(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = stringResource(R.string.search_apps),
                textStyle = androidx.compose.ui.text.TextStyle(color = mainTextColor, fontSize = 15.sp),
                textColor = mainTextColor,
                placeholderColor = grayTone,
                focusRequester = focusRequester,
                leadingIconTint = grayTone,
                leadingIconSize = 18.dp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App-Liste
        LazyColumn(
            state = favoritesListState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(swipeToCloseConnection),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Sortierungs-Bereich für ausgewählte Favoriten (Nur zeigen wenn nicht gesucht wird)
            if (searchQuery.isBlank() && selectedPackages.isNotEmpty()) {
                item { Text(stringResource(R.string.order_label), color = grayTone, fontSize = 12.sp) }
                itemsIndexed(selectedPackages) { index, pkg ->
                    apps.find { it.packageName == pkg }?.let { app ->
                        FavoriteOrderItem(
                            app = app,
                            index = index,
                            totalCount = selectedPackages.size,
                            mainTextColor = mainTextColor,
                            grayTone = grayTone,
                            isDarkTextEnabled = isDarkTextEnabled,
                            isLiquidGlassEnabled = isLiquidGlassEnabled,
                            onMoveUp = {
                                selectedPackages = LauncherLogic.moveFavoriteUp(selectedPackages, index)
                            },
                            onMoveDown = {
                                selectedPackages = LauncherLogic.moveFavoriteDown(selectedPackages, index)
                            }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            // Alle Apps
            item { Text(stringResource(R.string.all_apps_label), color = grayTone, fontSize = 12.sp) }
            itemsIndexed(items = filteredApps, key = { _, app -> app.packageName }) { index, app ->
                val isFav = app.packageName in selectedPackages
                
                val isSearching = searchQuery.isNotBlank()
                var isVisible by remember(app.packageName, isSearching) { mutableStateOf(!isSearching) }
                
                LaunchedEffect(app.packageName, isSearching) {
                    if (isSearching) {
                        delay((index % 12) * 30L)
                        isVisible = true
                    }
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(400)) + 
                            scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)) +
                            slideInVertically(initialOffsetY = { 20 }, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    val intSrc = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isFav) {
                                    Modifier.conditionalGlass(
                                        RoundedCornerShape(12.dp), isDarkTextEnabled, isLiquidGlassEnabled,
                                        fallbackAlpha = 0.05f
                                    )
                                } else {
                                    Modifier.background(Color.Transparent, RoundedCornerShape(12.dp))
                                }
                            )
                            .bounceClick(intSrc)
                            .clickable {
                                val newFavs = LauncherLogic.toggleFavorite(selectedPackages, app.packageName)
                                if (newFavs.size <= LauncherLogic.MAX_FAVORITES) {
                                    selectedPackages = newFavs
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.max_favorites_reached),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconView(app)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                app.label, color = mainTextColor, fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = isFav,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = mainTextColor,
                                    uncheckedColor = mainTextColor.copy(alpha = 0.4f),
                                    checkmarkColor = if (isDarkTextEnabled) Color.White else Color(0xFF0F172A)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Bestätigungs-Button
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(32.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        val intSrc = remember { MutableInteractionSource() }
        val checkmarkColor = if (isDarkTextEnabled) Color.White else Color(0xFF0F172A)

        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer(alpha = 0.99f)
                .drawBehind {
                    drawCircle(
                        color = mainTextColor,
                        radius = (size.minDimension / 2.0f) - 2.0f,
                        center = center
                    )
                }
                .clickable(
                    interactionSource = intSrc,
                    indication = null,
                    onClick = {
                        if (selectedPackages.isNotEmpty()) {
                            onConfirm(selectedPackages)
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.no_selection),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = checkmarkColor, modifier = Modifier.size(28.dp))
        }
    }
}

// ── Einzelnes Favoriten-Reihenfolge-Element ──────────────────────

/**
 * Listenelement in der Favoriten-Sortierung.
 * Zeigt App-Icon, Name, Position und Pfeil-Buttons zum Umsortieren.
 */
@Composable
private fun FavoriteOrderItem(
    app: AppInfo,
    index: Int,
    totalCount: Int,
    mainTextColor: Color,
    grayTone: Color,
    isDarkTextEnabled: Boolean,
    isLiquidGlassEnabled: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .conditionalGlass(RoundedCornerShape(12.dp), isDarkTextEnabled, isLiquidGlassEnabled, fallbackAlpha = 0.05f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${index + 1}.", color = mainTextColor, fontSize = 14.sp, modifier = Modifier.width(24.dp))
            AppIconView(app)
            Spacer(modifier = Modifier.width(16.dp))
            Text(app.label, color = mainTextColor, fontSize = 16.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onMoveUp, enabled = index > 0) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = if (index > 0) grayTone else mainTextColor
                )
            }
            IconButton(onClick = onMoveDown, enabled = index < totalCount - 1) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (index < totalCount - 1) grayTone else mainTextColor
                )
            }
        }
    }
}
