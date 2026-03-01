package com.example.androidlauncher.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.LauncherLogic
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.FolderInfo
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalFontWeight
import com.example.androidlauncher.ui.theme.LocalLiquidGlassEnabled
import kotlinx.coroutines.delay

/**
 * Menu used to configure the contents of a specific folder.
 * Features:
 * - Adding new apps to the folder
 * - Removing apps from the folder
 * - Renaming the folder
 * - Deleting the folder
 * - NEW: Prevention of duplicate apps (disabling apps already in other folders)
 */
@Composable
fun FolderConfigMenu(
    folder: FolderInfo,
    allApps: List<AppInfo>,
    allFolders: List<FolderInfo>, // Added to check for apps in other folders
    onConfirm: (FolderInfo) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val colorTheme = LocalColorTheme.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val isLiquidGlassEnabled = LocalLiquidGlassEnabled.current
    val fontWeight = LocalFontWeight.current

    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White

    var searchQuery by remember { mutableStateOf("") }
    var selectedPackages by remember { mutableStateOf(folder.appPackageNames) }
    var folderName by remember { mutableStateOf(folder.name) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    // Calculate which apps are already in OTHER folders to disable them
    val appsInOtherFolders = remember(allFolders, folder.id) {
        allFolders
            .filter { it.id != folder.id }
            .flatMap { it.appPackageNames }
            .toSet()
    }

    val filteredApps = remember(allApps, searchQuery) { LauncherLogic.filterApps(allApps, searchQuery) }
    val focusRequester = remember { FocusRequester() }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    textStyle = androidx.compose.ui.text.TextStyle(color = mainTextColor, fontSize = 24.sp, fontWeight = fontWeight.weight),
                    cursorBrush = SolidColor(mainTextColor),
                    decorationBox = { 
                        if (folderName.isEmpty()) {
                            Text("Ordnername", color = mainTextColor.copy(alpha = 0.4f), fontSize = 24.sp)
                        }
                        it() 
                    }
                )
                Text("${selectedPackages.size} Apps ausgewählt", fontSize = 14.sp, color = mainTextColor.copy(alpha = 0.6f))
            }
            Row {
                IconButton(onClick = { showDeleteConfirm = true }) { 
                    Icon(Lucide.Trash2, contentDescription = "Ordner löschen", tint = Color.Red.copy(alpha = 0.8f)) 
                }
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = null, tint = mainTextColor) }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        val searchIntSrc = remember { MutableInteractionSource() }
        val searchBarModifier = if (isLiquidGlassEnabled) {
            Modifier
                .background(LiquidGlass.glassBrush(isDarkTextEnabled), RoundedCornerShape(12.dp))
                .border(BorderStroke(1.2.dp, LiquidGlass.borderBrush(isDarkTextEnabled)), RoundedCornerShape(12.dp))
        } else {
            Modifier.background(mainTextColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
        }

        Box(modifier = Modifier.fillMaxWidth().then(searchBarModifier).padding(horizontal = 16.dp, vertical = 12.dp).clickable(
            interactionSource = searchIntSrc,
            indication = null
        ) { focusRequester.requestFocus() }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = mainTextColor.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
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
                            Text("Apps suchen...", color = mainTextColor.copy(alpha = 0.4f), fontSize = 15.sp)
                        }
                        it() 
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item { Text("Apps verwalten", color = mainTextColor.copy(alpha = 0.5f), fontSize = 12.sp) }
            itemsIndexed(items = filteredApps, key = { _, app -> app.packageName }) { index, app ->
                val isSelected = app.packageName in selectedPackages
                // Requirement 2: Disable app if it's already in another folder
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

                    val itemModifier = if (isSelected && isLiquidGlassEnabled) {
                        Modifier
                            .background(LiquidGlass.glassBrush(isDarkTextEnabled), RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.2.dp, LiquidGlass.borderBrush(isDarkTextEnabled)), RoundedCornerShape(12.dp))
                    } else if (isSelected) {
                        Modifier.background(mainTextColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    } else {
                        Modifier.background(Color.Transparent, RoundedCornerShape(12.dp))
                    }

                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .then(itemModifier)
                        .graphicsLayer { 
                            // Visually disable apps that are already in other folders
                            alpha = if (isAlreadyInAnotherFolder) 0.35f else 1f 
                        }
                        .bounceClick(intSrc, enabled = !isAlreadyInAnotherFolder)
                        .clickable(
                            interactionSource = intSrc,
                            indication = null,
                            enabled = !isAlreadyInAnotherFolder // Disable interaction
                        ) {
                            selectedPackages = if (isSelected) {
                                selectedPackages - app.packageName
                            } else {
                                // Logic: App is added only if it was NOT already in the list
                                // This implicitly prevents duplicates within the local list as isSelected checks presence
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
                                        color = mainTextColor.copy(alpha = 0.5f), 
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
                            // Final safety check: ensure the list of packages is unique
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
