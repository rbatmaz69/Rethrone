package com.example.androidlauncher.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.LauncherLogic
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.FolderInfo
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.example.androidlauncher.ui.LiquidGlass.conditionalGlass
import com.example.androidlauncher.ui.theme.LocalAppFont
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalFontWeight
import com.example.androidlauncher.ui.theme.LocalLiquidGlassEnabled
import kotlinx.coroutines.delay

/**
 * Menu used to configure the contents of a specific folder.
 *
 * ── CUSTOM: Kompletter Umbau nach dem Muster des FavoritesConfigMenu ──
 * Vorher: Einfache Checkbox-Liste ohne Sortierung.
 * Jetzt: Zwei getrennte Sektionen wie im FavoritesConfigMenu:
 *   1. Oben: Ausgewählte Apps mit Nummerierung und ↑/↓-Reorder-Buttons
 *   2. Unten: Vollständige App-Liste mit Checkboxen zur Auswahl/Abwahl
 * Nutzt dieselben UI-Patterns (conditionalGlass, LiquidGlass-Farben, bounceClick).
 * develop: AnimatedVisibility Stagger-Einblendung für die App-Liste beim Suchen.
 */
@Composable
fun FolderConfigMenu(
    folder: FolderInfo,
    allApps: List<AppInfo>,
    allFolders: List<FolderInfo>,
    onConfirm: (FolderInfo) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val colorTheme = LocalColorTheme.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val isLiquidGlassEnabled = LocalLiquidGlassEnabled.current
    val fontWeight = LocalFontWeight.current
    val appFont = LocalAppFont.current

    // ── CUSTOM: Nutzt LiquidGlass-Helper statt manueller Color-Berechnung ──
    // Einheitlich mit FavoritesConfigMenu für konsistentes Theming.
    val mainTextColor = LiquidGlass.mainTextColor(isDarkTextEnabled)
    val grayTone = LiquidGlass.secondaryTextColor(isDarkTextEnabled)

    var searchQuery by remember { mutableStateOf("") }
    var selectedPackages by remember { mutableStateOf(folder.appPackageNames) }
    var folderName by remember { mutableStateOf(folder.name) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Apps die bereits in ANDEREN Ordnern sind (deaktiviert anzeigen)
    val appsInOtherFolders = remember(allFolders, folder.id) {
        allFolders
            .filter { it.id != folder.id }
            .flatMap { it.appPackageNames }
            .toSet()
    }

    val filteredApps = remember(allApps, searchQuery) { LauncherLogic.filterApps(allApps, searchQuery) }
    val focusRequester = remember { FocusRequester() }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
        // Header mit Ordnername und Buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    // ── CUSTOM: fontFamily aus AppFont damit der Ordnername
                    // die vom Nutzer gewählte Schriftart übernimmt.
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = mainTextColor,
                        fontSize = 24.sp,
                        fontWeight = fontWeight.weight,
                        fontFamily = appFont.fontFamily
                    ),
                    cursorBrush = SolidColor(mainTextColor),
                    decorationBox = {
                        if (folderName.isEmpty()) {
                            Text("Ordnername", color = mainTextColor.copy(alpha = 0.4f), fontSize = 24.sp, fontFamily = appFont.fontFamily)
                        }
                        it()
                    }
                )
                Text("${selectedPackages.size} Apps ausgewählt", fontSize = 14.sp, color = grayTone)
            }
            Row {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Lucide.Trash2, contentDescription = "Ordner löschen", tint = Color.Red.copy(alpha = 0.8f))
                }
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = null, tint = mainTextColor) }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── CUSTOM: Suchfeld mit conditionalGlass statt manueller background/border ──
        // Einheitlich mit dem Suchfeld im FavoritesConfigMenu.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .conditionalGlass(RoundedCornerShape(12.dp), isDarkTextEnabled, isLiquidGlassEnabled)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clickable { focusRequester.requestFocus() }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = grayTone, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    textStyle = androidx.compose.ui.text.TextStyle(color = mainTextColor, fontSize = 15.sp),
                    cursorBrush = SolidColor(mainTextColor),
                    singleLine = true,
                    decorationBox = {
                        if (searchQuery.isEmpty()) {
                            Text("Apps suchen...", color = grayTone, fontSize = 15.sp)
                        }
                        it()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App-Liste (gleiche Struktur wie FavoritesConfigMenu)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // ── CUSTOM: Sortierungs-Bereich für ausgewählte Apps ──
            // Zeigt die im Ordner enthaltenen Apps als nummerierte Liste mit
            // Pfeil-Buttons zum Umsortieren (identisch mit FavoritesConfigMenu).
            // Nutzt LauncherLogic.moveFavoriteUp/Down für die Reihenfolge-Logik.
            if (selectedPackages.isNotEmpty()) {
                item { Text("Reihenfolge", color = grayTone, fontSize = 12.sp) }
                itemsIndexed(selectedPackages) { index, pkg ->
                    allApps.find { it.packageName == pkg }?.let { app ->
                        FolderOrderItem(
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

            // Alle Apps – develop: AnimatedVisibility mit Stagger-Delay beim Suchen
            item { Text("Alle Apps", color = grayTone, fontSize = 12.sp) }
            itemsIndexed(items = filteredApps, key = { _, app -> app.packageName }) { index, app ->
                val isSelected = app.packageName in selectedPackages
                val isAlreadyInAnotherFolder = app.packageName in appsInOtherFolders

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
                                if (isSelected) {
                                    Modifier.conditionalGlass(
                                        RoundedCornerShape(12.dp), isDarkTextEnabled, isLiquidGlassEnabled,
                                        fallbackAlpha = 0.05f
                                    )
                                } else {
                                    Modifier.background(Color.Transparent, RoundedCornerShape(12.dp))
                                }
                            )
                            .graphicsLayer {
                                alpha = if (isAlreadyInAnotherFolder) 0.35f else 1f
                            }
                            .bounceClick(intSrc, enabled = !isAlreadyInAnotherFolder)
                            .clickable(
                                interactionSource = intSrc,
                                indication = null,
                                enabled = !isAlreadyInAnotherFolder
                            ) {
                                selectedPackages = if (isSelected) {
                                    selectedPackages - app.packageName
                                } else {
                                    selectedPackages + app.packageName
                                }
                            }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AppIconView(app)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.label, color = mainTextColor, fontSize = 16.sp)
                                if (isAlreadyInAnotherFolder) {
                                    Text(
                                        "Bereits in einem anderen Ordner",
                                        color = grayTone,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                enabled = !isAlreadyInAnotherFolder,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = mainTextColor,
                                    uncheckedColor = mainTextColor.copy(alpha = 0.4f),
                                    checkmarkColor = if (isDarkTextEnabled) Color.White else Color(0xFF0F172A),
                                    disabledCheckedColor = mainTextColor.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Ordner löschen?", color = mainTextColor) },
            text = { Text("Möchtest du diesen Ordner wirklich entfernen? Die Apps bleiben weiterhin im AppDrawer verfügbar.", color = mainTextColor.copy(alpha = 0.8f)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(folder.id)
                    showDeleteConfirm = false
                }) { Text("Löschen", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Abbrechen", color = Color.Gray) }
            },
            containerColor = colorTheme.drawerBackground
        )
    }

    // Bestätigungs-Button
    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(32.dp), contentAlignment = Alignment.BottomEnd) {
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
                        if (folderName.isNotBlank()) {
                            val finalPackages = selectedPackages.distinct()
                            onConfirm(folder.copy(name = folderName, appPackageNames = finalPackages))
                        } else {
                            Toast.makeText(context, "Bitte Namen eingeben", Toast.LENGTH_SHORT).show()
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Bestätigen",
                tint = checkmarkColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * ── CUSTOM: Einzelnes Reihenfolge-Element für den Ordner ──
 * Spiegelung des FavoriteOrderItem aus FavoritesConfigMenu.
 * Zeigt: Nummer | Icon | App-Name | ↑-Button | ↓-Button
 * Nutzt conditionalGlass für den Hintergrund.
 */
@Composable
private fun FolderOrderItem(
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
