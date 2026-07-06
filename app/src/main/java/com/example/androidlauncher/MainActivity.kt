package com.example.androidlauncher

import android.Manifest
import android.app.ActivityManager
import android.app.role.RoleManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
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
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.androidlauncher.data.AppAccessMode
import com.example.androidlauncher.data.AppFont
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.AppRepository
import com.example.androidlauncher.data.AutoIconRule
import com.example.androidlauncher.data.DesignStyle
import com.example.androidlauncher.data.EdgeLightingStyle
import com.example.androidlauncher.data.FavoriteSpacing
import com.example.androidlauncher.data.FavoritesBorderStyle
import com.example.androidlauncher.data.FavoritesManager
import com.example.androidlauncher.data.FolderInfo
import com.example.androidlauncher.data.FolderManager
import com.example.androidlauncher.data.FontSize
import com.example.androidlauncher.data.FontWeightLevel
import com.example.androidlauncher.data.GestureAction
import com.example.androidlauncher.data.HomeLayout
import com.example.androidlauncher.data.HostedWidget
import com.example.androidlauncher.data.IconManager
import com.example.androidlauncher.data.IconQualityEvaluator
import com.example.androidlauncher.data.IconSize
import com.example.androidlauncher.data.IslandAnimationStyle
import com.example.androidlauncher.data.IslandContent
import com.example.androidlauncher.data.PendingWidgetBind
import com.example.androidlauncher.data.SearchSuggestionsManager
import com.example.androidlauncher.data.ThemeManager
import com.example.androidlauncher.data.WidgetBindFlow
import com.example.androidlauncher.data.WidgetBindStep
import com.example.androidlauncher.data.WidgetHostManager
import com.example.androidlauncher.data.WidgetSizeDp
import com.example.androidlauncher.data.backup.BackupSerializer
import com.example.androidlauncher.data.backup.RestoreResult
import com.example.androidlauncher.data.defaultWidgetSizeDp
import com.example.androidlauncher.data.resolveResizeLimits
import com.example.androidlauncher.gesture.GestureActionEffects
import com.example.androidlauncher.gesture.GestureActionHandler
import com.example.androidlauncher.ui.AnimationsConfigMenu
import com.example.androidlauncher.ui.AppDrawer
import com.example.androidlauncher.ui.AppLockMenu
import com.example.androidlauncher.ui.ColorConfigMenu
import com.example.androidlauncher.ui.DesignStyleMenu
import com.example.androidlauncher.ui.DynamicIsland
import com.example.androidlauncher.ui.EdgeLighting
import com.example.androidlauncher.ui.EdgeLightingConfigMenu
import com.example.androidlauncher.ui.EditConfigMenu
import com.example.androidlauncher.ui.FavoritesConfigMenu
import com.example.androidlauncher.ui.FolderConfigMenu
import com.example.androidlauncher.ui.FontSelectionMenu
import com.example.androidlauncher.ui.GesturesConfigMenu
import com.example.androidlauncher.ui.HiddenAppsMenu
import com.example.androidlauncher.ui.HomeScreen
import com.example.androidlauncher.ui.HybridSearch
import com.example.androidlauncher.ui.IconConfigMenu
import com.example.androidlauncher.ui.InfoDialog
import com.example.androidlauncher.ui.IslandEditControls
import com.example.androidlauncher.ui.IslandExpandedCard
import com.example.androidlauncher.ui.LaunchAnimationOverlay
import com.example.androidlauncher.ui.NiagaraAppDrawer
import com.example.androidlauncher.ui.ReturnAnimationOverlay
import com.example.androidlauncher.ui.SizeConfigMenu
import com.example.androidlauncher.ui.SystemWallpaperView
import com.example.androidlauncher.ui.ThemeSelectionMenu
import com.example.androidlauncher.ui.UninstallAppsMenu
import com.example.androidlauncher.ui.WallpaperConfigMenu
import com.example.androidlauncher.ui.WallpaperCropScreen
import com.example.androidlauncher.ui.WidgetPickerSheet
import com.example.androidlauncher.ui.expandNotifications
import com.example.androidlauncher.ui.home.ActiveOverlay
import com.example.androidlauncher.ui.home.EditConfigActions
import com.example.androidlauncher.ui.home.HomeViewModel
import com.example.androidlauncher.ui.home.rememberLaunchTransitionState
import com.example.androidlauncher.ui.launchAppNoTransition
import com.example.androidlauncher.ui.onboarding.OnboardingFlow
import com.example.androidlauncher.ui.theme.AndroidLauncherTheme
import com.example.androidlauncher.ui.theme.ColorTheme
import com.example.androidlauncher.ui.theme.LocalAnimationSpeed
import com.example.androidlauncher.ui.theme.LocalAnimationsEnabled
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalMenuAnimationEnabled
import com.example.androidlauncher.ui.theme.RethroneSprings
import com.example.androidlauncher.ui.theme.seedRevision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Haupt-Activity des Launchers.
 */
