package com.example.androidlauncher.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.composables.icons.lucide.*
import com.example.androidlauncher.LauncherLogic
import com.example.androidlauncher.R
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.DesignStyle
import com.example.androidlauncher.data.FolderInfo
import com.example.androidlauncher.data.IconSize
import com.example.androidlauncher.ui.LiquidGlass.designSurface
import com.example.androidlauncher.ui.theme.LocalAppFont
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle
import com.example.androidlauncher.ui.theme.LocalFontSize
import com.example.androidlauncher.ui.theme.LocalFontWeight
import com.example.androidlauncher.ui.theme.LocalHapticFeedbackEnabled
import com.example.androidlauncher.ui.theme.LocalIconSize
import com.example.androidlauncher.ui.theme.RethroneSprings
import com.example.androidlauncher.ui.theme.seedRevision
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * AppDrawer ist die Hauptkomponente, die die App-Übersicht, Ordner und die Suchfunktion anzeigt.
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
    onLaunchApp: (String, Intent, Rect?) -> Unit,
    returnIconPackage: String?,
    onHideApp: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val colorTheme = LocalColorTheme.current
    val fontSize = LocalFontSize.current
    val fontWeight = LocalFontWeight.current
    val iconSize = LocalIconSize.current
    val folderLayoutSpec = remember(configuration.screenWidthDp, configuration.screenHeightDp, iconSize) {
        calculateFolderOverlayLayoutSpec(
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
            iconSize = iconSize
        )
    }
    val appFont = LocalAppFont.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val designStyle = LocalDesignStyle.current
    val surfaceAccent = colorTheme.menuSurfaceColor(isDarkTextEnabled)
    val hapticEnabled = LocalHapticFeedbackEnabled.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val drawerBackgroundBrush = remember(colorTheme, isDarkTextEnabled, colorTheme.seedRevision()) {
        colorTheme.backgroundBrush(isDarkTextEnabled, alpha = 0.88f)
    }
    val menuSurfaceColor = remember(colorTheme, isDarkTextEnabled, colorTheme.seedRevision()) {
        colorTheme.menuSurfaceColor(isDarkTextEnabled)
    }
    val overlayScrimColor = remember(colorTheme, isDarkTextEnabled, colorTheme.seedRevision()) {
        colorTheme.overlayScrimColor(isDarkTextEnabled)
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val appDrawerVm: AppDrawerViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val customIcons by appDrawerVm.customIcons.collectAsState()
    val searchQuery by appDrawerVm.searchQuery.collectAsState()
    var searchExpanded by remember { mutableStateOf(searchQuery.isNotBlank()) }
    val visibleApps by appDrawerVm.visibleApps.collectAsState()

    // apps ist eine in-place mutierte SnapshotStateList – auf den Inhalt keyen (Kopie),
    // damit Installationen/Deinstallationen den ViewModel-State tatsächlich erreichen.
    val appsSnapshot = apps.toList()
    LaunchedEffect(appsSnapshot) { appDrawerVm.updateApps(appsSnapshot) }
    LaunchedEffect(folders) { appDrawerVm.updateFolders(folders) }

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

    LaunchedEffect(folders) {
        if (activeFolderId != null && folders.none { it.id == activeFolderId }) {
            activeFolderId = null
        }
    }

    var drawerSize by remember { mutableStateOf(IntSize.Zero) }
    var folderPosition by remember { mutableStateOf(Offset.Zero) }
    var isEditMode by remember { mutableStateOf(false) }
    var isFolderNameFocused by remember { mutableStateOf(false) }
    var editingFolderName by remember { mutableStateOf("") }

    // Predictive Back: die Zurück-Geste „schrumpft" den geöffneten Ordner live mit.
    val folderBackProgress = remember { Animatable(0f) }
    PredictiveBackHandler(enabled = activeFolderId != null) { backEvent ->
        try {
            backEvent.collect { ev ->
                if (!isEditMode) folderBackProgress.snapTo(ev.progress)
            }
            // Geste vollzogen → schließen.
            if (isEditMode) isEditMode = false else activeFolderId = null
            folderBackProgress.snapTo(0f)
        } catch (e: CancellationException) {
            // Geste abgebrochen → zurückfedern.
            folderBackProgress.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessMedium))
        }
    }

    val draggingItemPkg by appDrawerVm.draggingItemPkg.collectAsState()
    val touchPosition by appDrawerVm.touchPosition.collectAsState()
    val initialTouchOffsetInItem by appDrawerVm.initialTouchOffset.collectAsState()
    var gridAreaSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(activeFolderId) {
        if (activeFolderId != null) {
            editingFolderName = folders.find { it.id == activeFolderId }?.name ?: ""
        } else {
            isEditMode = false
            isFolderNameFocused = false
            appDrawerVm.onDragEnd()
        }
    }

    LaunchedEffect(isEditMode) {
        if (!isEditMode) {
            appDrawerVm.onDragEnd()
            isFolderNameFocused = false
        }
    }

    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    var menuAppBounds by remember { mutableStateOf<Rect?>(null) }
    var showFolderSelection by remember { mutableStateOf(false) }
    var folderSelectionApp by remember { mutableStateOf<AppInfo?>(null) }

    Box(modifier = Modifier.fillMaxSize().testTag("app_drawer").onGloballyPositioned { drawerSize = it.size }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(drawerBackgroundBrush)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
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
                    IconButton(
                        onClick = {
                            if (searchExpanded) {
                                searchExpanded = false
                                appDrawerVm.setSearchQuery("")
                            } else {
                                searchExpanded = true
                            }
                        },
                        modifier = Modifier.testTag("app_drawer_search_toggle")
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.cd_search),
                            tint = mainTextColor
                        )
                    }
                    if (searchQuery.isBlank()) {
                        var isCreateFolderDialogOpen by remember { mutableStateOf(false) }
                        var folderNameInput by remember { mutableStateOf("") }

                        IconButton(onClick = { isCreateFolderDialogOpen = true }) {
                            Icon(
                                Lucide.FolderPlus,
                                contentDescription = stringResource(R.string.cd_create_folder),
                                tint = mainTextColor
                            )
                        }

                        if (isCreateFolderDialogOpen) {
                            Dialog(onDismissRequest = { isCreateFolderDialogOpen = false }) {
                                val dialogBorderModifier = when (designStyle) {
                                    DesignStyle.GLASS -> Modifier.border(
                                        BorderStroke(1.2.dp, LiquidGlass.borderBrush(isDarkTextEnabled)),
                                        RoundedCornerShape(28.dp)
                                    )
                                    DesignStyle.TINTED -> Modifier.border(
                                        BorderStroke(1.2.dp, surfaceAccent.copy(alpha = 0.4f)),
                                        RoundedCornerShape(28.dp)
                                    )
                                    DesignStyle.MINIMAL -> Modifier
                                    else -> Modifier.border(
                                        BorderStroke(1.dp, mainTextColor.copy(alpha = 0.12f)),
                                        RoundedCornerShape(28.dp)
                                    )
                                }

                                Surface(
                                    modifier = Modifier
                                        .wrapContentWidth()
                                        .wrapContentHeight()
                                        .then(dialogBorderModifier),
                                    shape = RoundedCornerShape(28.dp),
                                    color = menuSurfaceColor,
                                    tonalElevation = 6.dp
                                ) {
                                    Column(modifier = Modifier.padding(24.dp)) {
                                        Text(
                                            text = stringResource(R.string.new_folder),
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = mainTextColor
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))

                                        val inputModifier = Modifier.designSurface(
                                            designStyle,
                                            RoundedCornerShape(16.dp),
                                            isDarkTextEnabled,
                                            surfaceAccent,
                                            fillAlpha = 0.1f
                                        )

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
                                                            stringResource(R.string.folder_name_hint),
                                                            color = mainTextColor.copy(alpha = 0.4f),
                                                            fontSize = 16.sp
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            @Suppress("DEPRECATION")
                                            TextButton(onClick = { isCreateFolderDialogOpen = false }) {
                                                Text(stringResource(R.string.cancel), color = Color.Gray)
                                            }
                                            @Suppress("DEPRECATION")
                                            TextButton(onClick = {
                                                if (folderNameInput.isNotBlank()) {
                                                    val nameExists = folders.any { it.name == folderNameInput }
                                                    if (nameExists) {
                                                        Toast.makeText(
                                                            context,
                                                            context.getString(R.string.folder_name_exists),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    } else {
                                                        val newFolder = LauncherLogic.createNewFolder(folderNameInput)
                                                        onOpenFolderConfig(newFolder)
                                                        folderNameInput = ""
                                                        isCreateFolderDialogOpen = false
                                                    }
                                                }
                                            }) {
                                                Text(stringResource(R.string.create), color = mainTextColor)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = mainTextColor
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = searchExpanded,
                enter = expandVertically(animationSpec = RethroneSprings.spatial(), expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    DrawerSearchField(
                        searchQuery = searchQuery,
                        onValueChange = { appDrawerVm.setSearchQuery(it) },
                        mainTextColor = mainTextColor,
                        designStyle = designStyle,
                        surfaceAccent = surfaceAccent,
                        isDarkTextEnabled = isDarkTextEnabled,
                        fontScale = fontSize.scale,
                        testTag = "app_drawer_search_field",
                        onCollapse = { searchExpanded = false }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            val adaptiveColumns = 4

            val gridState = rememberLazyGridState()
            // Material-3-Expressive: elastisches Rubber-Band-Feedback beim Ziehen
            // am oberen Rand, federt zurück wenn die Close-Schwelle nicht erreicht wird.
            val swipeToClose = rememberSwipeToCloseRubberBand(
                isAtTop = { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 },
                isAtBottom = { !gridState.canScrollForward },
                onClose = onClose
            )

            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(adaptiveColumns),
                    modifier = Modifier
                        .fillMaxSize()
                        // Clip vor der Translation: das elastische Ziehen/Bounce
                        // bleibt im Grid-Bereich und überlappt nicht die Kopfzeile.
                        .clipToBounds()
                        .graphicsLayer { translationY = swipeToClose.offsetY }
                        .nestedScroll(swipeToClose.connection),
                    state = gridState,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    if (searchQuery.isBlank()) {
                        itemsIndexed(
                            items = folders,
                            key = { _, folder -> folder.id },
                            contentType = { _, _ -> "folder" }
                        ) { _, folder ->
                            Box(modifier = Modifier.animateItem()) {
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
                    }

                    // U2: Leerer Zustand, wenn die Drawer-Suche nichts findet.
                    if (searchQuery.isNotBlank() && visibleApps.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
                            Text(
                                text = stringResource(R.string.drawer_search_no_results),
                                color = mainTextColor.copy(alpha = 0.6f),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp)
                                    .testTag("drawer_search_no_results")
                            )
                        }
                    }

                    itemsIndexed(
                        items = visibleApps,
                        key = { _, app -> app.packageName },
                        contentType = { _, _ -> "app" }
                    ) { _, app ->
                        Box(modifier = Modifier.animateItem()) {
                            AppItem(
                                app = app,
                                adaptiveColumns = adaptiveColumns,
                                customIcons = customIcons,
                                bouncePackage = returnIconPackage,
                                onLongPress = { appInfo, bounds ->
                                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuApp = appInfo
                                    menuAppBounds = bounds
                                },
                                onAppLaunchRequested = { requestedApp, bounds ->
                                    context.packageManager.getLaunchIntentForPackage(
                                        requestedApp.packageName
                                    )?.let { intent ->
                                        onLaunchApp(requestedApp.packageName, intent, bounds)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        val showFolder = activeFolderId != null
        val folderTransition = updateTransition(targetState = showFolder, label = "FolderTransition")
        val isCurrentFolderDeleted = lastValidFolder != null && folders.none { it.id == lastValidFolder?.id }

        val folderProgress by folderTransition.animateFloat(
            transitionSpec = {
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
        ) { if (it) 1f else 0f }
        val overlayAlpha by folderTransition.animateFloat(
            transitionSpec = { tween(durationMillis = 260, easing = LinearEasing) },
            label = "OverlayAlpha"
        ) { if (it) 0.35f else 0f }
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
                    modifier = Modifier.fillMaxSize().background(overlayScrimColor.copy(alpha = overlayAlpha))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {
                            if (isEditMode) isEditMode = false else activeFolderId = null
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    val predict = folderBackProgress.value
                    val scale = (0.05f + (1f - 0.05f) * folderProgress) * (1f - 0.12f * predict)
                    val translationX = folderTranslation.x * (1f - folderProgress)
                    val translationY = folderTranslation.y * (1f - folderProgress)

                    val folderBorder = when (designStyle) {
                        DesignStyle.GLASS -> BorderStroke(1.2.dp, LiquidGlass.borderBrush(isDarkTextEnabled))
                        DesignStyle.TINTED -> BorderStroke(1.2.dp, surfaceAccent.copy(alpha = 0.4f))
                        DesignStyle.MINIMAL -> BorderStroke(0.dp, Color.Transparent)
                        else -> BorderStroke(1.dp, mainTextColor.copy(alpha = 0.15f))
                    }

                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .fillMaxWidth(folderLayoutSpec.widthFraction)
                            .widthIn(
                                min = folderLayoutSpec.minWidthDp.dp,
                                max = folderLayoutSpec.maxWidthDp.dp
                            )
                            .wrapContentHeight()
                            .graphicsLayer {
                                this.scaleX = scale
                                this.scaleY = scale
                                this.translationX = translationX
                                this.translationY = translationY
                                this.transformOrigin = TransformOrigin.Center
                                this.alpha = (if (isCurrentFolderDeleted) folderProgress else 1f) * (1f - 0.25f * predict)
                            }
                            .clickable(enabled = false) {},
                        color = menuSurfaceColor.copy(alpha = 0.98f),
                        shape = RoundedCornerShape(32.dp),
                        border = folderBorder,
                        shadowElevation = 24.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = folderLayoutSpec.horizontalPaddingDp.dp,
                                vertical = folderLayoutSpec.verticalPaddingDp.dp
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
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

                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                if (isEditMode) {
                                    BasicTextField(
                                        value = editingFolderName,
                                        onValueChange = { newName ->
                                            editingFolderName = newName
                                            val updatedFolder = currentActiveFolder.copy(name = newName)
                                            val updatedFolders = folders.map { if (it.id == currentActiveFolder.id) updatedFolder else it }
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
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                isFolderNameFocused = false
                                                focusManager.clearFocus()
                                                keyboardController?.hide()
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = folderLayoutSpec.titleSidePaddingDp.dp)
                                            .onFocusChanged { isFolderNameFocused = it.isFocused }
                                            .graphicsLayer { rotationZ = if (!isFolderNameFocused) wiggleAngle else 0f }
                                    )
                                } else {
                                    Text(
                                        currentActiveFolder.name,
                                        color = mainTextColor,
                                        fontSize = 22.sp * fontSize.scale,
                                        fontWeight = fontWeight.weight,
                                        fontFamily = appFont.fontFamily,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = folderLayoutSpec.titleSidePaddingDp.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { isEditMode = !isEditMode },
                                    modifier = Modifier.align(Alignment.CenterEnd).size(32.dp)
                                ) {
                                    Icon(
                                        if (isEditMode) Lucide.Check else Lucide.Pencil,
                                        contentDescription = stringResource(R.string.cd_edit_mode),
                                        tint = mainTextColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            val folderTitleSpacing = iconSize.lerpDp(5.dp, 12.dp)
                            val folderGridTopPadding = iconSize.lerpDp(3.dp, 8.dp)
                            val folderItemTopPadding = iconSize.lerpDp(2.dp, 4.dp)
                            val folderContentHeight = iconSize.lerpDp(292.dp, 340.dp)
                            val folderPagerIndicatorTopPadding = iconSize.lerpDp(8.dp, 16.dp)

                            Spacer(modifier = Modifier.height(folderTitleSpacing))

                            val folderApps = remember(
                                currentActiveFolder.appPackageNames,
                                apps
                            ) { currentActiveFolder.appPackageNames.mapNotNull { pkg -> apps.find { it.packageName == pkg } } }
                            val currentFolderApps by rememberUpdatedState(folderApps)

                            val itemsPerPage = LauncherLogic.FOLDER_ITEMS_PER_PAGE
                            val pages = LauncherLogic.folderPageCount(folderApps.size)
                            val pagerState = rememberPagerState(pageCount = { pages })

                            LaunchedEffect(draggingItemPkg) {
                                if (draggingItemPkg == null) return@LaunchedEffect
                                val edgeThreshold = with(density) { 32.dp.toPx() }
                                val dwellTime = 400L
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
                                                delay(600)
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

                            Box(modifier = Modifier.height(folderContentHeight)) {
                                val currentFolderState by rememberUpdatedState(currentActiveFolder)
                                val currentFoldersState by rememberUpdatedState(folders)

                                fun performReorder(currentTouch: Offset, currentPage: Int) {
                                    val pkg = draggingItemPkg ?: return
                                    // Treffer- und Umsortier-Logik liegt framework-frei in LauncherLogic
                                    // (unit-getestet); hier bleiben nur Persistenz + Haptik.
                                    val targetIdx = LauncherLogic.folderGridSlotAt(
                                        touchX = currentTouch.x,
                                        touchY = currentTouch.y,
                                        gridWidthPx = gridAreaSize.width,
                                        gridHeightPx = gridAreaSize.height,
                                        currentPage = currentPage,
                                    ) ?: return
                                    val newList = LauncherLogic.moveFolderApp(
                                        packages = currentFolderState.appPackageNames,
                                        pkg = pkg,
                                        targetIdx = targetIdx,
                                    ) ?: return
                                    onUpdateFolders(
                                        currentFoldersState.map {
                                            if (it.id == currentFolderState.id) {
                                                it.copy(appPackageNames = newList)
                                            } else {
                                                it
                                            }
                                        }
                                    )
                                    if (hapticEnabled) {
                                        haptic.performHapticFeedback(
                                            HapticFeedbackType.TextHandleMove
                                        )
                                    }
                                }

                                LaunchedEffect(
                                    pagerState.currentPage
                                ) { if (draggingItemPkg != null) performReorder(touchPosition, pagerState.currentPage) }

                                val folderBoundaryBlocker = remember(pagerState, pages) {
                                    object : NestedScrollConnection {
                                        override fun onPreScroll(
                                            available: Offset,
                                            source: NestedScrollSource
                                        ): Offset {
                                            if (pages <= 1) return Offset(x = available.x, y = 0f)
                                            val isAtStart = pagerState.currentPage == 0 && pagerState.currentPageOffsetFraction <= 0.001f
                                            val isAtEnd = pagerState.currentPage == pages - 1 && pagerState.currentPageOffsetFraction >= -0.001f
                                            if ((isAtStart && available.x > 0f) || (isAtEnd && available.x < 0f)) {
                                                return Offset(
                                                    x = available.x,
                                                    y = 0f
                                                )
                                            }
                                            return Offset.Zero
                                        }
                                        override suspend fun onPreFling(available: Velocity): Velocity {
                                            if (pages <= 1) return available
                                            val isAtStart = pagerState.currentPage == 0
                                            val isAtEnd = pagerState.currentPage == pages - 1
                                            if ((isAtStart && available.x > 0f) || (isAtEnd && available.x < 0f)) return available
                                            return Velocity.Zero
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier.fillMaxSize().onGloballyPositioned { gridAreaSize = it.size }
                                        .pointerInput(isEditMode) {
                                            if (!isEditMode) return@pointerInput
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { offset ->
                                                    val cellW = gridAreaSize.width / 3f
                                                    val cellH = gridAreaSize.height / 3f
                                                    val globalIdx = LauncherLogic.folderGridSlotAt(
                                                        touchX = offset.x,
                                                        touchY = offset.y,
                                                        gridWidthPx = gridAreaSize.width,
                                                        gridHeightPx = gridAreaSize.height,
                                                        currentPage = pagerState.currentPage,
                                                    ) ?: return@detectDragGesturesAfterLongPress
                                                    val currentApps = currentFolderApps
                                                    if (globalIdx < currentApps.size) {
                                                        appDrawerVm.onDragStart(
                                                            currentApps[globalIdx].packageName,
                                                            offset,
                                                            Offset(offset.x % cellW, offset.y % cellH)
                                                        )
                                                        if (hapticEnabled) {
                                                            haptic.performHapticFeedback(
                                                                HapticFeedbackType.LongPress
                                                            )
                                                        }
                                                    }
                                                },
                                                onDragEnd = { appDrawerVm.onDragEnd() },
                                                onDragCancel = { appDrawerVm.onDragEnd() },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    appDrawerVm.onDragUpdate(dragAmount)
                                                    performReorder(
                                                        appDrawerVm.touchPosition.value,
                                                        pagerState.currentPage
                                                    )
                                                }
                                            )
                                        }
                                ) {
                                    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                                        HorizontalPager(
                                            state = pagerState,
                                            modifier = Modifier.fillMaxSize().nestedScroll(folderBoundaryBlocker),
                                            pageSpacing = folderLayoutSpec.pageSpacingDp.dp,
                                            userScrollEnabled = draggingItemPkg == null && pages > 1,
                                            beyondViewportPageCount = 1
                                        ) { page ->
                                            val startIdx = page * itemsPerPage
                                            val endIdx = (startIdx + itemsPerPage).coerceAtMost(folderApps.size)
                                            val pageApps = folderApps.subList(startIdx, endIdx)

                                            val handleFolderLongPress: (AppInfo, Rect) -> Unit = remember(
                                                hapticEnabled
                                            ) {
                                                {
                                                        appInfo, bounds ->
                                                    if (hapticEnabled) {
                                                        haptic.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        )
                                                    }
                                                    menuApp = appInfo
                                                    menuAppBounds = bounds
                                                }
                                            }

                                            val handleFolderAppLaunch: (AppInfo, Rect?) -> Unit = remember(
                                                context,
                                                onLaunchApp
                                            ) {
                                                {
                                                        requestedApp, bounds ->
                                                    requestedApp.launchIntent?.let { intent ->
                                                        onLaunchApp(requestedApp.packageName, intent, bounds)
                                                    } ?: context.packageManager.getLaunchIntentForPackage(requestedApp.packageName)?.let { intent ->
                                                        onLaunchApp(requestedApp.packageName, intent, bounds)
                                                    }
                                                }
                                            }

                                            LazyVerticalGrid(
                                                columns = GridCells.Fixed(3),
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                                userScrollEnabled = false,
                                                contentPadding = PaddingValues(
                                                    top = folderGridTopPadding,
                                                    start = 8.dp,
                                                    end = 8.dp
                                                )
                                            ) {
                                                itemsIndexed(
                                                    pageApps,
                                                    key = { _, app -> app.packageName },
                                                    contentType = { _, _ -> "folder_app" }
                                                ) { indexInPage, app ->
                                                    val globalIndex = startIdx + indexInPage
                                                    val isDragging = draggingItemPkg == app.packageName
                                                    val badgeSize = LocalIconSize.current.size * 0.28f
                                                    val minusWidth = LocalIconSize.current.size * 0.14f
                                                    val minusHeight = 1.5.dp

                                                    Box(
                                                        modifier = Modifier.let { if (isDragging) it else Modifier.animateItem() }.zIndex(
                                                            if (isDragging) 0f else 1f
                                                        ).graphicsLayer {
                                                            rotationZ = if (isEditMode && !isDragging) {
                                                                if (globalIndex % 2 == 0) wiggleAngle else -wiggleAngle
                                                            } else {
                                                                0f
                                                            }
                                                            alpha = if (isDragging) 0f else 1f
                                                        }.padding(
                                                            top = folderItemTopPadding,
                                                            start = 2.dp,
                                                            end = 2.dp
                                                        )
                                                    ) {
                                                        AppItem(
                                                            app = app,
                                                            adaptiveColumns = 3,
                                                            isInFolder = true,
                                                            currentFolderId = currentActiveFolder.id,
                                                            isEditMode = isEditMode,
                                                            customIcons = customIcons,
                                                            bouncePackage = returnIconPackage,
                                                            onLongPress = handleFolderLongPress,
                                                            onAppLaunchRequested = handleFolderAppLaunch
                                                        )

                                                        if (isEditMode && !isDragging) {
                                                            val view = androidx.compose.ui.platform.LocalView.current
                                                            Box(
                                                                modifier = Modifier.align(
                                                                    Alignment.TopEnd
                                                                ).offset(
                                                                    x = (-2).dp,
                                                                    y = (-2).dp
                                                                ).zIndex(
                                                                    10f
                                                                ).size(
                                                                    badgeSize
                                                                ).background(
                                                                    mainTextColor.copy(alpha = 0.85f),
                                                                    CircleShape
                                                                )
                                                                    .clickable(
                                                                        interactionSource = remember { MutableInteractionSource() },
                                                                        indication = null
                                                                    ) {
                                                                        if (hapticEnabled) {
                                                                            view.performHapticFeedback(
                                                                                HapticFeedbackConstants.LONG_PRESS
                                                                            )
                                                                        }
                                                                        val updatedFolders = LauncherLogic.removeAppFromFolder(
                                                                            folders,
                                                                            currentActiveFolder.id,
                                                                            app.packageName
                                                                        )
                                                                        onUpdateFolders(updatedFolders)
                                                                        if (updatedFolders.none { it.id == currentActiveFolder.id }) activeFolderId = null
                                                                    },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier.size(
                                                                        width = minusWidth,
                                                                        height = minusHeight
                                                                    ).background(
                                                                        if (isDarkTextEnabled) {
                                                                            Color.White
                                                                        } else {
                                                                            Color(
                                                                                0xFF0F172A
                                                                            )
                                                                        },
                                                                        RoundedCornerShape(0.75.dp)
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (draggingItemPkg != null) {
                                        val draggedApp = currentFolderApps.find { it.packageName == draggingItemPkg }
                                        if (draggedApp != null) {
                                            Box(
                                                modifier = Modifier.size(
                                                    80.dp
                                                ).graphicsLayer {
                                                    this.translationX = touchPosition.x - initialTouchOffsetInItem.x
                                                    this.translationY = touchPosition.y - initialTouchOffsetInItem.y
                                                    this.scaleX = 1.25f
                                                    this.scaleY = 1.25f
                                                    this.alpha = 0.95f
                                                }.zIndex(
                                                    1000f
                                                ).shadow(24.dp, RoundedCornerShape(20.dp))
                                            ) {
                                                AppItem(
                                                    app = draggedApp,
                                                    adaptiveColumns = 3,
                                                    isInFolder = true,
                                                    currentFolderId = currentActiveFolder.id,
                                                    isEditMode = true,
                                                    customIcons = customIcons,
                                                    onLongPress = { _, _ -> },
                                                    onAppLaunchRequested = null
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (pages > 1) {
                                Row(
                                    Modifier.wrapContentHeight().fillMaxWidth().padding(
                                        top = folderPagerIndicatorTopPadding
                                    ),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    repeat(pages) { iteration ->
                                        val color = if (pagerState.currentPage == iteration) {
                                            mainTextColor
                                        } else {
                                            mainTextColor.copy(
                                                alpha = 0.3f
                                            )
                                        }
                                        Box(
                                            modifier = Modifier.padding(
                                                horizontal = 4.dp
                                            ).clip(CircleShape).background(color).size(6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val currentMenuApp = menuApp
        if (currentMenuApp != null) {
            AppContextMenu(
                isFavorite = isFavorite(currentMenuApp.packageName),
                targetBounds = menuAppBounds,
                onDismiss = { menuApp = null },
                onToggleFavorite = { onToggleFavorite(currentMenuApp.packageName) },
                onAppInfo = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        ).apply { data = Uri.fromParts("package", currentMenuApp.packageName, null) }
                    )
                },
                onUninstall = {
                    try {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_DELETE
                            ).apply {
                                data = Uri.fromParts("package", currentMenuApp.packageName, null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    } catch (_: Exception) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.uninstall_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onMoveToFolder = if (folders.isNotEmpty()) {
                    {
                        folderSelectionApp = currentMenuApp
                        showFolderSelection = true
                    }
                } else {
                    null
                },
                onRemoveFromFolder = folders.find {
                    it.appPackageNames.contains(
                        currentMenuApp.packageName
                    )
                }?.let { folder ->
                    {
                        onUpdateFolders(
                            LauncherLogic.removeAppFromFolder(folders, folder.id, currentMenuApp.packageName)
                        )
                    }
                },
                onHide = onHideApp?.let { hide -> { hide(currentMenuApp.packageName) } }
            )
        }

        if (showFolderSelection && folderSelectionApp != null) {
            Dialog(onDismissRequest = { showFolderSelection = false }) {
                val dialogBorderModifier = when (designStyle) {
                    DesignStyle.GLASS -> Modifier.border(
                        BorderStroke(1.2.dp, LiquidGlass.borderBrush(isDarkTextEnabled)),
                        RoundedCornerShape(28.dp)
                    )
                    DesignStyle.TINTED -> Modifier.border(
                        BorderStroke(1.2.dp, surfaceAccent.copy(alpha = 0.4f)),
                        RoundedCornerShape(28.dp)
                    )
                    DesignStyle.MINIMAL -> Modifier
                    else -> Modifier.border(
                        BorderStroke(1.dp, mainTextColor.copy(alpha = 0.12f)),
                        RoundedCornerShape(28.dp)
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(0.8f).wrapContentHeight().then(dialogBorderModifier),
                    shape = RoundedCornerShape(28.dp),
                    color = menuSurfaceColor.copy(alpha = 0.98f),
                    shadowElevation = 16.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            stringResource(R.string.ctx_move_to_folder),
                            fontSize = 18.sp * fontSize.scale,
                            fontWeight = fontWeight.weight,
                            color = mainTextColor,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        folders.forEach { folder ->
                            @Suppress("DEPRECATION")
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(
                                    RoundedCornerShape(16.dp)
                                ).clickable {
                                    onUpdateFolders(
                                        LauncherLogic.addAppToFolder(
                                            folders,
                                            folder.id,
                                            folderSelectionApp!!.packageName
                                        )
                                    )
                                    showFolderSelection = false
                                    menuApp = null
                                }.padding(
                                    vertical = 14.dp,
                                    horizontal = 12.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Lucide.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = mainTextColor
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(folder.name, fontSize = 16.sp * fontSize.scale, color = mainTextColor)
                            }
                        }
                        @Suppress("DEPRECATION")
                        TextButton(
                            onClick = { showFolderSelection = false },
                            modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                        ) { Text(stringResource(R.string.cancel), color = Color.Gray) }
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
    val designStyle = LocalDesignStyle.current
    val surfaceAccent = LocalColorTheme.current.menuSurfaceColor(isDarkTextEnabled)
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val intSrc = remember { MutableInteractionSource() }
    var itemOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier.width(80.dp).onGloballyPositioned { coordinates ->
            val position = coordinates.positionInRoot()
            val size = coordinates.size
            itemOffset = Offset(x = position.x + size.width / 2f, y = position.y + size.height / 2f)
        },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.bounceClick(
                intSrc
            ).combinedClickable(
                interactionSource = intSrc,
                indication = null,
                onClick = { onClick(itemOffset) },
                onLongClick = { onOpenFolderConfig(folder) }
            )
        ) {
            val folderBoxModifier = Modifier.designSurface(
                designStyle,
                RoundedCornerShape(16.dp),
                isDarkTextEnabled,
                surfaceAccent,
                fillAlpha = 0.1f
            )
            Box(
                modifier = Modifier.size(iconSizeValue).then(folderBoxModifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Lucide.Folder,
                    contentDescription = null,
                    tint = mainTextColor,
                    modifier = Modifier.size(iconSizeValue * 0.6f)
                )
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
    isInFolder: Boolean = false,
    currentFolderId: String? = null,
    isEditMode: Boolean = false,
    customIcons: Map<String, String>? = null,
    onLongPress: (AppInfo, Rect) -> Unit,
    onAppLaunchRequested: ((AppInfo, Rect?) -> Unit)? = null,
    bouncePackage: String? = null
) {
    val context = LocalContext.current
    val fontSize = LocalFontSize.current
    val fontWeight = LocalFontWeight.current
    val iconSize = LocalIconSize.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val intSrc = remember { MutableInteractionSource() }
    var itemBounds by remember { mutableStateOf<Rect?>(null) }
    var iconBounds by remember { mutableStateOf<Rect?>(null) }
    val animationsEnabled = com.example.androidlauncher.ui.theme.LocalAppCloseAnimationEnabled.current
    val bounceScale by animateFloatAsState(
        targetValue = if (!animationsEnabled) 1f else if (bouncePackage == app.packageName) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "DrawerReturnBounce"
    )
    val itemWidth = when {
        isInFolder -> iconSize.lerpDp(68.dp, 82.dp)
        adaptiveColumns == 5 -> 64.dp
        else -> 80.dp
    }

    Box(modifier = Modifier.width(itemWidth), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.onGloballyPositioned { coordinates -> itemBounds = coordinates.boundsInRoot() }.graphicsLayer {
                scaleX = bounceScale
                scaleY = bounceScale
            }.bounceClick(
                intSrc,
                enabled = !isEditMode
            ).combinedClickable(interactionSource = intSrc, indication = null, enabled = !isEditMode, onClick = {
                onAppLaunchRequested?.let { it(app, iconBounds ?: itemBounds) } ?: run {
                    app.launchIntent?.let {
                        context.startActivity(it)
                    } ?: context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it) }
                }
            }, onLongClick = { (iconBounds ?: itemBounds)?.let { onLongPress(app, it) } })
        ) {
            Box(modifier = Modifier.onGloballyPositioned { coordinates -> iconBounds = coordinates.boundsInRoot() }) {
                AppIconView(app, showBadge = true, customIcons = customIcons)
            }
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

internal data class FolderOverlayLayoutSpec(
    val widthFraction: Float,
    val minWidthDp: Int,
    val maxWidthDp: Int,
    val horizontalPaddingDp: Int,
    val verticalPaddingDp: Int,
    val titleSidePaddingDp: Int,
    val titleGridSpacingDp: Int,
    val gridHeightDp: Int,
    val gridSpacingDp: Int,
    val gridHorizontalPaddingDp: Int,
    val gridTopPaddingDp: Int,
    val gridItemTopPaddingDp: Int,
    val indicatorTopPaddingDp: Int,
    val pageSpacingDp: Int
)

internal fun calculateFolderOverlayLayoutSpec(
    screenWidthDp: Int,
    screenHeightDp: Int,
    iconSize: IconSize
): FolderOverlayLayoutSpec {
    val compactWidth = screenWidthDp < 360
    val compactHeight = screenHeightDp < 760
    val compactScreen = compactWidth || compactHeight
    val expandedScreen = screenWidthDp >= 600

    val widthFraction = when {
        expandedScreen -> 0.68f
        compactWidth -> 0.94f
        else -> 0.90f
    }

    val maxWidthDp = when {
        expandedScreen -> 480
        compactWidth -> 420
        else -> 440
    } + iconSize.lerpInt(0, 12).coerceAtLeast(0)

    val horizontalPaddingDp = when {
        compactScreen -> 16
        expandedScreen -> 24
        else -> 20
    }
    val verticalPaddingDp = if (compactScreen) 16 else 20
    val titleSidePaddingDp = if (compactWidth) 28 else 32
    val titleGridSpacingDp = when {
        compactScreen -> 10
        expandedScreen -> 14
        else -> 12
    }

    val baseGridSpacingDp = iconSize.lerpInt(12, 16)
    val gridSpacingDp = if (compactWidth) (baseGridSpacingDp - 2).coerceAtLeast(10) else baseGridSpacingDp
    val gridHorizontalPaddingDp = when {
        compactWidth -> 4
        expandedScreen -> 10
        else -> 6
    }
    val gridTopPaddingDp = when {
        compactScreen -> 4
        else -> 8
    } + iconSize.lerpInt(0, 2).coerceAtLeast(0)
    val gridItemTopPaddingDp = iconSize.lerpInt(4, 8)
    val indicatorTopPaddingDp = if (compactScreen) 12 else 14
    val pageSpacingDp = if (compactWidth) 10 else 12

    val gridHeightMinDp = iconSize.lerpInt(220, 268)
    val gridHeightMaxDp = when {
        expandedScreen -> 390
        compactHeight -> 312
        else -> 350
    }
    val gridHeightTargetDp = (
        screenHeightDp * if (compactScreen) 0.38f else if (expandedScreen) 0.40f else 0.39f
        ).roundToInt() + iconSize.lerpInt(-8, 14)

    return FolderOverlayLayoutSpec(
        widthFraction = widthFraction,
        minWidthDp = if (expandedScreen) 320 else 280,
        maxWidthDp = maxWidthDp,
        horizontalPaddingDp = horizontalPaddingDp,
        verticalPaddingDp = verticalPaddingDp,
        titleSidePaddingDp = titleSidePaddingDp,
        titleGridSpacingDp = titleGridSpacingDp,
        gridHeightDp = gridHeightTargetDp.coerceIn(gridHeightMinDp, gridHeightMaxDp),
        gridSpacingDp = gridSpacingDp,
        gridHorizontalPaddingDp = gridHorizontalPaddingDp,
        gridTopPaddingDp = gridTopPaddingDp,
        gridItemTopPaddingDp = gridItemTopPaddingDp,
        indicatorTopPaddingDp = indicatorTopPaddingDp,
        pageSpacingDp = pageSpacingDp
    )
}

/**
 * Lineare Interpolation eines Layoutwerts anhand der (jetzt stufenlosen) Icon-Größe.
 * Referenzpunkte: 40dp (klein) und 56dp (groß) – Werte außerhalb werden extrapoliert.
 */
private fun IconSize.layoutFraction(): Float = (size - 40.dp) / (56.dp - 40.dp)

/** Interpoliert einen Dp-Layoutwert; das Ergebnis ist nie negativ. */
private fun IconSize.lerpDp(atSmall: Dp, atLarge: Dp): Dp =
    (atSmall + (atLarge - atSmall) * layoutFraction()).coerceAtLeast(0.dp)

/** Interpoliert einen ganzzahligen Dp-Layoutwert (kann negativ sein, z. B. für Offsets). */
private fun IconSize.lerpInt(atSmall: Int, atLarge: Int): Int =
    (atSmall + (atLarge - atSmall) * layoutFraction()).roundToInt()
