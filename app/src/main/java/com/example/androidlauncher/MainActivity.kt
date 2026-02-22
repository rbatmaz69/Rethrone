package com.example.androidlauncher

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import com.example.androidlauncher.ui.theme.AndroidLauncherTheme
import com.example.androidlauncher.ui.theme.ColorTheme
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalFontSize
import com.example.androidlauncher.ui.theme.LocalIconSize
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
    // OnBackPressedCallback als Instanzvariable um ihn später zu steuern
    private lateinit var backCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Versuche das Exclude-Flag auch auf dem Start-Intent zu setzen
        intent?.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)

        // OnBackPressedCallback um Back-Gesten auf dem Homescreen zu ignorieren
        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Absichtlich leer: Back-Geste wird ignoriert
                // Dies ist das typische Verhalten eines System-Launchers
            }
        }
        onBackPressedDispatcher.addCallback(this, backCallback)

        // Erzwinge zur Sicherheit das Ausblenden aus dem Recents-Screen
        enforceExcludeFromRecents()

        setContent {
            val context = LocalContext.current
            val themeManager = remember { ThemeManager(context) }
            val folderManager = remember { FolderManager(context) }

            val currentTheme by themeManager.selectedTheme.collectAsState(initial = ColorTheme.LAUNCHER)
            val currentFontSize by themeManager.selectedFontSize.collectAsState(initial = FontSize.STANDARD)
            val currentIconSize by themeManager.selectedIconSize.collectAsState(initial = IconSize.STANDARD)
            val folders by folderManager.folders.collectAsState(initial = emptyList())

            val scope = rememberCoroutineScope()

            AndroidLauncherTheme(
                colorTheme = currentTheme,
                fontSize = currentFontSize,
                iconSize = currentIconSize
            ) {
                var isDrawerOpen by remember { mutableStateOf(false) }
                var isSettingsOpen by remember { mutableStateOf(false) }
                var isFavoritesConfigOpen by remember { mutableStateOf(false) }
                var isColorConfigOpen by remember { mutableStateOf(false) }
                var isSizeConfigOpen by remember { mutableStateOf(false) }
                var selectedFolderForConfig by remember { mutableStateOf<FolderInfo?>(null) }
                
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
                            val bitmap = loadSingleIcon(context, pm, cacheDir, app.packageName)
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

                // Steuere den OnBackPressedCallback basierend auf Modal-State
                // Wenn Menüs offen sind, deaktiviere den Callback, damit BackHandler funktioniert
                LaunchedEffect(isDrawerOpen, isFavoritesConfigOpen, isColorConfigOpen, isSizeConfigOpen, selectedFolderForConfig) {
                    val anyModalOpen = isDrawerOpen || isFavoritesConfigOpen || isColorConfigOpen || isSizeConfigOpen || selectedFolderForConfig != null
                    backCallback.isEnabled = !anyModalOpen
                }

                // BackHandler für das Schließen von offenen Menüs
                BackHandler(enabled = isDrawerOpen || isFavoritesConfigOpen || isColorConfigOpen || isSizeConfigOpen || selectedFolderForConfig != null) {
                    if (selectedFolderForConfig != null) selectedFolderForConfig = null
                    else if (isDrawerOpen) isDrawerOpen = false
                    else if (isFavoritesConfigOpen) isFavoritesConfigOpen = false
                    else if (isColorConfigOpen) isColorConfigOpen = false
                    else if (isSizeConfigOpen) isSizeConfigOpen = false
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    SystemWallpaperView()

                    // Haupt-Inhalt
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
                                onClose = { isDrawerOpen = false }
                            )
                        } else {
                            HomeScreen(
                                favorites = favorites,
                                isSettingsOpen = isSettingsOpen,
                                onOpenDrawer = { isDrawerOpen = true },
                                onToggleSettings = { isSettingsOpen = !isSettingsOpen },
                                onOpenFavoritesConfig = { isFavoritesConfigOpen = true },
                                onOpenColorConfig = { isColorConfigOpen = true },
                                onOpenSizeConfig = { isSizeConfigOpen = true }
                            )
                        }
                    }

                    // Overlay Menüs
                    AnimatedVisibility(
                        visible = isFavoritesConfigOpen,
                        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300, easing = EaseOutCubic)) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300, easing = EaseInCubic)) + fadeOut()
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(currentTheme.drawerBackground)) {
                            FavoritesConfigMenu(
                                apps = allApps,
                                initialFavoritePackages = favoritePackages,
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
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enforceExcludeFromRecents()
    }

    private fun enforceExcludeFromRecents() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.appTasks?.forEach { task ->
                val base = task.taskInfo.baseIntent.component
                if (base != null && base.className == componentName.className) {
                    task.setExcludeFromRecents(true)
                }
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
    } catch (e: Exception) {
        e.printStackTrace()
    }
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
            } catch (e: Exception) { e.printStackTrace() }
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
    onOpenSizeConfig: () -> Unit
) {
    val context = LocalContext.current
    val colorTheme = LocalColorTheme.current
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var launchRequest by remember { mutableStateOf<HomeLaunchRequest?>(null) }
    val rotation by animateFloatAsState(
        targetValue = if (isSettingsOpen) 180f else 0f,
        animationSpec = tween(300, easing = EaseInOutCubic)
    )

    val settingsButtonSize by animateDpAsState(
        targetValue = if (isSettingsOpen) 72.dp else 56.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    LaunchedEffect(launchRequest) {
        val request = launchRequest ?: return@LaunchedEffect
        delay(280)
        context.packageManager.getLaunchIntentForPackage(request.app.packageName)?.let {
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
                ClockHeader()

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    if (favorites.isEmpty()) {
                        val intSrc = remember { MutableInteractionSource() }
                        Surface(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = CircleShape,
                            modifier = Modifier.size(56.dp).bounceClick(intSrc).clickable(
                                interactionSource = intSrc,
                                indication = null
                            ) { onOpenFavoritesConfig() }
                        ) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) }
                        }
                    } else {
                        favorites.forEach { app ->
                            val intSrc = remember { MutableInteractionSource() }
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
                                            launchRequest = HomeLaunchRequest(app, itemBounds.value)
                                        }
                                    }
                            ) {
                                Box(modifier = Modifier.padding(6.dp)) { AppIconView(app) }
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
                    color = Color.White.copy(alpha = if (isSettingsOpen) 0.1f else 0.15f),
                    shape = CircleShape,
                    border = if (isSettingsOpen) BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)) else null
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.rotate(rotation)) {
                        Icon(
                            imageVector = if (isSettingsOpen) Icons.Default.Close else Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(if (isSettingsOpen) 32.dp else 28.dp)
                        )
                    }
                }
            }
        }

        val launchTransition = updateTransition(targetState = launchRequest != null, label = "HomeLaunchTransition")
        val launchProgress by launchTransition.animateFloat(
            transitionSpec = { tween(durationMillis = 280, easing = FastOutSlowInEasing) },
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

private data class HomeLaunchRequest(
    val app: AppInfo,
    val bounds: Rect?
)

@Composable
fun FavoritesConfigMenu(
    apps: List<AppInfo>,
    initialFavoritePackages: List<String>,
    onConfirm: (List<String>) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedPackages by remember { mutableStateOf(initialFavoritePackages) }
    val filteredApps = remember(apps, searchQuery) { LauncherLogic.filterApps(apps, searchQuery) }
    val focusRequester = remember { FocusRequester() }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Favoriten", fontSize = 24.sp, fontWeight = FontWeight.Light, color = Color.White)
                Text("${selectedPackages.size} von 8 ausgewählt", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))
            }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = null, tint = Color.White) }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
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
            if (selectedPackages.isNotEmpty()) {
                item { Text("Reihenfolge", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp) }
                itemsIndexed(selectedPackages) { index, pkg ->
                    apps.find { it.packageName == pkg }?.let { app ->
                        Surface(color = Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${index + 1}.", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, modifier = Modifier.width(24.dp))
                                AppIconView(app)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(app.label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { selectedPackages = LauncherLogic.moveFavoriteUp(selectedPackages, index) }, enabled = index > 0) { 
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = if (index > 0) Color.White else Color.White.copy(alpha = 0.2f)) 
                                }
                                IconButton(onClick = { selectedPackages = LauncherLogic.moveFavoriteDown(selectedPackages, index) }, enabled = index < selectedPackages.size - 1) { 
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = if (index < selectedPackages.size - 1) Color.White else Color.White.copy(alpha = 0.2f)) 
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
            item { Text("Alle Apps", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp) }
            items(filteredApps) { app ->
                val isFav = app.packageName in selectedPackages
                val intSrc = remember { MutableInteractionSource() }
                Surface(color = if (isFav) Color.White.copy(alpha = 0.05f) else Color.Transparent, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().bounceClick(intSrc).clickable(
                    interactionSource = intSrc,
                    indication = null
                ) {
                    val newFavs = LauncherLogic.toggleFavorite(selectedPackages, app.packageName)
                    if (newFavs.size <= LauncherLogic.MAX_FAVORITES) selectedPackages = newFavs else Toast.makeText(context, "Maximal 8 erlaubt", Toast.LENGTH_SHORT).show()
                }) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppIconView(app)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(app.label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Checkbox(checked = isFav, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color.White.copy(alpha = 0.4f), checkmarkColor = Color(0xFF0F172A)))
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.BottomEnd) {
        val intSrc = remember { MutableInteractionSource() }
        FloatingActionButton(
            onClick = { if (selectedPackages.isNotEmpty()) onConfirm(selectedPackages) else Toast.makeText(context, "Keine Auswahl", Toast.LENGTH_SHORT).show() }, 
            containerColor = Color.White, 
            contentColor = Color(0xFF0F172A), 
            shape = CircleShape, 
            modifier = Modifier.bounceClick(intSrc)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
        }
    }
}

@Composable
fun ClockHeader() {
    val context = LocalContext.current
    val fontSize = LocalFontSize.current
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
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = timeFormat.format(currentTime),
            fontSize = 72.sp * fontSize.scale,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-2).sp,
            color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .bounceClick(intSrcTime)
                .clickable(
                    interactionSource = intSrcTime,
                    indication = null
                ) {
                    var started = false
                    try {
                        val intent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory("android.intent.category.APP_CLOCK")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                        started = true
                    } catch (e: Exception) {}

                    if (!started) {
                        try {
                            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            started = true
                        } catch (e: Exception) {}
                    }

                    if (!started) {
                        val packages = listOf(
                            "com.google.android.deskclock",
                            "com.android.deskclock",
                            "com.sec.android.app.clockpackage",
                            "com.huawei.android.clock",
                            "com.miui.clock",
                            "cn.nubia.deskclock.preset", 
                            "cn.nubia.deskclock",
                            "com.zte.deskclock"
                        )
                        for (pkg in packages) {
                            try {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                    started = true
                                    break
                                }
                            } catch (e: Exception) {}
                        }
                    }

                    if (!started) {
                        try {
                            val pm = context.packageManager
                            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                            val clockApp = apps.find { 
                                val label = it.loadLabel(pm).toString().lowercase()
                                val pkg = it.packageName.lowercase()
                                (pkg.contains("clock") || pkg.contains("deskclock") || label.contains("uhr") || label.contains("clock")) && 
                                pm.getLaunchIntentForPackage(it.packageName) != null
                            }
                            clockApp?.let {
                                context.startActivity(pm.getLaunchIntentForPackage(it.packageName))
                                started = true
                            }
                        } catch (e: Exception) {}
                    }

                    if (!started) {
                        Toast.makeText(context, "Uhr-App nicht gefunden", Toast.LENGTH_SHORT).show()
                    }
                }
        )
        Text(
            text = dateFormat.format(currentTime),
            fontSize = 18.sp * fontSize.scale,
            fontWeight = FontWeight.Normal,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .bounceClick(intSrcDate)
                .clickable(
                    interactionSource = intSrcDate,
                    indication = null
                ) {
                    val calendarIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").appendPath(System.currentTimeMillis().toString()).build()
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(calendarIntent)
                    } catch (e: Exception) {
                        val selectorIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(selectorIntent)
                        } catch (e2: Exception) {}
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

private suspend fun loadSingleIcon(context: Context, pm: PackageManager, cacheDir: File, packageName: String): ImageBitmap? {
    val iconFile = File(cacheDir, "$packageName.png")
    if (iconFile.exists()) {
        try {
            val b = BitmapFactory.decodeFile(iconFile.absolutePath)
            if (b != null) {
                val ib = b.asImageBitmap()
                ib.prepareToDraw()
                return ib
            }
        } catch (e: Exception) { }
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
            
            val b = Bitmap.createBitmap(144, 144, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(b)
            foregroundDrawable.setBounds(0, 0, 144, 144)
            foregroundDrawable.draw(canvas)
            
            val ib = b.asImageBitmap()
            ib.prepareToDraw()

            FileOutputStream(iconFile).use { out ->
                b.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            ib
        } catch (e: Exception) { null }
    }
}

private fun getSavedFavorites(context: Context): List<String> {
    val prefs = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
    val favoritesString = prefs.getString("favorites_list", "") ?: ""
    return if (favoritesString.isEmpty()) emptyList() else favoritesString.split(",")
}

private fun saveFavorites(context: Context, favorites: List<String>) {
    val prefs = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("favorites_list", favorites.joinToString(",")).apply()
}
