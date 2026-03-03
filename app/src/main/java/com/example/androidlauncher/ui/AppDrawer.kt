package com.example.androidlauncher.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.androidlauncher.ui.theme.LocalAppFont
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalFontSize
import com.example.androidlauncher.ui.theme.LocalFontWeight
import com.example.androidlauncher.ui.theme.LocalIconSize
import com.example.androidlauncher.ui.theme.LocalLiquidGlassEnabled
import com.composables.icons.lucide.*
import kotlinx.coroutines.delay
import kotlin.math.max

/**
 * AppDrawer ist die Hauptkomponente, die die App-Übersicht, Ordner und die Suchfunktion anzeigt.
 * Sie verwaltet den Zustand für die Interaktionen wie das Öffnen von Ordnern, den Bearbeitungsmodus und Drag-and-Drop.
 */
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
    // Lokale Kontext- und Themenvariablen für den Zugriff auf Systemressourcen und UI-Parameter.
    val context = LocalContext.current
    val colorTheme = LocalColorTheme.current
    val fontSize = LocalFontSize.current
    val fontWeight = LocalFontWeight.current
    val iconSize = LocalIconSize.current
    val appFont = LocalAppFont.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val isLiquidGlassEnabled = LocalLiquidGlassEnabled.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White

    // Thematisch angepasster heller Hintergrund für Dialoge im dunklen Textmodus.
    val themedLightBackground = remember(colorTheme.primary) {
        val primary = colorTheme.primary
        Color(
            red = primary.red * 0.90f + 0.10f,
            green = primary.green * 0.90f + 0.10f,
            blue = primary.blue * 0.90f + 0.10f,
            alpha = 1f
        )
    }

    // Controller für Tastatur und UI-Elemente.
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Zustand für die Suchanfrage.
    var searchQuery by remember { mutableStateOf("") }

    // Filtert die sichtbaren Apps basierend auf der aktuellen Suchanfrage.
    val visibleApps = remember(apps.toList(), folders, searchQuery) {
        if (searchQuery.isBlank()) {
            LauncherLogic.getVisibleApps(apps.toList(), folders)
        } else {
            LauncherLogic.filterApps(apps.toList(), searchQuery)
        }
    }

    // Fokus-Requester für das Suchfeld.
    val focusRequester = remember { FocusRequester() }

    // Zustand für die aktuell geöffneten Ordner.
    var activeFolderId by remember { mutableStateOf<String?>(null) }
    val activeFolder = remember(activeFolderId, folders) {
        if (activeFolderId != null) folders.find { it.id == activeFolderId } else null
    }
    
    // Merkt sich den letzten gültigen Ordner, um die UI beim Schließen des Ordners beizubehalten.
    var lastValidFolder by remember { mutableStateOf<FolderInfo?>(null) }
    LaunchedEffect(activeFolder) {
        if (activeFolder != null) {
            lastValidFolder = activeFolder
        }
    }

    // CUSTOM: Schließt den Ordner automatisch mit Exit-Animation, wenn er gelöscht wurde
    // (z.B. über das Konfigurationsmenü, wenn alle Apps abgewählt wurden).
    LaunchedEffect(folders) {
        if (activeFolderId != null && folders.none { it.id == activeFolderId }) {
            activeFolderId = null
        }
    }
    
    // Zustände für UI-Layout und Animationen.
    var drawerSize by remember { mutableStateOf(IntSize.Zero) }
    var folderPosition by remember { mutableStateOf(Offset.Zero) }
    var launchRequest by remember { mutableStateOf<LaunchRequest?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    // CUSTOM: Verfolgt, ob der Ordnername gerade bearbeitet wird, um das Jigglen zu pausieren.
    var isFolderNameFocused by remember { mutableStateOf(false) }
    var editingFolderName by remember { mutableStateOf("") }
    // Behandelt den Zurück-Button, um den Ordner oder den Bearbeitungsmodus zu schließen.
    BackHandler(enabled = activeFolderId != null) {
        if (isEditMode) {
            isEditMode = false
        } else {
            activeFolderId = null
        }
    }

    // Drag-and-Drop-Zustand für Elemente innerhalb eines Ordners.
    var draggingItemPkg by remember { mutableStateOf<String?>(null) }
    var touchPosition by remember { mutableStateOf(Offset.Zero) }
    var initialTouchOffsetInItem by remember { mutableStateOf(Offset.Zero) }
    var gridAreaSize by remember { mutableStateOf(IntSize.Zero) }

    // Setzt den Bearbeitungsmodus zurück, wenn ein Ordner geschlossen wird.
    LaunchedEffect(activeFolderId) {
        if (activeFolderId != null) {
            editingFolderName = folders.find { it.id == activeFolderId }?.name ?: ""
        } else {
            isEditMode = false
            isFolderNameFocused = false
            draggingItemPkg = null
        }
    }

    // Sorgt dafür, dass ein Verlassen des Jiggle-Modus alle Drag-Zustände klickfertig zurücksetzt.
    LaunchedEffect(isEditMode) {
        if (!isEditMode) {
            draggingItemPkg = null
            touchPosition = Offset.Zero
            initialTouchOffsetInItem = Offset.Zero
            isFolderNameFocused = false
        }
    }

    // Startet die App nach einer kurzen Verzögerung für die Animation.
    LaunchedEffect(launchRequest) {
        val request = launchRequest ?: return@LaunchedEffect
        delay(280)
        context.packageManager.getLaunchIntentForPackage(request.app.packageName)?.let {
            launchAppNoTransition(context, it)
        }
        launchRequest = null
    }

    // Zustände für das App-Kontextmenü.
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    var menuAppBounds by remember { mutableStateOf<Rect?>(null) }
    var showFolderSelection by remember { mutableStateOf(false) }
    var folderSelectionApp by remember { mutableStateOf<AppInfo?>(null) }

    Box(modifier = Modifier.fillMaxSize().onGloballyPositioned { drawerSize = it.size }) {
        // Hintergrund-Overlay des App Drawers.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SolidColor(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)))
        )
        
        // Haupt-Layout für den App Drawer-Inhalt.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Kopfzeile mit Titel und Schließen-Button.
            @Suppress("DEPRECATION")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Apps",
                    fontSize = 24.sp * fontSize.scale,
                    fontWeight = fontWeight.weight,
                    color = mainTextColor
                )
                Row {
                    // Button zum Erstellen eines neuen Ordners (nur sichtbar, wenn nicht gesucht wird).
                    if (searchQuery.isBlank()) {
                        var isCreateFolderDialogOpen by remember { mutableStateOf(false) }
                        var folderNameInput by remember { mutableStateOf("") }
                        
                        IconButton(onClick = { isCreateFolderDialogOpen = true }) { 
                            Icon(Lucide.FolderPlus, contentDescription = "Create Folder", tint = mainTextColor) 
                        }
                        
                        // Dialog zum Erstellen eines neuen Ordners.
                        if (isCreateFolderDialogOpen) {
                            Dialog(onDismissRequest = { isCreateFolderDialogOpen = false }) {
                                val dialogBorderModifier = if (isLiquidGlassEnabled) {
                                    Modifier.border(BorderStroke(1.2.dp, LiquidGlass.borderBrush(isDarkTextEnabled)), RoundedCornerShape(28.dp))
                                } else {
                                    Modifier.border(BorderStroke(1.dp, mainTextColor.copy(alpha = 0.12f)), RoundedCornerShape(28.dp))
                                }

                                Surface(
                                    modifier = Modifier
                                        .wrapContentWidth()
                                        .wrapContentHeight()
                                        .then(dialogBorderModifier),
                                    shape = RoundedCornerShape(28.dp),
                                    color = if (isDarkTextEnabled) themedLightBackground else colorTheme.drawerBackground,
                                    tonalElevation = 6.dp
                                ) {
                                    Column(modifier = Modifier.padding(24.dp)) {
                                        Text(
                                            text = "Neuer Ordner",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = mainTextColor
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Eingabefeld für den Ordnernamen.
                                        val inputModifier = if (isLiquidGlassEnabled) {
                                            Modifier
                                                .background(LiquidGlass.glassBrush(isDarkTextEnabled), RoundedCornerShape(12.dp))
                                                .border(BorderStroke(1.2.dp, LiquidGlass.borderBrush(isDarkTextEnabled)), RoundedCornerShape(12.dp))
                                        } else {
                                            Modifier.background(mainTextColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(inputModifier)
                                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                        ) {
                                            BasicTextField(
                                                value = folderNameInput,
                                                onValueChange = { folderNameInput = it },
                                                modifier = Modifier.fillMaxWidth(),
                                                textStyle = androidx.compose.ui.text.TextStyle(
                                                    color = mainTextColor,
                                                    fontSize = 16.sp
                                                ),
                                                cursorBrush = SolidColor(mainTextColor),
                                                singleLine = true,
                                                decorationBox = { innerTextField ->
                                                    if (folderNameInput.isEmpty()) {
                                                        Text(
                                                            "Name eingeben",
                                                            color = mainTextColor.copy(alpha = 0.4f),
                                                            fontSize = 16.sp
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))

                                        // Buttons zum Erstellen oder Abbrechen.
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = { isCreateFolderDialogOpen = false }) {
                                                Text("Abbrechen", color = Color.Gray)
                                            }
                                            TextButton(onClick = {
                                                if (folderNameInput.isNotBlank()) {
                                                    // Verhindert doppelte Ordnernamen.
                                                    val nameExists = folders.any { it.name == folderNameInput }
                                                    if (nameExists) {
                                                        Toast.makeText(context, "Ordner mit diesem Namen existiert bereits", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        val newFolder = LauncherLogic.createNewFolder(folderNameInput)
                                                        onOpenFolderConfig(newFolder)
                                                        folderNameInput = ""
                                                        isCreateFolderDialogOpen = false
                                                    }
                                                }
                                            }) {
                                                Text("Erstellen", color = mainTextColor)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = mainTextColor)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Suchleiste mit glasartigem Effekt.
            val searchIntSrc = remember { MutableInteractionSource() }
            val searchBarModifier = if (isLiquidGlassEnabled) {
                Modifier
                    .background(LiquidGlass.glassBrush(isDarkTextEnabled), RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.2.dp, LiquidGlass.borderBrush(isDarkTextEnabled)), RoundedCornerShape(12.dp))
            } else {
                Modifier.background(mainTextColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            }

            Box(modifier = Modifier.fillMaxWidth().then(searchBarModifier).padding(horizontal = 16.dp, vertical = 14.dp).clickable(
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

            // Passt die Anzahl der Spalten im Grid an die ausgewählte Icon-Größe an.
            val adaptiveColumns = when (iconSize) {
                IconSize.SMALL -> 5
                IconSize.STANDARD -> 4
                IconSize.LARGE -> 3
            }

            // Logik zum Schließen des Drawers durch Wischen nach unten.
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

            // Haupt-Grid für Apps und Ordner.
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
                    // Zeigt Ordner an, wenn keine Suche aktiv ist.
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

                    // Zeigt die gefilterten Apps an mit Staggered-Animation bei Suche.
                    itemsIndexed(items = visibleApps, key = { _, app -> app.packageName }) { index, app ->
                        val isSearching = searchQuery.isNotBlank()
                        var isVisible by remember(app.packageName, isSearching) { mutableStateOf(!isSearching) }
                        
                        LaunchedEffect(app.packageName, isSearching) {
                            if (isSearching) {
                                delay((index % 12) * 35L)
                                isVisible = true
                            }
                        }

                        Box(modifier = Modifier.animateItem()) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isVisible,
                                enter = fadeIn(animationSpec = tween(400)) + 
                                        scaleIn(initialScale = 0.85f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) +
                                        slideInVertically(initialOffsetY = { 30 }, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)),
                                exit = fadeOut(animationSpec = tween(200))
                            ) {
                                AppItem(
                                    app = app,
                                    adaptiveColumns = adaptiveColumns,
                                    isFavorite = isFavorite(app.packageName),
                                    onToggleFavorite = onToggleFavorite,
                                    folders = folders,
                                    onUpdateFolders = onUpdateFolders,
                                    bouncePackage = returnIconPackage,
                                    onLongPress = { appInfo, bounds ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuApp = appInfo
                                        menuAppBounds = bounds
                                    },
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
            }
        }

        // Animation für den App-Start.
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
                        .background(MaterialTheme.colorScheme.background)
                )
            }
        }

        // Popup-Overlay für den Ordner.
        val showFolder = activeFolderId != null
        val folderTransition = updateTransition(targetState = showFolder, label = "FolderTransition")
        
        // CUSTOM: Berechnet, ob der Ordner gerade gelöscht wurde (für die Exit-Animation).
        val isCurrentFolderDeleted = lastValidFolder != null && folders.none { it.id == lastValidFolder?.id }

        val folderProgress by folderTransition.animateFloat(
            transitionSpec = {
                // CUSTOM: Spezielle Animation für das Löschen (Exit) - schneller und federnd.
                if (targetState) {
                    tween(durationMillis = 420, easing = FastOutSlowInEasing)
                } else {
                    if (isCurrentFolderDeleted) {
                        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                    } else {
                        tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    }
                }
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

                    val folderBorder = if (isLiquidGlassEnabled) {
                        BorderStroke(1.2.dp, LiquidGlass.borderBrush(isDarkTextEnabled))
                    } else {
                        BorderStroke(1.dp, mainTextColor.copy(alpha = 0.15f))
                    }

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
                                // CUSTOM: Zusätzlicher Fade-out Effekt beim Löschen.
                                if (isCurrentFolderDeleted) {
                                    this.alpha = folderProgress
                                }
                            }
                            .clickable(enabled = false) {},
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
                        shape = RoundedCornerShape(32.dp),
                        border = folderBorder,
                        shadowElevation = 24.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // CUSTOM: Gemeinsame Wackel-Animation für Apps und den Ordnernamen im Bearbeitungsmodus.
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

                            // Kopfzeile des Ordners mit Titel und Bearbeiten-Button.
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                if (isEditMode) {
                                    // Eingabefeld für den Ordnernamen im Bearbeitungsmodus.
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
                                            fontWeight = fontWeight.weight,
                                            fontFamily = appFont.fontFamily,
                                            textAlign = TextAlign.Center
                                        ),
                                        cursorBrush = SolidColor(mainTextColor),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = {
                                            // CUSTOM: Bestätigt den Namen und blendet die Tastatur aus, bleibt aber im Jiggle-Modus.
                                            isFolderNameFocused = false
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                        }),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 40.dp)
                                            .onFocusChanged { 
                                                // CUSTOM: Aktualisiert den Fokus-Status, um die Animation bei Bedarf zu stoppen.
                                                isFolderNameFocused = it.isFocused 
                                            }
                                            .graphicsLayer {
                                                // CUSTOM: Lässt den Namen nur jigglen, wenn er nicht aktiv bearbeitet wird.
                                                rotationZ = if (!isFolderNameFocused) wiggleAngle else 0f
                                            }
                                    )
                                } else {
                                    Text(
                                        currentActiveFolder.name,
                                        color = mainTextColor,
                                        fontSize = 22.sp * fontSize.scale,
                                        fontWeight = fontWeight.weight,
                                        fontFamily = appFont.fontFamily,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp)
                                    )
                                }
                                
                                IconButton(
                                    onClick = {
                                        if (isEditMode) {
                                            draggingItemPkg = null
                                            isEditMode = false
                                            isFolderNameFocused = false
                                        } else {
                                            isEditMode = true
                                        }
                                    },
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
                            
                            // Lädt die Apps, die sich im Ordner befinden.
                            val folderApps = remember(currentActiveFolder.appPackageNames, apps) {
                                currentActiveFolder.appPackageNames.mapNotNull { pkg -> 
                                    apps.find { it.packageName == pkg } 
                                }
                            }
                            val currentFolderApps by rememberUpdatedState(folderApps)
                            
                            // Pager-Setup für die Ordnerseiten.
                            val itemsPerPage = 9
                            val pages = max(1, (folderApps.size + itemsPerPage - 1) / itemsPerPage)
                            val pagerState = rememberPagerState(pageCount = { pages })
                            
                            // Logik für das automatische Blättern der Seiten beim Ziehen an den Rand.
                            LaunchedEffect(draggingItemPkg) {
                                if (draggingItemPkg == null) return@LaunchedEffect
                                
                                val edgeThreshold = with(density) { 32.dp.toPx() }
                                val dwellTime = 400L // Reaktionszeit für das Halten am Rand.
                                
                                while (draggingItemPkg != null) {
                                    val currentWidth = gridAreaSize.width.toFloat()
                                    if (currentWidth > 0) {
                                        if (touchPosition.x < edgeThreshold && pagerState.currentPage > 0) {
                                            delay(dwellTime)
                                            if (draggingItemPkg != null && touchPosition.x < edgeThreshold && pagerState.currentPage > 0) {
                                                pagerState.animateScrollToPage(
                                                    pagerState.currentPage - 1,
                                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                                )
                                                delay(600) // Abklingzeit nach dem Scrollen.
                                            }
                                        } else if (touchPosition.x > currentWidth - edgeThreshold && pagerState.currentPage < pages - 1) {
                                            delay(dwellTime)
                                            if (draggingItemPkg != null && touchPosition.x > currentWidth - edgeThreshold && pagerState.currentPage < pages - 1) {
                                                pagerState.animateScrollToPage(
                                                    pagerState.currentPage + 1,
                                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                                )
                                                delay(600)
                                            }
                                        }
                                    }
                                    delay(50)
                                }
                            }

                            // Container für das App-Grid im Ordner.
                            Box(modifier = Modifier.height(340.dp)) {
                                val currentFolderState by rememberUpdatedState(currentActiveFolder)
                                val currentFoldersState by rememberUpdatedState(folders)

                                // Logik zum Neuanordnen der Apps beim Ziehen.
                                fun performReorder(currentTouch: Offset, currentPage: Int) {
                                    val pkg = draggingItemPkg ?: return
                                    val fromIdx = currentFolderState.appPackageNames.indexOf(pkg)
                                    if (fromIdx == -1) return

                                    if (gridAreaSize.width <= 0 || gridAreaSize.height <= 0) return

                                    val cellW = gridAreaSize.width / 3f
                                    val cellH = gridAreaSize.height / 3f
                                    
                                    val col = (currentTouch.x / cellW).toInt().coerceIn(0, 2)
                                    val row = (currentTouch.y / cellH).toInt().coerceIn(0, 2)
                                    val targetIdxInPage = row * 3 + col
                                    
                                    val targetIdx = (currentPage * itemsPerPage + targetIdxInPage).coerceIn(0, currentFolderState.appPackageNames.size - 1)

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

                                // Aktualisiert die Anordnung, wenn sich die Seite ändert.
                                LaunchedEffect(pagerState.currentPage) {
                                    if (draggingItemPkg != null) {
                                        performReorder(touchPosition, pagerState.currentPage)
                                    }
                                }

                                // Verhindert das Scrollen über die Pager-Grenzen hinaus.
                                val folderBoundaryBlocker = remember(pagerState, pages) {
                                    object : NestedScrollConnection {
                                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                            if (pages <= 1) return Offset(x = available.x, y = 0f)
                                            
                                            val isAtStart = pagerState.currentPage == 0 && pagerState.currentPageOffsetFraction <= 0.001f
                                            val isAtEnd = pagerState.currentPage == pages - 1 && pagerState.currentPageOffsetFraction >= -0.001f
                                            
                                            if ((isAtStart && available.x > 0f) || (isAtEnd && available.x < 0f)) {
                                                return Offset(x = available.x, y = 0f) 
                                            }
                                            return Offset.Zero
                                        }

                                        override suspend fun onPreFling(available: Velocity): Velocity {
                                            if (pages <= 1) return available
                                            
                                            val isAtStart = pagerState.currentPage == 0
                                            val isAtEnd = pagerState.currentPage == pages - 1
                                            
                                            if ((isAtStart && available.x > 0f) || (isAtEnd && available.x < 0f)) {
                                                return available
                                            }
                                            return Velocity.Zero
                                        }
                                    }
                                }

                                // Drag-and-Drop-Gesten-Erkennung.
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
                                                    val globalIdx = pagerState.currentPage * itemsPerPage + idxInPage
                                                    
                                                    val currentApps = currentFolderApps
                                                    if (globalIdx < currentApps.size) {
                                                        draggingItemPkg = currentApps[globalIdx].packageName
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
                                    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                                        HorizontalPager(
                                            state = pagerState,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .nestedScroll(folderBoundaryBlocker),
                                            pageSpacing = 16.dp,
                                            userScrollEnabled = draggingItemPkg == null && pages > 1,
                                            beyondViewportPageCount = 1
                                        ) { page ->
                                            val startIdx = page * itemsPerPage
                                            val endIdx = (startIdx + itemsPerPage).coerceAtMost(folderApps.size)
                                            val pageApps = folderApps.subList(startIdx, endIdx)
                                            
                                            LazyVerticalGrid(
                                                columns = GridCells.Fixed(3),
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                                userScrollEnabled = false,
                                                // ── CUSTOM: Extra Padding damit Minus-Badges an allen Rändern
                                                // (oben, links, rechts) nicht am Ordner-Rand abgeschnitten werden.
                                                contentPadding = PaddingValues(top = 20.dp, start = 8.dp, end = 8.dp)
                                            ) {
                                                itemsIndexed(pageApps, key = { _, app -> app.packageName }) { indexInPage, app ->
                                                    val globalIndex = startIdx + indexInPage
                                                    val isDragging = draggingItemPkg == app.packageName

                                                    // ── CUSTOM: Badge-Größe relativ zur Icon-Größe ──
                                                    // Skaliert proportional mit der gewählten Icon-Größe (SMALL/STANDARD/LARGE),
                                                    // damit das Badge immer klein aber bemerkbar bleibt.
                                                    val badgeSize = iconSize.size * 0.28f
                                                    val minusWidth = iconSize.size * 0.14f
                                                    val minusHeight = 1.5.dp

                                                    Box(
                                                        modifier = Modifier
                                                            .let { if (isDragging) it else Modifier.animateItem() }
                                                            .zIndex(if (isDragging) 0f else 1f)
                                                            .graphicsLayer { 
                                                                rotationZ = if (isEditMode && !isDragging) {
                                                                    if (globalIndex % 2 == 0) wiggleAngle else -wiggleAngle
                                                                } else 0f
                                                                alpha = if (isDragging) 0f else 1f
                                                            }
                                                            // ── CUSTOM: Padding damit Badge innerhalb der Item-Bounds bleibt und Platz zum Wackeln hat ──
                                                            .padding(top = 12.dp, start = 2.dp, end = 2.dp)
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
                                                            onLongPress = { appInfo, bounds ->
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                menuApp = appInfo
                                                                menuAppBounds = bounds
                                                            },
                                                            onAppLaunchRequested = { requestedApp, bounds ->
                                                                if (launchRequest == null) {
                                                                    onAppLaunchForReturn(requestedApp.packageName, bounds)
                                                                    launchRequest = LaunchRequest(requestedApp, bounds)
                                                                }
                                                            }
                                                        )

                                                        // ── CUSTOM: Minus-Badge zum Entfernen im Jiggle-Modus ──
                                                        // Zeigt ein kleines rundes Badge mit Minus-Strich oben rechts am App-Icon.
                                                        if (isEditMode && !isDragging) {
                                                            val view = androidx.compose.ui.platform.LocalView.current
                                                            Box(
                                                                modifier = Modifier
                                                                    // FIX: Positionierung relativ zur Mitte, damit es am Icon klebt und nicht rechts abgeschnitten wird
                                                                    .align(Alignment.TopCenter)
                                                                    .offset(x = (iconSize.size / 2) - (badgeSize / 1.2f), y = (-4).dp)
                                                                    .zIndex(10f)
                                                                    .size(badgeSize)
                                                                    .background(
                                                                        mainTextColor.copy(alpha = 0.85f),
                                                                        CircleShape
                                                                    )
                                                                    .clickable(
                                                                        interactionSource = remember { MutableInteractionSource() },
                                                                        indication = null
                                                                    ) {
                                                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                                        // CUSTOM: Nutzt LauncherLogic um die App zu entfernen und den Ordner ggf. zu löschen.
                                                                        val updatedFolders = LauncherLogic.removeAppFromFolder(folders, currentActiveFolder.id, app.packageName)
                                                                        onUpdateFolders(updatedFolders)
                                                                        
                                                                        // CUSTOM: Wenn der Ordner gelöscht wurde (da leer), schließe das Popup sofort um die Animation zu starten.
                                                                        if (updatedFolders.none { it.id == currentActiveFolder.id }) {
                                                                            activeFolderId = null
                                                                        }
                                                                    },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(width = minusWidth, height = minusHeight)
                                                                        .background(
                                                                            if (isDarkTextEnabled) Color.White else Color(0xFF0F172A),
                                                                            RoundedCornerShape(0.75.dp)
                                                                        ))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Zeigt das gezogene App-Icon an der Touch-Position an.
                                    if (draggingItemPkg != null) {
                                        val draggedApp = currentFolderApps.find { it.packageName == draggingItemPkg }
                                        if (draggedApp != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .graphicsLayer {
                                                        this.translationX = touchPosition.x - initialTouchOffsetInItem.x
                                                        this.translationY = touchPosition.y - initialTouchOffsetInItem.y
                                                        this.scaleX = 1.25f
                                                        this.scaleY = 1.25f
                                                        this.alpha = 0.95f
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
                                                    onLongPress = { _, _ -> },
                                                    onAppLaunchRequested = null
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Seitenindikatoren für den Ordner-Pager.
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

        // Globales App-Kontextmenü.
        val currentMenuApp = menuApp
        if (currentMenuApp != null) {
            AppContextMenu(
                isFavorite = isFavorite(currentMenuApp.packageName),
                targetBounds = menuAppBounds,
                onDismiss = { menuApp = null },
                onToggleFavorite = { onToggleFavorite(currentMenuApp.packageName) },
                onAppInfo = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", currentMenuApp.packageName, null) })
                },
                onUninstall = {
                    try {
                        val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                            data = Uri.fromParts("package", currentMenuApp.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(uninstallIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Deinstallation konnte nicht gestartet werden", Toast.LENGTH_SHORT).show()
                    }
                },
                onMoveToFolder = if (folders.isNotEmpty()) { {
                    folderSelectionApp = currentMenuApp
                    showFolderSelection = true
                } } else null,
                onRemoveFromFolder = folders.find { it.appPackageNames.contains(currentMenuApp.packageName) }?.let { folder ->
                    { onUpdateFolders(LauncherLogic.removeAppFromFolder(folders, folder.id, currentMenuApp.packageName)) }
                }
            )
        }

        // Dialog zur Auswahl eines Ordners, in den eine App verschoben werden soll.
        if (showFolderSelection && folderSelectionApp != null) {
            Dialog(onDismissRequest = { showFolderSelection = false }) {
                val dialogBorderModifier = if (isLiquidGlassEnabled) {
                    Modifier.border(BorderStroke(1.2.dp, LiquidGlass.borderBrush(isDarkTextEnabled)), RoundedCornerShape(28.dp))
                } else {
                    Modifier.border(BorderStroke(1.dp, mainTextColor.copy(alpha = 0.12f)), RoundedCornerShape(28.dp))
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .wrapContentHeight()
                        .then(dialogBorderModifier),
                    shape = RoundedCornerShape(28.dp),
                    color = (if (isDarkTextEnabled) themedLightBackground else colorTheme.drawerBackground).copy(alpha = 0.98f),
                    shadowElevation = 16.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "In Ordner verschieben",
                            fontSize = 18.sp * fontSize.scale,
                            fontWeight = fontWeight.weight,
                            color = mainTextColor,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        folders.forEach { folder ->
                            @Suppress("DEPRECATION")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onUpdateFolders(LauncherLogic.addAppToFolder(folders, folder.id, folderSelectionApp!!.packageName))
                                        showFolderSelection = false
                                        menuApp = null
                                    }
                                    .padding(vertical = 14.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Lucide.Folder, contentDescription = null, modifier = Modifier.size(20.dp), tint = mainTextColor)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(folder.name, fontSize = 16.sp * fontSize.scale, color = mainTextColor)
                            }
                        }
                        
                        TextButton(
                            onClick = { showFolderSelection = false },
                            modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                        ) {
                            Text("Abbrechen", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Zeigt ein einzelnes Ordner-Icon im App Drawer an.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderItem(
    folder: FolderInfo,
    onClick: (Offset) -> Unit,
    onOpenFolderConfig: (FolderInfo) -> Unit
) {
    val fontSize = LocalFontSize.current
    val iconSizeValue = LocalIconSize.current.size
    val fontWeight = LocalFontWeight.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val isLiquidGlassEnabled = LocalLiquidGlassEnabled.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White

    val intSrc = remember { MutableInteractionSource() }
    var itemOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .width(80.dp)
            .onGloballyPositioned { coordinates ->
                // Speichert die Position des Ordners für die Animation.
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
            val folderBoxModifier = if (isLiquidGlassEnabled) {
                Modifier
                    .background(LiquidGlass.glassBrush(isDarkTextEnabled), RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.2.dp, LiquidGlass.borderBrush(isDarkTextEnabled)), RoundedCornerShape(12.dp))
            } else {
                Modifier.background(mainTextColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            }

            Box(modifier = Modifier.size(iconSizeValue).then(folderBoxModifier), contentAlignment = Alignment.Center) {
                Icon(Lucide.Folder, contentDescription = null, tint = mainTextColor, modifier = Modifier.size(iconSizeValue * 0.6f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = folder.name,
                fontSize = 11.sp * fontSize.scale,
                color = mainTextColor.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = fontWeight.weight
            )
        }
    }
}

/**
 * Zeigt ein einzelnes App-Icon an.
 */
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
    onLongPress: (AppInfo, Rect) -> Unit,
    onAppLaunchRequested: ((AppInfo, Rect?) -> Unit)? = null,
    bouncePackage: String? = null
) {
    val context = LocalContext.current
    val fontSize = LocalFontSize.current
    val fontWeight = LocalFontWeight.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White

    val intSrc = remember { MutableInteractionSource() }
    var itemBounds by remember { mutableStateOf<Rect?>(null) }
    // Animation für das "Zurückprallen" des Icons nach dem App-Start.
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
                        itemBounds?.let { onLongPress(app, it) }
                    }
                )
        ) {
            AppIconView(app)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = app.label,
                fontSize = 11.sp * fontSize.scale,
                color = mainTextColor.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = fontWeight.weight
            )
        }
    }
}

// Datenklasse für eine App-Start-Anfrage.
private data class LaunchRequest(
    val app: AppInfo,
    val bounds: Rect?
)
