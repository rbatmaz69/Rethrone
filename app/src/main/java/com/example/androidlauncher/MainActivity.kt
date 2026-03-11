package com.example.androidlauncher

import android.Manifest
import android.app.ActivityManager
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.androidlauncher.data.AppFont
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.AppRepository
import com.example.androidlauncher.data.AutoIconRule
import com.example.androidlauncher.data.FavoritesManager
import com.example.androidlauncher.data.FolderInfo
import com.example.androidlauncher.data.FolderManager
import com.example.androidlauncher.data.FontSize
import com.example.androidlauncher.data.FontWeightLevel
import com.example.androidlauncher.data.IconManager
import com.example.androidlauncher.data.IconQualityEvaluator
import com.example.androidlauncher.data.IconSize
import com.example.androidlauncher.data.SearchSuggestionsManager
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
import com.example.androidlauncher.ui.LaunchAnimationOverlay
import com.example.androidlauncher.ui.ReturnAnimationOverlay
import com.example.androidlauncher.ui.SizeConfigMenu
import com.example.androidlauncher.ui.SystemWallpaperView
import com.example.androidlauncher.ui.WallpaperConfigMenu
import com.example.androidlauncher.ui.WallpaperCropScreen
import com.example.androidlauncher.ui.launchAppNoTransition
import com.example.androidlauncher.ui.theme.AndroidLauncherTheme
import com.example.androidlauncher.ui.theme.ColorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Haupt-Activity des Launchers.
 */
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val SEARCH_RETURN_TARGET = "__search_return_target__"
        private const val RETURN_TAG = "ReturnFlow"
    }

    private lateinit var backCallback: OnBackPressedCallback
    private var lastDefaultLauncherPackage: String? = null
    private var lastHomeIntentTimestamp: Long = 0
    private var defaultLauncherWarningShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        }
        onBackPressedDispatcher.addCallback(this, backCallback)

        enforceExcludeFromRecents()

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            val themeManager = remember { ThemeManager(context) }
            val folderManager = remember { FolderManager(context) }
            val iconManager = remember { IconManager(context) }
            val favoritesManager = remember { FavoritesManager(context) }
            val appRepository = remember { AppRepository(context) }
            val searchSuggestionsManager = remember { SearchSuggestionsManager(context) }
            val launcherDeviceActions = remember { LauncherDeviceActions(context) }
            val shakeManager = remember { LauncherShakeManager(context) }

            LaunchedEffect(Unit) {
                favoritesManager.migrateFromSharedPreferences(context)
            }

            val currentTheme by themeManager.selectedTheme.collectAsState(initial = ColorTheme.SIGNATURE)
            val currentFontSize by themeManager.selectedFontSize.collectAsState(initial = FontSize.STANDARD)
            val currentFontWeight by themeManager.selectedFontWeight.collectAsState(initial = FontWeightLevel.NORMAL)
            val currentIconSize by themeManager.selectedIconSize.collectAsState(initial = IconSize.STANDARD)
            val currentAppFont by themeManager.selectedAppFont.collectAsState(initial = AppFont.SYSTEM_DEFAULT)
            val isDarkTextEnabled by themeManager.isDarkTextEnabled.collectAsState(initial = false)
            val showFavoriteLabels by themeManager.showFavoriteLabels.collectAsState(initial = false)
            val isLiquidGlassEnabled by themeManager.isLiquidGlassEnabled.collectAsState(initial = true)
            val isShakeGesturesEnabled by themeManager.isShakeGesturesEnabled.collectAsState(initial = true)
            val isSmartSuggestionsEnabled by themeManager.isSmartSuggestionsEnabled.collectAsState(initial = true)
            val isHapticFeedbackEnabled by themeManager.isHapticFeedbackEnabled.collectAsState(initial = true)

            val customWallpaperUri by themeManager.customWallpaperUri.collectAsState(initial = null)
            val wallpaperBlur by themeManager.wallpaperBlur.collectAsState(initial = 0f)
            val wallpaperDim by themeManager.wallpaperDim.collectAsState(initial = 0.1f)
            val wallpaperZoom by themeManager.wallpaperZoom.collectAsState(initial = 1.0f)
            val searchHistory by searchSuggestionsManager.webHistory.collectAsState(initial = emptyList())
            val appUsageStats by searchSuggestionsManager.appUsageStats.collectAsState(initial = emptyMap())

            // UI Offset States
            val favoritesOffsetX by themeManager.favoritesOffsetX.collectAsState(initial = 0f)
            val favoritesOffsetY by themeManager.favoritesOffsetY.collectAsState(initial = 0f)
            val clockOffsetY by themeManager.clockOffsetY.collectAsState(initial = 0f)

            val folders by folderManager.folders.collectAsState(initial = emptyList())
            val customIcons by iconManager.customIcons.collectAsState(initial = emptyMap())
            val autoIconFallbacks by iconManager.autoIconFallbacks.collectAsState(initial = emptyMap())
            val autoIconRules by iconManager.autoIconRules.collectAsState(initial = emptyMap())
            val favoritePackages by favoritesManager.favorites.collectAsState(initial = emptyList())

            var isWallpaperCropOpen by remember { mutableStateOf(false) }
            var pendingWallpaperUri by remember { mutableStateOf<Uri?>(null) }
            var showUsageAccessPrompt by remember { mutableStateOf(false) }
            var hasShownUsageAccessPrompt by remember { mutableStateOf(false) }
            var pendingPermissionGestureAction by remember {
                mutableStateOf<LauncherShakeGestureDetector.GestureAction?>(null)
            }

            val wallpaperPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri: Uri? ->
                uri?.let { sourceUri ->
                    pendingWallpaperUri = sourceUri
                    isWallpaperCropOpen = true
                }
            }

            val cameraPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                val pendingAction = pendingPermissionGestureAction
                pendingPermissionGestureAction = null

                if (pendingAction != LauncherShakeGestureDetector.GestureAction.TOGGLE_FLASHLIGHT) {
                    return@rememberLauncherForActivityResult
                }

                if (!isGranted) {
                    Toast.makeText(
                        context,
                        "Kamerazugriff wird für die Taschenlampe benötigt",
                        Toast.LENGTH_LONG
                    ).show()
                    return@rememberLauncherForActivityResult
                }

                when (launcherDeviceActions.toggleFlashlight()) {
                    is FlashlightToggleResult.Success -> {
                        launcherDeviceActions.vibrateGestureFeedback(this@MainActivity)
                    }
                    FlashlightToggleResult.Unsupported -> {
                        Toast.makeText(context, "Keine Taschenlampe verfügbar", Toast.LENGTH_SHORT).show()
                    }
                    FlashlightToggleResult.MissingPermission -> {
                        Toast.makeText(
                            context,
                            "Kamerazugriff wird für die Taschenlampe benötigt",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    FlashlightToggleResult.Error -> {
                        Toast.makeText(context, "Taschenlampe konnte nicht geändert werden", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            shakeManager.onGestureAction = { gestureAction ->
                when (gestureAction) {
                    LauncherShakeGestureDetector.GestureAction.TOGGLE_FLASHLIGHT -> {
                        when (launcherDeviceActions.toggleFlashlight()) {
                            is FlashlightToggleResult.Success -> {
                                launcherDeviceActions.vibrateGestureFeedback(this@MainActivity)
                            }
                            FlashlightToggleResult.Unsupported -> {
                                Toast.makeText(context, "Keine Taschenlampe verfügbar", Toast.LENGTH_SHORT).show()
                            }
                            FlashlightToggleResult.MissingPermission -> {
                                pendingPermissionGestureAction = gestureAction
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                            FlashlightToggleResult.Error -> {
                                Toast.makeText(context, "Taschenlampe konnte nicht geändert werden", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    LauncherShakeGestureDetector.GestureAction.OPEN_CAMERA -> {
                        val didOpenCamera = launcherDeviceActions.openCamera(this@MainActivity)
                        if (didOpenCamera) {
                            launcherDeviceActions.vibrateGestureFeedback(this@MainActivity)
                        } else {
                            Toast.makeText(context, "Keine Kamera-App gefunden", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            AndroidLauncherTheme(
                colorTheme = currentTheme,
                fontSize = currentFontSize,
                fontWeight = currentFontWeight,
                iconSize = currentIconSize,
                darkTextEnabled = isDarkTextEnabled,
                showFavoriteLabels = showFavoriteLabels,
                liquidGlassEnabled = isLiquidGlassEnabled,
                appFont = currentAppFont,
                hapticFeedbackEnabled = isHapticFeedbackEnabled
            ) {
                @Suppress("DEPRECATION")
                val lifecycleOwner = LocalLifecycleOwner.current
                val menuBackgroundColor = currentTheme.menuSurfaceColor(isDarkTextEnabled)
                val searchLaunchOverlayColor = currentTheme.searchSurfaceColor(isDarkTextEnabled).copy(
                    alpha = if (isDarkTextEnabled) 0.97f else 0.985f
                )
                val launchOverlayBrush = remember(currentTheme, isDarkTextEnabled) {
                    currentTheme.animationBrush(isDarkTextEnabled, alpha = 0.98f)
                }
                val returnOverlayBrush = remember(currentTheme, isDarkTextEnabled) {
                    currentTheme.animationBrush(isDarkTextEnabled, alpha = 0.92f)
                }

                var rootSize by remember { mutableStateOf(IntSize.Zero) }
                var pendingReturnAnimation by remember { mutableStateOf<ReturnAnimation?>(null) }
                var pendingReturnAnimationStartedWallClockMs by remember { mutableStateOf(0L) }
                var activeReturnAnimation by remember { mutableStateOf<ReturnAnimation?>(null) }
                var returnIconPackage by remember { mutableStateOf<String?>(null) }
                var searchButtonBounceToken by remember { mutableStateOf(0) }
                val returnOverlayDurationMs = 260L
                val returnBounceDelayMs = 185L
                var isDrawerOpen by remember { mutableStateOf(false) }
                var isSettingsOpen by remember { mutableStateOf(false) }
                var isSearchOpen by remember { mutableStateOf(false) }
                var isHomeEditMode by remember { mutableStateOf(false) }
                var homeSearchButtonBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
                var isSearchLaunching by remember { mutableStateOf(false) }
                var isAppLaunchAnimating by remember { mutableStateOf(false) }
                var activeLaunchBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
                var activeLaunchBackground by remember { mutableStateOf(searchLaunchOverlayColor) }
                var activeLaunchBackgroundBrush by remember { mutableStateOf<Brush?>(launchOverlayBrush) }
                val searchLaunchDurationMs = 260L
                val searchLaunchSettleAfterStartMs = 30L
                var isFavoritesConfigOpen by remember { mutableStateOf(false) }
                var isColorConfigOpen by remember { mutableStateOf(false) }
                var isSizeConfigOpen by remember { mutableStateOf(false) }
                var isFontSelectionOpen by remember { mutableStateOf(false) }
                var isEditConfigOpen by remember { mutableStateOf(false) }
                var isIconConfigOpen by remember { mutableStateOf(false) }
                var isWallpaperConfigOpen by remember { mutableStateOf(false) }
                var isInfoOpen by remember { mutableStateOf(false) }
                var selectedFolderForConfig by remember { mutableStateOf<FolderInfo?>(null) }
                var isLauncherResumed by remember { mutableStateOf(false) }
                var returnResumeGuardState by remember {
                    mutableStateOf(ReturnResumeGuardState())
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            isLauncherResumed = true
                        }
                        if (event == Lifecycle.Event.ON_PAUSE) {
                            isLauncherResumed = false
                        }
                        if (event == Lifecycle.Event.ON_RESUME) {
                            val resumeDecision = ReturnResumeGuard.onResume(returnResumeGuardState)
                            returnResumeGuardState = resumeDecision.nextState
                            if (resumeDecision.shouldSuppress) {
                                val awaitingUserPresent = returnResumeGuardState.awaitingUserPresent
                                val skipNextResume = returnResumeGuardState.skipNextResume
                                 Log.d(
                                     RETURN_TAG,
                                    "skip return animation on launcher resume caused by lockscreen state awaitingUserPresent=$awaitingUserPresent skipNextResume=$skipNextResume"
                                 )
                                 return@LifecycleEventObserver
                             }
                            val accessibilityEnabled = LauncherAccessibilityService.isAccessibilityServiceEnabled(context)
                            val usageAccessEnabled = ForegroundAppResolver.hasUsageAccess(context)
                            val storedPackages = ReturnOriginStore.getStoredPackageNames(context)
                            val beforeLauncherObservation = if (accessibilityEnabled) {
                                LauncherAccessibilityService.getLastForegroundObservationBeforeLauncher(context)
                            } else {
                                null
                            }
                            val usageObservation = if (usageAccessEnabled) {
                                ForegroundAppResolver.getRecentForegroundObservation(context, storedPackages)
                            } else {
                                null
                            }
                            val storedOriginCount = ReturnOriginStore.getStoredOriginCount(context)
                            Log.d(
                                RETURN_TAG,
                                "resume a11y=$accessibilityEnabled usage=$usageAccessEnabled storedPackages=$storedPackages storedOrigins=$storedOriginCount pending=${pendingReturnAnimation?.launchedPackageName} pendingLaunchWall=${pendingReturnAnimationStartedWallClockMs} beforeLauncher=$beforeLauncherObservation usageObservation=$usageObservation"
                            )

                            if (!accessibilityEnabled && !usageAccessEnabled && storedOriginCount > 1 && !hasShownUsageAccessPrompt) {
                                showUsageAccessPrompt = true
                                hasShownUsageAccessPrompt = true
                                Log.d(RETURN_TAG, "prompt usage access because multiple origins exist and no foreground tracking is available")
                            }

                            val gateDecision = ReturnAnimationGate.resolve(
                                pendingReturnAnimation = pendingReturnAnimation,
                                pendingLaunchStartedAtMs = pendingReturnAnimationStartedWallClockMs,
                                observations = listOfNotNull(beforeLauncherObservation, usageObservation)
                            )
                            val selectedReturnAnimation = gateDecision.returnAnimation
                             val gateReason = gateDecision.reason
                             val gateMatchedObservation = gateDecision.matchedObservation
                            val chosenPackage = selectedReturnAnimation?.launchedPackageName
                            val targetPackage = selectedReturnAnimation?.packageName
                            val source = selectedReturnAnimation?.source
                            val hasBounds = selectedReturnAnimation?.bounds != null

                             Log.d(
                                 RETURN_TAG,
                                 "resume gateReason=$gateReason matchedObservation=$gateMatchedObservation chosen=$chosenPackage target=$targetPackage source=$source bounds=$hasBounds"
                             )

                            selectedReturnAnimation?.let { animation ->
                                isDrawerOpen = animation.source == LaunchSource.DRAWER
                                if (!isDrawerOpen) {
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
                                activeReturnAnimation = animation
                                returnIconPackage = null
                                pendingReturnAnimation = null
                                pendingReturnAnimationStartedWallClockMs = 0L
                                ReturnOriginStore.clear(context, animation.launchedPackageName)
                                Log.d(RETURN_TAG, "activateReturn launched=${animation.launchedPackageName} target=${animation.packageName} source=${animation.source}")
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                DisposableEffect(isLauncherResumed, isShakeGesturesEnabled) {
                    if (isLauncherResumed && isShakeGesturesEnabled) {
                        launcherDeviceActions.startTorchMonitoring()
                        shakeManager.start()
                    } else {
                        shakeManager.stop()
                        launcherDeviceActions.stopTorchMonitoring()
                    }

                    onDispose {
                        shakeManager.stop()
                        launcherDeviceActions.stopTorchMonitoring()
                    }
                }

                LaunchedEffect(activeReturnAnimation?.packageName) {
                    val packageName = activeReturnAnimation?.packageName ?: return@LaunchedEffect
                    delay(returnBounceDelayMs)
                    if (activeReturnAnimation?.packageName == packageName) {
                        if (packageName == SEARCH_RETURN_TARGET) {
                            Log.d(RETURN_TAG, "bounce searchButton target=$packageName")
                             searchButtonBounceToken += 1
                        } else {
                            Log.d(RETURN_TAG, "bounce icon target=$packageName")
                             returnIconPackage = packageName
                        }
                    } else {
                        Log.d(RETURN_TAG, "bounce skipped stale target=$packageName active=${activeReturnAnimation?.packageName}")
                    }
                }

                LaunchedEffect(returnIconPackage) {
                    if (returnIconPackage != null) {
                        delay(220)
                        returnIconPackage = null
                    }
                }

                val allApps = remember { mutableStateListOf<AppInfo>() }
                val favorites = remember(allApps.toList(), favoritePackages) {
                    LauncherLogic.getFavoriteApps(allApps.toList(), favoritePackages)
                }

                fun refreshAppList(targetPackageName: String? = null) {
                    scope.launch {
                        appRepository.cleanupLegacyCache()
                        val basicList = appRepository.getInstalledApps()
                        val currentIcons = allApps.associate { it.packageName to it.iconBitmap }
                        val currentFallbacks = allApps.associate { it.packageName to it.autoIconFallback }
                        val mergedList = basicList.map {
                            val resolvedRule = autoIconRules[it.packageName]
                                ?: IconQualityEvaluator.resolveDefaultRule(it.packageName)
                            it.copy(
                                iconBitmap = currentIcons[it.packageName],
                                autoIconFallback = autoIconFallbacks[it.packageName] ?: currentFallbacks[it.packageName],
                                autoIconRule = resolvedRule
                            )
                        }
                        if (allApps.size != mergedList.size || allApps.map { it.packageName } != mergedList.map { it.packageName }) {
                            allApps.clear()
                            allApps.addAll(mergedList)
                        } else {
                            mergedList.forEachIndexed { index, appInfo ->
                                if (index < allApps.size && allApps[index] != appInfo) {
                                    allApps[index] = appInfo
                                }
                            }
                        }

                        val appSnapshot = allApps.toList()
                        if (targetPackageName != null) {
                            val targetIndex = appSnapshot.indexOfFirst { it.packageName == targetPackageName }
                            if (targetIndex >= 0) {
                                appRepository.loadResolvedIcon(appSnapshot[targetIndex])?.let { resolvedIcon ->
                                    val snapshotApp = appSnapshot[targetIndex]
                                    val fallback = resolvedIcon.autoFallback
                                    if (autoIconFallbacks[snapshotApp.packageName] != fallback) {
                                        iconManager.setAutoIconFallback(snapshotApp.packageName, fallback)
                                    }
                                    withContext(Dispatchers.Main) {
                                        if (targetIndex < allApps.size && allApps[targetIndex].packageName == snapshotApp.packageName) {
                                            allApps[targetIndex] = allApps[targetIndex].copy(
                                                iconBitmap = resolvedIcon.imageBitmap,
                                                autoIconFallback = fallback
                                            )
                                        }
                                    }
                                }
                            }
                            return@launch
                        }

                        appRepository.loadIconsWithPriority(
                            apps = appSnapshot,
                            favoritePackages = favoritePackages
                        ) { idx, resolvedIcon ->
                            val snapshotApp = appSnapshot.getOrNull(idx) ?: return@loadIconsWithPriority
                            val fallback = resolvedIcon.autoFallback
                            if (autoIconFallbacks[snapshotApp.packageName] != fallback) {
                                iconManager.setAutoIconFallback(snapshotApp.packageName, fallback)
                            }
                            withContext(Dispatchers.Main) {
                                if (idx < allApps.size && allApps[idx].packageName == snapshotApp.packageName) {
                                    allApps[idx] = allApps[idx].copy(
                                        iconBitmap = resolvedIcon.imageBitmap,
                                        autoIconFallback = fallback
                                    )
                                }
                            }
                        }
                    }
                }

                fun requestLauncherLaunch(
                    packageName: String,
                    intent: Intent,
                    bounds: androidx.compose.ui.geometry.Rect?,
                    source: LaunchSource,
                    overlayColor: Color = searchLaunchOverlayColor,
                    overlayBrush: Brush? = launchOverlayBrush,
                    returnBounds: androidx.compose.ui.geometry.Rect? = bounds,
                    returnPackageName: String = packageName,
                    trackAppLaunch: Boolean = true,
                    onCompleted: (() -> Unit)? = null
                ) {
                    if (isAppLaunchAnimating) return
                    val returnAnimation = ReturnAnimation(
                        bounds = returnBounds,
                        source = source,
                        packageName = returnPackageName,
                        launchedPackageName = packageName
                    )
                    Log.d(
                        RETURN_TAG,
                        "saveReturn launched=$packageName target=$returnPackageName source=$source launchBounds=${bounds != null} returnBounds=${returnBounds != null}"
                    )
                    pendingReturnAnimation = returnAnimation
                    pendingReturnAnimationStartedWallClockMs = System.currentTimeMillis()
                    ReturnOriginStore.save(context, packageName, returnAnimation)
                    isAppLaunchAnimating = true
                    activeLaunchBackground = overlayColor
                    activeLaunchBackgroundBrush = overlayBrush
                    activeLaunchBounds = bounds

                    scope.launch {
                        try {
                            delay(searchLaunchDurationMs)
                            launchAppNoTransition(context, Intent(intent))
                            if (trackAppLaunch && isSmartSuggestionsEnabled) {
                                searchSuggestionsManager.recordAppLaunch(packageName)
                            }
                            delay(searchLaunchSettleAfterStartMs)
                        } finally {
                            activeLaunchBounds = null
                            isAppLaunchAnimating = false
                            onCompleted?.invoke()
                        }
                    }
                }

                fun requestSearchLaunch(
                    intent: Intent,
                    bounds: androidx.compose.ui.geometry.Rect?,
                    webQuery: String? = null
                ) {
                    if (isSearchLaunching || isAppLaunchAnimating) return
                    isSearchLaunching = true
                    isSearchOpen = false
                    if (!webQuery.isNullOrBlank() && isSmartSuggestionsEnabled) {
                        scope.launch {
                            searchSuggestionsManager.recordWebSearch(webQuery)
                        }
                    }
                    val resolvedPackageName = intent.`package`
                        ?: intent.component?.packageName
                        ?: context.packageManager.resolveActivity(Intent(intent), PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
                        ?: context.packageManager.resolveActivity(Intent(intent), 0)?.activityInfo?.packageName
                        ?: intent.data?.host
                        ?: "search-launch"
                    Log.d(RETURN_TAG, "requestSearchLaunch resolved=$resolvedPackageName bounds=${bounds != null} searchButton=${homeSearchButtonBounds != null}")
                    requestLauncherLaunch(
                        packageName = resolvedPackageName,
                        intent = intent,
                        bounds = bounds,
                        source = LaunchSource.HOME,
                        overlayColor = searchLaunchOverlayColor,
                        overlayBrush = launchOverlayBrush,
                        returnBounds = homeSearchButtonBounds ?: bounds,
                        returnPackageName = SEARCH_RETURN_TARGET,
                        trackAppLaunch = webQuery.isNullOrBlank(),
                        onCompleted = {
                            isSearchLaunching = false
                        }
                    )
                }

                LaunchedEffect(Unit) { refreshAppList() }
                LaunchedEffect(autoIconFallbacks, autoIconRules) {
                    allApps.indices.forEach { index ->
                        val app = allApps[index]
                        val storedFallback = autoIconFallbacks[app.packageName]
                        val resolvedRule = autoIconRules[app.packageName]
                            ?: IconQualityEvaluator.resolveDefaultRule(app.packageName)
                        if (app.autoIconFallback != storedFallback || app.autoIconRule != resolvedRule) {
                            allApps[index] = app.copy(
                                autoIconFallback = storedFallback,
                                autoIconRule = resolvedRule
                            )
                        }
                    }
                }

                DisposableEffect(Unit) {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(ctx: Context?, intent: Intent?) {
                            when (intent?.action) {
                                Intent.ACTION_SCREEN_OFF -> {
                                    returnResumeGuardState = ReturnResumeGuard.onScreenOff(
                                         state = returnResumeGuardState,
                                         launcherWasForeground = isLauncherResumed
                                     )
                                    if (isLauncherResumed) {
                                        Log.d(RETURN_TAG, "screen off while launcher foreground -> suppress return during lockscreen cycle")
                                    }
                                }
                                Intent.ACTION_USER_PRESENT -> {
                                    val previousState = returnResumeGuardState
                                    returnResumeGuardState = ReturnResumeGuard.onUserPresent(returnResumeGuardState)
                                    if (previousState != returnResumeGuardState) {
                                        Log.d(RETURN_TAG, "user present after launcher screen off -> suppress next launcher resume return")
                                    }
                                }
                                Intent.ACTION_PACKAGE_ADDED,
                                Intent.ACTION_PACKAGE_REMOVED,
                                Intent.ACTION_PACKAGE_REPLACED -> {
                                    val packageName = intent.data?.schemeSpecificPart
                                    scope.launch {
                                        if (!packageName.isNullOrBlank()) {
                                            appRepository.clearIconCache(packageName)
                                            iconManager.invalidatePackage(packageName)
                                        }
                                        delay(800)
                                        refreshAppList(packageName)
                                    }
                                }
                            }
                        }
                    }
                    val filter = IntentFilter().apply {
                        addAction(Intent.ACTION_PACKAGE_ADDED)
                        addAction(Intent.ACTION_PACKAGE_REMOVED)
                        addAction(Intent.ACTION_PACKAGE_REPLACED)
                        addAction(Intent.ACTION_SCREEN_OFF)
                        addAction(Intent.ACTION_USER_PRESENT)
                        addDataScheme("package")
                    }
                    context.registerReceiver(receiver, filter)
                    onDispose { context.unregisterReceiver(receiver) }
                }

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
                        isHomeEditMode = false
                        selectedFolderForConfig = null
                    }
                }

                LaunchedEffect(
                    isDrawerOpen, isFavoritesConfigOpen, isColorConfigOpen,
                    isSizeConfigOpen, isFontSelectionOpen, isEditConfigOpen,
                    isIconConfigOpen, isWallpaperConfigOpen, isInfoOpen,
                    selectedFolderForConfig, isSearchOpen, isHomeEditMode
                ) {
                    val anyModalOpen = isDrawerOpen || isFavoritesConfigOpen ||
                        isColorConfigOpen || isSizeConfigOpen || isFontSelectionOpen ||
                        isEditConfigOpen || isIconConfigOpen || isWallpaperConfigOpen ||
                        isInfoOpen || selectedFolderForConfig != null || isSearchOpen || isHomeEditMode
                    backCallback.isEnabled = !anyModalOpen
                }

                BackHandler(
                    enabled = isDrawerOpen || isFavoritesConfigOpen || isColorConfigOpen ||
                        isSizeConfigOpen || isFontSelectionOpen || isEditConfigOpen ||
                        isIconConfigOpen || isWallpaperConfigOpen || isInfoOpen ||
                        selectedFolderForConfig != null || isSearchOpen || isHomeEditMode
                ) {
                    when {
                        selectedFolderForConfig != null -> selectedFolderForConfig = null
                        isHomeEditMode -> isHomeEditMode = false
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

                    AnimatedContent(
                        targetState = isDrawerOpen,
                        transitionSpec = {
                            if (targetState) {
                                (
                                    slideInVertically(
                                        initialOffsetY = { it },
                                        animationSpec = tween(300, easing = EaseOutCubic)
                                    ) + fadeIn(animationSpec = tween(200))
                                ).togetherWith(fadeOut(animationSpec = tween(200)))
                            } else {
                                fadeIn(animationSpec = tween(200)).togetherWith(
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
                                onLaunchApp = { pkg, intent, bounds ->
                                    requestLauncherLaunch(
                                        packageName = pkg,
                                        intent = intent,
                                        bounds = bounds,
                                        source = LaunchSource.DRAWER,
                                        overlayColor = searchLaunchOverlayColor,
                                        overlayBrush = launchOverlayBrush
                                    )
                                },
                                returnIconPackage = returnIconPackage
                            )
                        } else {
                            HomeScreen(
                                favorites = favorites,
                                isSettingsOpen = isSettingsOpen,
                                isSearchOpen = isSearchOpen,
                                isEditMode = isHomeEditMode,
                                favoritesOffsetX = favoritesOffsetX,
                                favoritesOffsetY = favoritesOffsetY,
                                clockOffsetY = clockOffsetY,
                                onOpenDrawer = { isDrawerOpen = true },
                                onOpenSearch = { isSearchOpen = true },
                                onToggleSettings = { isSettingsOpen = !isSettingsOpen },
                                onToggleEditMode = { isHomeEditMode = !isHomeEditMode },
                                onOpenFavoritesConfig = { isFavoritesConfigOpen = true },
                                onOpenColorConfig = { isColorConfigOpen = true },
                                onOpenSizeConfig = { isSizeConfigOpen = true },
                                onOpenSystemSettings = { isEditConfigOpen = true },
                                onOpenInfo = { isInfoOpen = true },
                                onSaveFavoritesOffset = { x, y ->
                                    scope.launch { themeManager.setFavoritesOffset(x, y) }
                                },
                                onSaveClockOffset = { y ->
                                    scope.launch { themeManager.setClockOffset(y) }
                                },
                                onLaunchApp = { pkg, intent, bounds ->
                                    requestLauncherLaunch(
                                        packageName = pkg,
                                        intent = intent,
                                        bounds = bounds,
                                        source = LaunchSource.HOME,
                                        overlayColor = searchLaunchOverlayColor,
                                        overlayBrush = launchOverlayBrush
                                    )
                                },
                                returnIconPackage = returnIconPackage,
                                searchButtonBounceToken = searchButtonBounceToken,
                                onSearchButtonBoundsChanged = { bounds ->
                                    homeSearchButtonBounds = bounds
                                }
                            )
                        }
                    }

                    MenuOverlay(
                        visible = isFavoritesConfigOpen,
                        backgroundColor = menuBackgroundColor,
                        enableDragToClose = false,
                        onClose = { isFavoritesConfigOpen = false }
                    ) {
                        @Suppress("DEPRECATION")
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

                    MenuOverlay(
                        visible = selectedFolderForConfig != null,
                        backgroundColor = menuBackgroundColor,
                        enableDragToClose = false,
                        onClose = { selectedFolderForConfig = null }
                    ) {
                        selectedFolderForConfig?.let { folder ->
                            FolderConfigMenu(
                                folder = folder,
                                allApps = allApps,
                                allFolders = folders,
                                onConfirm = { updatedFolder ->
                                    val folderExists = folders.any { it.id == updatedFolder.id }
                                    val newFolders = if (updatedFolder.appPackageNames.isEmpty()) {
                                        folders.filter { it.id != updatedFolder.id }
                                    } else if (folderExists) {
                                        folders.map { currentFolder ->
                                            if (currentFolder.id == updatedFolder.id) updatedFolder else currentFolder
                                        }
                                    } else {
                                        folders + updatedFolder
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

                    MenuOverlay(
                        visible = isColorConfigOpen,
                        backgroundColor = menuBackgroundColor,
                        onClose = { isColorConfigOpen = false }
                    ) {
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

                    MenuOverlay(
                        visible = isSizeConfigOpen,
                        backgroundColor = menuBackgroundColor,
                        onClose = { isSizeConfigOpen = false }
                    ) {
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

                    MenuOverlay(
                        visible = isFontSelectionOpen,
                        backgroundColor = menuBackgroundColor,
                        onClose = { isFontSelectionOpen = false }
                    ) {
                        FontSelectionMenu(
                            currentAppFont = currentAppFont,
                            onAppFontSelected = { scope.launch { themeManager.setAppFont(it) } },
                            onBack = { isFontSelectionOpen = false }
                        )
                    }

                    MenuOverlay(
                        visible = isEditConfigOpen && !isWallpaperCropOpen,
                        backgroundColor = menuBackgroundColor,
                        onClose = { isEditConfigOpen = false }
                    ) {
                        EditConfigMenu(
                            onOpenIconConfig = { isIconConfigOpen = true },
                            onChangeWallpaper = {
                                isEditConfigOpen = false
                                isWallpaperConfigOpen = false
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
                            isShakeGesturesEnabled = isShakeGesturesEnabled,
                            onShakeGesturesToggled = { enabled ->
                                scope.launch { themeManager.setShakeGesturesEnabled(enabled) }
                            },
                            isSmartSuggestionsEnabled = isSmartSuggestionsEnabled,
                            onSmartSuggestionsToggled = { enabled ->
                                scope.launch { themeManager.setSmartSuggestionsEnabled(enabled) }
                            },
                            onClearSearchHistory = {
                                scope.launch { searchSuggestionsManager.clearWebHistory() }
                                Toast.makeText(context, "Suchverlauf gelöscht", Toast.LENGTH_SHORT).show()
                            },
                            isHapticFeedbackEnabled = isHapticFeedbackEnabled,
                            onHapticFeedbackToggled = { enabled ->
                                scope.launch {
                                    if (Settings.System.canWrite(context)) {
                                        themeManager.setHapticFeedbackEnabled(enabled)
                                    } else {
                                        val writeSettingsIntent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                            data = ("package:" + context.packageName).toUri()
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(writeSettingsIntent)
                                        Toast.makeText(
                                            context,
                                            "Bitte erlauben Sie dem Launcher, Systemeinstellungen zu ändern",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                            onClose = { isEditConfigOpen = false }
                        )
                    }

                    MenuOverlay(
                        visible = isWallpaperConfigOpen,
                        backgroundColor = menuBackgroundColor,
                        onClose = { isWallpaperConfigOpen = false }
                    ) {
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

                    MenuOverlay(
                        visible = isIconConfigOpen,
                        backgroundColor = menuBackgroundColor,
                        onClose = { isIconConfigOpen = false }
                    ) {
                        IconConfigMenu(
                            apps = allApps,
                            customIcons = customIcons,
                            iconRules = autoIconRules,
                            onIconSelected = { pkg, iconName ->
                                scope.launch { iconManager.setCustomIcon(pkg, iconName) }
                            },
                            onAutoRuleSelected = { pkg, mode ->
                                scope.launch {
                                    val rule = mode?.let { AutoIconRule(mode = it, reason = "user_config") }
                                    iconManager.setAutoIconRule(pkg, rule)
                                    iconManager.invalidatePackage(pkg)
                                    refreshAppList(pkg)
                                }
                            },
                            onReanalyzeRequested = { pkg ->
                                scope.launch {
                                    iconManager.invalidatePackage(pkg)
                                    refreshAppList(pkg)
                                }
                            },
                            onClose = { isIconConfigOpen = false }
                        )
                    }

                    MenuOverlay(
                        visible = isInfoOpen,
                        backgroundColor = menuBackgroundColor,
                        onClose = { isInfoOpen = false }
                    ) {
                        InfoDialog(
                            customWallpaperUri = customWallpaperUri,
                            onClose = { isInfoOpen = false }
                        )
                    }

                    if (isSearchOpen && !isSearchLaunching) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        isSearchOpen = false
                                    }
                                }
                        ) {
                            BottomSearch(
                                apps = allApps,
                                onClose = { isSearchOpen = false },
                                onAppLaunch = { app, bounds ->
                                    val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                        ?: return@BottomSearch
                                    requestSearchLaunch(intent, bounds)
                                },
                                onWebLaunch = { intent, bounds, query ->
                                    requestSearchLaunch(intent, bounds, webQuery = query)
                                },
                                preferredImeWebLaunchBounds = homeSearchButtonBounds,
                                webHistory = searchHistory,
                                appUsageStats = appUsageStats,
                                smartSuggestionsEnabled = isSmartSuggestionsEnabled,
                                onRemoveHistorySuggestion = { queryToRemove ->
                                    scope.launch { searchSuggestionsManager.removeWebSearch(queryToRemove) }
                                }
                            )
                        }
                    }

                    LaunchAnimationOverlay(
                        bounds = activeLaunchBounds,
                        rootSize = rootSize,
                        background = activeLaunchBackground,
                        backgroundBrush = activeLaunchBackgroundBrush,
                        durationMillis = searchLaunchDurationMs.toInt(),
                        scrimColor = Color.Transparent
                    )

                    activeReturnAnimation?.let { animation ->
                        ReturnAnimationOverlay(
                            bounds = animation.bounds,
                            rootSize = rootSize,
                            background = currentTheme.menuSurfaceColor(isDarkTextEnabled),
                            backgroundBrush = returnOverlayBrush,
                            onFinished = {
                                Log.d(
                                    RETURN_TAG,
                                    "returnOverlayFinished launched=${animation.launchedPackageName} target=${animation.packageName}"
                                )
                                activeReturnAnimation = null
                            },
                            durationMillis = returnOverlayDurationMs.toInt(),
                            targetScale = if (animation.source == LaunchSource.DRAWER) 0.78f else 0.84f
                        )
                    }

                    if (isWallpaperCropOpen) {
                        pendingWallpaperUri?.let { selectedUri ->
                            WallpaperCropScreen(
                                sourceUri = selectedUri,
                                onCropFinished = { uri ->
                                    scope.launch {
                                        themeManager.setCustomWallpaperUri(uri.toString())
                                        isWallpaperCropOpen = false
                                        delay(280)
                                        pendingWallpaperUri = null
                                    }
                                },
                                onCancel = {
                                    isWallpaperCropOpen = false
                                    scope.launch {
                                        delay(280)
                                        pendingWallpaperUri = null
                                    }
                                },
                                homeScreenPreview = {
                                    HomeScreen(
                                        favorites = favorites,
                                        isSettingsOpen = false,
                                        isSearchOpen = false,
                                        isEditMode = false,
                                        favoritesOffsetX = 0f,
                                        favoritesOffsetY = 0f,
                                        clockOffsetY = 0f,
                                        onOpenDrawer = {},
                                        onOpenSearch = {},
                                        onToggleSettings = {},
                                        onToggleEditMode = {},
                                        onOpenFavoritesConfig = {},
                                        onOpenColorConfig = {},
                                        onOpenSizeConfig = {},
                                        onOpenSystemSettings = {},
                                        onOpenInfo = {},
                                        onSaveFavoritesOffset = { _, _ -> },
                                        onSaveClockOffset = { _ -> },
                                        onLaunchApp = { _, _, _ -> },
                                        returnIconPackage = null,
                                        searchButtonBounceToken = 0,
                                        onSearchButtonBoundsChanged = {},
                                        isPreview = true
                                    )
                                }
                            )
                        }
                    }

                    if (showUsageAccessPrompt) {
                        AlertDialog(
                            onDismissRequest = { showUsageAccessPrompt = false },
                            title = { Text("Nutzungszugriff aktivieren") },
                            text = {
                                Text(
                                    "Damit die Rückkehranimation nach dem Öffnen über Recents exakt zur richtigen App-Position zurückgeht, aktiviere bitte den Nutzungszugriff für den Launcher."
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    showUsageAccessPrompt = false
                                    ForegroundAppResolver.openUsageAccessSettings(context)
                                }) {
                                    Text("Öffnen")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showUsageAccessPrompt = false }) {
                                    Text("Später")
                                }
                            }
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

    private fun enforceExcludeFromRecents() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val tasks = am.appTasks
        if (!tasks.isNullOrEmpty()) {
            tasks[0].setExcludeFromRecents(true)
        }
    }

    private fun validateDefaultLauncher() {
        val resolvedPackage = resolveDefaultHomePackage() ?: return
        if (resolvedPackage == packageName) {
            defaultLauncherWarningShown = false
            lastDefaultLauncherPackage = resolvedPackage
            return
        }

        if (!defaultLauncherWarningShown || lastDefaultLauncherPackage != resolvedPackage) {
            Toast.makeText(this, getString(R.string.default_launcher_warning), Toast.LENGTH_LONG).show()
            defaultLauncherWarningShown = true
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

    private fun resolveDefaultHomePackage(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true) {
                return packageName
            }
        }

        val resolvedDefault = packageManager.resolveActivity(
            createHomeIntent(),
            PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.packageName
        if (resolvedDefault != null) {
            return resolvedDefault
        }

        val homeActivities = packageManager.queryIntentActivities(
            createHomeIntent(),
            PackageManager.MATCH_DEFAULT_ONLY
        )
        val uniqueHomePackages = homeActivities
            .mapNotNull { resolveInfo -> resolveInfo.activityInfo?.packageName }
            .distinct()

        return when {
            uniqueHomePackages.size == 1 -> uniqueHomePackages.first()
            packageName in uniqueHomePackages && uniqueHomePackages.none { it != packageName } -> packageName
            else -> null
        }
    }
}

/**
 * Ein Overlay für Menüs mit Ein-/Ausblend-Animation und optionalem Drag-to-close.
 */
@Composable
private fun MenuOverlay(
    visible: Boolean,
    backgroundColor: Color,
    enableDragToClose: Boolean = true,
    onClose: () -> Unit,
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
        var totalDragY by remember { mutableStateOf(0f) }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { } // Verhindert Klicks durch das Overlay hindurch
                }
                .then(
                    if (enableDragToClose) {
                        Modifier.pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (totalDragY > 300f) onClose()
                                    totalDragY = 0f
                                },
                                onDragCancel = { totalDragY = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragY += dragAmount
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .background(backgroundColor)
        ) {
            content()
        }
    }
}
