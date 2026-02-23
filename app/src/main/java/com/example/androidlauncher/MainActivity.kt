package com.example.androidlauncher

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.edit
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.androidlauncher.ui.theme.AndroidLauncherTheme
import com.example.androidlauncher.ui.theme.ColorTheme
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalFontSize
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalShowFavoriteLabels
import com.example.androidlauncher.data.ThemeManager
import com.example.androidlauncher.data.FontSize
import com.example.androidlauncher.data.IconSize
import com.example.androidlauncher.data.FolderInfo
import com.example.androidlauncher.data.FolderManager
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.ui.ColorConfigMenu
import com.example.androidlauncher.ui.SettingsPaletteMenu
import com.example.androidlauncher.ui.SizeConfigMenu
import com.example.androidlauncher.ui.AppDrawer
import com.example.androidlauncher.ui.AppIconView
import com.example.androidlauncher.ui.bounceClick
import com.example.androidlauncher.ui.FolderConfigMenu
import com.example.androidlauncher.ui.launchAppNoTransition
import com.example.androidlauncher.ui.ReturnAnimationOverlay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

class MainActivity : ComponentActivity() {
    private lateinit var backCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        intent?.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)

        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        }
        onBackPressedDispatcher.addCallback(this, backCallback)

        enforceExcludeFromRecents()

        setContent {
            val context = LocalContext.current
            val themeManager = remember { ThemeManager(context) }
            val folderManager = remember { FolderManager(context) }

            val currentTheme by themeManager.selectedTheme.collectAsState(initial = ColorTheme.LAUNCHER)
            val currentFontSize by themeManager.selectedFontSize.collectAsState(initial = FontSize.STANDARD)
            val currentIconSize by themeManager.selectedIconSize.collectAsState(initial = IconSize.STANDARD)
            val isDarkTextEnabled by themeManager.isDarkTextEnabled.collectAsState(initial = false)
            val showFavoriteLabels by themeManager.showFavoriteLabels.collectAsState(initial = false)
            val folders by folderManager.folders.collectAsState(initial = emptyList())

            val scope = rememberCoroutineScope()

            AndroidLauncherTheme(
                colorTheme = currentTheme,
                fontSize = currentFontSize,
                iconSize = currentIconSize,
                darkTextEnabled = isDarkTextEnabled,
                showFavoriteLabels = showFavoriteLabels
            ) {
                val lifecycleOwner = LocalLifecycleOwner.current
                var rootSize by remember { mutableStateOf(IntSize.Zero) }
                var pendingReturnAnimation by remember { mutableStateOf<ReturnAnimation?>(null) }
                var activeReturnAnimation by remember { mutableStateOf<ReturnAnimation?>(null) }
                var returnIconPackage by remember { mutableStateOf<String?>(null) }
                var isDrawerOpen by remember { mutableStateOf(false) }
                var isSettingsOpen by remember { mutableStateOf(false) }
                var isFavoritesConfigOpen by remember { mutableStateOf(false) }
                var isColorConfigOpen by remember { mutableStateOf(false) }
                var isSizeConfigOpen by remember { mutableStateOf(false) }
                var selectedFolderForConfig by remember { mutableStateOf<FolderInfo?>(null) }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            pendingReturnAnimation?.let {
                                isDrawerOpen = it.source == LaunchSource.DRAWER
                                if (!isDrawerOpen) {
                                    isSettingsOpen = false
                                    isFavoritesConfigOpen = false
                                    isColorConfigOpen = false
                                    isSizeConfigOpen = false
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

                LaunchedEffect(returnIconPackage) {
                    if (returnIconPackage != null) {
                        delay(300)
                        returnIconPackage = null
                    }
                }

                val allApps = remember { mutableStateListOf<AppInfo>() }
                var favoritePackages by remember { mutableStateOf(getSavedFavorites(context)) }

                val favorites = remember(allApps.toList(), favoritePackages) {
                    LauncherLogic.getFavoriteApps(allApps.toList(), favoritePackages)
                }

                LaunchedEffect(Unit) {
                    val basicList = withContext(Dispatchers.IO) { getAppListBasic(context) }
                    allApps.clear()
                    allApps.addAll(basicList)

                    withContext(Dispatchers.IO) {
                        val pm = context.packageManager
                        val cacheDir = File(context.cacheDir, "app_icons")
                        if (!cacheDir.exists()) cacheDir.mkdirs()

                        val favSet = favoritePackages.toSet()
                        val sortedIndices = allApps.indices.sortedByDescending { allApps[it].packageName in favSet }

                        sortedIndices.forEachIndexed { loopIdx, appIdx ->
                            val app = allApps[appIdx]
                            val bitmap = loadSingleIcon(pm, cacheDir, app.packageName)
                            if (bitmap != null) {
                                withContext(Dispatchers.Main) {
                                    allApps[appIdx] = app.copy(iconBitmap = bitmap)
                                }
                            }
                            if (loopIdx % 5 == 0) delay(1)
                        }
                    }
                }

                LaunchedEffect(isDrawerOpen) {
                    if (isDrawerOpen) {
                        isSettingsOpen = false
                        isFavoritesConfigOpen = false
                        isColorConfigOpen = false
                        isSizeConfigOpen = false
                        selectedFolderForConfig = null
                    }
                }

                LaunchedEffect(isDrawerOpen, isFavoritesConfigOpen, isColorConfigOpen, isSizeConfigOpen, selectedFolderForConfig) {
                    val anyModalOpen = isDrawerOpen || isFavoritesConfigOpen || isColorConfigOpen || isSizeConfigOpen || selectedFolderForConfig != null
                    backCallback.isEnabled = !anyModalOpen
                }

                BackHandler(enabled = isDrawerOpen || isFavoritesConfigOpen || isColorConfigOpen || isSizeConfigOpen || selectedFolderForConfig != null) {
                    if (selectedFolderForConfig != null) selectedFolderForConfig = null
                    else if (isDrawerOpen) isDrawerOpen = false
                    else if (isFavoritesConfigOpen) isFavoritesConfigOpen = false
                    else if (isColorConfigOpen) isColorConfigOpen = false
                    else if (isSizeConfigOpen) isSizeConfigOpen = false
                }

                Box(
                    modifier = Modifier.fillMaxSize()
                        .onGloballyPositioned { rootSize = it.size }
                ) {
                    SystemWallpaperView()

                    AnimatedContent(
                         targetState = isDrawerOpen,
                         transitionSpec = {
                             if (targetState) {
                                 (slideInVertically(initialOffsetY = { it }, animationSpec = tween(300, easing = EaseOutCubic)) + fadeIn(animationSpec = tween(200)))
                                     .togetherWith(fadeOut(animationSpec = tween(200)))
                             } else {
                                 fadeIn(animationSpec = tween(200))
                                     .togetherWith(slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300, easing = EaseInCubic)) + fadeOut(animationSpec = tween(200)))
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
                                         saveFavorites(context, newFavs)
                                         favoritePackages = newFavs
                                     }
                                 },
                                 isFavorite = { pkg -> pkg in favoritePackages },
                                 onUpdateFolders = { newFolders ->
                                     scope.launch { folderManager.saveFolders(newFolders) }
                                 },
                                 onOpenFolderConfig = { folder ->
                                     selectedFolderForConfig = folder
                                 },
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
                                onOpenDrawer = { isDrawerOpen = true },
                                onToggleSettings = { isSettingsOpen = !isSettingsOpen },
                                onOpenFavoritesConfig = { isFavoritesConfigOpen = true },
                                onOpenColorConfig = { isColorConfigOpen = true },
                                onOpenSizeConfig = { isSizeConfigOpen = true },
                                onAppLaunchForReturn = { pkg, bounds ->
                                    pendingReturnAnimation = ReturnAnimation(bounds, LaunchSource.HOME, pkg)
                                },
                                returnIconPackage = returnIconPackage
                            )
                        }
                     }

                    AnimatedVisibility(
                        visible = isFavoritesConfigOpen,
                        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300, easing = EaseOutCubic)) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300, easing = EaseInCubic)) + fadeOut()
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(currentTheme.drawerBackground)) {
                            FavoritesConfigMenu(
                                apps = allApps,
                                initialFavoritePackages = favoritePackages,
                                showFavoriteLabels = showFavoriteLabels,
                                onShowLabelsToggled = { show ->
                                    scope.launch { themeManager.setShowFavoriteLabels(show) }
                                },
                                onConfirm = { newFavs ->
                                    saveFavorites(context, newFavs)
                                    favoritePackages = newFavs
                                    isFavoritesConfigOpen = false
                                },
                                onClose = { isFavoritesConfigOpen = false }
                            )
                        }
                    }

                    AnimatedVisibility(
                         visible = selectedFolderForConfig != null,
                         enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300, easing = EaseOutCubic)) + fadeIn(),
                         exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300, easing = EaseInCubic)) + fadeOut()
                     ) {
                         selectedFolderForConfig?.let { folder ->
                             Box(modifier = Modifier.fillMaxSize().background(currentTheme.drawerBackground)) {
                                 FolderConfigMenu(
                                     folder = folder,
                                     allApps = allApps,
                                     onConfirm = { updatedFolder ->
                                         val newFolders = folders.map { if (it.id == updatedFolder.id) updatedFolder else it }
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
                     }

                    AnimatedVisibility(
                         visible = isColorConfigOpen,
                         enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300, easing = EaseOutCubic)) + fadeIn(),
                         exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300, easing = EaseInCubic)) + fadeOut()
                     ) {
                         Box(modifier = Modifier.fillMaxSize().background(currentTheme.drawerBackground)) {
                             ColorConfigMenu(
                                 selectedTheme = currentTheme,
                                 onThemeSelected = { theme ->
                                     scope.launch { themeManager.setTheme(theme) }
                                 },
                                 isDarkTextEnabled = isDarkTextEnabled,
                                 onDarkTextToggled = { enabled ->
                                     scope.launch { themeManager.setDarkTextEnabled(enabled) }
                                 },
                                 onClose = { isColorConfigOpen = false }
                             )
                         }
                     }

                    AnimatedVisibility(
                         visible = isSizeConfigOpen,
                         enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300, easing = EaseOutCubic)) + fadeIn(),
                         exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300, easing = EaseInCubic)) + fadeOut()
                     ) {
                         Box(modifier = Modifier.fillMaxSize().background(currentTheme.drawerBackground)) {
                             SizeConfigMenu(
                                 currentFontSize = currentFontSize,
                                 onFontSizeSelected = { size ->
                                     scope.launch { themeManager.setFontSize(size) }
                                 },
                                 currentIconSize = currentIconSize,
                                 onIconSizeSelected = { size ->
                                     scope.launch { themeManager.setIconSize(size) }
                                 },
                                 onClose = { isSizeConfigOpen = false }
                             )
                         }
                     }

                    activeReturnAnimation?.let { animation ->
                        ReturnAnimationOverlay(
                            bounds = animation.bounds,
                            rootSize = rootSize,
                            background = currentTheme.drawerBackground,
                            onFinished = { activeReturnAnimation = null },
                            targetScale = if (animation.source == LaunchSource.DRAWER) 0.65f else 0.7f
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enforceExcludeFromRecents()
    }

    private fun enforceExcludeFromRecents() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        am.appTasks?.forEach { task ->
            val base = task.taskInfo.baseIntent.component
            if (base != null && base.className == componentName.className) {
                task.setExcludeFromRecents(true)
            }
        }
    }
}

