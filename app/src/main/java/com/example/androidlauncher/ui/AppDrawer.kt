package com.example.androidlauncher.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.androidlauncher.LauncherLogic
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.FolderInfo
import com.example.androidlauncher.data.IconSize
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalFontSize
import com.example.androidlauncher.ui.theme.LocalIconSize
import com.composables.icons.lucide.*
import kotlinx.coroutines.delay
import kotlin.math.max
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
    onClose: () -> Unit,
    onAppLaunchForReturn: (String, Rect?) -> Unit,
    returnIconPackage: String?
) {
    val context = LocalContext.current
    val colorTheme = LocalColorTheme.current
    val fontSize = LocalFontSize.current
    val iconSize = LocalIconSize.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White

    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

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
        if (activeFolderId != null) folders.find { it.id == activeFolderId } else null
    }
    
    var lastValidFolder by remember { mutableStateOf<FolderInfo?>(null) }
    LaunchedEffect(activeFolder) {
        if (activeFolder != null) {
            lastValidFolder = activeFolder
        }
    }
    
    var drawerSize by remember { mutableStateOf(IntSize.Zero) }
    var folderPosition by remember { mutableStateOf(Offset.Zero) }
    var launchRequest by remember { mutableStateOf<LaunchRequest?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    var editingFolderName by remember { mutableStateOf("") }
    
    BackHandler(enabled = activeFolderId != null) {
        if (isEditMode) {
            isEditMode = false
        } else {
            activeFolderId = null
        }
    }

    var draggingItemPkg by remember { mutableStateOf<String?>(null) }
    var touchPosition by remember { mutableStateOf(Offset.Zero) }
    var initialTouchOffsetInItem by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(activeFolderId) {
        if (activeFolderId != null) {
            editingFolderName = folders.find { it.id == activeFolderId }?.name ?: ""
        } else {
            isEditMode = false
            draggingItemPkg = null
        }
    }

    LaunchedEffect(launchRequest) {
        val request = launchRequest ?: return@LaunchedEffect
        delay(280)
        context.packageManager.getLaunchIntentForPackage(request.app.packageName)?.let {
            launchAppNoTransition(context, it)
        }
        launchRequest = null
    }

    Box(modifier = Modifier.fillMaxSize().onGloballyPositioned { drawerSize = it.size }) {
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
                    color = mainTextColor
                )
                Row {
                    if (searchQuery.isBlank()) {
                        var isCreateFolderDialogOpen by remember { mutableStateOf(false) }
                        var folderNameInput by remember { mutableStateOf("") }
                        
                        IconButton(onClick = { isCreateFolderDialogOpen = true }) { 
                            Icon(Lucide.FolderPlus, contentDescription = "Create Folder", tint = mainTextColor) 
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
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = mainTextColor)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            val searchIntSrc = remember { MutableInteractionSource() }
            Box(modifier = Modifier.fillMaxWidth().background(mainTextColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 14.dp).clickable(
                interactionSource = searchIntSrc,
                indication = null
            ) { focusRequester.requestFocus() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = mainTextColor.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = searchQuery, onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        textStyle = androidx.compose.ui.text.TextStyle(color = mainTextColor, fontSize = 16.sp * fontSize.scale),
                        cursorBrush = SolidColor(mainTextColor), singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                        decorationBox = { if (searchQuery.isEmpty()) Text("Apps durchsuchen...", color = mainTextColor.copy(alpha = 0.4f), fontSize = 16.sp * fontSize.scale); it() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            val adaptiveColumns = when (iconSize) {
                IconSize.SMALL -> 5
                IconSize.STANDARD -> 4
                IconSize.LARGE -> 3
            }

            val gridState = rememberLazyGridState()
            var swipeDragY by remember { mutableStateOf(0f) }
            val swipeCloseThresholdPx = with(density) { 64.dp.toPx() }
            val swipeToCloseConnection = remember(gridState, swipeCloseThresholdPx, onClose) {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        val atTop = gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
                        if (source == NestedScrollSource.UserInput && atTop && available.y > 0f) {
                            swipeDragY += available.y
                            if (swipeDragY >= swipeCloseThresholdPx) {
                                swipeDragY = 0f
                                onClose()
                            }
                            return Offset(0f, available.y)
                        }
                        if (!atTop || available.y < 0f) {
                            swipeDragY = 0f
                        }
                        return Offset.Zero
                    }

                    override suspend fun onPreFling(available: Velocity): Velocity {
                        val atTop = gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
                        if (atTop && available.y > 1500f) {
                            swipeDragY = 0f
                            onClose()
                            return available
                        }
                        return Velocity.Zero
                    }

                    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                        swipeDragY = 0f
                        return Velocity.Zero
                    }
                }
            }

            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(adaptiveColumns),
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(swipeToCloseConnection),
                    state = gridState,
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
                            onUpdateFolders = onUpdateFolders,
                            bouncePackage = returnIconPackage,
                            onAppLaunchRequested = { requestedApp, bounds ->
                                if (launchRequest == null) {
                                    onAppLaunchForReturn(requestedApp.packageName, bounds)
                                    launchRequest = LaunchRequest(requestedApp, bounds)
                                }
                            }
                        )
                    }
                }
            }
        }

        val launchTransition = updateTransition(targetState = launchRequest != null, label = "LaunchTransition")
        val launchProgress by launchTransition.animateFloat(
            transitionSpec = { spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium) },
            label = "LaunchProgress"
        ) { isVisible ->
            if (isVisible) 1f else 0f
        }
        val launchOverlayAlpha by launchTransition.animateFloat(
            transitionSpec = { tween(durationMillis = 220, easing = LinearEasing) },
            label = "LaunchOverlayAlpha"
        ) { isVisible ->
            if (isVisible) 1f else 0f
        }
        val launchBounds = launchRequest?.bounds
        val launchTranslation = remember(launchBounds, drawerSize) {
            if (launchBounds != null && drawerSize.width > 0 && drawerSize.height > 0) {
                val centerX = drawerSize.width / 2f
                val centerY = drawerSize.height / 2f
                Offset(launchBounds.center.x - centerX, launchBounds.center.y - centerY)
            } else {
                Offset.Zero
            }
        }
        val launchStartScale = remember(launchBounds, drawerSize) {
            if (launchBounds != null && drawerSize.width > 0 && drawerSize.height > 0) {
                val wScale = launchBounds.width / drawerSize.width.toFloat()
                val hScale = launchBounds.height / drawerSize.height.toFloat()
                max(wScale, hScale).coerceIn(0.06f, 0.35f)
            } else {
                0.08f
            }
        }

        if (launchProgress > 0f) {
            val scale = launchStartScale + (1f - launchStartScale) * launchProgress
            val translationX = launchTranslation.x * (1f - launchProgress)
            val translationY = launchTranslation.y * (1f - launchProgress)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2000f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            this.scaleX = scale
                            this.scaleY = scale
                            this.translationX = translationX
                            this.translationY = translationY
                            this.transformOrigin = TransformOrigin.Center
                            this.alpha = launchOverlayAlpha
                        }
                        .background(colorTheme.drawerBackground)
                )
            }
        }

        // Folder Popup Overlay with symmetric animation
        val showFolder = activeFolderId != null
        val folderTransition = updateTransition(targetState = showFolder, label = "FolderTransition")
        val folderProgress by folderTransition.animateFloat(
            transitionSpec = {
                tween(
                    durationMillis = if (targetState) 420 else 320,
                    easing = FastOutSlowInEasing
                )
            },
            label = "FolderProgress"
        ) { isVisible ->
            if (isVisible) 1f else 0f
        }
        val overlayAlpha by folderTransition.animateFloat(
            transitionSpec = { tween(durationMillis = 260, easing = LinearEasing) },
            label = "OverlayAlpha"
        ) { isVisible ->
            if (isVisible) 0.35f else 0f
        }
        val folderTranslation = remember(folderPosition, drawerSize) {
            if (drawerSize.width > 0 && drawerSize.height > 0) {
                val centerX = drawerSize.width / 2f
                val centerY = drawerSize.height / 2f
                Offset(folderPosition.x - centerX, folderPosition.y - centerY)
            } else {
                Offset.Zero
            }
        }

        if (folderProgress > 0f || showFolder) {
            // Nutze lastValidFolder, damit der Inhalt während der gesamten Schließanimation bleibt
            lastValidFolder?.let { currentActiveFolder ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = overlayAlpha))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (isEditMode) isEditMode = false else activeFolderId = null
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val scale = 0.05f + (1f - 0.05f) * folderProgress
                    val translationX = folderTranslation.x * (1f - folderProgress)
                    val translationY = folderTranslation.y * (1f - folderProgress)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .wrapContentHeight()
                            .graphicsLayer {
                                this.scaleX = scale
                                this.scaleY = scale
                                this.translationX = translationX
                                this.translationY = translationY
                                this.transformOrigin = TransformOrigin.Center
                            }
                            .clickable(enabled = false) {},
                        color = colorTheme.drawerBackground.copy(alpha = 0.98f),
                        shape = RoundedCornerShape(32.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, mainTextColor.copy(alpha = 0.15f)),
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
                                            color = mainTextColor,
                                            fontSize = 22.sp * fontSize.scale,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        ),
                                        cursorBrush = SolidColor(mainTextColor),
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
                                        color = mainTextColor,
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
                                        tint = mainTextColor,
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

                            LaunchedEffect(draggingItemPkg, touchPosition) {
                                if (draggingItemPkg != null) {
                                    val threshold = with(density) { 45.dp.toPx() }
                                    while (draggingItemPkg != null) {
                                        val surfaceWidth = drawerSize.width * 0.85f
                                        if (touchPosition.x < threshold && pagerState.currentPage > 0) {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                            delay(900)
                                        } else if (touchPosition.x > surfaceWidth - threshold && pagerState.currentPage < pages - 1) {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                            delay(900)
                                        }
                                        delay(100)
                                    }
                                }
                            }

                            Box(modifier = Modifier.height(340.dp)) {
                                var gridAreaSize by remember { mutableStateOf(IntSize.Zero) }

                                val currentFolderState by rememberUpdatedState(currentActiveFolder)
                                val currentFoldersState by rememberUpdatedState(folders)

                                fun performReorder(currentTouch: Offset, currentPage: Int) {
                                    val pkg = draggingItemPkg ?: return
                                    val fromIdx = currentFolderState.appPackageNames.indexOf(pkg)
                                    if (fromIdx == -1) return

                                    val cellW = gridAreaSize.width / 3f
                                    val cellH = gridAreaSize.height / 3f
                                    val targetCol = (currentTouch.x / cellW).toInt().coerceIn(0, 2)
                                    val targetRow = (currentTouch.y / cellH).toInt().coerceIn(0, 2)
                                    val targetIdx = (currentPage * 9 + (targetRow * 3 + targetCol)).coerceIn(0, currentFolderState.appPackageNames.size - 1)

                                    if (targetIdx != fromIdx) {
                                        val newList = currentFolderState.appPackageNames.toMutableList()
                                        newList.removeAt(fromIdx)
                                        newList.add(targetIdx, pkg)

                                        onUpdateFolders(currentFoldersState.map {
                                            if (it.id == currentFolderState.id) it.copy(appPackageNames = newList) else it
                                        })
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }

                                LaunchedEffect(pagerState.currentPage) {
                                    if (draggingItemPkg != null) {
                                        performReorder(touchPosition, pagerState.currentPage)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .onGloballyPositioned { 
                                            gridAreaSize = it.size 
                                        }
                                        .pointerInput(isEditMode) {
                                            if (!isEditMode) return@pointerInput
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { offset ->
                                                    val cellW = gridAreaSize.width / 3f
                                                    val cellH = gridAreaSize.height / 3f
                                                    
                                                    val col = (offset.x / cellW).toInt().coerceIn(0, 2)
                                                    val row = (offset.y / cellH).toInt().coerceIn(0, 2)
                                                    val idxInPage = row * 3 + col
                                                    val globalIdx = pagerState.currentPage * 9 + idxInPage
                                                    
                                                    if (globalIdx < folderApps.size) {
                                                        draggingItemPkg = folderApps[globalIdx].packageName
                                                        touchPosition = offset
                                                        initialTouchOffsetInItem = Offset(offset.x % cellW, offset.y % cellH)
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                },
                                                onDragEnd = { draggingItemPkg = null },
                                                onDragCancel = { draggingItemPkg = null },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    touchPosition += dragAmount
                                                    performReorder(touchPosition, pagerState.currentPage)
                                                }
                                            )
                                        }
                                ) {
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxSize(),
                                        pageSpacing = 16.dp,
                                        userScrollEnabled = !isEditMode && draggingItemPkg == null
                                    ) { page ->
                                        val startIdx = page * 9
                                        val endIdx = (startIdx + 9).coerceAtMost(folderApps.size)
                                        val pageApps = folderApps.subList(startIdx, endIdx)
                                        
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(3),
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(24.dp),
                                            userScrollEnabled = false
                                        ) {
                                            itemsIndexed(pageApps, key = { _, app -> app.packageName }) { indexInPage, app ->
                                                val globalIndex = startIdx + indexInPage
                                                val isDragging = draggingItemPkg == app.packageName
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .let { if (isDragging) it else it.animateItem() }
                                                        .zIndex(if (isDragging) 0f else 1f)
                                                        .graphicsLayer { 
                                                            rotationZ = if (isEditMode && !isDragging) {
                                                                if (globalIndex % 2 == 0) wiggleAngle else -wiggleAngle
                                                            } else 0f
                                                            alpha = if (isDragging) 0f else 1f
                                                        }
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
                                                        isEditMode = isEditMode,
                                                        bouncePackage = returnIconPackage,
                                                        onAppLaunchRequested = { requestedApp, bounds ->
                                                            if (launchRequest == null) {
                                                                onAppLaunchForReturn(requestedApp.packageName, bounds)
                                                                launchRequest = LaunchRequest(requestedApp, bounds)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    if (draggingItemPkg != null) {
                                        val draggedApp = folderApps.find { it.packageName == draggingItemPkg }
                                        if (draggedApp != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .graphicsLayer {
                                                        this.translationX = touchPosition.x - initialTouchOffsetInItem.x
                                                        this.translationY = touchPosition.y - initialTouchOffsetInItem.y
                                                        this.scaleX = 1.3f
                                                        this.scaleY = 1.3f
                                                        this.alpha = 0.9f
                                                    }
                                                    .zIndex(1000f)
                                                    .shadow(24.dp, RoundedCornerShape(16.dp))
                                            ) {
                                                AppItem(
                                                    app = draggedApp,
                                                    adaptiveColumns = 3,
                                                    isFavorite = isFavorite(draggedApp.packageName),
                                                    onToggleFavorite = onToggleFavorite,
                                                    folders = folders,
                                                    onUpdateFolders = onUpdateFolders,
                                                    isInFolder = true,
                                                    currentFolderId = currentActiveFolder.id,
                                                    isEditMode = true,
                                                    onAppLaunchRequested = null
                                                )
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
                                        val color = if (pagerState.currentPage == iteration) mainTextColor else mainTextColor.copy(alpha = 0.3f)
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
    onOpenFolderConfig: (FolderInfo) -> Unit
) {
    val fontSize = LocalFontSize.current
    val iconSizeValue = LocalIconSize.current.size
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White

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
            Box(modifier = Modifier.size(iconSizeValue).background(mainTextColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Lucide.Folder, contentDescription = null, tint = mainTextColor, modifier = Modifier.size(iconSizeValue * 0.6f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = folder.name, fontSize = 11.sp * fontSize.scale, color = mainTextColor.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
    isEditMode: Boolean = false,
    onAppLaunchRequested: ((AppInfo, Rect?) -> Unit)? = null,
    bouncePackage: String? = null
) {
    val context = LocalContext.current
    val fontSize = LocalFontSize.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White

    var showActions by remember { mutableStateOf(false) }
    var showFolderSelection by remember { mutableStateOf(false) }
    val intSrc = remember { MutableInteractionSource() }
    var itemBounds by remember { mutableStateOf<Rect?>(null) }
    val bounceScale by animateFloatAsState(
        targetValue = if (bouncePackage == app.packageName) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "DrawerReturnBounce"
    )

    Box(modifier = Modifier.width(if (adaptiveColumns == 5) 64.dp else 80.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    itemBounds = coordinates.boundsInRoot()
                }
                .graphicsLayer {
                    scaleX = bounceScale
                    scaleY = bounceScale
                }
                .bounceClick(intSrc, enabled = !isEditMode)
                .combinedClickable(
                    interactionSource = intSrc,
                    indication = null,
                    enabled = !isEditMode,
                    onClick = {
                        onAppLaunchRequested?.let { it(app, itemBounds) } ?: run {
                            context.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                                context.startActivity(it)
                            }
                        }
                    },
                    onLongClick = {
                        showActions = true
                    }
                )
        ) {
            AppIconView(app)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = app.label, fontSize = 11.sp * fontSize.scale, color = mainTextColor.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        
        if (showActions) {
            AppContextMenu(
                app = app,
                isFavorite = isFavorite,
                onDismiss = { showActions = false },
                onToggleFavorite = { onToggleFavorite(app.packageName) },
                onAppInfo = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", app.packageName, null) })
                },
                onUninstall = {
                    context.startActivity(Intent(Intent.ACTION_DELETE).apply { data = Uri.fromParts("package", app.packageName, null) })
                },
                onMoveToFolder = if (!isInFolder) { { showFolderSelection = true } } else null,
                onRemoveFromFolder = if (isInFolder && currentFolderId != null) {
                    { onUpdateFolders(LauncherLogic.removeAppFromFolder(folders, currentFolderId, app.packageName)) }
                } else null
            )
        }

        if (showFolderSelection) {
            Dialog(onDismissRequest = { showFolderSelection = false }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "In Ordner verschieben",
                            fontSize = 18.sp * fontSize.scale,
                            fontWeight = FontWeight.SemiBold,
                            color = mainTextColor,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        if (folders.isEmpty()) {
                            Text("Keine Ordner vorhanden", color = mainTextColor.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
                        }
                        
                        folders.forEach { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onUpdateFolders(LauncherLogic.addAppToFolder(folders, folder.id, app.packageName))
                                        showFolderSelection = false
                                    }
                                    .padding(vertical = 14.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Lucide.Folder, contentDescription = null, modifier = Modifier.size(20.dp), tint = mainTextColor)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(folder.name, fontSize = 16.sp * fontSize.scale, color = mainTextColor)
                            }
                        }
                        
                        TextButton(
                            onClick = { showFolderSelection = false },
                            modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                        ) {
                            Text("Abbrechen")
                        }
                    }
                }
            }
        }
    }
}

private data class LaunchRequest(
    val app: AppInfo,
    val bounds: Rect?
)