@dagger.hilt.android.AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val SEARCH_RETURN_TARGET = "__search_return_target__"
        private const val RETURN_TAG = "ReturnFlow"

        // Begrenzter vertikaler Verschiebebereich der Dynamic Island (dp) im Layout-Edit-Modus.
        private const val DYNAMIC_ISLAND_MIN_OFFSET_DP = -12f
        private const val DYNAMIC_ISLAND_MAX_OFFSET_DP = 40f

        // B1: Request-Code der Widget-Configure-Activity. Muss über den deprecated
        // onActivityResult-Pfad laufen – startAppWidgetConfigureActivityForResult hat
        // keinen ActivityResult-Contract (Launcher3 macht dasselbe).
        private const val REQUEST_CONFIGURE_WIDGET = 4201

        // Fallback, falls beim Commit keine berechnete Widget-Größe mehr vorliegt.
        private val FALLBACK_WIDGET_SIZE = WidgetSizeDp(widthDp = 180, heightDp = 110)
    }

    // Von Hilt bereitgestellte Datenschicht-Singletons (siehe di/DataModule).
    @javax.inject.Inject
    lateinit var themeManager: ThemeManager

    @javax.inject.Inject
    lateinit var folderManager: FolderManager

    @javax.inject.Inject
    lateinit var iconManager: IconManager

    @javax.inject.Inject
    lateinit var favoritesManager: FavoritesManager

    @javax.inject.Inject
    lateinit var appRepository: AppRepository

    @javax.inject.Inject
    lateinit var searchSuggestionsManager: SearchSuggestionsManager

    @javax.inject.Inject
    lateinit var shakeManager: LauncherShakeManager

    @javax.inject.Inject
    lateinit var dynamicIslandManager: com.example.androidlauncher.data.DynamicIslandManager

    @javax.inject.Inject
    lateinit var widgetHostManager: WidgetHostManager

    @javax.inject.Inject
    lateinit var iconPackRepository: com.example.androidlauncher.data.IconPackRepository

    @javax.inject.Inject
    lateinit var backupManager: com.example.androidlauncher.data.backup.BackupManager

    // B1: laufender Widget-Bind-Flow. Übersteht bewusst keinen Prozess-Tod –
    // geleakte Widget-IDs räumt cleanupOrphans beim nächsten Start ab.
    private var pendingWidgetBind: PendingWidgetBind? = null
    private var pendingWidgetSize: WidgetSizeDp? = null

    // Gleiche Instanz wie hiltViewModel() in setContent (gemeinsamer ViewModelStore der
    // Activity) – als Feld, damit der Widget-Bind-Commit außerhalb der Composition
    // (onActivityResult-Pfad) in den Edit-Modus wechseln kann.
    private val activityHomeViewModel: HomeViewModel by viewModels()

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

            // Datenschicht kommt als Hilt-Singletons aus den injizierten Feldern (siehe oben).
            val themeManager = this@MainActivity.themeManager
            val folderManager = this@MainActivity.folderManager
            val iconManager = this@MainActivity.iconManager
            val favoritesManager = this@MainActivity.favoritesManager
            val appRepository = this@MainActivity.appRepository
            val searchSuggestionsManager = this@MainActivity.searchSuggestionsManager
            val launcherDeviceActions = remember { LauncherDeviceActions(context) }
            val shakeManager = this@MainActivity.shakeManager

            LaunchedEffect(Unit) {
                favoritesManager.migrateFromSharedPreferences(context)
            }

            val currentTheme by themeManager.selectedTheme.collectAsState(initial = ColorTheme.SOFT_SAND)
            val currentFontSize by themeManager.selectedFontSize.collectAsState(initial = FontSize.STANDARD)
            val currentFontWeight by themeManager.selectedFontWeight.collectAsState(initial = FontWeightLevel.NORMAL)
            val currentIconSize by themeManager.selectedIconSize.collectAsState(initial = IconSize.STANDARD)
            val currentFavoriteSpacing by themeManager.selectedFavoriteSpacing.collectAsState(
                initial = FavoriteSpacing.STANDARD
            )
            val currentAppFont by themeManager.selectedAppFont.collectAsState(initial = AppFont.SYSTEM_DEFAULT)
            // Initiale Werte passend zum warmen Standard-Theme "Tulpe" (vermeidet weißes Aufblitzen).
            val isDarkTextEnabled by themeManager.isDarkTextEnabled.collectAsState(initial = true)
            val iconColor by themeManager.iconColor.collectAsState(initial = Color(0xFF2C2A28))
            val homeTextColor by themeManager.homeTextColor.collectAsState(initial = Color(0xFF2C2A28))
            val customBackgroundColor by themeManager.customBackgroundColor.collectAsState(
                initial = ColorTheme.FallbackCustomBackground
            )
            val customMenuColor by themeManager.customMenuColor.collectAsState(initial = ColorTheme.FallbackCustomMenu)
            // CUSTOM-Theme: gewählte Flächenfarben in den Holder spiegeln (treibt die Farb-Pipeline).
            LaunchedEffect(customBackgroundColor, customMenuColor) {
                com.example.androidlauncher.data.CustomColorHolder.set(customBackgroundColor, customMenuColor)
            }
            val showFavoriteLabels by themeManager.showFavoriteLabels.collectAsState(initial = false)
            val notificationDotsEnabled by themeManager.isNotificationDotsEnabled.collectAsState(initial = true)
            val hiddenApps by themeManager.hiddenApps.collectAsState(initial = emptySet())
            val lockedApps by themeManager.lockedApps.collectAsState(initial = emptySet())
            val lockType by themeManager.lockType.collectAsState(initial = "none")
            val lockBiometricEnabled by themeManager.isLockBiometricEnabled.collectAsState(initial = false)
            val designStyle by themeManager.designStyle.collectAsState(initial = DesignStyle.GLASS)
            val favoritesBorderStyle by themeManager.favoritesBorderStyle.collectAsState(
                initial = FavoritesBorderStyle.NONE
            )
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
            val isDynamicIslandEnabled by themeManager.isDynamicIslandEnabled.collectAsState(initial = true)
            val dynamicIslandOffset by themeManager.dynamicIslandOffset.collectAsState(initial = 0f)
            val dynamicIslandColor by themeManager.dynamicIslandColor.collectAsState(initial = Color(0xFF0B0B0C))
            val islandAnimationStyle by themeManager.islandAnimationStyle.collectAsState(
                initial = IslandAnimationStyle.FROM_NOTCH
            )
            val isEdgeLightingEnabled by themeManager.isEdgeLightingEnabled.collectAsState(initial = false)
            val edgeLightingColor by themeManager.edgeLightingColor.collectAsState(initial = Color(0xFF0A84FF))
            val edgeLightingSpeed by themeManager.edgeLightingSpeed.collectAsState(initial = 1f)
            val edgeLightingLaps by themeManager.edgeLightingLaps.collectAsState(initial = 1)
            val edgeLightingThickness by themeManager.edgeLightingThickness.collectAsState(initial = 1f)
            val edgeLightingStyle by themeManager.edgeLightingStyle.collectAsState(initial = EdgeLightingStyle.SWEEP)
            // Edge-Lighting-Puls: jede neue Benachrichtigung erhöht den Zähler → eine Lauf-Runde.
            // Nur einsammeln, während der Launcher im Vordergrund (RESUMED) ist – so löst eine
            // Benachrichtigung, die in einer anderen App eintrifft, kein nachträgliches Seitenlicht
            // beim Zurückkehren aus (notificationPulse hat replay=0, Hintergrund-Pulse verfallen).
            var edgePulseId by remember { mutableStateOf(0) }
            val edgePulseLifecycleOwner = LocalLifecycleOwner.current
            LaunchedEffect(edgePulseLifecycleOwner) {
                edgePulseLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    dynamicIslandManager.notificationPulse.collect { edgePulseId++ }
                }
            }
            val appAccessMode by themeManager.appAccessMode.collectAsState(initial = AppAccessMode.DRAWER_LIST)
            // Erststart-Onboarding: Default true vermeidet Aufblitzen für Bestandsnutzer,
            // bis der echte Wert aus DataStore geladen ist.
            val onboardingCompleted by themeManager.isOnboardingCompleted.collectAsState(initial = true)

            val customWallpaperUri by themeManager.customWallpaperUri.collectAsState(initial = null)
            val wallpaperBlur by themeManager.wallpaperBlur.collectAsState(initial = 0f)
            val wallpaperDim by themeManager.wallpaperDim.collectAsState(initial = 0.1f)
            val wallpaperZoom by themeManager.wallpaperZoom.collectAsState(initial = 1.0f)
            val searchHistory by searchSuggestionsManager.webHistory.collectAsState(initial = emptyList())
            val appUsageStats by searchSuggestionsManager.appUsageStats.collectAsState(initial = emptyMap())

            // Positionen der unabhängig verschiebbaren Startbildschirm-Elemente.
            val homeLayout by themeManager.homeLayout.collectAsState(initial = HomeLayout())

            // B1: platzierte System-Widgets (AppWidgetHost).
            val hostedWidgets by widgetHostManager.widgets.collectAsState(initial = emptyList())

            val folders by folderManager.folders.collectAsState(initial = emptyList())
            val customIcons by iconManager.customIcons.collectAsState(initial = emptyMap())
            val autoIconFallbacks by iconManager.autoIconFallbacks.collectAsState(initial = emptyMap())
            val autoIconRules by iconManager.autoIconRules.collectAsState(initial = emptyMap())

            // B4: global gewähltes Icon-Pack (nur für die Anzeige; refreshAppList liest
            // den Wert autoritativ direkt aus dem Store).
            val selectedIconPack by iconManager.selectedIconPack.collectAsState(initial = null)
            val favoritePackages by favoritesManager.favorites.collectAsState(initial = emptyList())

            var isWallpaperCropOpen by remember { mutableStateOf(false) }
            var pendingWallpaperUri by remember { mutableStateOf<Uri?>(null) }

            // A2-Split: Navigations-/Overlay-Zustand lebt im HomeViewModel und
            // überlebt damit Konfigurationswechsel. Abgeleitete Werte halten die
            // bestehenden Lese-Stellen stabil.
            val homeViewModel: HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val homeUi by homeViewModel.uiState.collectAsState()
            val showUsageAccessPrompt = homeUi.showUsageAccessPrompt
            val hasShownUsageAccessPrompt = homeUi.hasShownUsageAccessPrompt

            val wallpaperPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri: Uri? ->
                uri?.let { sourceUri ->
                    pendingWallpaperUri = sourceUri
                    isWallpaperCropOpen = true
                }
            }

            // B5: SAF-Dialoge für Backup-Export/-Import; die eigentliche Arbeit
            // (Streams + BackupManager) übernehmen die Activity-Helfer auf Dispatchers.IO.
            val backupExportLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json")
            ) { uri: Uri? ->
                uri?.let { exportBackupTo(it) }
            }

            val backupImportLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                uri?.let { importBackupFrom(it) }
            }

            val cameraPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                val pendingAction = homeViewModel.consumePendingPermissionShakeAction()

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
                        Toast.makeText(
                            context,
                            context.getString(R.string.flashlight_unsupported),
                            Toast.LENGTH_SHORT
                        ).show()
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

            // Geräte-/System-Gesten führt der testbare GestureActionHandler aus; UI-Rückmeldungen
            // (Toast/Permission/Settings) reicht er über die Effects-Bridge an die Activity zurück.
            val gestureActionHandler = remember(launcherDeviceActions, context) {
                GestureActionHandler(
                    deviceActions = launcherDeviceActions,
                    isAccessibilityEnabled = {
                        LauncherAccessibilityService.isAccessibilityServiceEnabled(context)
                    },
                    requestLockScreen = { LauncherAccessibilityService.requestLockScreen(context) },
                )
            }
            val gestureEffects = object : GestureActionEffects {
                override fun showMessage(messageRes: Int, longDuration: Boolean) {
                    Toast.makeText(
                        context,
                        context.getString(messageRes),
                        if (longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                    ).show()
                }

                override fun requestCameraPermission() {
                    homeViewModel.setPendingPermissionShakeAction(GestureAction.FLASHLIGHT)
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }

                override fun openAccessibilitySettings() {
                    runCatching {
                        startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
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

            // B1: System-Consent-Dialog für das Widget-Binden (ACTION_APPWIDGET_BIND).
            val appWidgetBindLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                pendingWidgetBind?.let { pending ->
                    handleWidgetBindStep(
                        WidgetBindFlow.afterBindPermission(pending, granted = result.resultCode == RESULT_OK)
                    )
                }
            }

            val configuration = LocalConfiguration.current

            // Startet den Bind-Flow für ein im Picker gewähltes Widget; die Schrittfolge
            // entscheidet die pure State-Machine WidgetBindFlow.
            fun startWidgetBind(info: AppWidgetProviderInfo) {
                val appWidgetId = widgetHostManager.allocateWidgetId()
                val pending = PendingWidgetBind(
                    appWidgetId = appWidgetId,
                    provider = info.provider.flattenToString(),
                    needsConfigure = info.configure != null,
                )
                pendingWidgetBind = pending
                pendingWidgetSize = defaultWidgetSizeDp(
                    targetCellWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) info.targetCellWidth else 0,
                    targetCellHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) info.targetCellHeight else 0,
                    minWidthDp = info.minWidth,
                    minHeightDp = info.minHeight,
                    screenWidthDp = configuration.screenWidthDp,
                    screenHeightDp = configuration.screenHeightDp,
                )
                homeViewModel.closeOverlay()
                val bound = widgetHostManager.bindIfAllowed(appWidgetId, info.provider)
                when (val step = WidgetBindFlow.afterAllocate(pending, bound)) {
                    is WidgetBindStep.RequestBindPermission -> runCatching {
                        appWidgetBindLauncher.launch(
                            Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                        )
                    }.onFailure { abortWidgetBind(appWidgetId, showError = true) }
                    else -> handleWidgetBindStep(step)
                }
            }

            // Geräte-/System-Aktionen einer Geste werden an den testbaren GestureActionHandler
            // delegiert. Launcher-interne Aktionen (App-Drawer/Suche/Benachrichtigungen) setzen
            // UI-State und werden weiterhin im inneren dispatchGestureAction behandelt.
            fun runGestureAction(action: GestureAction, appPackage: String?) {
                gestureActionHandler.handle(action, appPackage, this@MainActivity, gestureEffects)
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
                notificationDotsEnabled = notificationDotsEnabled,
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
                val searchLaunchOverlayColor = Color.Black
                val launchOverlayBrush: Brush? = null
                val returnOverlayBrush: Brush? = null

                var rootSize by remember { mutableStateOf(IntSize.Zero) }

                // A2-Split: transiente Start-/Rückkehr-Animations-Zustände leben gebündelt
                // im LaunchTransitionStateHolder (remember, bewusst kein ViewModel). Die
                // lokalen Delegates halten die bestehenden Lese-/Schreib-Stellen stabil.
                val launchTransitions = rememberLaunchTransitionState(searchLaunchOverlayColor)
                var pendingReturnAnimation by launchTransitions::pendingReturnAnimation
                var pendingReturnAnimationStartedWallClockMs by
                    launchTransitions::pendingReturnAnimationStartedWallClockMs
                var activeReturnAnimation by launchTransitions::activeReturnAnimation
                var returnIconPackage by launchTransitions::returnIconPackage
                var searchButtonBounceToken by launchTransitions::searchButtonBounceToken
                var returnBounceToken by launchTransitions::returnBounceToken
                var returnBounceTargetPackage by launchTransitions::returnBounceTargetPackage
                var launchIconPackage by launchTransitions::launchIconPackage
                val returnOverlayDurationMs = (260L / animationSpeed).toLong()
                // Bounce erst nach Abschluss des Schließen-Panels (260ms), damit er nicht
                // gegen das noch schrumpfende Panel läuft.
                val returnBounceDelayMs = (270L / animationSpeed).toLong()
                val isDrawerOpen = homeUi.isDrawerOpen
                val isSettingsOpen = homeUi.isSettingsOpen
                val isSearchOpen = homeUi.isSearchOpen
                var isSearchClosingState by remember { mutableStateOf(false) }
                val isHomeEditMode = homeUi.isHomeEditMode

                // Zentraler Geste-Dispatch: Launcher-interne Aktionen setzen hier den
                // UI-State, alles andere geht an runGestureAction (Geräte-/System-Aktionen).
                fun dispatchGestureAction(action: GestureAction, appPackage: String?) {
                    when (action) {
                        GestureAction.APP_DRAWER -> homeViewModel.setDrawerOpen(true)
                        GestureAction.SEARCH -> {
                            isSearchClosingState = false
                            homeViewModel.setSearchOpen(true)
                        }
                        GestureAction.NOTIFICATIONS -> expandNotifications(context)
                        else -> runGestureAction(action, appPackage)
                    }
                }
                shakeManager.onDoubleShake = { dispatchGestureAction(doubleShakeAction, shakeOpenAppPackage) }
                var homeSearchButtonBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
                var isSearchLaunching by launchTransitions::isSearchLaunching
                var isAppLaunchAnimating by launchTransitions::isAppLaunchAnimating
                var activeLaunchBounds by launchTransitions::activeLaunchBounds
                var activeLaunchBackground by launchTransitions::activeLaunchBackground
                var activeLaunchBackgroundBrush by launchTransitions::activeLaunchBackgroundBrush
                // Treibt das leichte Zurücktreten (Skalieren/Abdunkeln) des Homescreen-/Drawer-Inhalts,
                // damit das Start-/Rückkehr-Panel nicht als lose Schicht über eingefrorenem Inhalt wirkt.
                val contentRevealProgress = remember { Animatable(0f) }
                val searchLaunchDurationMs = (260L / animationSpeed).toLong()
                val searchLaunchSettleAfterStartMs = (30L / animationSpeed).toLong()
                // Kurzer Vorlauf, in dem nur das gedrückte Icon poppt, bevor das
                // wachsende Panel es verdeckt.
                val launchBounceLeadMs = (120L / animationSpeed).toLong()
                // A2-Split: genau EIN Overlay kann offen sein (sealed class statt 17 Booleans);
                // der Zustand lebt im HomeViewModel. Die abgeleiteten Werte darunter halten
                // die vielen Lese-Stellen stabil.
                val activeOverlay = homeUi.activeOverlay
                val isFavoritesConfigOpen = activeOverlay is ActiveOverlay.FavoritesConfig
                val isColorConfigOpen = activeOverlay is ActiveOverlay.ColorConfig
                val isAnimationsConfigOpen = activeOverlay is ActiveOverlay.AnimationsConfig
                val isEdgeLightingConfigOpen = activeOverlay is ActiveOverlay.EdgeLightingConfig
                val isGesturesConfigOpen = activeOverlay is ActiveOverlay.GesturesConfig
                val isDesignMenuOpen = activeOverlay is ActiveOverlay.DesignMenu
                val isThemeMenuOpen = activeOverlay is ActiveOverlay.ThemeMenu
                val isSizeConfigOpen = activeOverlay is ActiveOverlay.SizeConfig
                val isFontSelectionOpen = activeOverlay is ActiveOverlay.FontSelection
                val isEditConfigOpen = activeOverlay is ActiveOverlay.EditConfig
                val isIconConfigOpen = activeOverlay is ActiveOverlay.IconConfig
                val isUninstallAppsOpen = activeOverlay is ActiveOverlay.UninstallApps
                val isHiddenAppsOpen = activeOverlay is ActiveOverlay.HiddenApps
                val isAppLockOpen = activeOverlay is ActiveOverlay.AppLock
                val isWallpaperConfigOpen = activeOverlay is ActiveOverlay.WallpaperConfig
                val isInfoOpen = activeOverlay is ActiveOverlay.Info
                val isWidgetPickerOpen = activeOverlay is ActiveOverlay.WidgetPicker
                val selectedFolderForConfig = (activeOverlay as? ActiveOverlay.FolderConfig)?.folder
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
                            val accessibilityEnabled = LauncherAccessibilityService.isAccessibilityServiceEnabled(
                                context
                            )
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
                                "resume a11y=$accessibilityEnabled usage=$usageAccessEnabled storedPackages=$storedPackages storedOrigins=$storedOriginCount pending=${pendingReturnAnimation?.launchedPackageName} pendingLaunchWall=$pendingReturnAnimationStartedWallClockMs beforeLauncher=$beforeLauncherObservation usageObservation=$usageObservation"
                            )

                            if (!accessibilityEnabled && !usageAccessEnabled && storedOriginCount > 1 && !hasShownUsageAccessPrompt) {
                                homeViewModel.requestUsageAccessPromptOnce()
                                Log.d(
                                    RETURN_TAG,
                                    "prompt usage access because multiple origins exist and no foreground tracking is available"
                                )
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
                                val viaDrawer = animation.source == LaunchSource.DRAWER
                                homeViewModel.setDrawerOpen(viaDrawer)
                                if (!viaDrawer) {
                                    homeViewModel.setSettingsOpen(false)
                                    homeViewModel.closeOverlay()
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
                                Log.d(
                                    RETURN_TAG,
                                    "activateReturn launched=${animation.launchedPackageName} target=${animation.packageName} source=${animation.source}"
                                )
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                // B1: Widget-Host lauscht zwischen ON_START und ON_STOP auf RemoteViews-Updates
                // (Updates unter Dialogen laufen weiter; verpasste spielt das Framework nach).
                // Orphan-Cleanup nur ohne laufenden Bind-Flow – die Rückkehr aus Bind-/Configure-
                // Dialogen durchläuft ON_START, bevor onActivityResult eintrifft.
                DisposableEffect(lifecycleOwner) {
                    val widgetHostObserver = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_START -> {
                                widgetHostManager.startListening()
                                if (pendingWidgetBind == null) {
                                    lifecycleScope.launch { widgetHostManager.cleanupOrphans() }
                                }
                            }
                            Lifecycle.Event.ON_STOP -> widgetHostManager.stopListening()
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(widgetHostObserver)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(widgetHostObserver) }
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
                    if (hiddenApps.isEmpty()) {
                        allApps.toList()
                    } else {
                        allApps.filterNot { it.packageName in hiddenApps }
                    }
                }
                val favorites = remember(visibleApps, favoritePackages) {
                    LauncherLogic.getFavoriteApps(visibleApps, favoritePackages)
                }

                fun refreshAppList(targetPackageName: String? = null) {
                    scope.launch {
                        // B4: gewähltes Icon-Pack direkt aus dem Store lesen (kein Race mit
                        // dem asynchronen collectAsState nach einem Pack-Wechsel).
                        val currentIconPack = iconManager.selectedIconPack.first()
                        appRepository.cleanupLegacyCache()
                        appRepository.invalidateCacheOnAppUpdate()
                        val basicList = appRepository.getInstalledApps()
                        // Geladene Icons/Fallbacks bewahren + Regeln anwenden – reine, testbare Transformation.
                        val mergedList = LauncherLogic.mergeInstalledApps(
                            basicApps = basicList,
                            existingApps = allApps,
                            storedFallbacks = autoIconFallbacks,
                            autoIconRules = autoIconRules,
                        )
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
                                appRepository.loadResolvedIcon(
                                    appSnapshot[targetIndex],
                                    currentIconPack
                                )?.let { resolvedIcon ->
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
                            favoritePackages = favoritePackages,
                            iconPack = currentIconPack
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
                                cx - sizePx / 2f,
                                cy - sizePx / 2f,
                                cx + sizePx / 2f,
                                cy + sizePx / 2f
                            )
                        } else {
                            null
                        }
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
                    homeViewModel.setSearchOpen(false)
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
                    Log.d(
                        RETURN_TAG,
                        "requestSearchLaunch resolved=$resolvedPackageName bounds=${bounds != null} searchButton=${homeSearchButtonBounds != null}"
                    )
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
                                        Log.d(
                                            RETURN_TAG,
                                            "screen off while launcher foreground -> suppress return during lockscreen cycle"
                                        )
                                    }
                                }
                                Intent.ACTION_USER_PRESENT -> {
                                    val previousState = returnResumeGuardState
                                    returnResumeGuardState = ReturnResumeGuard.onUserPresent(returnResumeGuardState)
                                    if (previousState != returnResumeGuardState) {
                                        Log.d(
                                            RETURN_TAG,
                                            "user present after launcher screen off -> suppress next launcher resume return"
                                        )
                                    }
                                }
                                Intent.ACTION_PACKAGE_ADDED,
                                Intent.ACTION_PACKAGE_REMOVED,
                                Intent.ACTION_PACKAGE_REPLACED -> {
                                    val packageName = intent.data?.schemeSpecificPart
                                    val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                                    scope.launch {
                                        // B4: betrifft der Broadcast das gewählte Icon-Pack, alle
                                        // Icons neu aufbauen; bei echter Deinstallation (nicht dem
                                        // REMOVED-Teil eines Updates) Auswahl auf System-Icons zurück.
                                        val selectedPack = iconManager.selectedIconPack.first()
                                        if (!packageName.isNullOrBlank() && packageName == selectedPack) {
                                            if (intent.action == Intent.ACTION_PACKAGE_REMOVED && !isReplacing) {
                                                iconManager.setSelectedIconPack(null)
                                            }
                                            iconPackRepository.invalidate(packageName)
                                            appRepository.clearIconCache()
                                            delay(800)
                                            refreshAppList()
                                            return@launch
                                        }
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

                // Das frühere "Drawer öffnet → alles andere schließen" erledigt jetzt der
                // Reducer in HomeViewModel.setDrawerOpen(true) deterministisch.

                LaunchedEffect(homeUi.hasModalSurface) {
                    backCallback.isEnabled = !homeUi.hasModalSurface
                }

                BackHandler(enabled = homeUi.hasModalSurface) {
                    // Prioritätsreihenfolge (Ordner → Edit-Modus → Suche → innere Menüs →
                    // Drawer → restliche Overlays) lebt testbar im HomeViewModel.
                    homeViewModel.onBack()
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
                    // Material-3-Expressive: federbasierter Container-Übergang statt hartem
                    // Cubic-Tween. `container` ist knackig-räumlich mit minimalem Overshoot,
                    // damit am unteren Drawer-Rand keine Wallpaper-Lücke aufblitzt.
                    val drawerSlideSpec = if (animationsEnabled) {
                        RethroneSprings.container<IntOffset>(animationSpeed)
                    } else {
                        snap<IntOffset>()
                    }
                    val drawerFadeSpec = if (animationsEnabled) {
                        RethroneSprings.effects<Float>(animationSpeed)
                    } else {
                        snap<Float>()
                    }

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
                                        animationSpec = drawerSlideSpec
                                    ) + fadeIn(animationSpec = drawerFadeSpec)
                                    ).togetherWith(fadeOut(animationSpec = drawerFadeSpec))
                            } else {
                                fadeIn(animationSpec = drawerFadeSpec).togetherWith(
                                    slideOutVertically(
                                        targetOffsetY = { it },
                                        animationSpec = drawerSlideSpec
                                    ) + fadeOut(animationSpec = drawerFadeSpec)
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
                                    onClose = { homeViewModel.setDrawerOpen(false) },
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
                                    onOpenFolderConfig = { folder ->
                                        homeViewModel.openOverlay(ActiveOverlay.FolderConfig(folder))
                                    },
                                    onClose = { homeViewModel.setDrawerOpen(false) },
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
                                onOpenDrawer = { homeViewModel.setDrawerOpen(true) },
                                onOpenSearch = {
                                    isSearchClosingState = false
                                    homeViewModel.setSearchOpen(true)
                                },
                                doubleTapAction = doubleTapAction,
                                doubleTapAppPackage = doubleTapAppPackage,
                                onGestureAction = { action, pkg -> dispatchGestureAction(action, pkg) },
                                onToggleSettings = { homeViewModel.toggleSettings() },
                                onToggleEditMode = { homeViewModel.toggleHomeEditMode() },
                                onOpenFavoritesConfig = { homeViewModel.openOverlay(ActiveOverlay.FavoritesConfig) },
                                onOpenColorConfig = { homeViewModel.openOverlay(ActiveOverlay.ColorConfig) },
                                onOpenSizeConfig = { homeViewModel.openOverlay(ActiveOverlay.SizeConfig) },
                                onOpenSystemSettings = { homeViewModel.openOverlay(ActiveOverlay.EditConfig) },
                                onOpenInfo = { homeViewModel.openOverlay(ActiveOverlay.Info) },
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
                                },
                                hostedWidgets = hostedWidgets,
                                widgetViewProvider = { id -> widgetHostManager.createView(id) },
                                onSaveWidgetLayout = { widgetOffsets, widgetSizes ->
                                    scope.launch {
                                        widgetHostManager.updateWidgetPlacement(widgetOffsets, widgetSizes)
                                    }
                                },
                                onRemoveWidget = { id ->
                                    scope.launch { widgetHostManager.removeWidget(id) }
                                },
                                // B1-PR4: Resize-Grenzen aus den Provider-Angaben; maxResize*
                                // gibt es erst ab API 31 (0 = vom Provider nicht begrenzt).
                                widgetResizeLimits = { id ->
                                    widgetHostManager.providerInfo(id)?.let { info ->
                                        val current = hostedWidgets.firstOrNull { it.appWidgetId == id }
                                        resolveResizeLimits(
                                            resizeMode = info.resizeMode,
                                            minResizeWidthDp = if (info.minResizeWidth > 0) {
                                                info.minResizeWidth
                                            } else {
                                                info.minWidth
                                            },
                                            minResizeHeightDp = if (info.minResizeHeight > 0) {
                                                info.minResizeHeight
                                            } else {
                                                info.minHeight
                                            },
                                            maxResizeWidthDp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                info.maxResizeWidth
                                            } else {
                                                0
                                            },
                                            maxResizeHeightDp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                info.maxResizeHeight
                                            } else {
                                                0
                                            },
                                            currentWidthDp = current?.widthDp ?: 0,
                                            currentHeightDp = current?.heightDp ?: 0,
                                            screenWidthDp = configuration.screenWidthDp,
                                            screenHeightDp = configuration.screenHeightDp,
                                        )
                                    }
                                }
                            )
                        }
                    }

                    // Edge Lighting: leuchtender Rand-Lauf bei neuen Benachrichtigungen (Samsung-Stil).
                    // Eigenständig (unabhängig von Island-Toggle & Drawer); zeichnet nur während einer
                    // Runde und fängt keine Touches ab.
                    if (isEdgeLightingEnabled) {
                        EdgeLighting(
                            modifier = Modifier.fillMaxSize().zIndex(6500f),
                            color = edgeLightingColor,
                            pulseId = edgePulseId,
                            style = edgeLightingStyle,
                            lapDurationMs = (1400f / edgeLightingSpeed).roundToInt(),
                            laps = edgeLightingLaps,
                            thickness = edgeLightingThickness
                        )
                    }

                    // Dynamic Island: ereignisgesteuerte Pille an der Kamera. Nur auf dem
                    // Home-Screen (nicht im Drawer) und wenn per Einstellung aktiviert.
                    // Im Leerlauf zeichnet die Composable selbst nichts – außer im Layout-Edit-Modus,
                    // wo eine ziehbare Platzhalter-Pille zum vertikalen Feinjustieren erscheint.
                    if (isDynamicIslandEnabled && !isDrawerOpen) {
                        val islandState by dynamicIslandManager.state.collectAsState()
                        val islandExpanded by dynamicIslandManager.expandedContent.collectAsState()
                        DynamicIsland(
                            state = islandState,
                            onTap = { content -> dynamicIslandManager.expand(content) },
                            loadIcon = { pkg -> appRepository.loadIcon(pkg) },
                            verticalOffsetDp = dynamicIslandOffset,
                            islandColor = dynamicIslandColor,
                            editMode = isHomeEditMode,
                            onSwipeNext = { dynamicIslandManager.selectNext() },
                            onSwipePrevious = { dynamicIslandManager.selectPrevious() },
                            animationStyle = islandAnimationStyle,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                        // Höhen-Steuerung im Layout-Editor: unter der Statusleiste, damit die
                        // Taps ankommen (in der Statusleiste würde das System sie abfangen).
                        if (isHomeEditMode) {
                            IslandEditControls(
                                onNudge = { deltaDp ->
                                    val next = (dynamicIslandOffset + deltaDp)
                                        .coerceIn(DYNAMIC_ISLAND_MIN_OFFSET_DP, DYNAMIC_ISLAND_MAX_OFFSET_DP)
                                    scope.launch { themeManager.setDynamicIslandOffset(next) }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .statusBarsPadding()
                                    .padding(top = 8.dp)
                                    .zIndex(7000f)
                            )
                        }
                        // Aufgeklappte Karte: federnd aus dem Kamerabereich (oben-mitte) wachsen
                        // bzw. dahin zurückschrumpfen. lastExpanded puffert den Inhalt, damit die
                        // Schließen-Animation nach dem Dismiss noch etwas anzuzeigen hat.
                        var lastExpanded by remember { mutableStateOf<IslandContent?>(null) }
                        LaunchedEffect(islandExpanded) {
                            if (islandExpanded != null) lastExpanded = islandExpanded
                        }
                        val islandAnimEnabled = LocalAnimationsEnabled.current
                        AnimatedVisibility(
                            visible = islandExpanded != null,
                            modifier = Modifier.fillMaxSize().zIndex(7000f),
                            enter = if (islandAnimEnabled) {
                                scaleIn(
                                    RethroneSprings.container(animationSpeed),
                                    initialScale = 0.85f,
                                    transformOrigin = TransformOrigin(0.5f, 0f)
                                ) + fadeIn(RethroneSprings.effects(animationSpeed))
                            } else {
                                fadeIn()
                            },
                            exit = if (islandAnimEnabled) {
                                scaleOut(
                                    RethroneSprings.effects(animationSpeed),
                                    targetScale = 0.85f,
                                    transformOrigin = TransformOrigin(0.5f, 0f)
                                ) + fadeOut(RethroneSprings.effects(animationSpeed))
                            } else {
                                fadeOut()
                            }
                        ) {
                            val expandedContent = islandExpanded ?: lastExpanded
                            if (expandedContent != null) {
                                // Medien-/Timer-Karte muss live sein (Play/Pause-Icon & Restzeit
                                // aktualisieren sich): den aktuellen Zustand nehmen, sonst die
                                // eingefrorene Kopie.
                                val liveMedia = islandState.all
                                    .firstOrNull { it is IslandContent.Media } as? IslandContent.Media
                                val liveTimer = islandState.all
                                    .firstOrNull { it is IslandContent.Timer } as? IslandContent.Timer
                                val cardContent = when (expandedContent) {
                                    is IslandContent.Media -> liveMedia ?: expandedContent
                                    is IslandContent.Timer -> liveTimer ?: expandedContent
                                    else -> expandedContent
                                }
                                IslandExpandedCard(
                                    content = cardContent,
                                    allContents = islandState.all,
                                    onSwitch = { c ->
                                        dynamicIslandManager.selectActivity(c)
                                        dynamicIslandManager.expand(c)
                                    },
                                    onAction = { action ->
                                        sendPendingIntent(context, action.intent)
                                        dynamicIslandManager.dismissExpanded()
                                    },
                                    onReply = { action, text ->
                                        sendNotificationReply(context, action, text)
                                        dynamicIslandManager.dismissExpanded()
                                    },
                                    // Timer steuern (Pause/Play/…): Karte bewusst offen lassen,
                                    // damit man direkt weiter steuern kann.
                                    onTimerControl = { action -> sendPendingIntent(context, action.intent) },
                                    onOpen = { content ->
                                        val intent = when (content) {
                                            is IslandContent.Notification -> content.contentIntent
                                            is IslandContent.Timer -> content.contentIntent
                                            else -> null
                                        }
                                        // Bei Medien & Timer Karte offen lassen (nur Buttons/Scrim agieren).
                                        if (content !is IslandContent.Media && content !is IslandContent.Timer) {
                                            sendPendingIntent(context, intent)
                                            dynamicIslandManager.dismissExpanded()
                                        }
                                    },
                                    onDismiss = { dynamicIslandManager.dismissExpanded() },
                                    onMediaPlayPause = { dynamicIslandManager.mediaPlayPause() },
                                    onMediaNext = { dynamicIslandManager.mediaNext() },
                                    onMediaPrev = { dynamicIslandManager.mediaPrevious() },
                                    onMediaSeekTo = { positionMs -> dynamicIslandManager.mediaSeekTo(positionMs) },
                                    islandColor = dynamicIslandColor
                                )
                            }
                        }
                    }

                    MenuOverlay(
                        visible = isFavoritesConfigOpen,
                        customWallpaperUri = customWallpaperUri,
                        enableDragToClose = false,
                        onClose = { homeViewModel.closeOverlay() }
                    ) {
                        @Suppress("DEPRECATION")
                        FavoritesConfigMenu(
                            apps = allApps,
                            initialFavoritePackages = favoritePackages,
                            showFavoriteLabels = showFavoriteLabels,
                            onShowLabelsToggled = { show ->
                                scope.launch { themeManager.setShowFavoriteLabels(show) }
                            },
                            notificationDotsEnabled = notificationDotsEnabled,
                            onNotificationDotsToggled = { enabled ->
                                scope.launch { themeManager.setNotificationDotsEnabled(enabled) }
                            },
                            favoritesBorderStyle = favoritesBorderStyle,
                            onBorderStyleSelected = { style ->
                                scope.launch { themeManager.setFavoritesBorderStyle(style) }
                            },
                            onConfirm = { newFavs ->
                                scope.launch { favoritesManager.saveFavorites(newFavs) }
                                homeViewModel.closeOverlay()
                            },
                            onClose = { homeViewModel.closeOverlay() }
                        )
                    }

                    MenuOverlay(
                        visible = selectedFolderForConfig != null,
                        customWallpaperUri = customWallpaperUri,
                        enableDragToClose = false,
                        onClose = { homeViewModel.closeOverlay() }
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
                                    homeViewModel.closeOverlay()
                                },
                                onDelete = { folderId ->
                                    val newFolders = folders.filter { it.id != folderId }
                                    scope.launch { folderManager.saveFolders(newFolders) }
                                    homeViewModel.closeOverlay()
                                },
                                onClose = { homeViewModel.closeOverlay() }
                            )
                        }
                    }

                    MenuOverlay(
                        visible = isColorConfigOpen,
                        customWallpaperUri = customWallpaperUri,
                        onClose = { homeViewModel.closeOverlay() }
                    ) {
                        ColorConfigMenu(
                            selectedTheme = currentTheme,
                            isDarkTextEnabled = isDarkTextEnabled,
                            iconColor = iconColor,
                            onIconColorChange = { scope.launch { themeManager.setIconColor(it) } },
                            homeTextColor = homeTextColor,
                            onHomeTextColorChange = { scope.launch { themeManager.setHomeTextColor(it) } },
                            designStyle = designStyle,
                            onOpenDesignMenu = { homeViewModel.openOverlay(ActiveOverlay.DesignMenu) },
                            onOpenThemeMenu = { homeViewModel.openOverlay(ActiveOverlay.ThemeMenu) },
                            customBackgroundColor = customBackgroundColor,
                            onCustomBackgroundChange = { scope.launch { themeManager.setCustomBackgroundColor(it) } },
                            customMenuColor = customMenuColor,
                            onCustomMenuChange = { scope.launch { themeManager.setCustomMenuColor(it) } },
                            dynamicIslandColor = dynamicIslandColor,
                            onDynamicIslandColorChange = { scope.launch { themeManager.setDynamicIslandColor(it) } },
                            edgeLightingColor = edgeLightingColor,
                            onEdgeLightingColorChange = { scope.launch { themeManager.setEdgeLightingColor(it) } },
                            customWallpaperUri = customWallpaperUri,
                            onClose = { homeViewModel.closeOverlay() }
                        )
                    }

                    MenuOverlay(
                        visible = isThemeMenuOpen,
                        customWallpaperUri = customWallpaperUri,
                        onClose = { homeViewModel.closeOverlay() }
                    ) {
                        ThemeSelectionMenu(
                            selectedTheme = currentTheme,
                            onThemeSelected = { scope.launch { themeManager.setTheme(it) } },
                            isDarkTextEnabled = isDarkTextEnabled,
                            designStyle = designStyle,
                            customWallpaperUri = customWallpaperUri,
                            onClose = { homeViewModel.closeOverlay() }
                        )
                    }

                    MenuOverlay(
                        visible = isDesignMenuOpen,
                        customWallpaperUri = customWallpaperUri,
                        onClose = { homeViewModel.closeOverlay() }
                    ) {
                        DesignStyleMenu(
                            currentStyle = designStyle,
                            selectedTheme = currentTheme,
                            isDarkTextEnabled = isDarkTextEnabled,
                            onStyleSelected = { style ->
                                scope.launch { themeManager.setDesignStyle(style) }
                                homeViewModel.closeOverlay()
                            },
                            onClose = { homeViewModel.closeOverlay() }
                        )
                    }

                    MenuOverlay(
                        visible = isSizeConfigOpen,
                        customWallpaperUri = customWallpaperUri,
                        onClose = { homeViewModel.closeOverlay() }
                    ) {
                        SizeConfigMenu(
                            currentFontSize = currentFontSize,
                            onFontSizeSelected = { scope.launch { themeManager.setFontSize(it.scale) } },
                            currentFontWeight = currentFontWeight,
                            onFontWeightSelected = { scope.launch { themeManager.setFontWeight(it.weightValue) } },
                            currentIconSize = currentIconSize,
                            onIconSizeSelected = { scope.launch { themeManager.setIconSize(it.size) } },
                            currentFavoriteSpacing = currentFavoriteSpacing,
                            onFavoriteSpacingSelected = {
                                scope.launch {
                                    themeManager.setFavoriteSpacing(
                                        it.spacing
                                    )
                                }
                            },
                            currentAppFont = currentAppFont,
                            onOpenFontSelection = { homeViewModel.openOverlay(ActiveOverlay.FontSelection) },
                            customWallpaperUri = customWallpaperUri,
                            onClose = { homeViewModel.closeOverlay() }
                        )
                    }

                    MenuOverlay(
                        visible = isFontSelectionOpen,
                        customWallpaperUri = customWallpaperUri,
                        onClose = { homeViewModel.closeOverlay() }
                    ) {
                        FontSelectionMenu(
                            currentAppFont = currentAppFont,
                            onAppFontSelected = { scope.launch { themeManager.setAppFont(it) } },
                            onBack = { homeViewModel.closeOverlay() }
                        )
                    }

                    MenuOverlay(
                        visible = isEditConfigOpen && !isWallpaperCropOpen,
                        customWallpaperUri = customWallpaperUri,
                        onClose = { homeViewModel.closeOverlay() }
                    ) {
                        // A8-Split: Navigation + Einstellungen holt sich das Menü selbst
                        // (HomeViewModel/EditConfigViewModel); hier bleiben nur die Effekte,
                        // die die Activity braucht (ActivityResult-Launcher).
                        val editConfigActions = remember {
                            object : EditConfigActions {
                                override fun openDefaultLauncherPrompt() = requestDefaultLauncher()

                                override fun pickWallpaper() {
                                    wallpaperPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }

                                override fun exportBackup() {
                                    backupExportLauncher.launch(suggestedBackupFileName())
                                }

                                override fun importBackup() {
                                    backupImportLauncher.launch(
                                        arrayOf("application/json", "application/octet-stream")
                                    )
                                }
                            }
                        }
                        EditConfigMenu(actions = editConfigActions)
                    }

                    // B1: Widget-Auswahl – nach dem Edit-Menü komponiert, damit sie darüber
                    // liegt (öffnet aus dessen "Widget hinzufügen"-Eintrag).
                    MenuOverlay(
                        visible = isWidgetPickerOpen,
                        customWallpaperUri = customWallpaperUri,
                        onClose = { homeViewModel.closeOverlay() }
                    ) {
                        WidgetPickerSheet(
                            onWidgetChosen = { startWidgetBind(it) },
                            onClose = { homeViewModel.closeOverlay() }
                        )
                    }

                    // Untermenü der Animationen – nach dem Edit-Menü komponiert, damit es
                    // darüber liegt (das Edit-Menü bleibt geöffnet im Hintergrund).
                    MenuOverlay(
                        visible = isAnimationsConfigOpen,
                        customWallpaperUri = customWallpaperUri,
                        onClose = { homeViewModel.closeOverlay() }
                    ) {
                        AnimationsConfigMenu(
                            isAnimationsEnabled = isAnimationsEnabled,
                            onAnimationsToggled = { scope.launch { themeManager.setAnimationsEnabled(it) } },
                            isAppOpenAnimationEnabled = isAppOpenAnimationEnabled,
                            onAppOpenAnimationToggled = {
                                scope.launch {
                                    themeManager.setAppOpenAnimationEnabled(
                                        it
                                    )
                                }
                            },
                            isAppCloseAnimationEnabled = isAppCloseAnimationEnabled,
                            onAppCloseAnimationToggled = {
                                scope.launch {
                                    themeManager.setAppCloseAnimationEnabled(
                                        it
                                    )
                                }
                            },
                            isMenuAnimationEnabled = isMenuAnimationEnabled,
                            onMenuAnimationToggled = { scope.launch { themeManager.setMenuAnimationEnabled(it) } },
                            isFavoritesAnimationEnabled = isFavoritesAnimationEnabled,
                            onFavoritesAnimationToggled = {
                                scope.launch {
                                    themeManager.setFavoritesAnimationEnabled(
                                        it
                                    )
                                }
                            },
                            animationSpeed = animationSpeed,
                            onAnimationSpeedChanged = { scope.launch { themeManager.setAnimationSpeed(it) } },
                            onClose = { homeViewModel.closeOverlay() }
                        )
                    }

                    MenuOverlay(
                        visible = isEdgeLightingConfigOpen,
                        customWallpaperUri = customWallpaperUri,
                        onClose = { homeViewModel.closeOverlay() }
                    ) {
                        EdgeLightingConfigMenu(
                            isEnabled = isEdgeLightingEnabled,
                            onEnabledChange = { scope.launch { themeManager.setEdgeLightingEnabled(it) } },
                            style = edgeLightingStyle,
                            onStyleChange = { scope.launch { themeManager.setEdgeLightingStyle(it) } },
                            speed = edgeLightingSpeed,
                            onSpeedChange = { scope.launch { themeManager.setEdgeLightingSpeed(it) } },
                            laps = edgeLightingLaps,
                            onLapsChange = { scope.launch { themeManager.setEdgeLightingLaps(it) } },
                            thickness = edgeLightingThickness,
                            onThicknessChange = { scope.launch { themeManager.setEdgeLightingThickness(it) } },
                            onClose = { homeViewModel.closeOverlay() }
                        )
                    }

                    MenuOverlay(
                        visible = isGesturesConfigOpen,
                        customWallpaperUri = customWallpaperUri,
                        onClose = { homeViewModel.closeOverlay() }
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
                            onClose = { homeViewModel.closeOverlay() }
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
                        customWallpaperUri = customWallpaperUri,
                        onClose = { homeViewModel.closeOverlay() }
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
                            onClose = { homeViewModel.closeOverlay() }
                        )
                    }

                    MenuOverlay(
                        visible = isIconConfigOpen,
                        customWallpaperUri = customWallpaperUri,
                        onClose = { homeViewModel.closeOverlay() }
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
                            onClose = { homeViewModel.closeOverlay() },
                            selectedIconPack = selectedIconPack,
                            loadIconPacks = { iconPackRepository.installedIconPacks() },
                            onIconPackSelected = { packPkg ->
                                scope.launch {
                                    iconManager.setSelectedIconPack(packPkg)
                                    iconPackRepository.invalidate()
                                    appRepository.clearIconCache()
                                    refreshAppList()
                                }
                            }
                        )
                    }

                    MenuOverlay(
                        visible = isUninstallAppsOpen,
                        customWallpaperUri = customWallpaperUri,
                        onClose = { homeViewModel.closeOverlay() }
                    ) {
                        UninstallAppsMenu(
                            apps = allApps,
                            onRefreshApps = { pkg -> refreshAppList(pkg) },
                            onClose = { homeViewModel.closeOverlay() }
                        )
                    }

                    MenuOverlay(
                        visible = isHiddenAppsOpen,
                        customWallpaperUri = customWallpaperUri,
                        onClose = { homeViewModel.closeOverlay() }
                    ) {
                        HiddenAppsMenu(
                            apps = allApps,
                            hiddenPackages = hiddenApps,
                            onToggleHidden = { pkg ->
                                val newHidden = if (pkg in hiddenApps) hiddenApps - pkg else hiddenApps + pkg
                                scope.launch { themeManager.setHiddenApps(newHidden) }
                            },
                            onClose = { homeViewModel.closeOverlay() }
                        )
                    }

                    MenuOverlay(
                        visible = isAppLockOpen,
                        customWallpaperUri = customWallpaperUri,
                        onClose = { homeViewModel.closeOverlay() }
                    ) {
                        AppLockMenu(
                            apps = allApps,
                            lockedPackages = lockedApps,
                            lockType = lockType,
                            biometricEnabled = lockBiometricEnabled,
                            onToggleLocked = { pkg ->
                                val newLocked = if (pkg in lockedApps) lockedApps - pkg else lockedApps + pkg
                                scope.launch { themeManager.setLockedApps(newLocked) }
                            },
                            onSetSecret = { type, token ->
                                scope.launch { themeManager.setLockSecret(type, token) }
                            },
                            onClearSecret = {
                                scope.launch { themeManager.clearLockSecret() }
                            },
                            onToggleBiometric = { enabled ->
                                scope.launch { themeManager.setLockBiometricEnabled(enabled) }
                            },
                            onClose = { homeViewModel.closeOverlay() }
                        )
                    }

                    MenuOverlay(
                        visible = isInfoOpen,
                        customWallpaperUri = customWallpaperUri,
                        onClose = { homeViewModel.closeOverlay() }
                    ) {
                        InfoDialog(
                            onClose = { homeViewModel.closeOverlay() }
                        )
                    }

                    if (isSearchOpen && !isSearchLaunching) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        isSearchClosingState = false
                                        homeViewModel.setSearchOpen(false)
                                    }
                                }
                        ) {
                            HybridSearch(
                                apps = allApps,
                                onClose = {
                                    isSearchClosingState = false
                                    homeViewModel.setSearchOpen(false)
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
                            onDismissRequest = { homeViewModel.dismissUsageAccessPrompt() },
                            title = { Text(stringResource(R.string.usage_access_title)) },
                            text = {
                                Text(stringResource(R.string.usage_access_description))
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    homeViewModel.dismissUsageAccessPrompt()
                                    ForegroundAppResolver.openUsageAccessSettings(context)
                                }) {
                                    Text(stringResource(R.string.open))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { homeViewModel.dismissUsageAccessPrompt() }) {
                                    Text(stringResource(R.string.later))
                                }
                            }
                        )
                    }

                    // Erststart-Onboarding als oberste Ebene über dem gesamten UI-Baum.
                    if (!onboardingCompleted) {
                        OnboardingFlow(
                            themeManager = themeManager,
                            apps = visibleApps,
                            favoritePackages = favoritePackages,
                            onFavoritesChange = { newFavorites ->
                                scope.launch { favoritesManager.saveFavorites(newFavorites) }
                            },
                            onRequestDefaultLauncher = { requestDefaultLauncher() },
                            onComplete = {
                                scope.launch { themeManager.setOnboardingCompleted(true) }
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * Führt den nächsten Schritt des Widget-Bind-Flows aus (B1, Entscheidungen:
     * [WidgetBindFlow]). `RequestBindPermission` wird hier bewusst nicht behandelt –
     * der Schritt kann nur direkt nach `afterAllocate` auftreten und braucht den
     * Compose-ActivityResult-Launcher (siehe `startWidgetBind` in setContent).
     */
    private fun handleWidgetBindStep(step: WidgetBindStep) {
        when (step) {
            is WidgetBindStep.RequestBindPermission -> Unit
            is WidgetBindStep.LaunchConfigure -> {
                val started = widgetHostManager.startConfigureActivity(
                    this,
                    step.pending.appWidgetId,
                    REQUEST_CONFIGURE_WIDGET
                )
                if (!started) abortWidgetBind(step.pending.appWidgetId, showError = true)
            }
            is WidgetBindStep.Commit -> commitWidgetBind(step.pending)
            is WidgetBindStep.Abort -> abortWidgetBind(step.appWidgetId)
        }
    }

    /** Persistiert das fertig gebundene Widget und wechselt zum Positionieren in den Edit-Modus. */
    private fun commitWidgetBind(pending: PendingWidgetBind) {
        val size = pendingWidgetSize ?: FALLBACK_WIDGET_SIZE
        pendingWidgetBind = null
        pendingWidgetSize = null
        lifecycleScope.launch {
            widgetHostManager.addWidget(
                HostedWidget(
                    appWidgetId = pending.appWidgetId,
                    provider = pending.provider,
                    widthDp = size.widthDp,
                    heightDp = size.heightDp,
                )
            )
            activityHomeViewModel.setHomeEditMode(true)
        }
    }

    /** Bricht den Bind-Flow ab und gibt die allokierte Widget-ID wieder frei. */
    private fun abortWidgetBind(appWidgetId: Int, showError: Boolean = false) {
        pendingWidgetBind = null
        pendingWidgetSize = null
        widgetHostManager.deleteWidgetId(appWidgetId)
        if (showError) {
            Toast.makeText(this, getString(R.string.widget_bind_failed), Toast.LENGTH_SHORT).show()
        }
    }

    /** Vorschlags-Dateiname für den SAF-Dialog, z. B. `rethrone-backup-20260706.json`. */
    private fun suggestedBackupFileName(): String {
        val date = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
        return "rethrone-backup-$date.json"
    }

    /** B5: schreibt den aktuellen Konfigurations-Snapshot als JSON an die gewählte URI. */
    private fun exportBackupTo(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val snapshot = backupManager.createSnapshot(BuildConfig.VERSION_CODE.toLong())
                val json = BackupSerializer.serialize(snapshot)
                contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("OutputStream konnte nicht geöffnet werden")
            }
            withContext(Dispatchers.Main) {
                val messageRes = if (result.isSuccess) {
                    R.string.backup_export_success
                } else {
                    R.string.backup_export_error
                }
                Toast.makeText(this@MainActivity, getString(messageRes), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** B5: liest und validiert eine Backup-Datei und spielt sie ein (Overwrite-all). */
    private fun importBackupFrom(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val messageRes = runCatching {
                val json = contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes().toString(Charsets.UTF_8)
                } ?: return@runCatching R.string.backup_import_error
                val snapshot = BackupSerializer.parse(json) ?: return@runCatching R.string.backup_import_error
                when (backupManager.restore(snapshot)) {
                    RestoreResult.APPLIED -> R.string.backup_import_success
                    RestoreResult.UNSUPPORTED_VERSION -> R.string.backup_import_unsupported
                }
            }.getOrDefault(R.string.backup_import_error)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, getString(messageRes), Toast.LENGTH_LONG).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURE_WIDGET) {
            val pending = pendingWidgetBind ?: return
            handleWidgetBindStep(WidgetBindFlow.afterConfigure(pending, resultOk = resultCode == RESULT_OK))
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
        // Reine Entscheidung in LauncherLogic (unit-getestet); hier nur der Toast-Seiteneffekt.
        val decision = LauncherLogic.evaluateDefaultLauncherWarning(
            resolvedPackage = resolvedPackage,
            ownPackage = packageName,
            warningAlreadyShown = defaultLauncherWarningShown,
            lastPackage = lastDefaultLauncherPackage,
        )
        if (decision.showWarning) {
            Toast.makeText(this, getString(R.string.default_launcher_warning), Toast.LENGTH_LONG).show()
        }
        defaultLauncherWarningShown = decision.warningShown
        lastDefaultLauncherPackage = decision.lastPackage
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
    customWallpaperUri: String?,
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

    // Verlaufs-Hintergrund identisch zum „Farben"-Menü (ColorConfigMenu.backgroundBrush).
    val overlayTheme = LocalColorTheme.current
    val overlayDarkText = LocalDarkTextEnabled.current
    val overlayBackgroundBrush = remember(overlayTheme, overlayDarkText, overlayTheme.seedRevision()) {
        overlayTheme.backgroundBrush(overlayDarkText, alpha = 0.95f)
    }

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
        ) {
            // Einheitlicher Hintergrund für ALLE Einstellungsmenüs: derselbe Farbverlauf
            // wie im „Farben"-Menü (Wallpaper/Theme-Hintergrund + Theme-Verlaufs-Brush).
            // Das Hero-Motiv (Tulpe) bleibt dem Startbildschirm vorbehalten (showHero = false).
            SystemWallpaperView(customWallpaperUri, showHero = false)
            Box(modifier = Modifier.fillMaxSize().background(overlayBackgroundBrush))

            content()
        }
    }
}
