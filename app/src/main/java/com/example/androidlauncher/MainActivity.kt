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
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import com.example.androidlauncher.ui.theme.RethroneSprings
import kotlin.coroutines.cancellation.CancellationException
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.androidlauncher.data.AppFont
import com.example.androidlauncher.data.AppAccessMode
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.AppRepository
import com.example.androidlauncher.data.AutoIconRule
import com.example.androidlauncher.data.DesignStyle
import com.example.androidlauncher.data.FavoritesBorderStyle
import com.example.androidlauncher.data.FavoritesManager
import com.example.androidlauncher.data.FolderInfo
import com.example.androidlauncher.data.FolderManager
import com.example.androidlauncher.data.HomeLayout
import com.example.androidlauncher.data.FavoriteSpacing
import com.example.androidlauncher.data.FontSize
import com.example.androidlauncher.data.FontWeightLevel
import com.example.androidlauncher.data.IconManager
import com.example.androidlauncher.data.IconQualityEvaluator
import com.example.androidlauncher.data.IconSize
import com.example.androidlauncher.data.SearchSuggestionsManager
import com.example.androidlauncher.data.GestureAction
import com.example.androidlauncher.ui.expandNotifications
import com.example.androidlauncher.data.ThemeManager
import com.example.androidlauncher.ui.AppDrawer
import com.example.androidlauncher.ui.HybridSearch
import com.example.androidlauncher.ui.ColorConfigMenu
import com.example.androidlauncher.ui.AnimationsConfigMenu
import com.example.androidlauncher.ui.GesturesConfigMenu
import com.example.androidlauncher.ui.ThemeSelectionMenu
import com.example.androidlauncher.ui.DesignStyleMenu
import com.example.androidlauncher.ui.EditConfigMenu
import com.example.androidlauncher.ui.FavoritesConfigMenu
import com.example.androidlauncher.ui.FolderConfigMenu
import com.example.androidlauncher.ui.FontSelectionMenu
import com.example.androidlauncher.ui.HomeScreen
import com.example.androidlauncher.ui.IconConfigMenu
import com.example.androidlauncher.ui.InfoDialog
import com.example.androidlauncher.ui.UninstallAppsMenu
import com.example.androidlauncher.ui.HiddenAppsMenu
import com.example.androidlauncher.ui.LaunchAnimationOverlay
import com.example.androidlauncher.ui.NiagaraAppDrawer
import com.example.androidlauncher.ui.ReturnAnimationOverlay
import com.example.androidlauncher.ui.SizeConfigMenu
import com.example.androidlauncher.ui.SystemWallpaperView
import com.example.androidlauncher.ui.WallpaperConfigMenu
import com.example.androidlauncher.ui.WallpaperCropScreen
import com.example.androidlauncher.ui.launchAppNoTransition
import com.example.androidlauncher.ui.openDefaultLauncherSettings
import com.example.androidlauncher.ui.theme.AndroidLauncherTheme
import com.example.androidlauncher.ui.theme.ColorTheme
import com.example.androidlauncher.ui.theme.LocalAnimationSpeed
import com.example.androidlauncher.ui.theme.LocalAnimationsEnabled
import com.example.androidlauncher.ui.theme.LocalMenuAnimationEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

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

        // Hinweis: FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS nachträglich am erhaltenen Intent zu setzen, hat keine
        // Wirkung – das Flag zählt nur, wenn der Aufrufer es vor startActivity() setzt. Recents-Ausschluss
        // läuft über das Manifest (excludeFromRecents=true) + enforceExcludeFromRecents() unten.
        logHomeIntent(intent)

        // Material You: Wallpaper-Farben für das DYNAMIC-Theme laden + auf Wechsel lauschen.
        com.example.androidlauncher.data.DynamicColorHolder.register(applicationContext)

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
            val currentFavoriteSpacing by themeManager.selectedFavoriteSpacing.collectAsState(initial = FavoriteSpacing.STANDARD)
            val currentAppFont by themeManager.selectedAppFont.collectAsState(initial = AppFont.SYSTEM_DEFAULT)
            val isDarkTextEnabled by themeManager.isDarkTextEnabled.collectAsState(initial = false)
            val iconColor by themeManager.iconColor.collectAsState(initial = Color.White)
            val homeTextColor by themeManager.homeTextColor.collectAsState(initial = Color.White)
            val customBackgroundColor by themeManager.customBackgroundColor.collectAsState(initial = ColorTheme.FallbackCustomBackground)
            val customMenuColor by themeManager.customMenuColor.collectAsState(initial = ColorTheme.FallbackCustomMenu)
            // CUSTOM-Theme: gewählte Flächenfarben in den Holder spiegeln (treibt die Farb-Pipeline).
            LaunchedEffect(customBackgroundColor, customMenuColor) {
                com.example.androidlauncher.data.CustomColorHolder.set(customBackgroundColor, customMenuColor)
            }
            val showFavoriteLabels by themeManager.showFavoriteLabels.collectAsState(initial = false)
            val hiddenApps by themeManager.hiddenApps.collectAsState(initial = emptySet())
            val designStyle by themeManager.designStyle.collectAsState(initial = DesignStyle.GLASS)
            val favoritesBorderStyle by themeManager.favoritesBorderStyle.collectAsState(initial = FavoritesBorderStyle.NONE)
            val isShakeGesturesEnabled by themeManager.isShakeGesturesEnabled.collectAsState(initial = true)
            val doubleShakeAction by themeManager.doubleShakeAction.collectAsState(initial = GestureAction.FLASHLIGHT)
            val shakeOpenAppPackage by themeManager.shakeOpenAppPackage.collectAsState(initial = null)
            val doubleTapAction by themeManager.doubleTapAction.collectAsState(initial = GestureAction.LOCK_SCREEN)
            val doubleTapAppPackage by themeManager.doubleTapAppPackage.collectAsState(initial = null)
            val isSmartSuggestionsEnabled by themeManager.isSmartSuggestionsEnabled.collectAsState(initial = true)
            val isHapticFeedbackEnabled by themeManager.isHapticFeedbackEnabled.collectAsState(initial = true)
            val isAnimationsEnabled by themeManager.isAnimationsEnabled.collectAsState(initial = true)
            val isAppOpenAnimationEnabled by themeManager.isAppOpenAnimationEnabled.collectAsState(initial = true)
            val isAppCloseAnimationEnabled by themeManager.isAppCloseAnimationEnabled.collectAsState(initial = true)
            val isMenuAnimationEnabled by themeManager.isMenuAnimationEnabled.collectAsState(initial = true)
            val isFavoritesAnimationEnabled by themeManager.isFavoritesAnimationEnabled.collectAsState(initial = true)
            val animationSpeed by themeManager.animationSpeed.collectAsState(initial = 1f)
            // Master UND Einzel: Animation läuft nur, wenn beide aktiv sind.
            val appOpenAnimActive = isAnimationsEnabled && isAppOpenAnimationEnabled
            val appCloseAnimActive = isAnimationsEnabled && isAppCloseAnimationEnabled
            // Frischer Zugriff aus dem (einmalig erstellten) Lifecycle-Observer.
            val appCloseAnimActiveRef = rememberUpdatedState(appCloseAnimActive)
            val isWeatherWidgetEnabled by themeManager.isWeatherWidgetEnabled.collectAsState(initial = true)
            val isClockWidgetEnabled by themeManager.isClockWidgetEnabled.collectAsState(initial = true)
            val isCalendarWidgetEnabled by themeManager.isCalendarWidgetEnabled.collectAsState(initial = true)
            val appAccessMode by themeManager.appAccessMode.collectAsState(initial = AppAccessMode.DRAWER_LIST)

            val customWallpaperUri by themeManager.customWallpaperUri.collectAsState(initial = null)
            val wallpaperBlur by themeManager.wallpaperBlur.collectAsState(initial = 0f)
            val wallpaperDim by themeManager.wallpaperDim.collectAsState(initial = 0.1f)
            val wallpaperZoom by themeManager.wallpaperZoom.collectAsState(initial = 1.0f)
            val searchHistory by searchSuggestionsManager.webHistory.collectAsState(initial = emptyList())
            val appUsageStats by searchSuggestionsManager.appUsageStats.collectAsState(initial = emptyMap())

            // Positionen der unabhängig verschiebbaren Startbildschirm-Elemente.
            val homeLayout by themeManager.homeLayout.collectAsState(initial = HomeLayout())

            val folders by folderManager.folders.collectAsState(initial = emptyList())
            val customIcons by iconManager.customIcons.collectAsState(initial = emptyMap())
            val autoIconFallbacks by iconManager.autoIconFallbacks.collectAsState(initial = emptyMap())
            val autoIconRules by iconManager.autoIconRules.collectAsState(initial = emptyMap())
            val favoritePackages by favoritesManager.favorites.collectAsState(initial = emptyList())

            var isWallpaperCropOpen by remember { mutableStateOf(false) }
            var pendingWallpaperUri by remember { mutableStateOf<Uri?>(null) }
            var showUsageAccessPrompt by remember { mutableStateOf(false) }
            var hasShownUsageAccessPrompt by remember { mutableStateOf(false) }
            var pendingPermissionShakeAction by remember {
                mutableStateOf<GestureAction?>(null)
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
                val pendingAction = pendingPermissionShakeAction
                pendingPermissionShakeAction = null

                if (pendingAction != GestureAction.FLASHLIGHT) {
                    return@rememberLauncherForActivityResult
                }

                if (!isGranted) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.flashlight_permission_required),
                        Toast.LENGTH_LONG
                    ).show()
                    return@rememberLauncherForActivityResult
                }

                when (launcherDeviceActions.toggleFlashlight()) {
                    is FlashlightToggleResult.Success -> {
                        launcherDeviceActions.vibrateGestureFeedback(this@MainActivity)
                    }
                    FlashlightToggleResult.Unsupported -> {
                        Toast.makeText(context, context.getString(R.string.flashlight_unsupported), Toast.LENGTH_SHORT).show()
                    }
                    FlashlightToggleResult.MissingPermission -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.flashlight_permission_required),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    FlashlightToggleResult.Error -> {
                        Toast.makeText(context, context.getString(R.string.flashlight_error), Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Rollen-Dialog (ROLE_HOME) zuverlässig über die ActivityResult-API; der Status
            // ("An"/"Aus") aktualisiert sich ohnehin beim nächsten ON_RESUME.
            val defaultLauncherRoleLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { /* Ergebnis egal – Status wird bei ON_RESUME neu gelesen */ }

            fun requestDefaultLauncher() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = getSystemService(RoleManager::class.java)
                    if (roleManager != null &&
                        roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                        !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
                    ) {
                        runCatching {
                            defaultLauncherRoleLauncher.launch(
                                roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                            )
                        }.onFailure { openDefaultLauncherSettings(context) }
                        return
                    }
                }
                // Bereits Standard, pre-Q oder Rolle nicht verfügbar: System-Einstellungen öffnen.
                openDefaultLauncherSettings(context)
            }

            // Geräte-/System-Aktionen einer Geste. Launcher-interne Aktionen
            // (App-Drawer/Suche/Benachrichtigungen) werden im inneren
            // dispatchGestureAction behandelt, da sie UI-State setzen.
            fun runGestureAction(action: GestureAction, appPackage: String?) {
                when (action) {
                    GestureAction.NONE,
                    GestureAction.APP_DRAWER,
                    GestureAction.SEARCH,
                    GestureAction.NOTIFICATIONS -> Unit
                    GestureAction.FLASHLIGHT -> {
                        when (launcherDeviceActions.toggleFlashlight()) {
                            is FlashlightToggleResult.Success -> {
                                launcherDeviceActions.vibrateGestureFeedback(this@MainActivity)
                            }
                            FlashlightToggleResult.Unsupported -> {
                                Toast.makeText(context, context.getString(R.string.flashlight_unsupported), Toast.LENGTH_SHORT).show()
                            }
                            FlashlightToggleResult.MissingPermission -> {
                                pendingPermissionShakeAction = GestureAction.FLASHLIGHT
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                            FlashlightToggleResult.Error -> {
                                Toast.makeText(context, context.getString(R.string.flashlight_error), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    GestureAction.CAMERA -> {
                        val didOpenCamera = launcherDeviceActions.openCamera(this@MainActivity)
                        if (didOpenCamera) {
                            launcherDeviceActions.vibrateGestureFeedback(this@MainActivity)
                        } else {
                            Toast.makeText(context, context.getString(R.string.camera_app_not_found), Toast.LENGTH_SHORT).show()
                        }
                    }
                    GestureAction.OPEN_APP -> {
                        val targetPackage = appPackage
                        if (targetPackage.isNullOrBlank()) {
                            Toast.makeText(context, context.getString(R.string.shake_no_app_selected), Toast.LENGTH_SHORT).show()
                        } else if (launcherDeviceActions.openApp(this@MainActivity, targetPackage)) {
                            launcherDeviceActions.vibrateGestureFeedback(this@MainActivity)
                        } else {
                            Toast.makeText(context, context.getString(R.string.shake_app_not_found), Toast.LENGTH_SHORT).show()
                        }
                    }
                    GestureAction.LOCK_SCREEN -> {
                        if (LauncherAccessibilityService.isAccessibilityServiceEnabled(context)) {
                            LauncherAccessibilityService.requestLockScreen(context)
                            launcherDeviceActions.vibrateGestureFeedback(this@MainActivity)
                        } else {
                            Toast.makeText(context, context.getString(R.string.shake_lock_needs_accessibility), Toast.LENGTH_LONG).show()
                            runCatching {
                                startActivity(
                                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    }
                    GestureAction.TOGGLE_DND -> {
                        when (launcherDeviceActions.toggleDoNotDisturb()) {
                            is DndToggleResult.Success -> {
                                launcherDeviceActions.vibrateGestureFeedback(this@MainActivity)
                            }
                            DndToggleResult.MissingPermission -> {
                                Toast.makeText(context, context.getString(R.string.shake_dnd_needs_permission), Toast.LENGTH_LONG).show()
                                launcherDeviceActions.openDndPolicySettings(this@MainActivity)
                            }
                            DndToggleResult.Unsupported -> {
                                Toast.makeText(context, context.getString(R.string.shake_dnd_unsupported), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    GestureAction.OPEN_SETTINGS -> {
                        if (launcherDeviceActions.openSystemSettings(this@MainActivity)) {
                            launcherDeviceActions.vibrateGestureFeedback(this@MainActivity)
                        } else {
                            Toast.makeText(context, context.getString(R.string.shake_settings_not_found), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            AndroidLauncherTheme(
                colorTheme = currentTheme,
                fontSize = currentFontSize,
                fontWeight = currentFontWeight,
                iconSize = currentIconSize,
                favoriteSpacing = currentFavoriteSpacing,
                darkTextEnabled = isDarkTextEnabled,
                iconColor = iconColor,
                homeTextColor = homeTextColor,
                showFavoriteLabels = showFavoriteLabels,
                designStyle = designStyle,
                favoritesBorderStyle = favoritesBorderStyle,
                appFont = currentAppFont,
                hapticFeedbackEnabled = isHapticFeedbackEnabled,
                animationsEnabled = isAnimationsEnabled,
                appOpenAnimationEnabled = isAppOpenAnimationEnabled,
                appCloseAnimationEnabled = isAppCloseAnimationEnabled,
                menuAnimationEnabled = isMenuAnimationEnabled,
                favoritesAnimationEnabled = isFavoritesAnimationEnabled,
                animationSpeed = animationSpeed,
                weatherWidgetEnabled = isWeatherWidgetEnabled,
                clockWidgetEnabled = isClockWidgetEnabled,
                calendarWidgetEnabled = isCalendarWidgetEnabled
            ) {
                // Determine whether to use dark or light text/colors dynamically
                val lifecycleOwner = LocalLifecycleOwner.current
                val menuBackgroundColor = currentTheme.menuSurfaceColor(isDarkTextEnabled)
                val searchLaunchOverlayColor = Color.Black
                val launchOverlayBrush: Brush? = null
                val returnOverlayBrush: Brush? = null

                var rootSize by remember { mutableStateOf(IntSize.Zero) }
                var pendingReturnAnimation by remember { mutableStateOf<ReturnAnimation?>(null) }
                var pendingReturnAnimationStartedWallClockMs by remember { mutableStateOf(0L) }
                var activeReturnAnimation by remember { mutableStateOf<ReturnAnimation?>(null) }
                var returnIconPackage by remember { mutableStateOf<String?>(null) }
                var searchButtonBounceToken by remember { mutableStateOf(0) }
                // Eigener Trigger für den Rückkehr-Bounce, entkoppelt von
                // activeReturnAnimation. Letzteres wird vom Schließen-Overlay nach
                // ~260ms auf null gesetzt; würde der Bounce darauf gekeyt sein, würde
                // die delay(270)-Coroutine ~10ms vor dem Feuern abgebrochen -> der
                // Bounce käme nur „manchmal". Das Token steigt nur bei einer neuen
                // Aktivierung, sodass die Coroutine zuverlässig zu Ende läuft.
                var returnBounceToken by remember { mutableStateOf(0) }
                var returnBounceTargetPackage by remember { mutableStateOf<String?>(null) }
                // Beim Öffnen poppt das gedrückte Icon kurz, bevor das Panel es
                // verdeckt – symmetrisch zum Rückkehr-Bounce.
                var launchIconPackage by remember { mutableStateOf<String?>(null) }
                val returnOverlayDurationMs = (260L / animationSpeed).toLong()
                // Bounce erst nach Abschluss des Schließen-Panels (260ms), damit er nicht
                // gegen das noch schrumpfende Panel läuft.
                val returnBounceDelayMs = (270L / animationSpeed).toLong()
                var isDrawerOpen by remember { mutableStateOf(false) }
                var isSettingsOpen by remember { mutableStateOf(false) }
                var isSearchOpen by remember { mutableStateOf(false) }
                var isSearchClosingState by remember { mutableStateOf(false) }
                var isHomeEditMode by remember { mutableStateOf(false) }

                // Zentraler Geste-Dispatch: Launcher-interne Aktionen setzen hier den
                // UI-State, alles andere geht an runGestureAction (Geräte-/System-Aktionen).
                fun dispatchGestureAction(action: GestureAction, appPackage: String?) {
                    when (action) {
                        GestureAction.APP_DRAWER -> isDrawerOpen = true
                        GestureAction.SEARCH -> {
                            isSearchClosingState = false
                            isSearchOpen = true
                        }
                        GestureAction.NOTIFICATIONS -> expandNotifications(context)
                        else -> runGestureAction(action, appPackage)
                    }
                }
                shakeManager.onDoubleShake = { dispatchGestureAction(doubleShakeAction, shakeOpenAppPackage) }
                var homeSearchButtonBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
                var isSearchLaunching by remember { mutableStateOf(false) }
                var isAppLaunchAnimating by remember { mutableStateOf(false) }
                var activeLaunchBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
                var activeLaunchBackground by remember { mutableStateOf(searchLaunchOverlayColor) }
                var activeLaunchBackgroundBrush by remember { mutableStateOf<Brush?>(launchOverlayBrush) }
                // Treibt das leichte Zurücktreten (Skalieren/Abdunkeln) des Homescreen-/Drawer-Inhalts,
                // damit das Start-/Rückkehr-Panel nicht als lose Schicht über eingefrorenem Inhalt wirkt.
                val contentRevealProgress = remember { Animatable(0f) }
                val searchLaunchDurationMs = (260L / animationSpeed).toLong()
                val searchLaunchSettleAfterStartMs = (30L / animationSpeed).toLong()
                // Kurzer Vorlauf, in dem nur das gedrückte Icon poppt, bevor das
                // wachsende Panel es verdeckt.
                val launchBounceLeadMs = (120L / animationSpeed).toLong()
                var isFavoritesConfigOpen by remember { mutableStateOf(false) }
                var isColorConfigOpen by remember { mutableStateOf(false) }
                var isAnimationsConfigOpen by remember { mutableStateOf(false) }
                var isGesturesConfigOpen by remember { mutableStateOf(false) }
                var isDesignMenuOpen by remember { mutableStateOf(false) }
                var isThemeMenuOpen by remember { mutableStateOf(false) }
                var isSizeConfigOpen by remember { mutableStateOf(false) }
                var isFontSelectionOpen by remember { mutableStateOf(false) }
                var isEditConfigOpen by remember { mutableStateOf(false) }
                var isIconConfigOpen by remember { mutableStateOf(false) }
                var isUninstallAppsOpen by remember { mutableStateOf(false) }
                var isHiddenAppsOpen by remember { mutableStateOf(false) }
                var isWallpaperConfigOpen by remember { mutableStateOf(false) }
                var isInfoOpen by remember { mutableStateOf(false) }
                var selectedFolderForConfig by remember { mutableStateOf<FolderInfo?>(null) }
                var selectedFolderForConfigSnapshot by remember { mutableStateOf<FolderInfo?>(null) }
                val folderConfigExitHoldMs = 320L
                var isLauncherResumed by remember { mutableStateOf(false) }
                var returnResumeGuardState by remember {
                    mutableStateOf(ReturnResumeGuardState())
                }

                LaunchedEffect(selectedFolderForConfig) {
                    val selected = selectedFolderForConfig
                    if (selected != null) {
                        selectedFolderForConfigSnapshot = selected
                    } else {
                        // Keep content alive briefly so AnimatedVisibility can render the exit smoothly.
                        delay(folderConfigExitHoldMs)
                        selectedFolderForConfigSnapshot = null
                    }
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
                                storedAnimations = ReturnOriginStore.getAll(context),
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
                                if (appCloseAnimActiveRef.value) {
                                    activeReturnAnimation = animation
                                    returnIconPackage = null
                                    returnBounceTargetPackage = animation.packageName
                                    returnBounceToken += 1
                                } else {
                                    // Rückkehr-Animation deaktiviert: kein Schrumpfen/Bounce,
                                    // nur Zustand aufräumen.
                                    activeReturnAnimation = null
                                }
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

                // Inhalt synchron zum schrumpfenden Rückkehr-Panel zurückholen.
                LaunchedEffect(activeReturnAnimation) {
                    if (activeReturnAnimation != null && appCloseAnimActive) {
                        contentRevealProgress.snapTo(1f)
                        contentRevealProgress.animateTo(
                            0f,
                            tween(returnOverlayDurationMs.toInt(), easing = FastOutSlowInEasing)
                        )
                    }
                }

                LaunchedEffect(returnBounceToken) {
                    if (returnBounceToken == 0) return@LaunchedEffect
                    val packageName = returnBounceTargetPackage ?: return@LaunchedEffect
                    delay(returnBounceDelayMs)
                    if (packageName == SEARCH_RETURN_TARGET) {
                        Log.d(RETURN_TAG, "bounce searchButton target=$packageName")
                        searchButtonBounceToken += 1
                    } else {
                        Log.d(RETURN_TAG, "bounce icon target=$packageName")
                        returnIconPackage = packageName
                    }
                }

                LaunchedEffect(returnIconPackage) {
                    if (returnIconPackage != null) {
                        delay(300)
                        returnIconPackage = null
                    }
                }

                // Gemeinsamer Bounce-Auslöser für Icons: Öffnen (launchIconPackage)
                // und Zurückkehren (returnIconPackage) sind zeitlich getrennt, daher
                // ist höchstens einer gesetzt.
                val bounceIconPackage = returnIconPackage ?: launchIconPackage

                val allApps = remember { mutableStateListOf<AppInfo>() }
                // Anzeige-Liste ohne ausgeblendete Apps (Drawer/Startseiten-Liste/Suche/Favoriten).
                // Das Ausblenden-Menü selbst nutzt weiterhin die volle `allApps`-Liste.
                val visibleApps = remember(allApps.toList(), hiddenApps) {
                    if (hiddenApps.isEmpty()) allApps.toList()
                    else allApps.filterNot { it.packageName in hiddenApps }
                }
                val favorites = remember(visibleApps, favoritePackages) {
                    LauncherLogic.getFavoriteApps(visibleApps, favoritePackages)
                }

                fun refreshAppList(targetPackageName: String? = null) {
                    scope.launch {
                        appRepository.cleanupLegacyCache()
                        appRepository.invalidateCacheOnAppUpdate()
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
                    // Letzte-Instanz-Bounds: Falls weder Launch- noch Return-Bounds
                    // gemessen wurden (z. B. onGloballyPositioned hat noch nicht
                    // gefeuert), nutzen wir einen kleinen zentrierten Rect, damit die
                    // Overlays nie still ausfallen.
                    val centerFallbackRect: androidx.compose.ui.geometry.Rect? =
                        if (rootSize.width > 0 && rootSize.height > 0) {
                            val sizePx = 48f * context.resources.displayMetrics.density
                            val cx = rootSize.width / 2f
                            val cy = rootSize.height / 2f
                            androidx.compose.ui.geometry.Rect(
                                cx - sizePx / 2f, cy - sizePx / 2f,
                                cx + sizePx / 2f, cy + sizePx / 2f
                            )
                        } else null
                    val effectiveReturnBounds = returnBounds ?: bounds ?: centerFallbackRect
                    val returnAnimation = ReturnAnimation(
                        bounds = effectiveReturnBounds,
                        source = source,
                        packageName = returnPackageName,
                        launchedPackageName = packageName,
                        launchedAtMs = System.currentTimeMillis()
                    )
                    Log.d(
                        RETURN_TAG,
                        "saveReturn launched=$packageName target=$returnPackageName source=$source launchBounds=${bounds != null} returnBounds=${returnBounds != null}"
                    )
                    pendingReturnAnimation = returnAnimation
                    pendingReturnAnimationStartedWallClockMs = System.currentTimeMillis()
                    ReturnOriginStore.save(context, packageName, returnAnimation)
                    isAppLaunchAnimating = true
                    
                    if (appOpenAnimActive) {
                        activeLaunchBackground = overlayColor
                        activeLaunchBackgroundBrush = overlayBrush
                    }

                    scope.launch {
                        try {
                            if (appOpenAnimActive) {
                                // 1) Gedrücktes Icon poppt zuerst, damit sichtbar ist,
                                //    was getippt wurde (Bounce auch beim Öffnen).
                                launchIconPackage = returnPackageName
                                delay(launchBounceLeadMs)
                                launchIconPackage = null
                                // 2) Container-Transform: Panel wächst aus dem Icon heraus.
                                activeLaunchBounds = bounds ?: centerFallbackRect
                                launch {
                                    contentRevealProgress.snapTo(0f)
                                    contentRevealProgress.animateTo(
                                        1f,
                                        tween(searchLaunchDurationMs.toInt(), easing = FastOutSlowInEasing)
                                    )
                                }
                                delay(searchLaunchDurationMs)
                            }
                            launchAppNoTransition(context, Intent(intent))
                            if (trackAppLaunch && isSmartSuggestionsEnabled) {
                                searchSuggestionsManager.recordAppLaunch(packageName)
                            }
                            if (appOpenAnimActive) {
                                delay(searchLaunchSettleAfterStartMs)
                            }
                        } finally {
                            launchIconPackage = null
                            activeLaunchBounds = null
                            isAppLaunchAnimating = false
                            // Hinter dem Vollbild-Panel/der App unsichtbar zurücksetzen.
                            contentRevealProgress.snapTo(0f)
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
                        isUninstallAppsOpen = false
                        isHiddenAppsOpen = false
                        isWallpaperConfigOpen = false
                        isInfoOpen = false
                        isHomeEditMode = false
                        selectedFolderForConfig = null
                    }
                }

                LaunchedEffect(
                    isDrawerOpen, isFavoritesConfigOpen, isColorConfigOpen, isAnimationsConfigOpen, isGesturesConfigOpen, isDesignMenuOpen, isThemeMenuOpen,
                    isSizeConfigOpen, isFontSelectionOpen, isEditConfigOpen,
                    isIconConfigOpen, isUninstallAppsOpen, isHiddenAppsOpen, isWallpaperConfigOpen, isInfoOpen,
                    selectedFolderForConfig, isSearchOpen, isHomeEditMode
                ) {
                    val anyModalOpen = isDrawerOpen || isFavoritesConfigOpen ||
                        isColorConfigOpen || isAnimationsConfigOpen || isGesturesConfigOpen || isDesignMenuOpen || isThemeMenuOpen || isSizeConfigOpen || isFontSelectionOpen ||
                        isEditConfigOpen || isIconConfigOpen || isUninstallAppsOpen || isHiddenAppsOpen || isWallpaperConfigOpen ||
                        isInfoOpen || selectedFolderForConfig != null || isSearchOpen || isHomeEditMode
                    backCallback.isEnabled = !anyModalOpen
                }

                BackHandler(
                    enabled = isDrawerOpen || isFavoritesConfigOpen || isColorConfigOpen || isAnimationsConfigOpen || isGesturesConfigOpen || isDesignMenuOpen || isThemeMenuOpen ||
                        isSizeConfigOpen || isFontSelectionOpen || isEditConfigOpen ||
                        isIconConfigOpen || isUninstallAppsOpen || isHiddenAppsOpen || isWallpaperConfigOpen || isInfoOpen ||
                        selectedFolderForConfig != null || isSearchOpen || isHomeEditMode
                ) {
                    when {
                        selectedFolderForConfig != null -> selectedFolderForConfig = null
                        isHomeEditMode -> isHomeEditMode = false
                        isSearchOpen -> isSearchOpen = false
                        isFontSelectionOpen -> isFontSelectionOpen = false
                        isWallpaperConfigOpen -> isWallpaperConfigOpen = false
                        isUninstallAppsOpen -> isUninstallAppsOpen = false
                        isHiddenAppsOpen -> isHiddenAppsOpen = false
                        isIconConfigOpen -> isIconConfigOpen = false
                        isDrawerOpen -> isDrawerOpen = false
                        isFavoritesConfigOpen -> isFavoritesConfigOpen = false
                        isThemeMenuOpen -> isThemeMenuOpen = false
                        isDesignMenuOpen -> isDesignMenuOpen = false
                        isAnimationsConfigOpen -> isAnimationsConfigOpen = false
                        isGesturesConfigOpen -> isGesturesConfigOpen = false
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

                    val animationsEnabled = LocalAnimationsEnabled.current
                    val slideTweenDuration = if (animationsEnabled) (300 / animationSpeed).roundToInt() else 0
                    val fadeTweenDuration = if (animationsEnabled) (200 / animationSpeed).roundToInt() else 0

                    AnimatedContent(
                        modifier = Modifier.graphicsLayer {
                            val p = contentRevealProgress.value
                            scaleX = 1f - 0.06f * p
                            scaleY = 1f - 0.06f * p
                            alpha = 1f - 0.25f * p
                        },
                        targetState = isDrawerOpen,
                        transitionSpec = {
                            if (targetState) {
                                (
                                    slideInVertically(
                                        initialOffsetY = { it },
                                        animationSpec = tween(slideTweenDuration, easing = EaseOutCubic)
                                    ) + fadeIn(animationSpec = tween(fadeTweenDuration))
                                ).togetherWith(fadeOut(animationSpec = tween(fadeTweenDuration)))
                            } else {
                                fadeIn(animationSpec = tween(fadeTweenDuration)).togetherWith(
                                    slideOutVertically(
                                        targetOffsetY = { it },
                                        animationSpec = tween(slideTweenDuration, easing = EaseInCubic)
                                    ) + fadeOut(animationSpec = tween(fadeTweenDuration))
                                )
                            }
                        },
                        label = "DrawerTransition"
                    ) { targetIsDrawerOpen ->
                        if (targetIsDrawerOpen) {
                            when (appAccessMode) {
                                AppAccessMode.DRAWER_LIST -> NiagaraAppDrawer(
                                    apps = visibleApps,
                                    onToggleFavorite = { pkg ->
                                        val newFavs = LauncherLogic.toggleFavorite(favoritePackages, pkg)
                                        if (newFavs != favoritePackages) {
                                            scope.launch { favoritesManager.saveFavorites(newFavs) }
                                        }
                                    },
                                    isFavorite = { pkg -> pkg in favoritePackages },
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
                                    returnIconPackage = bounceIconPackage
                                )
                                // DRAWER_GRID (und HOME_LIST als harmloser Fallback – im HOME_LIST-Modus
                                // wird der Drawer nie geöffnet).
                                else -> AppDrawer(
                                    apps = visibleApps,
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
                                    returnIconPackage = bounceIconPackage
                                )
                            }
                        } else {
                            HomeScreen(
                                favorites = favorites,
                                allApps = visibleApps,
                                appAccessMode = appAccessMode,
                                isSettingsOpen = isSettingsOpen,
                                isSearchOpen = isSearchOpen && !isSearchClosingState,
                                isEditMode = isHomeEditMode,
                                homeLayout = homeLayout,
                                onOpenDrawer = { isDrawerOpen = true },
                                onOpenSearch = {
                                    isSearchClosingState = false
                                    isSearchOpen = true
                                },
                                doubleTapAction = doubleTapAction,
                                doubleTapAppPackage = doubleTapAppPackage,
                                onGestureAction = { action, pkg -> dispatchGestureAction(action, pkg) },
                                onToggleSettings = { isSettingsOpen = !isSettingsOpen },
                                onToggleEditMode = { isHomeEditMode = !isHomeEditMode },
                                onOpenFavoritesConfig = { isFavoritesConfigOpen = true },
                                onOpenColorConfig = { isColorConfigOpen = true },
                                onOpenSizeConfig = { isSizeConfigOpen = true },
                                onOpenSystemSettings = { isEditConfigOpen = true },
                                onOpenInfo = { isInfoOpen = true },
                                onSaveHomeLayout = { layout ->
                                    scope.launch { themeManager.setHomeLayout(layout) }
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
                                returnIconPackage = bounceIconPackage,
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
                            favoritesBorderStyle = favoritesBorderStyle,
                            onBorderStyleSelected = { style ->
                                scope.launch { themeManager.setFavoritesBorderStyle(style) }
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
                        val folderForConfig = selectedFolderForConfig ?: selectedFolderForConfigSnapshot
                        folderForConfig?.let { folder ->
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
                            isDarkTextEnabled = isDarkTextEnabled,
                            iconColor = iconColor,
                            onIconColorChange = { scope.launch { themeManager.setIconColor(it) } },
                            homeTextColor = homeTextColor,
                            onHomeTextColorChange = { scope.launch { themeManager.setHomeTextColor(it) } },
                            designStyle = designStyle,
                            onOpenDesignMenu = { isDesignMenuOpen = true },
                            onOpenThemeMenu = { isThemeMenuOpen = true },
                            customBackgroundColor = customBackgroundColor,
                            onCustomBackgroundChange = { scope.launch { themeManager.setCustomBackgroundColor(it) } },
                            customMenuColor = customMenuColor,
                            onCustomMenuChange = { scope.launch { themeManager.setCustomMenuColor(it) } },
                            customWallpaperUri = customWallpaperUri,
                            onClose = { isColorConfigOpen = false }
                        )
                    }

                    MenuOverlay(
                        visible = isThemeMenuOpen,
                        backgroundColor = menuBackgroundColor,
                        onClose = { isThemeMenuOpen = false }
                    ) {
                        ThemeSelectionMenu(
                            selectedTheme = currentTheme,
                            onThemeSelected = { scope.launch { themeManager.setTheme(it) } },
                            isDarkTextEnabled = isDarkTextEnabled,
                            designStyle = designStyle,
                            customWallpaperUri = customWallpaperUri,
                            onClose = { isThemeMenuOpen = false }
                        )
                    }

                    MenuOverlay(
                        visible = isDesignMenuOpen,
                        backgroundColor = menuBackgroundColor,
                        onClose = { isDesignMenuOpen = false }
                    ) {
                        DesignStyleMenu(
                            currentStyle = designStyle,
                            selectedTheme = currentTheme,
                            isDarkTextEnabled = isDarkTextEnabled,
                            onStyleSelected = { style ->
                                scope.launch { themeManager.setDesignStyle(style) }
                                isDesignMenuOpen = false
                            },
                            onClose = { isDesignMenuOpen = false }
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
                            currentFavoriteSpacing = currentFavoriteSpacing,
                            onFavoriteSpacingSelected = { scope.launch { themeManager.setFavoriteSpacing(it) } },
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
                            onOpenHomeLayoutEdit = {
                                isEditConfigOpen = false
                                isHomeEditMode = true
                            },
                            onResetHomeLayout = {
                                scope.launch {
                                    themeManager.setHomeLayout(HomeLayout())
                                }
                                Toast.makeText(context, context.getString(R.string.home_layout_reset), Toast.LENGTH_SHORT).show()
                            },
                            onOpenIconConfig = { isIconConfigOpen = true },
                            onOpenUninstallApps = { isUninstallAppsOpen = true },
                            onOpenHiddenApps = { isHiddenAppsOpen = true },
                            onOpenDefaultLauncher = { requestDefaultLauncher() },
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
                                Toast.makeText(context, context.getString(R.string.wallpaper_removed), Toast.LENGTH_SHORT).show()
                            },
                            onOpenWallpaperAdjust = { isWallpaperConfigOpen = true },
                            isCustomHomeLayoutSet =
                                abs(homeLayout.clock.x) > 0.5f || abs(homeLayout.clock.y) > 0.5f ||
                                abs(homeLayout.date.x) > 0.5f || abs(homeLayout.date.y) > 0.5f ||
                                abs(homeLayout.weather.x) > 0.5f || abs(homeLayout.weather.y) > 0.5f ||
                                abs(homeLayout.favorites.x) > 0.5f || abs(homeLayout.favorites.y) > 0.5f,
                            isCustomWallpaperSet = customWallpaperUri != null,
                            onOpenGesturesConfig = { isGesturesConfigOpen = true },
                            isSmartSuggestionsEnabled = isSmartSuggestionsEnabled,
                            onSmartSuggestionsToggled = { enabled ->
                                scope.launch { themeManager.setSmartSuggestionsEnabled(enabled) }
                            },
                            isAnimationsEnabled = isAnimationsEnabled,
                            onAnimationsToggled = { enabled ->
                                scope.launch { themeManager.setAnimationsEnabled(enabled) }
                            },
                            onOpenAnimationsConfig = { isAnimationsConfigOpen = true },
                            isWeatherWidgetEnabled = isWeatherWidgetEnabled,
                            onWeatherWidgetToggled = { enabled ->
                                scope.launch { themeManager.setWeatherWidgetEnabled(enabled) }
                            },
                            isClockWidgetEnabled = isClockWidgetEnabled,
                            onClockWidgetToggled = { enabled ->
                                scope.launch { themeManager.setClockWidgetEnabled(enabled) }
                            },
                            isCalendarWidgetEnabled = isCalendarWidgetEnabled,
                            onCalendarWidgetToggled = { enabled ->
                                scope.launch { themeManager.setCalendarWidgetEnabled(enabled) }
                            },
                            appAccessMode = appAccessMode,
                            onAppAccessModeChange = { mode ->
                                scope.launch { themeManager.setAppAccessMode(mode) }
                            },
                            onClearSearchHistory = {
                                scope.launch { searchSuggestionsManager.clearWebHistory() }
                                Toast.makeText(context, context.getString(R.string.search_history_cleared), Toast.LENGTH_SHORT).show()
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
                                            context.getString(R.string.write_settings_permission_required),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                            onClose = { isEditConfigOpen = false }
                        )
                    }

                    // Untermenü der Animationen – nach dem Edit-Menü komponiert, damit es
                    // darüber liegt (das Edit-Menü bleibt geöffnet im Hintergrund).
                    MenuOverlay(
                        visible = isAnimationsConfigOpen,
                        backgroundColor = menuBackgroundColor,
                        onClose = { isAnimationsConfigOpen = false }
                    ) {
                        AnimationsConfigMenu(
                            isAnimationsEnabled = isAnimationsEnabled,
                            onAnimationsToggled = { scope.launch { themeManager.setAnimationsEnabled(it) } },
                            isAppOpenAnimationEnabled = isAppOpenAnimationEnabled,
                            onAppOpenAnimationToggled = { scope.launch { themeManager.setAppOpenAnimationEnabled(it) } },
                            isAppCloseAnimationEnabled = isAppCloseAnimationEnabled,
                            onAppCloseAnimationToggled = { scope.launch { themeManager.setAppCloseAnimationEnabled(it) } },
                            isMenuAnimationEnabled = isMenuAnimationEnabled,
                            onMenuAnimationToggled = { scope.launch { themeManager.setMenuAnimationEnabled(it) } },
                            isFavoritesAnimationEnabled = isFavoritesAnimationEnabled,
                            onFavoritesAnimationToggled = { scope.launch { themeManager.setFavoritesAnimationEnabled(it) } },
                            animationSpeed = animationSpeed,
                            onAnimationSpeedChanged = { scope.launch { themeManager.setAnimationSpeed(it) } },
                            onClose = { isAnimationsConfigOpen = false }
                        )
                    }

                    MenuOverlay(
                        visible = isGesturesConfigOpen,
                        backgroundColor = menuBackgroundColor,
                        onClose = { isGesturesConfigOpen = false }
                    ) {
                        GesturesConfigMenu(
                            apps = allApps,
                            doubleTapAction = doubleTapAction,
                            onDoubleTapActionChange = { scope.launch { themeManager.setDoubleTapAction(it) } },
                            doubleTapAppPackage = doubleTapAppPackage,
                            onDoubleTapAppPackageChange = { scope.launch { themeManager.setDoubleTapAppPackage(it) } },
                            isShakeGesturesEnabled = isShakeGesturesEnabled,
                            onShakeGesturesToggled = { scope.launch { themeManager.setShakeGesturesEnabled(it) } },
                            doubleShakeAction = doubleShakeAction,
                            onDoubleShakeActionChange = { scope.launch { themeManager.setDoubleShakeAction(it) } },
                            shakeOpenAppPackage = shakeOpenAppPackage,
                            onShakeOpenAppPackageChange = { scope.launch { themeManager.setShakeOpenAppPackage(it) } },
                            onClose = { isGesturesConfigOpen = false }
                        )
                    }

                    // Gemeinsame Live-Vorschau des echten Startbildschirms (Layout + Farben) für
                    // Wallpaper-Anpassung und Crop-Ansicht.
                    val wallpaperHomePreview: @Composable () -> Unit = {
                        HomeScreen(
                            favorites = favorites,
                            isSettingsOpen = false,
                            isSearchOpen = false,
                            isEditMode = false,
                            homeLayout = homeLayout,
                            onOpenDrawer = {},
                            onOpenSearch = {},
                            onToggleSettings = {},
                            onToggleEditMode = {},
                            onOpenFavoritesConfig = {},
                            onOpenColorConfig = {},
                            onOpenSizeConfig = {},
                            onOpenSystemSettings = {},
                            onOpenInfo = {},
                            onSaveHomeLayout = { },
                            onLaunchApp = { _, _, _ -> },
                            returnIconPackage = null,
                            searchButtonBounceToken = 0,
                            onSearchButtonBoundsChanged = {},
                            isPreview = true
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
                            homeScreenPreview = wallpaperHomePreview,
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
                        visible = isUninstallAppsOpen,
                        backgroundColor = menuBackgroundColor,
                        onClose = { isUninstallAppsOpen = false }
                    ) {
                        UninstallAppsMenu(
                            apps = allApps,
                            onRefreshApps = { pkg -> refreshAppList(pkg) },
                            onClose = { isUninstallAppsOpen = false }
                        )
                    }

                    MenuOverlay(
                        visible = isHiddenAppsOpen,
                        backgroundColor = menuBackgroundColor,
                        onClose = { isHiddenAppsOpen = false }
                    ) {
                        HiddenAppsMenu(
                            apps = allApps,
                            hiddenPackages = hiddenApps,
                            onToggleHidden = { pkg ->
                                val newHidden = if (pkg in hiddenApps) hiddenApps - pkg else hiddenApps + pkg
                                scope.launch { themeManager.setHiddenApps(newHidden) }
                            },
                            onClose = { isHiddenAppsOpen = false }
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
                                        isSearchClosingState = false
                                        isSearchOpen = false
                                    }
                                }
                        ) {
                            HybridSearch(
                                apps = allApps,
                                onClose = {
                                    isSearchClosingState = false
                                    isSearchOpen = false
                                },
                                onClosingStart = {
                                    isSearchClosingState = true
                                },
                                onAppLaunch = { app, bounds ->
                                    val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                        ?: return@HybridSearch
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
                        durationMillis = if (animationsEnabled) searchLaunchDurationMs.toInt() else 0,
                        scrimColor = Color.Transparent
                    )

                    activeReturnAnimation?.let { animation ->
                        ReturnAnimationOverlay(
                            bounds = animation.bounds,
                            rootSize = rootSize,
                            background = Color.Black,
                            backgroundBrush = null,
                            onFinished = {
                                Log.d(
                                    RETURN_TAG,
                                    "returnOverlayFinished launched=${animation.launchedPackageName} target=${animation.packageName}"
                                )
                                activeReturnAnimation = null
                            },
                            durationMillis = if (animationsEnabled) returnOverlayDurationMs.toInt() else 0,
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
                                homeScreenPreview = wallpaperHomePreview
                            )
                        }
                    }

                    if (showUsageAccessPrompt) {
                        AlertDialog(
                            onDismissRequest = { showUsageAccessPrompt = false },
                            title = { Text(stringResource(R.string.usage_access_title)) },
                            text = {
                                Text(stringResource(R.string.usage_access_description))
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    showUsageAccessPrompt = false
                                    ForegroundAppResolver.openUsageAccessSettings(context)
                                }) {
                                    Text(stringResource(R.string.open))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showUsageAccessPrompt = false }) {
                                    Text(stringResource(R.string.later))
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
        // Alle App-Tasks ausschließen (nicht nur tasks[0]), damit auch vereinzelt entstehende Tasks
        // verschwinden. Auf manchen OEMs steuert der System-Launcher die Übersicht selbst – dort ist das
        // Verhalten ggf. nicht beeinflussbar, deshalb defensiv mit try/catch.
        try {
            val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            am.appTasks.forEach { it.setExcludeFromRecents(true) }
        } catch (_: Exception) {
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
    enterSlideDuration: Int = 300,
    enterFadeDuration: Int = 200,
    exitSlideDuration: Int = 300,
    exitFadeDuration: Int = 200,
    enterSlideEasing: Easing = EaseOutCubic,
    exitSlideEasing: Easing = EaseInCubic,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    val animationsEnabled = LocalMenuAnimationEnabled.current
    val speed = LocalAnimationSpeed.current
    val actualEnterSlideDuration = if (animationsEnabled) (enterSlideDuration / speed).roundToInt() else 0
    val actualEnterFadeDuration = if (animationsEnabled) (enterFadeDuration / speed).roundToInt() else 0
    val actualExitSlideDuration = if (animationsEnabled) (exitSlideDuration / speed).roundToInt() else 0
    val actualExitFadeDuration = if (animationsEnabled) (exitFadeDuration / speed).roundToInt() else 0

    AnimatedVisibility(
        visible = visible,
        // Seitliches Öffnen/Schließen (von rechts) – kohärent mit der Predictive-Back-Geste.
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(actualEnterSlideDuration, easing = enterSlideEasing)
        ) + fadeIn(animationSpec = tween(actualEnterFadeDuration)),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(actualExitSlideDuration, easing = exitSlideEasing)
        ) + fadeOut(animationSpec = tween(actualExitFadeDuration))
    ) {
        var totalDragX by remember { mutableStateOf(0f) }

        // Predictive Back: durchgehender Dismiss-Wert (0 = offen, 1 = ganz draußen).
        // Beim Bestätigen läuft die Animation NAHTLOS von der gehaltenen Position weiter
        // nach draußen – kein Zurückspringen in die Ausgangsposition.
        val dismiss = remember { Animatable(0f) }
        // +1 = nach rechts (Wisch von linker Kante), -1 = nach links (Wisch von rechter Kante).
        var backSign by remember { mutableStateOf(1f) }
        PredictiveBackHandler(enabled = true) { backEvent ->
            try {
                backEvent.collect { ev ->
                    backSign = if (ev.swipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
                    dismiss.snapTo(ev.progress * 0.20f) // Live-„Peek" während der Geste
                }
                // Commit: von der aktuellen Position weiter ganz hinausgleiten, dann schließen.
                dismiss.animateTo(1f, animationSpec = tween((220 / speed).roundToInt(), easing = EaseInCubic))
                onClose()
            } catch (e: CancellationException) {
                // Abbruch: zurückfedern.
                dismiss.animateTo(0f, animationSpec = RethroneSprings.effects(speed))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = dismiss.value
                    scaleX = 1f - 0.10f * p
                    scaleY = 1f - 0.10f * p
                    alpha = 1f - 0.15f * p
                    translationX = size.width * p * backSign
                    // Ecken runden sich ab, sobald Predictive Back aktiv ist – schon beim Halten
                    // voll gerundet (Peek liegt nur bei 0.20, daher 5x verstärkt), offen = eckig.
                    val cornerFraction = (p * 5f).coerceAtMost(1f)
                    shape = RoundedCornerShape((cornerFraction * 44f).dp)
                    clip = true
                }
                .pointerInput(Unit) {
                    detectTapGestures { } // Verhindert Klicks durch das Overlay hindurch
                }
                .then(
                    if (enableDragToClose) {
                        Modifier.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (abs(totalDragX) > 300f) onClose()
                                    totalDragX = 0f
                                },
                                onDragCancel = { totalDragX = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragX += dragAmount
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