@SuppressLint("WrongConstant")
private fun expandNotifications(context: Context) {
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val method = statusBarManager.getMethod("expandNotificationsPanel")
        method.invoke(statusBarService)
    } catch (_: Exception) {}
}

@SuppressLint("MissingPermission")
@Composable
fun SystemWallpaperView() {
    val context = LocalContext.current
    val colorTheme = LocalColorTheme.current
    val wallpaperManager = WallpaperManager.getInstance(context)
    var wallpaperBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        delay(300)
        withContext(Dispatchers.IO) {
            try {
                val drawable = wallpaperManager.drawable
                drawable?.let { wallpaperBitmap = it.toBitmap().asImageBitmap() }
            } catch (_: Exception) {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        wallpaperBitmap?.let {
            Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } ?: Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colorTheme.primary, colorTheme.secondary))))
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
    }
}

@Composable
fun HomeScreen(
    favorites: List<AppInfo>,
    isSettingsOpen: Boolean,
    onOpenDrawer: () -> Unit,
    onToggleSettings: () -> Unit,
    onOpenFavoritesConfig: () -> Unit,
    onOpenColorConfig: () -> Unit,
    onOpenSizeConfig: () -> Unit,
    onAppLaunchForReturn: (String, Rect?) -> Unit,
    returnIconPackage: String?
) {
    val context = LocalContext.current
    val colorTheme = LocalColorTheme.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val showLabels = LocalShowFavoriteLabels.current
    val fontSize = LocalFontSize.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var launchRequest by remember { mutableStateOf<HomeLaunchRequest?>(null) }
    val rotation by animateFloatAsState(
        targetValue = if (isSettingsOpen) 180f else 0f,
        animationSpec = tween(300, easing = EaseInOutCubic), label = ""
    )

    val settingsButtonSize by animateDpAsState(
        targetValue = if (isSettingsOpen) 72.dp else 56.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = ""
    )

    LaunchedEffect(launchRequest) {
        val request = launchRequest ?: return@LaunchedEffect
        delay(280)
        request.intent?.let {
            launchAppNoTransition(context, it)
        }
        launchRequest = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen")
            .onGloballyPositioned { rootSize = it.size }
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -50) onOpenDrawer()
                    else if (dragAmount > 50) expandNotifications(context)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(64.dp))
                ClockHeader(onAppLaunchForReturn = onAppLaunchForReturn, onLaunchRequest = { launchRequest = it }, returnIconPackage = returnIconPackage)

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    if (favorites.isEmpty()) {
                        val intSrc = remember { MutableInteractionSource() }
                        Surface(
                            color = mainTextColor.copy(alpha = 0.1f),
                            shape = CircleShape,
                            modifier = Modifier.size(56.dp).bounceClick(intSrc).clickable(
                                interactionSource = intSrc,
                                indication = null
                            ) { onOpenFavoritesConfig() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = mainTextColor)
                            }
                        }
                    } else {
                        favorites.forEach { app ->
                            val intSrc = remember { MutableInteractionSource() }
                            val bounceScale by animateFloatAsState(
                                targetValue = if (returnIconPackage == app.packageName) 1.12f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "HomeReturnBounce"
                            )
                            val itemBounds = remember(app.packageName) { mutableStateOf<Rect?>(null) }
                            Surface(
                                color = Color.Transparent,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .onGloballyPositioned { coordinates ->
                                        itemBounds.value = coordinates.boundsInRoot()
                                    }
                                    .bounceClick(intSrc)
                                    .clickable(
                                        interactionSource = intSrc,
                                        indication = null
                                    ) {
                                        if (launchRequest == null) {
                                            onAppLaunchForReturn(app.packageName, itemBounds.value)
                                            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                            launchRequest = HomeLaunchRequest(app.packageName, itemBounds.value, intent)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(modifier = Modifier.graphicsLayer {
                                        scaleX = bounceScale
                                        scaleY = bounceScale
                                    }) {
                                        AppIconView(app)
                                    }
                                    if (showLabels) {
                                        Text(
                                            text = app.label,
                                            color = mainTextColor,
                                            fontSize = 18.sp * fontSize.scale,
                                            fontWeight = FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            SettingsPaletteMenu(
                isSettingsOpen = isSettingsOpen,
                onToggleSettings = onToggleSettings,
                onOpenFavoritesConfig = onOpenFavoritesConfig,
                onOpenColorConfig = onOpenColorConfig,
                onOpenSizeConfig = onOpenSizeConfig,
                onOpenSystemSettings = { context.startActivity(Intent(Settings.ACTION_SETTINGS)) },
                onOpenInfo = { /* Action */ }
            )

            Box(modifier = Modifier.fillMaxSize().navigationBarsPadding(), contentAlignment = Alignment.BottomEnd) {
                val intSrc = remember { MutableInteractionSource() }
                Surface(
                    modifier = Modifier.padding(8.dp).size(settingsButtonSize).clip(CircleShape).bounceClick(intSrc).clickable(
                        interactionSource = intSrc,
                        indication = null
                    ) { onToggleSettings() },
                    color = mainTextColor.copy(alpha = if (isSettingsOpen) 0.1f else 0.15f),
                    shape = CircleShape,
                    border = if (isSettingsOpen) BorderStroke(1.dp, mainTextColor.copy(alpha = 0.2f)) else null
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.rotate(rotation)) {
                        Icon(
                            imageVector = if (isSettingsOpen) Icons.Default.Close else Icons.Default.Settings,
                            contentDescription = null,
                            tint = mainTextColor,
                            modifier = Modifier.size(if (isSettingsOpen) 32.dp else 28.dp)
                        )
                    }
                }
            }
        }

        val launchTransition = updateTransition(targetState = launchRequest != null, label = "HomeLaunchTransition")
        val launchProgress by launchTransition.animateFloat(
            transitionSpec = { spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium) },
            label = "HomeLaunchProgress"
        ) { isVisible ->
            if (isVisible) 1f else 0f
        }
        val launchOverlayAlpha by launchTransition.animateFloat(
            transitionSpec = { tween(durationMillis = 220, easing = LinearEasing) },
            label = "HomeLaunchOverlayAlpha"
        ) { isVisible ->
            if (isVisible) 1f else 0f
        }
        val launchBounds = launchRequest?.bounds
        val launchTranslation = remember(launchBounds, rootSize) {
            if (launchBounds != null && rootSize.width > 0 && rootSize.height > 0) {
                val centerX = rootSize.width / 2f
                val centerY = rootSize.height / 2f
                Offset(launchBounds.center.x - centerX, launchBounds.center.y - centerY)
            } else {
                Offset.Zero
            }
        }
        val launchStartScale = remember(launchBounds, rootSize) {
            if (launchBounds != null && rootSize.width > 0 && rootSize.height > 0) {
                val wScale = launchBounds.width / rootSize.width.toFloat()
                val hScale = launchBounds.height / rootSize.height.toFloat()
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
    }
}

data class HomeLaunchRequest(
    val packageName: String,
    val bounds: Rect?,
    val intent: Intent?
)

@Composable
fun FavoritesConfigMenu(
    apps: List<AppInfo>,
    initialFavoritePackages: List<String>,
    showFavoriteLabels: Boolean,
    onShowLabelsToggled: (Boolean) -> Unit,
    onConfirm: (List<String>) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    
    // Einheitliche Textfarbe für "App-Titel" (Grau wie im White Mode)
    val labelTextColor = Color.White.copy(alpha = 0.6f)
    
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val grayTone = if (isDarkTextEnabled) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f)

    var searchQuery by remember { mutableStateOf("") }
    var selectedPackages by remember { mutableStateOf(initialFavoritePackages) }
    val filteredApps = remember(apps, searchQuery) { LauncherLogic.filterApps(apps, searchQuery) }
    val focusRequester = remember { FocusRequester() }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(stringResource(R.string.favorites_title), fontSize = 24.sp, fontWeight = FontWeight.Light, color = mainTextColor)
                Text(stringResource(R.string.favorites_count, selectedPackages.size, LauncherLogic.MAX_FAVORITES), fontSize = 14.sp, color = grayTone)
            }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = null, tint = mainTextColor) }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // Option Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("App-Titel", color = labelTextColor, fontSize = 14.sp)
            
            // Toggle Logik: 
            // White Mode: Kreis Schwarz, Symbole Weiß
            // Dark Mode: Kreis Weiß, Symbole Schwarz
            val thumbColor = if (isDarkTextEnabled) Color.Black else Color.White
            val symbolColor = if (isDarkTextEnabled) Color.White else Color.Black
            
            Switch(
                checked = showFavoriteLabels,
                onCheckedChange = onShowLabelsToggled,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Color.White.copy(alpha = 0.2f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.2f),
                    checkedThumbColor = thumbColor,
                    uncheckedThumbColor = thumbColor,
                    checkedBorderColor = Color.White.copy(alpha = 0.1f),
                    uncheckedBorderColor = Color.White.copy(alpha = 0.1f)
                ),
                thumbContent = {
                    Box(contentAlignment = Alignment.Center) {
                        if (showFavoriteLabels) {
                            // Symbol für AN (I)
                            Box(
                                modifier = Modifier
                                    .size(width = 1.5.dp, height = 12.dp)
                                    .background(symbolColor, RoundedCornerShape(0.5.dp))
                            )
                        } else {
                            // Symbol für AUS (0)
                            Surface(
                                modifier = Modifier.size(width = 8.dp, height = 12.dp),
                                color = Color.Transparent,
                                border = BorderStroke(1.5.dp, symbolColor),
                                shape = RoundedCornerShape(4.dp)
                            ) {}
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        val searchIntSrc = remember { MutableInteractionSource() }
        Box(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp).clickable(
            interactionSource = searchIntSrc,
            indication = null
        ) { focusRequester.requestFocus() }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = grayTone, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    textStyle = androidx.compose.ui.text.TextStyle(color = mainTextColor, fontSize = 15.sp),
                    cursorBrush = SolidColor(mainTextColor),
                    singleLine = true,
                    decorationBox = { if (searchQuery.isEmpty()) Text(stringResource(R.string.search_apps), color = grayTone, fontSize = 15.sp); it() }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 150.dp)) {
            if (selectedPackages.isNotEmpty()) {
                item { Text(stringResource(R.string.order_label), color = grayTone, fontSize = 12.sp) }
                itemsIndexed(selectedPackages) { index, pkg ->
                    apps.find { it.packageName == pkg }?.let { app ->
                        Surface(color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${index + 1}.", color = mainTextColor, fontSize = 14.sp, modifier = Modifier.width(24.dp))
                                AppIconView(app)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(app.label, color = mainTextColor, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { selectedPackages = LauncherLogic.moveFavoriteUp(selectedPackages, index) }, enabled = index > 0) { 
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = if (index > 0) grayTone else mainTextColor) 
                                }
                                IconButton(onClick = { selectedPackages = LauncherLogic.moveFavoriteDown(selectedPackages, index) }, enabled = index < selectedPackages.size - 1) { 
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = if (index < selectedPackages.size - 1) grayTone else mainTextColor) 
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
            item { Text(stringResource(R.string.all_apps_label), color = grayTone, fontSize = 12.sp) }
            items(filteredApps) { app ->
                val isFav = app.packageName in selectedPackages
                val intSrc = remember { MutableInteractionSource() }
                Surface(color = if (isFav) Color.White.copy(alpha = 0.05f) else Color.Transparent, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().bounceClick(intSrc).clickable(
                    interactionSource = intSrc,
                    indication = null
                ) {
                    val newFavs = LauncherLogic.toggleFavorite(selectedPackages, app.packageName)
                    if (newFavs.size <= LauncherLogic.MAX_FAVORITES) selectedPackages = newFavs else Toast.makeText(context, context.getString(R.string.max_favorites_reached), Toast.LENGTH_SHORT).show()
                }) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppIconView(app)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(app.label, color = mainTextColor, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Checkbox(
                            checked = isFav, 
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

    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.BottomEnd) {
        val intSrc = remember { MutableInteractionSource() }
        val checkmarkColor = if (isDarkTextEnabled) Color.White else Color(0xFF0F172A)
        
        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer(alpha = 0.99f)
                .drawBehind {
                    drawCircle(
                        color = mainTextColor,
                        radius = (size.minDimension / 2.0f) - 2.0f,
                        center = center
                    )
                }
                .clickable(
                    interactionSource = intSrc,
                    indication = null,
                    onClick = { if (selectedPackages.isNotEmpty()) onConfirm(selectedPackages) else Toast.makeText(context, context.getString(R.string.no_selection), Toast.LENGTH_SHORT).show() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = checkmarkColor, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun ClockHeader(
    onAppLaunchForReturn: (String, Rect?) -> Unit,
    onLaunchRequest: (HomeLaunchRequest) -> Unit,
    returnIconPackage: String?
) {
    val context = LocalContext.current
    val fontSize = LocalFontSize.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White

    var currentTime by remember { mutableStateOf(Calendar.getInstance().time) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance().time
            delay(1000)
        }
    }
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEEE, d. MMMM", Locale.getDefault())
    val intSrcTime = remember { MutableInteractionSource() }
    val intSrcDate = remember { MutableInteractionSource() }
    val clockBounds = remember { mutableStateOf<Rect?>(null) }
    val calendarBounds = remember { mutableStateOf<Rect?>(null) }

    // Identifizieren der Pakete für die Return-Animation
    var clockPackage by remember { mutableStateOf<String?>(null) }
    var calendarPackage by remember { mutableStateOf<String?>(null) }

    val bounceScaleTime by animateFloatAsState(
        targetValue = if (returnIconPackage != null && returnIconPackage == clockPackage) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "ClockReturnBounce"
    )
    val bounceScaleDate by animateFloatAsState(
        targetValue = if (returnIconPackage != null && returnIconPackage == calendarPackage) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "CalendarReturnBounce"
    )
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = timeFormat.format(currentTime),
            fontSize = 72.sp * fontSize.scale,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-2).sp,
            color = mainTextColor,
            modifier = Modifier
                .onGloballyPositioned { clockBounds.value = it.boundsInRoot() }
                .graphicsLayer {
                    scaleX = bounceScaleTime
                    scaleY = bounceScaleTime
                }
                .clip(RoundedCornerShape(8.dp))
                .bounceClick(intSrcTime)
                .clickable(
                    interactionSource = intSrcTime,
                    indication = null
                ) {
                    val pm = context.packageManager
                    var foundPkg: String? = null

                    val packages = listOf(
                        "cn.nubia.deskclock.preset",
                        "cn.nubia.deskclock",
                        "cn.nubia.clock",
                        "com.android.deskclock",
                        "com.google.android.deskclock",
                        "com.sec.android.app.clockpackage",
                        "com.huawei.android.clock",
                        "com.miui.clock",
                        "com.zte.deskclock",
                        "com.android.clock"
                    )

                    for (pkg in packages) {
                        try {
                            if (pm.getLaunchIntentForPackage(pkg) != null) {
                                foundPkg = pkg
                                break
                            }
                        } catch (_: Exception) {}
                    }

                    if (foundPkg == null) {
                        try {
                            val stdIntent = Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.APP_CLOCK")
                            val res = pm.resolveActivity(stdIntent, 0)
                            if (res != null) {
                                foundPkg = res.activityInfo.packageName
                            }
                        } catch (_: Exception) {}
                    }

                    if (foundPkg == null) {
                        try {
                            val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                            val res = pm.resolveActivity(alarmIntent, 0)
                            if (res != null) {
                                foundPkg = res.activityInfo.packageName
                            }
                        } catch (_: Exception) {}
                    }

                    if (foundPkg == null) {
                        try {
                            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                            val foundApp = apps.find {
                                val pkg = it.packageName.lowercase()
                                (pkg.contains("deskclock") || pkg.contains("uhr") || (pkg.contains("clock") && !pkg.contains("widget"))) &&
                                    pm.getLaunchIntentForPackage(it.packageName) != null
                            }
                            foundPkg = foundApp?.packageName
                        } catch (_: Exception) {}
                    }

                    if (foundPkg == null) {
                        try {
                            val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                            val clockApp = apps.find {
                                val labelText = it.loadLabel(pm).toString().lowercase()
                                val appPackageName = it.activityInfo.packageName.lowercase()
                                (appPackageName.contains("clock") || appPackageName.contains("deskclock") || labelText.contains("uhr") || labelText.contains("clock")) &&
                                    pm.getLaunchIntentForPackage(it.activityInfo.packageName) != null
                            }
                            foundPkg = clockApp?.activityInfo?.packageName
                        } catch (_: Exception) {}
                    }

                    if (foundPkg != null) {
                        clockPackage = foundPkg
                        val launchIntent = pm.getLaunchIntentForPackage(foundPkg)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                            onAppLaunchForReturn(foundPkg, clockBounds.value)
                            onLaunchRequest(HomeLaunchRequest(foundPkg, clockBounds.value, launchIntent))
                            return@clickable
                        }
                    }

                    try {
                        val intent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory("android.intent.category.APP_CLOCK")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "Uhr-App nicht gefunden", Toast.LENGTH_SHORT).show()
                    }
                }
        )
        Text(
            text = dateFormat.format(currentTime),
            fontSize = 18.sp * fontSize.scale,
            fontWeight = FontWeight.Normal,
            color = mainTextColor.copy(alpha = 0.7f),
            modifier = Modifier
                .onGloballyPositioned { calendarBounds.value = it.boundsInRoot() }
                .graphicsLayer {
                    scaleX = bounceScaleDate
                    scaleY = bounceScaleDate
                }
                .clip(RoundedCornerShape(8.dp))
                .bounceClick(intSrcDate)
                .clickable(
                    interactionSource = intSrcDate,
                    indication = null
                ) {
                    val pm = context.packageManager
                    var calendarIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").appendPath(System.currentTimeMillis().toString()).build()
                    }

                    val res = pm.resolveActivity(calendarIntent, 0)
                    if (res == null) {
                        calendarIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
                    }

                    val foundPkg = pm.resolveActivity(calendarIntent, 0)?.activityInfo?.packageName
                    if (foundPkg != null) {
                        calendarPackage = foundPkg
                        val launchIntent = pm.getLaunchIntentForPackage(foundPkg)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                            onAppLaunchForReturn(foundPkg, calendarBounds.value)
                            onLaunchRequest(HomeLaunchRequest(foundPkg, calendarBounds.value, launchIntent))
                        } else {
                            calendarIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                            onAppLaunchForReturn(foundPkg, calendarBounds.value)
                            onLaunchRequest(HomeLaunchRequest(foundPkg, calendarBounds.value, calendarIntent))
                        }
                        return@clickable
                    }

                    try {
                        calendarIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(calendarIntent)
                    } catch (_: Exception) {
                        val selectorIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(selectorIntent)
                        } catch (_: Exception) {}
                    }
                }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

private fun getAppListBasic(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    return pm.queryIntentActivities(intent, 0).map { resolveInfo ->
        AppInfo(
            label = resolveInfo.loadLabel(pm).toString(),
            packageName = resolveInfo.activityInfo.packageName
        )
    }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
}

private suspend fun loadSingleIcon(pm: PackageManager, cacheDir: File, packageName: String): ImageBitmap? {
    val iconFile = File(cacheDir, "$packageName.png")
    if (iconFile.exists()) {
        try {
            val b = BitmapFactory.decodeFile(iconFile.absolutePath)
            if (b != null) {
                val ib = b.asImageBitmap()
                ib.prepareToDraw()
                return ib
            }
        } catch (_: Exception) { }
    }
    return withContext(Dispatchers.IO) {
        try {
            val info = pm.getApplicationInfo(packageName, 0)
            val icon = info.loadIcon(pm)
            val foregroundDrawable = if (icon is AdaptiveIconDrawable) {
                icon.foreground ?: icon
            } else {
                icon
            }
            
            val b = createBitmap(144, 144)
            b.applyCanvas {
                foregroundDrawable.setBounds(0, 0, 144, 144)
                foregroundDrawable.draw(this)
            }
            
            val ib = b.asImageBitmap()
            ib.prepareToDraw()

            FileOutputStream(iconFile).use { out ->
                b.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            ib
        } catch (_: Exception) { null }
    }
}

private fun getSavedFavorites(context: Context): List<String> {
    val prefs = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
    val favoritesString = prefs.getString("favorites_list", "") ?: ""
    return if (favoritesString.isEmpty()) emptyList() else favoritesString.split(",")
}

private fun saveFavorites(context: Context, favorites: List<String>) {
    val prefs = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
    prefs.edit { putString("favorites_list", favorites.joinToString(",")) }
}
