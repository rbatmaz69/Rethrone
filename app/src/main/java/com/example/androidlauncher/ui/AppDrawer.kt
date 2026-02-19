package com.example.androidlauncher.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.androidlauncher.LauncherLogic
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.FolderInfo
import com.example.androidlauncher.data.IconSize
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalFontSize
import com.example.androidlauncher.ui.theme.LocalIconSize
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.FolderPlus
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Check
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    folders: List<FolderInfo>,
    onToggleFavorite: (String) -> Unit,
    isFavorite: (String) -> Boolean,
    onUpdateFolders: (List<FolderInfo>) -> Unit,
    onOpenFolderConfig: (FolderInfo) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val colorTheme = LocalColorTheme.current
    val fontSize = LocalFontSize.current
    val iconSize = LocalIconSize.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    
    var searchQuery by remember { mutableStateOf("") }

    val visibleApps = remember(apps.toList(), folders, searchQuery) {
        if (searchQuery.isBlank()) {
            LauncherLogic.getVisibleApps(apps.toList(), folders)
        } else {
            LauncherLogic.filterApps(apps.toList(), searchQuery)
        }
    }

    val focusRequester = remember { FocusRequester() }

    var activeFolderId by remember { mutableStateOf<String?>(null) }
    val activeFolder = remember(activeFolderId, folders) {
        folders.find { it.id == activeFolderId }
    }
    
    var folderPosition by remember { mutableStateOf(Offset.Zero) }
    var isEditMode by remember { mutableStateOf(false) }
    var editingFolderName by remember { mutableStateOf("") }
    
    LaunchedEffect(activeFolderId) {
        if (activeFolderId != null) {
            editingFolderName = folders.find { it.id == activeFolderId }?.name ?: ""
        } else {
            isEditMode = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SolidColor(colorTheme.drawerBackground.copy(alpha = 0.85f)))
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Apps",
                    fontSize = 24.sp * fontSize.scale,
                    fontWeight = FontWeight.Light,
                    color = Color.White
                )
                Row {
                    if (searchQuery.isBlank()) {
                        var isCreateFolderDialogOpen by remember { mutableStateOf(false) }
                        var folderNameInput by remember { mutableStateOf("") }
                        
                        IconButton(onClick = { isCreateFolderDialogOpen = true }) { 
                            Icon(Lucide.FolderPlus, contentDescription = "Create Folder", tint = Color.White) 
                        }
                        
                        if (isCreateFolderDialogOpen) {
                            AlertDialog(
                                onDismissRequest = { isCreateFolderDialogOpen = false },
                                title = { Text("Neuer Ordner") },
                                text = {
                                    TextField(
                                        value = folderNameInput,
                                        onValueChange = { folderNameInput = it },
                                        placeholder = { Text("Name eingeben") }
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        if (folderNameInput.isNotBlank()) {
                                            onUpdateFolders(LauncherLogic.createFolder(folders, folderNameInput))
                                            folderNameInput = ""
                                            isCreateFolderDialogOpen = false
                                        }
                                    }) { Text("Erstellen") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { isCreateFolderDialogOpen = false }) { Text("Abbrechen") }
                                }
                            )
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            val searchIntSrc = remember { MutableInteractionSource() }
            Box(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 14.dp).clickable(
                interactionSource = searchIntSrc,
                indication = null
            ) { focusRequester.requestFocus() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = searchQuery, onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp * fontSize.scale),
                        cursorBrush = SolidColor(Color.White), singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                        decorationBox = { if (searchQuery.isEmpty()) Text("Apps durchsuchen...", color = Color.White.copy(alpha = 0.4f), fontSize = 16.sp * fontSize.scale); it() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            val adaptiveColumns = when (iconSize) {
                IconSize.SMALL -> 5
                IconSize.STANDARD -> 4
                IconSize.LARGE -> 3
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(adaptiveColumns),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.spacedBy(32.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                if (searchQuery.isBlank()) {
                    itemsIndexed(items = folders, key = { _, folder -> folder.id }) { _, folder ->
                        FolderItem(
                            folder = folder, 
                            onClick = { pos -> 
                                folderPosition = pos
                                activeFolderId = folder.id 
                            }, 
                            onUpdateFolders = onUpdateFolders, 
                            onOpenFolderConfig = onOpenFolderConfig
                        )
                    }
                }

                itemsIndexed(items = visibleApps, key = { _, app -> app.packageName }) { _, app ->
                    AppItem(
                        app = app,
                        adaptiveColumns = adaptiveColumns,
                        isFavorite = isFavorite(app.packageName),
                        onToggleFavorite = onToggleFavorite,
                        folders = folders,
                        onUpdateFolders = onUpdateFolders
                    )
                }
            }
        }

        // Folder Popup Overlay
        AnimatedVisibility(
            visible = activeFolder != null,
            enter = fadeIn(animationSpec = tween(150)) + scaleIn(
                initialScale = 0.3f,
                transformOrigin = TransformOrigin(
                    pivotFractionX = if (folderPosition.x > 0) {
                        folderPosition.x / (context.resources.displayMetrics.widthPixels.toFloat())
                    } else 0.5f,
                    pivotFractionY = if (folderPosition.y > 0) {
                        folderPosition.y / (context.resources.displayMetrics.heightPixels.toFloat())
                    } else 0.5f
                ),
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(100)) + scaleOut(
                targetScale = 0.3f,
                transformOrigin = TransformOrigin(
                    pivotFractionX = if (folderPosition.x > 0) {
                        folderPosition.x / (context.resources.displayMetrics.widthPixels.toFloat())
                    } else 0.5f,
                    pivotFractionY = if (folderPosition.y > 0) {
                        folderPosition.y / (context.resources.displayMetrics.heightPixels.toFloat())
                    } else 0.5f
                ),
                animationSpec = tween(150)
            )
        ) {
            activeFolder?.let { currentActiveFolder ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { 
                                if (isEditMode) isEditMode = false else activeFolderId = null 
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .wrapContentHeight()
                            .clickable(enabled = false) {},
                        color = colorTheme.drawerBackground.copy(alpha = 0.98f),
                        shape = RoundedCornerShape(32.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        shadowElevation = 24.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                if (isEditMode) {
                                    BasicTextField(
                                        value = editingFolderName,
                                        onValueChange = { newName ->
                                            editingFolderName = newName
                                            val updatedFolder = currentActiveFolder.copy(name = newName)
                                            val updatedFolders = folders.map { 
                                                if (it.id == currentActiveFolder.id) updatedFolder else it 
                                            }
                                            onUpdateFolders(updatedFolders)
                                        },
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            color = Color.White,
                                            fontSize = 22.sp * fontSize.scale,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        ),
                                        cursorBrush = SolidColor(Color.White),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { 
                                            isEditMode = false
                                            keyboardController?.hide() 
                                        }),
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp)
                                    )
                                } else {
                                    Text(
                                        currentActiveFolder.name,
                                        color = Color.White,
                                        fontSize = 22.sp * fontSize.scale,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp)
                                    )
                                }
                                
                                IconButton(
                                    onClick = { isEditMode = !isEditMode },
                                    modifier = Modifier.align(Alignment.CenterEnd).size(32.dp)
                                ) {
                                    Icon(
                                        if (isEditMode) Lucide.Check else Lucide.Pencil, 
                                        contentDescription = "EditMode", 
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            val folderApps = remember(currentActiveFolder.appPackageNames, apps) {
                                currentActiveFolder.appPackageNames.mapNotNull { pkg -> 
                                    apps.find { it.packageName == pkg } 
                                }
                            }
                            
                            val pages = (folderApps.size + 8) / 9
                            val pagerState = rememberPagerState(pageCount = { pages })
                            
                            val infiniteTransition = rememberInfiniteTransition(label = "WiggleTransition")
                            val wiggleAngle by infiniteTransition.animateFloat(
                                initialValue = -2.5f,
                                targetValue = 2.5f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(110, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "WiggleAngle"
                            )

                            Box(modifier = Modifier.height(340.dp)) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    pageSpacing = 16.dp,
                                    userScrollEnabled = !isEditMode
                                ) { page ->
                                    val startIdx = page * 9
                                    val endIdx = (startIdx + 9).coerceAtMost(folderApps.size)
                                    val pageApps = folderApps.subList(startIdx, endIdx)
                                    
                                    var draggingItemPackageName by remember { mutableStateOf<String?>(null) }
                                    var dragOffset by remember { mutableStateOf(Offset.Zero) }
                                    
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(3),
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(24.dp),
                                            userScrollEnabled = false
                                        ) {
                                            itemsIndexed(pageApps, key = { _, app -> app.packageName }) { index, app ->
                                                val isDragging = draggingItemPackageName == app.packageName
                                                
                                                val rotation = if (isEditMode && !isDragging) {
                                                    if ((startIdx + index) % 2 == 0) wiggleAngle else -wiggleAngle
                                                } else 0f

                                                Box(
                                                    modifier = Modifier
                                                        .zIndex(if (isDragging) 10f else 1f)
                                                        .graphicsLayer { rotationZ = rotation }
                                                        .then(if (isEditMode) {
                                                            Modifier.pointerInput(app.packageName) {
                                                                detectDragGestures(
                                                                    onDragStart = { 
                                                                        draggingItemPackageName = app.packageName 
                                                                    },
                                                                    onDragEnd = {
                                                                        draggingItemPackageName?.let { pkg ->
                                                                            val fromIdx = currentActiveFolder.appPackageNames.indexOf(pkg)
                                                                            if (fromIdx != -1) {
                                                                                // Improved calculation for grid reordering
                                                                                val cellW = size.width.toFloat() + with(density) { 16.dp.toPx() }
                                                                                val cellH = size.height.toFloat() + with(density) { 24.dp.toPx() }
                                                                                
                                                                                val colMove = (dragOffset.x / (size.width.toFloat() / 3f)).roundToInt()
                                                                                val rowMove = (dragOffset.y / (size.height.toFloat() / 3f)).roundToInt()
                                                                                
                                                                                var targetIdx = fromIdx + rowMove * 3 + colMove
                                                                                targetIdx = targetIdx.coerceIn(0, currentActiveFolder.appPackageNames.size - 1)
                                                                                
                                                                                if (fromIdx != targetIdx) {
                                                                                    val newList = currentActiveFolder.appPackageNames.toMutableList()
                                                                                    val item = newList.removeAt(fromIdx)
                                                                                    newList.add(targetIdx, item)
                                                                                    val updatedFolders = folders.map { 
                                                                                        if (it.id == currentActiveFolder.id) it.copy(appPackageNames = newList) else it 
                                                                                    }
                                                                                    onUpdateFolders(updatedFolders)
                                                                                }
                                                                            }
                                                                        }
                                                                        draggingItemPackageName = null
                                                                        dragOffset = Offset.Zero
                                                                    },
                                                                    onDragCancel = {
                                                                        draggingItemPackageName = null
                                                                        dragOffset = Offset.Zero
                                                                    },
                                                                    onDrag = { change, dragAmount ->
                                                                        change.consume()
                                                                        dragOffset += dragAmount
                                                                    }
                                                                )
                                                            }
                                                        } else Modifier)
                                                        .offset {
                                                            if (isDragging) IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt())
                                                            else IntOffset.Zero
                                                        }
                                                        .scale(if (isDragging) 1.15f else 1f)
                                                        .shadow(if (isDragging) 12.dp else 0.dp, RoundedCornerShape(12.dp))
                                                ) {
                                                    AppItem(
                                                        app = app,
                                                        adaptiveColumns = 3,
                                                        isFavorite = isFavorite(app.packageName),
                                                        onToggleFavorite = onToggleFavorite,
                                                        folders = folders,
                                                        onUpdateFolders = onUpdateFolders,
                                                        isInFolder = true,
                                                        currentFolderId = currentActiveFolder.id,
                                                        isEditMode = isEditMode
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (pages > 1) {
                                Row(
                                    Modifier
                                        .wrapContentHeight()
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    repeat(pages) { iteration ->
                                        val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.3f)
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .size(6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderItem(
    folder: FolderInfo,
    onClick: (Offset) -> Unit,
    onUpdateFolders: (List<FolderInfo>) -> Unit,
    onOpenFolderConfig: (FolderInfo) -> Unit
) {
    val fontSize = LocalFontSize.current
    val iconSizeValue = LocalIconSize.current.size
    val intSrc = remember { MutableInteractionSource() }
    var itemOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .width(80.dp)
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                val size = coordinates.size
                itemOffset = Offset(
                    x = position.x + size.width / 2f,
                    y = position.y + size.height / 2f
                )
            }, 
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.bounceClick(intSrc).combinedClickable(
            interactionSource = intSrc,
            indication = null,
            onClick = { onClick(itemOffset) },
            onLongClick = { onOpenFolderConfig(folder) }
        )) {
            Box(modifier = Modifier.size(iconSizeValue).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Lucide.Folder, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSizeValue * 0.6f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = folder.name, fontSize = 11.sp * fontSize.scale, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppItem(
    app: AppInfo,
    adaptiveColumns: Int,
    isFavorite: Boolean,
    onToggleFavorite: (String) -> Unit,
    folders: List<FolderInfo>,
    onUpdateFolders: (List<FolderInfo>) -> Unit,
    isInFolder: Boolean = false,
    currentFolderId: String? = null,
    isEditMode: Boolean = false
) {
    val context = LocalContext.current
    val fontSize = LocalFontSize.current
    var showActions by remember { mutableStateOf(false) }
    var showFolderSelection by remember { mutableStateOf(false) }
    val intSrc = remember { MutableInteractionSource() }

    Box(modifier = Modifier.width(if (adaptiveColumns == 5) 64.dp else 80.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.bounceClick(intSrc).combinedClickable(
            interactionSource = intSrc,
            indication = null,
            enabled = !isEditMode,
            onClick = { 
                context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it) } 
            },
            onLongClick = { 
                showActions = true
            }
        )) {
            AppIconView(app)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = app.label, fontSize = 11.sp * fontSize.scale, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }, modifier = Modifier.background(Color(0xFF1A1F2B))) {
            DropdownMenuItem(text = { Text(if (isFavorite) "Vom Home entfernen" else "Als Favorit setzen", color = Color.White, fontSize = 14.sp * fontSize.scale) }, onClick = { onToggleFavorite(app.packageName); showActions = false })

            if (isInFolder && currentFolderId != null) {
                DropdownMenuItem(text = { Text("Aus Ordner entfernen", color = Color.White, fontSize = 14.sp * fontSize.scale) }, onClick = {
                    onUpdateFolders(LauncherLogic.removeAppFromFolder(folders, currentFolderId, app.packageName))
                    showActions = false
                })
            } else {
                DropdownMenuItem(text = { Text("In Ordner verschieben", color = Color.White, fontSize = 14.sp * fontSize.scale) }, onClick = {
                    showFolderSelection = true
                    showActions = false
                })
            }

            DropdownMenuItem(text = { Text("App-Info", color = Color.White, fontSize = 14.sp * fontSize.scale) }, onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", app.packageName, null) }); showActions = false })
            DropdownMenuItem(text = { Text("Deinstallieren", color = Color.Red, fontSize = 14.sp * fontSize.scale) }, onClick = { context.startActivity(Intent(Intent.ACTION_DELETE).apply { data = Uri.fromParts("package", app.packageName, null) }); showActions = false })
        }

        if (showFolderSelection) {
            DropdownMenu(expanded = showFolderSelection, onDismissRequest = { showFolderSelection = false }, modifier = Modifier.background(Color(0xFF1A1F2B))) {
                if (folders.isEmpty()) {
                    DropdownMenuItem(text = { Text("Keine Ordner vorhanden", color = Color.White.copy(alpha = 0.5f)) }, onClick = { showFolderSelection = false })
                }
                folders.forEach { folder ->
                    DropdownMenuItem(text = { Text(folder.name, color = Color.White) }, onClick = {
                        onUpdateFolders(LauncherLogic.addAppToFolder(folders, folder.id, app.packageName))
                        showFolderSelection = false
                    })
                }
            }
        }
    }
}
