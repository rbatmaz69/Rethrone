package com.example.androidlauncher.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
 * ── CUSTOM: Partitioned list with selected apps at the top (alphabetical) ──
 * 1. Selected apps are shown at the top, sorted alphabetically for easy deselection.
 * 2. Unselected apps follow below, also sorted alphabetically.
 * 3. The actual folder order still follows the selection sequence (logic kept in selectedPackages).
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

    val mainTextColor = LiquidGlass.mainTextColor(isDarkTextEnabled)
    val grayTone = LiquidGlass.secondaryTextColor(isDarkTextEnabled)

    var searchQuery by remember { mutableStateOf("") }
    // CUSTOM: Holds the actual order of apps in the folder (selection sequence).
    var selectedPackages by remember { mutableStateOf(folder.appPackageNames) }
    var folderName by remember { mutableStateOf(folder.name) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val appsInOtherFolders = remember(allFolders, folder.id) {
        allFolders
            .filter { it.id != folder.id }
            .flatMap { it.appPackageNames }
            .toSet()
    }

    val filteredApps = remember(allApps, searchQuery) { LauncherLogic.filterApps(allApps, searchQuery) }
    
    // CUSTOM: Split filtered apps into selected and unselected, both sorted alphabetically for display.
    val displaySelectedApps = remember(filteredApps, selectedPackages) {
        filteredApps.filter { it.packageName in selectedPackages }.sortedBy { it.label.lowercase() }
    }
    val displayUnselectedApps = remember(filteredApps, selectedPackages) {
        filteredApps.filter { it.packageName !in selectedPackages }.sortedBy { it.label.lowercase() }
    }

    val focusRequester = remember { FocusRequester() }
    val folderListState = rememberLazyListState()
    val swipeToCloseConnection = rememberTopBoundarySwipeToCloseConnection(
        listState = folderListState,
        enabled = !showDeleteConfirm,
        onClose = onClose
    )

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
        // Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
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

        // Search Bar
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
                placeholder = "Apps suchen...",
                textStyle = androidx.compose.ui.text.TextStyle(color = mainTextColor, fontSize = 15.sp),
                textColor = mainTextColor,
                placeholderColor = grayTone,
                focusRequester = focusRequester,
                leadingIconTint = grayTone,
                leadingIconSize = 18.dp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Partitioned App List
        LazyColumn(
            state = folderListState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(swipeToCloseConnection),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Selected Apps Header & List
            if (displaySelectedApps.isNotEmpty()) {
                item { Text("Ausgewählt", color = grayTone, fontSize = 12.sp) }
                items(items = displaySelectedApps, key = { it.packageName }) { app ->
                    AppSelectionItem(
                        app = app,
                        isSelected = true,
                        isAlreadyInAnotherFolder = false, // Cannot be in another if selected here
                        mainTextColor = mainTextColor,
                        grayTone = grayTone,
                        isDarkTextEnabled = isDarkTextEnabled,
                        isLiquidGlassEnabled = isLiquidGlassEnabled,
                        onToggle = {
                            selectedPackages = selectedPackages - app.packageName
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Unselected Apps Header & List
            if (displayUnselectedApps.isNotEmpty()) {
                item { Text("Weitere Apps", color = grayTone, fontSize = 12.sp) }
                itemsIndexed(items = displayUnselectedApps, key = { _, app -> app.packageName }) { index, app ->
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
                        AppSelectionItem(
                            app = app,
                            isSelected = false,
                            isAlreadyInAnotherFolder = isAlreadyInAnotherFolder,
                            mainTextColor = mainTextColor,
                            grayTone = grayTone,
                            isDarkTextEnabled = isDarkTextEnabled,
                            isLiquidGlassEnabled = isLiquidGlassEnabled,
                            onToggle = {
                                if (!isAlreadyInAnotherFolder) {
                                    // CUSTOM: Add to end of list to preserve selection sequence for folder order.
                                    selectedPackages = selectedPackages + app.packageName
                                }
                            }
                        )
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

    // Confirmation Button
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
 * CUSTOM: Reusable item for app selection in the list.
 */
@Composable
private fun AppSelectionItem(
    app: AppInfo,
    isSelected: Boolean,
    isAlreadyInAnotherFolder: Boolean,
    mainTextColor: Color,
    grayTone: Color,
    isDarkTextEnabled: Boolean,
    isLiquidGlassEnabled: Boolean,
    onToggle: () -> Unit
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
                onToggle()
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
