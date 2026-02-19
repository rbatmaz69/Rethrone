package com.example.androidlauncher.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.LauncherLogic
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.FolderInfo

@Composable
fun FolderConfigMenu(
    folder: FolderInfo,
    allApps: List<AppInfo>,
    onConfirm: (FolderInfo) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedPackages by remember { mutableStateOf(folder.appPackageNames) }
    var folderName by remember { mutableStateOf(folder.name) }
    
    val filteredApps = remember(allApps, searchQuery) { LauncherLogic.filterApps(allApps, searchQuery) }
    val focusRequester = remember { FocusRequester() }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light),
                    cursorBrush = SolidColor(Color.White),
                    decorationBox = { if (folderName.isEmpty()) Text("Ordnername", color = Color.White.copy(alpha = 0.4f), fontSize = 24.sp); it() }
                )
                Text("${selectedPackages.size} Apps ausgewählt", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))
            }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = null, tint = Color.White) }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        // Horizontal Reordering Section
        if (selectedPackages.isNotEmpty()) {
            Text("Reihenfolge anpassen", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                itemsIndexed(selectedPackages) { index, pkg ->
                    val app = allApps.find { it.packageName == pkg }
                    if (app != null) {
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.width(100.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(modifier = Modifier.size(40.dp)) {
                                    AppIconView(app)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    app.label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val newList = selectedPackages.toMutableList()
                                                val item = newList.removeAt(index)
                                                newList.add(index - 1, item)
                                                selectedPackages = newList
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = null,
                                            tint = if (index > 0) Color.White else Color.White.copy(alpha = 0.2f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < selectedPackages.size - 1) {
                                                val newList = selectedPackages.toMutableList()
                                                val item = newList.removeAt(index)
                                                newList.add(index + 1, item)
                                                selectedPackages = newList
                                            }
                                        },
                                        enabled = index < selectedPackages.size - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = if (index < selectedPackages.size - 1) Color.White else Color.White.copy(alpha = 0.2f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        val searchIntSrc = remember { MutableInteractionSource() }
        Box(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp).clickable(
            interactionSource = searchIntSrc,
            indication = null
        ) { focusRequester.requestFocus() }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                    cursorBrush = SolidColor(Color.White),
                    singleLine = true,
                    decorationBox = { if (searchQuery.isEmpty()) Text("Apps suchen...", color = Color.White.copy(alpha = 0.4f), fontSize = 15.sp); it() }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
            item { Text("Alle Apps", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp) }
            items(filteredApps) { app ->
                val isSelected = app.packageName in selectedPackages
                val intSrc = remember { MutableInteractionSource() }
                Surface(color = if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().bounceClick(intSrc).clickable(
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
                        Text(app.label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Checkbox(checked = isSelected, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color.White.copy(alpha = 0.4f), checkmarkColor = Color(0xFF0F172A)))
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.BottomEnd) {
        val intSrc = remember { MutableInteractionSource() }
        FloatingActionButton(
            onClick = { 
                if (folderName.isNotBlank()) {
                    onConfirm(folder.copy(name = folderName, appPackageNames = selectedPackages))
                } else {
                    Toast.makeText(context, "Bitte Namen eingeben", Toast.LENGTH_SHORT).show()
                }
            }, 
            containerColor = Color.White, 
            contentColor = Color(0xFF0F172A), 
            shape = CircleShape, 
            modifier = Modifier.bounceClick(intSrc)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
        }
    }
}
