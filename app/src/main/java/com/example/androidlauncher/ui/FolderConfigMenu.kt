package com.example.androidlauncher.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled

@Composable
fun FolderConfigMenu(
    folder: FolderInfo,
    allApps: List<AppInfo>,
    onConfirm: (FolderInfo) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    
    // Vermeidung von Pure-Black, um HW-Overlay-Fehler zu umgehen
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White

    var searchQuery by remember { mutableStateOf("") }
    var selectedPackages by remember { mutableStateOf(folder.appPackageNames) }
    var folderName by remember { mutableStateOf(folder.name) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    val filteredApps = remember(allApps, searchQuery) { LauncherLogic.filterApps(allApps, searchQuery) }
    val focusRequester = remember { FocusRequester() }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    textStyle = androidx.compose.ui.text.TextStyle(color = mainTextColor, fontSize = 24.sp, fontWeight = FontWeight.Light),
                    cursorBrush = SolidColor(mainTextColor),
                    decorationBox = { if (folderName.isEmpty()) Text("Ordnername", color = mainTextColor.copy(alpha = 0.4f), fontSize = 24.sp); it() }
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
        Box(modifier = Modifier.fillMaxWidth().background(mainTextColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp).clickable(
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
                    decorationBox = { if (searchQuery.isEmpty()) Text("Apps suchen...", color = mainTextColor.copy(alpha = 0.4f), fontSize = 15.sp); it() }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 150.dp)) {
            item { Text("Apps verwalten", color = mainTextColor.copy(alpha = 0.5f), fontSize = 12.sp) }
            items(filteredApps) { app ->
                val isSelected = app.packageName in selectedPackages
                val intSrc = remember { MutableInteractionSource() }
                Surface(color = if (isSelected) mainTextColor.copy(alpha = 0.05f) else Color.Transparent, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().bounceClick(intSrc).clickable(
                    interactionSource = intSrc,
                    indication = null
                ) {
                    selectedPackages = if (isSelected) {
                        selectedPackages - app.packageName
                    } else {
                        selectedPackages + app.packageName
                    }
                }) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppIconView(app)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(app.label, color = mainTextColor, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Checkbox(
                            checked = isSelected, 
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Ordner löschen?") },
            text = { Text("Möchtest du diesen Ordner wirklich entfernen? Die Apps bleiben weiterhin im AppDrawer verfügbar.") },
            confirmButton = {
                TextButton(onClick = { 
                    onDelete(folder.id)
                    showDeleteConfirm = false 
                }) { Text("Löschen", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Abbrechen") }
            }
        )
    }

    // DER BUTTON - Finale Isolation gegen alle Rendering-Fehler
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.BottomEnd) {
        val intSrc = remember { MutableInteractionSource() }
        val checkmarkColor = if (isDarkTextEnabled) Color.White else Color(0xFF0F172A)
        
        Box(
            modifier = Modifier
                .size(56.dp)
                // Isolation in einen eigenen Graphics Layer
                .graphicsLayer(alpha = 0.99f) 
                .drawBehind {
                    // Safe Zone Radius (2.0f Pixel Abstand)
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
                            onConfirm(folder.copy(name = folderName, appPackageNames = selectedPackages))
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
