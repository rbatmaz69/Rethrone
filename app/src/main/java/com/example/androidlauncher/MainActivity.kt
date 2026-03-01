package com.example.androidlauncher

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.androidlauncher.data.AppFont
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.AppRepository
import com.example.androidlauncher.data.FavoritesManager
import com.example.androidlauncher.data.FolderInfo
import com.example.androidlauncher.data.FolderManager
import com.example.androidlauncher.data.FontSize
import com.example.androidlauncher.data.FontWeightLevel
import com.example.androidlauncher.data.IconManager
import com.example.androidlauncher.data.IconSize
import com.example.androidlauncher.data.ThemeManager
import com.example.androidlauncher.ui.AppDrawer
import com.example.androidlauncher.ui.BottomSearch
import com.example.androidlauncher.ui.ColorConfigMenu
import com.example.androidlauncher.ui.EditConfigMenu
import com.example.androidlauncher.ui.FavoritesConfigMenu
import com.example.androidlauncher.ui.FolderConfigMenu
import com.example.androidlauncher.ui.FontSelectionMenu
import com.example.androidlauncher.ui.HomeScreen
import com.example.androidlauncher.ui.IconConfigMenu
import com.example.androidlauncher.ui.InfoDialog
import com.example.androidlauncher.ui.ReturnAnimationOverlay
import com.example.androidlauncher.ui.SizeConfigMenu
import com.example.androidlauncher.ui.SystemWallpaperView
import com.example.androidlauncher.ui.WallpaperConfigMenu
import com.example.androidlauncher.ui.launchAppNoTransition
import com.example.androidlauncher.ui.theme.AndroidLauncherTheme
import com.example.androidlauncher.ui.theme.ColorTheme
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Haupt-Activity des Launchers.
 *
 * Dient als Einstiegspunkt der App und orchestriert:
 * - Theme- und Einstellungsverwaltung via [ThemeManager]
 * - Favoriten-Persistenz via [FavoritesManager]
 * - Ordner-Verwaltung via [FolderManager]
 * - App-Listen-Laden via [AppRepository]
 * - Navigation zwischen HomeScreen, AppDrawer und diversen Konfigurationsmenüs
 * - Rückkehr-Animationen beim Wechsel zwischen Apps und Launcher
 *
 * Die eigentlichen UI-Composables sind in separate Dateien ausgelagert,
 * um diese Activity schlank und wartbar zu halten.
 */
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var backCallback: OnBackPressedCallback
    private var lastDefaultLauncherPackage: String? = null
    private var lastHomeIntentTimestamp: Long = 0
    private var defaultLauncherWarningShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-Edge-Modus für transparente System-Bars
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        intent?.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        logHomeIntent(intent)

        // Verhindert Standard-Back-Navigation (Launcher soll nicht geschlossen werden)
        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        }
        onBackPressedDispatcher.addCallback(this, backCallback)

        enforceExcludeFromRecents()

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            // ── Manager-Instanzen (einmalig erzeugt) ─────────────
            val themeManager = remember { ThemeManager(context) }
            val folderManager = remember { FolderManager(context) }
            val iconManager = remember { IconManager(context) }
            val favoritesManager = remember { FavoritesManager(context) }
            val appRepository = remember { AppRepository(context) }

            // ── SharedPreferences → DataStore Migration (einmalig) ─
            LaunchedEffect(Unit) {
                favoritesManager.migrateFromSharedPreferences(context)
            }

            // ── Theme-State ──────────────────────────────────────
            val currentTheme by themeManager.selectedTheme.collectAsState(initial = ColorTheme.SIGNATURE)
            val currentFontSize by themeManager.selectedFontSize.collectAsState(initial = FontSize.STANDARD)
            val currentFontWeight by themeManager.selectedFontWeight.collectAsState(initial = FontWeightLevel.NORMAL)
            val currentIconSize by themeManager.selectedIconSize.collectAsState(initial = IconSize.STANDARD)
            val currentAppFont by themeManager.selectedAppFont.collectAsState(initial = AppFont.SYSTEM_DEFAULT)
            val isDarkTextEnabled by themeManager.isDarkTextEnabled.collectAsState(initial = false)
            val showFavoriteLabels by themeManager.showFavoriteLabels.collectAsState(initial = false)
            val isLiquidGlassEnabled by themeManager.isLiquidGlassEnabled.collectAsState(initial = true)

            // ── Wallpaper-State ──────────────────────────────────
            val customWallpaperUri by themeManager.customWallpaperUri.collectAsState(initial = null)
            val wallpaperBlur by themeManager.wallpaperBlur.collectAsState(initial = 0f)
            val wallpaperDim by themeManager.wallpaperDim.collectAsState(initial = 0.1f)
            val wallpaperZoom by themeManager.wallpaperZoom.collectAsState(initial = 1.0f)

            // ── Daten-State ──────────────────────────────────────
            val folders by folderManager.folders.collectAsState(initial = emptyList())
            val customIcons by iconManager.customIcons.collectAsState(initial = emptyMap())
            val favoritePackages by favoritesManager.favorites.collectAsState(initial = emptyList())

            // ── Wallpaper-Picker mit UCrop ────────────────────────
            val cropLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.let { data ->
                        UCrop.getOutput(data)?.let { uri ->
                            scope.launch { themeManager.setCustomWallpaperUri(uri.toString()) }
                        }
                    }
                }
            }

            val wallpaperPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri: Uri? ->
                uri?.let { sourceUri ->
                    val destinationUri = Uri.fromFile(
                        File(context.cacheDir, "cropped_wallpaper_${System.currentTimeMillis()}.jpg")
                    )
                    val bgColor = if (isDarkTextEnabled) currentTheme.lightBackground.toArgb()
                    else currentTheme.drawerBackground.toArgb()
                    val widgetColor = if (isDarkTextEnabled) Color.Black.toArgb() else Color.White.toArgb()

                    val options = UCrop.Options().apply {
                        setToolbarColor(bgColor)
                        setStatusBarColor(bgColor)
                        setToolbarWidgetColor(widgetColor)
                        setActiveControlsWidgetColor(currentTheme.primary.toArgb())
                        setToolbarTitle("Hintergrund zuschneiden")
                        setLogoColor(bgColor)
                    }

                    cropLauncher.launch(
                        UCrop.of(sourceUri, destinationUri)
                            .withAspectRatio(9f, 16f)
                            .withMaxResultSize(1080, 1920)
                            .withOptions(options)
                            .getIntent(context)
                    )
                }
            }

            // ── Theme anwenden ───────────────────────────────────
            AndroidLauncherTheme(
                colorTheme = currentTheme,
                fontSize = currentFontSize,
                fontWeight = currentFontWeight,
                iconSize = currentIconSize,
                darkTextEnabled = isDarkTextEnabled,
                showFavoriteLabels = showFavoriteLabels,
                liquidGlassEnabled = isLiquidGlassEnabled,
                appFont = currentAppFont
            ) {
                @Suppress("DEPRECATION")
                val lifecycleOwner = LocalLifecycleOwner.current
                val menuBackgroundColor = if (isDarkTextEnabled) currentTheme.lightBackground
                else currentTheme.drawerBackground

                // ── Navigation-State ─────────────────────────────
                var rootSize by remember { mutableStateOf(IntSize.Zero) }
                var pendingReturnAnimation by remember { mutableStateOf<ReturnAnimation?>(null) }
                var activeReturnAnimation by remember { mutableStateOf<ReturnAnimation?>(null) }
                var returnIconPackage by remember { mutableStateOf<String?>(null) }
                var isDrawerOpen by remember { mutableStateOf(false) }
                var isSettingsOpen by remember { mutableStateOf(false) }
                var isSearchOpen by remember { mutableStateOf(false) }
                var isFavoritesConfigOpen by remember { mutableStateOf(false) }
                var isColorConfigOpen by remember { mutableStateOf(false) }
                var isSizeConfigOpen by remember { mutableStateOf(false) }
                var isFontSelectionOpen by remember { mutableStateOf(false) }
                var isEditConfigOpen by remember { mutableStateOf(false) }
                var isIconConfigOpen by remember { mutableStateOf(false) }
                var isWallpaperConfigOpen by remember { mutableStateOf(false) }
                var isInfoOpen by remember { mutableStateOf(false) }
                var selectedFolderForConfig by remember { mutableStateOf<FolderInfo?>(null) }

                // ── Rückkehr-Animation bei ON_RESUME ─────────────
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            pendingReturnAnimation?.let {
                                isDrawerOpen = it.source == LaunchSource.DRAWER
                                if (!isDrawerOpen) {
                                    // Alle Menüs beim Rückkehr zum Homescreen schließen
                                    isSettingsOpen = false
                                    isFavoritesConfigOpen = false
                                    isColorConfigOpen = false
                                    isSizeConfigOpen = false
                                    isFontSelectionOpen = false
                                    isEditConfigOpen = false
                                    isIconConfigOpen = false
                                    isWallpaperConfigOpen = false
                                    isInfoOpen = false
                                    selectedFolderForConfig = null
                                }
                                activeReturnAnimation = it
                                returnIconPackage = it.packageName
                                pendingReturnAnimation = null
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                // Bounce-Animation zurücksetzen nach 300ms
                LaunchedEffect(returnIconPackage) {
                    if (returnIconPackage != null) {
                        delay(300)
                        returnIconPackage = null
                    }
                }

                // ── App-Liste laden ──────────────────────────────
                val allApps = remember { mutableStateListOf<AppInfo>() }
                val favorites = remember(allApps.toList(), favoritePackages) {
                    LauncherLogic.getFavoriteApps(allApps.toList(), favoritePackages)
                }

                /** Lädt die App-Liste und Icons (priorisiert Favoriten). */
                fun refreshAppList() {
                    scope.launch {
                        appRepository.cleanupLegacyCache()
                        val basicList = appRepository.getInstalledApps()

                        // Wenn die Liste sich grundlegend geändert hat (Anzahl oder Namen),
                        // dann einmalig neu setzen, aber Icons von alten Apps behalten falls möglich.
                        if (allApps.size != basicList.size || allApps.map { it.packageName } != basicList.map { it.packageName }) {
                            val currentIcons = allApps.associate { it.packageName to it.iconBitmap }
                            allApps.clear()
                            allApps.addAll(basicList.map {
                                it.copy(iconBitmap = currentIcons[it.packageName])
                            })
                        }

                        appRepository.loadIconsWithPriority(
                            apps = allApps.toList(),
                            favoritePackages = favoritePackages
                        ) { idx, bitmap ->
                            withContext(Dispatchers.Main) {
                                if (idx < allApps.size) {
                                    allApps[idx] = allApps[idx].copy(iconBitmap = bitmap)
                                }
                            }
                        }
                    }
                }

                LaunchedEffect(Unit) { refreshAppList() }

                // App-Installations-/Deinstallations-Listener
                DisposableEffect(Unit) {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(ctx: Context?, intent: Intent?) {
                            scope.launch {
                                delay(800) // Warten bis PackageManager aktualisiert hat
                                refreshAppList()
                            }
                        }
                    }
                    val filter = IntentFilter().apply {
                        addAction(Intent.ACTION_PACKAGE_ADDED)
                        addAction(Intent.ACTION_PACKAGE_REMOVED)
                        addAction(Intent.ACTION_PACKAGE_REPLACED)
                        addDataScheme("package")
                    }
                    context.registerReceiver(receiver, filter)
                    onDispose { context.unregisterReceiver(receiver) }
                }

                // ── Menü-Zustand bei Drawer-Wechsel zurücksetzen ─
                LaunchedEffect(isDrawerOpen) {
                    if (isDrawerOpen) {
                        isSettingsOpen = false
                        isSearchOpen = false
                        isFavoritesConfigOpen = false
                        isColorConfigOpen = false
                        isSizeConfigOpen = false
                        isFontSelectionOpen = false
                        isEditConfigOpen = false
                        isIconConfigOpen = false
                        isWallpaperConfigOpen = false
                        isInfoOpen = false
                        selectedFolderForConfig = null
                    }
                }

                // Back-Callback nur wenn kein Menü offen ist deaktivieren
                LaunchedEffect(
                    isDrawerOpen, isFavoritesConfigOpen, isColorConfigOpen,
                    isSizeConfigOpen, isFontSelectionOpen, isEditConfigOpen,
                    isIconConfigOpen, isWallpaperConfigOpen, isInfoOpen,
                    selectedFolderForConfig, isSearchOpen
                ) {
                    val anyModalOpen = isDrawerOpen || isFavoritesConfigOpen ||
                        isColorConfigOpen || isSizeConfigOpen || isFontSelectionOpen ||
                        isEditConfigOpen || isIconConfigOpen || isWallpaperConfigOpen ||
                        isInfoOpen || selectedFolderForConfig != null || isSearchOpen
                    backCallback.isEnabled = !anyModalOpen
                }

                // ── Back-Navigation-Hierarchie ───────────────────
                BackHandler(
                    enabled = isDrawerOpen || isFavoritesConfigOpen || isColorConfigOpen ||
                        isSizeConfigOpen || isFontSelectionOpen || isEditConfigOpen ||
                        isIconConfigOpen || isWallpaperConfigOpen || isInfoOpen ||
                        selectedFolderForConfig != null || isSearchOpen
                ) {
                    when {
                        selectedFolderForConfig != null -> selectedFolderForConfig = null
                        isSearchOpen -> isSearchOpen = false
                        isFontSelectionOpen -> isFontSelectionOpen = false
                        isWallpaperConfigOpen -> isWallpaperConfigOpen = false
                        isIconConfigOpen -> isIconConfigOpen = false
                        isDrawerOpen -> isDrawerOpen = false
                        isFavoritesConfigOpen -> isFavoritesConfigOpen = false
                        isColorConfigOpen -> isColorConfigOpen = false
                        isSizeConfigOpen -> isSizeConfigOpen = false
                        isEditConfigOpen -> isEditConfigOpen = false
                        isInfoOpen -> isInfoOpen = false
                    }
                }

                // ── Haupt-UI ─────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { rootSize = it.size }
                ) {
                    SystemWallpaperView(
                        customWallpaperUri = customWallpaperUri,
                        blurLevel = wallpaperBlur,
                        dimLevel = wallpaperDim,
                        zoomLevel = wallpaperZoom
                    )

                    // Drawer <-> HomeScreen Transition
                    AnimatedContent(
                        targetState = isDrawerOpen,
                        transitionSpec = {
                            if (targetState) {
                                (slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = tween(300, easing = EaseOutCubic)
                                ) + fadeIn(animationSpec = tween(200)))
                                    .togetherWith(fadeOut(animationSpec = tween(200)))
                            } else {
                                fadeIn(animationSpec = tween(200))
                                    .togetherWith(
                                        slideOutVertically(
                                            targetOffsetY = { it },
                                            animationSpec = tween(300, easing = EaseInCubic)
                                        ) + fadeOut(animationSpec = tween(200))
                                    )
                            }
                        },
                        label = "DrawerTransition"
                    ) { targetIsDrawerOpen ->
                        if (targetIsDrawerOpen) {
                            AppDrawer(
                                apps = allApps,
                                folders = folders,
                                onToggleFavorite = { pkg ->
                                    val newFavs = LauncherLogic.toggleFavorite(favoritePackages, pkg)
                                    if (newFavs != favoritePackages) {
                                        scope.launch { favoritesManager.saveFavorites(newFavs) }
                                    }
                                },
                                isFavorite = { pkg -> pkg in favoritePackages },
                                onUpdateFolders = { newFolders ->
                                    scope.launch { folderManager.saveFolders(newFolders) }
                                },
                                onOpenFolderConfig = { folder -> selectedFolderForConfig = folder },
                                onClose = { isDrawerOpen = false },
                                onAppLaunchForReturn = { pkg, bounds ->
                                    pendingReturnAnimation = ReturnAnimation(bounds, LaunchSource.DRAWER, pkg)
                                },
                                returnIconPackage = returnIconPackage
                            )
                        } else {
                            HomeScreen(
                                favorites = favorites,
                                isSettingsOpen = isSettingsOpen,
                                isSearchOpen = isSearchOpen,
                                onOpenDrawer = { isDrawerOpen = true },
                                onOpenSearch = { isSearchOpen = true },
                                onToggleSettings = { isSettingsOpen = !isSettingsOpen },
                                onOpenFavoritesConfig = { isFavoritesConfigOpen = true },
                                onOpenColorConfig = { isColorConfigOpen = true },
                                onOpenSizeConfig = { isSizeConfigOpen = true },
                                onOpenSystemSettings = { isEditConfigOpen = true },
                                onOpenInfo = { isInfoOpen = true },
                                onAppLaunchForReturn = { pkg, bounds ->
                                    pendingReturnAnimation = ReturnAnimation(bounds, LaunchSource.HOME, pkg)
                                },
                                returnIconPackage = returnIconPackage
                            )
                        }
                    }

                    // ── Overlay-Menüs ────────────────────────────
                    MenuOverlay(visible = isFavoritesConfigOpen, backgroundColor = menuBackgroundColor) {
                        FavoritesConfigMenu(
                            apps = allApps,
                            initialFavoritePackages = favoritePackages,
                            showFavoriteLabels = showFavoriteLabels,
                            onShowLabelsToggled = { show ->
                                scope.launch { themeManager.setShowFavoriteLabels(show) }
                            },
                            onConfirm = { newFavs ->
                                scope.launch { favoritesManager.saveFavorites(newFavs) }
                                isFavoritesConfigOpen = false
                            },
                            onClose = { isFavoritesConfigOpen = false }
                        )
                    }

                    MenuOverlay(visible = selectedFolderForConfig != null, backgroundColor = menuBackgroundColor) {
                        selectedFolderForConfig?.let { folder ->
                            FolderConfigMenu(
                                folder = folder,
                                allApps = allApps,
                                allFolders = folders,
                                onConfirm = { updatedFolder ->
                                    val folderExists = folders.any { it.id == updatedFolder.id }
                                    val newFolders = if (folderExists) {
                                        folders.map { if (it.id == updatedFolder.id) updatedFolder else it }
                                    } else if (updatedFolder.appPackageNames.isNotEmpty()) {
                                        folders + updatedFolder
                                    } else {
                                        folders
                                    }
                                    scope.launch { folderManager.saveFolders(newFolders) }
                                    selectedFolderForConfig = null
                                },
                                onDelete = { folderId ->
                                    val newFolders = folders.filter { it.id != folderId }
                                    scope.launch { folderManager.saveFolders(newFolders) }
                                    selectedFolderForConfig = null
                                },
                                onClose = { selectedFolderForConfig = null }
                            )
                        }
                    }

                    MenuOverlay(visible = isColorConfigOpen, backgroundColor = menuBackgroundColor) {
                        ColorConfigMenu(
                            selectedTheme = currentTheme,
                            onThemeSelected = { scope.launch { themeManager.setTheme(it) } },
                            isDarkTextEnabled = isDarkTextEnabled,
                            onDarkTextToggled = { scope.launch { themeManager.setDarkTextEnabled(it) } },
                            isLiquidGlassEnabled = isLiquidGlassEnabled,
                            onLiquidGlassToggled = { scope.launch { themeManager.setLiquidGlassEnabled(it) } },
                            customWallpaperUri = customWallpaperUri,
                            onClose = { isColorConfigOpen = false }
                        )
                    }

                    MenuOverlay(visible = isSizeConfigOpen, backgroundColor = menuBackgroundColor) {
                        SizeConfigMenu(
                            currentFontSize = currentFontSize,
                            onFontSizeSelected = { scope.launch { themeManager.setFontSize(it) } },
                            currentFontWeight = currentFontWeight,
                            onFontWeightSelected = { scope.launch { themeManager.setFontWeight(it) } },
                            currentIconSize = currentIconSize,
                            onIconSizeSelected = { scope.launch { themeManager.setIconSize(it) } },
                            currentAppFont = currentAppFont,
                            onOpenFontSelection = { isFontSelectionOpen = true },
                            customWallpaperUri = customWallpaperUri,
                            onClose = { isSizeConfigOpen = false }
                        )
                    }

                    MenuOverlay(visible = isFontSelectionOpen, backgroundColor = menuBackgroundColor) {
                        FontSelectionMenu(
                            currentAppFont = currentAppFont,
                            onAppFontSelected = { scope.launch { themeManager.setAppFont(it) } },
                            onBack = { isFontSelectionOpen = false }
                        )
                    }

                    MenuOverlay(visible = isEditConfigOpen, backgroundColor = menuBackgroundColor) {
                        EditConfigMenu(
                            onOpenIconConfig = { isIconConfigOpen = true },
                            onChangeWallpaper = {
                                wallpaperPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onResetWallpaper = {
                                scope.launch {
                                    themeManager.setCustomWallpaperUri(null)
                                    themeManager.setWallpaperBlur(0f)
                                    themeManager.setWallpaperDim(0.1f)
                                    themeManager.setWallpaperZoom(1.0f)
                                }
                                Toast.makeText(context, "Hintergrund entfernt", Toast.LENGTH_SHORT).show()
                            },
                            onOpenWallpaperAdjust = { isWallpaperConfigOpen = true },
                            isCustomWallpaperSet = customWallpaperUri != null,
                            onClose = { isEditConfigOpen = false }
                        )
                    }

                    MenuOverlay(visible = isWallpaperConfigOpen, backgroundColor = menuBackgroundColor) {
                        WallpaperConfigMenu(
                            blurLevel = wallpaperBlur,
                            onBlurChange = { scope.launch { themeManager.setWallpaperBlur(it) } },
                            dimLevel = wallpaperDim,
                            onDimChange = { scope.launch { themeManager.setWallpaperDim(it) } },
                            zoomLevel = wallpaperZoom,
                            onZoomChange = { scope.launch { themeManager.setWallpaperZoom(it) } },
                            customWallpaperUri = customWallpaperUri,
                            onClose = { isWallpaperConfigOpen = false }
                        )
                    }

                    MenuOverlay(visible = isIconConfigOpen, backgroundColor = menuBackgroundColor) {
                        IconConfigMenu(
                            apps = allApps,
                            customIcons = customIcons,
                            onIconSelected = { pkg, iconName ->
                                scope.launch { iconManager.setCustomIcon(pkg, iconName) }
                            },
                            onClose = { isIconConfigOpen = false }
                        )
                    }

                    MenuOverlay(visible = isInfoOpen, backgroundColor = menuBackgroundColor) {
                        InfoDialog(
                            customWallpaperUri = customWallpaperUri,
                            onClose = { isInfoOpen = false }
                        )
                    }

                    // Such-Overlay (eigene Animation)
                    AnimatedVisibility(
                        visible = isSearchOpen,
                        enter = fadeIn(animationSpec = tween(200)),
                        exit = fadeOut(animationSpec = tween(200))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f))
                                .pointerInput(Unit) {
                                    detectTapGestures { isSearchOpen = false }
                                }
                        ) {
                            BottomSearch(
                                apps = allApps,
                                onClose = { isSearchOpen = false },
                                onAppLaunch = { app ->
                                    isSearchOpen = false
                                    val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                    if (intent != null) launchAppNoTransition(context, intent)
                                }
                            )
                        }
                    }

                    // Rückkehr-Animation
                    activeReturnAnimation?.let { animation ->
                        ReturnAnimationOverlay(
                            bounds = animation.bounds,
                            rootSize = rootSize,
                            background = menuBackgroundColor,
                            onFinished = { activeReturnAnimation = null },
                            targetScale = if (animation.source == LaunchSource.DRAWER) 0.65f else 0.7f
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        logHomeIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        enforceExcludeFromRecents()
        validateDefaultLauncher()
    }

    /**
     * Stellt sicher, dass die Activity nicht in der Übersicht der letzten Apps erscheint.
     */
    private fun enforceExcludeFromRecents() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val tasks = am.appTasks
        if (!tasks.isNullOrEmpty()) {
            tasks[0].setExcludeFromRecents(true)
        }
    }

    private fun validateDefaultLauncher() {
        val resolveInfo = packageManager.resolveActivity(createHomeIntent(), PackageManager.MATCH_DEFAULT_ONLY)
        val resolvedPackage = resolveInfo?.activityInfo?.packageName
        if (resolvedPackage.isNullOrBlank()) {
            Log.w(TAG, "Default-Launcher konnte nicht ermittelt werden")
            return
        }

        if (resolvedPackage != packageName) {
            Log.w(TAG, "Aktueller Default-Launcher ist $resolvedPackage, erwartet $packageName")
            if (!defaultLauncherWarningShown || lastDefaultLauncherPackage != resolvedPackage) {
                Toast.makeText(this, getString(R.string.default_launcher_warning), Toast.LENGTH_LONG).show()
                defaultLauncherWarningShown = true
            }
        } else {
            defaultLauncherWarningShown = false
        }

        lastDefaultLauncherPackage = resolvedPackage
    }

    private fun logHomeIntent(intent: Intent?) {
        if (!isHomeIntent(intent)) return
        val now = SystemClock.elapsedRealtime()
        val delta = if (lastHomeIntentTimestamp == 0L) 0 else now - lastHomeIntentTimestamp
        Log.d(TAG, "Home-Intent empfangen (Δ=${delta}ms)")
        lastHomeIntentTimestamp = now
    }

    private fun isHomeIntent(intent: Intent?) =
        intent?.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)

    private fun createHomeIntent() = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
    }
}

// ── Wiederverwendbares Menü-Overlay ──────────────────────────────

/**
 * Wiederverwendbares animiertes Overlay für Vollbild-Konfigurationsmenüs.
 * Reduziert die Duplikation der AnimatedVisibility + Box + Background Logik
 * die vorher für jedes Menü einzeln geschrieben wurde.
 *
 * @param visible Ob das Overlay sichtbar sein soll.
 * @param backgroundColor Hintergrundfarbe des Overlays.
 * @param content Der Menü-Inhalt.
 */
@Composable
private fun MenuOverlay(
    visible: Boolean,
    backgroundColor: Color,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(300, easing = EaseOutCubic)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300, easing = EaseInCubic)
        ) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { /* Blockiert Klicks auf den Hintergrund */ }
                }
                .background(backgroundColor)
        ) {
            content()
        }
    }
}

