package com.example.androidlauncher

import android.annotation.SuppressLint
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
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.androidlauncher.ui.theme.AndroidLauncherTheme
import com.example.androidlauncher.ui.theme.ColorTheme
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.data.ThemeManager
import com.example.androidlauncher.ui.ColorConfigMenu
import com.example.androidlauncher.ui.SettingsPaletteMenu
import com.composables.icons.lucide.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Stable
data class AppInfo(
    val label: String,
    val packageName: String,
    val iconBitmap: ImageBitmap? = null,
    val lucideIcon: ImageVector? = null,
    val customIconResId: Int? = null
) {
    // Übergangskonstruktor für bestehende Aufrufer, die noch ein Drawable übergeben
    constructor(label: String, packageName: String, iconDrawable: Drawable?) : this(
        label = label,
        packageName = packageName,
        iconBitmap = iconDrawable?.toBitmap()?.asImageBitmap(),
        lucideIcon = null,
        customIconResId = null
    )
}

// Verbesserter bounceClick Modifier
fun Modifier.bounceClick(interactionSource: MutableInteractionSource) = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f, // Deutlicher auf 0.90f
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium // Schneller reagieren
        ),
        label = "bounceScale"
    )
    this.scale(scale)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val themeManager = remember { ThemeManager(context) }
            val currentTheme by themeManager.selectedTheme.collectAsState(initial = ColorTheme.LAUNCHER)
            val scope = rememberCoroutineScope()

            AndroidLauncherTheme(colorTheme = currentTheme) {
                var isDrawerOpen by remember { mutableStateOf(false) }
                var isSettingsOpen by remember { mutableStateOf(false) }
                var isFavoritesConfigOpen by remember { mutableStateOf(false) }
                var isColorConfigOpen by remember { mutableStateOf(false) }
                
                val allApps = remember { mutableStateListOf<AppInfo>() }
                var favoritePackages by remember { mutableStateOf(getSavedFavorites(context)) }
                
                val favorites = remember(allApps.toList(), favoritePackages) {
                    LauncherLogic.getFavoriteApps(allApps.toList(), favoritePackages)
                }

                LaunchedEffect(Unit) {
                    val basicList = withContext(Dispatchers.IO) { getAppListBasic(context) }
                    allApps.clear()
                    allApps.addAll(basicList)

                    // Take immutable snapshots of Compose state on the main thread
                    val appsSnapshot = allApps.toList()
                    val favSet = favoritePackages.toSet()

                    withContext(Dispatchers.IO) {
                        val pm = context.packageManager
                        val cacheDir = File(context.cacheDir, "app_icons")
                        if (!cacheDir.exists()) cacheDir.mkdirs()

                        val sortedIndices = appsSnapshot.indices
                            .sortedByDescending { appsSnapshot[it].packageName in favSet }

                        sortedIndices.forEachIndexed { loopIdx, appIdx ->
                            val app = appsSnapshot[appIdx]
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
                    }
                }

                BackHandler(enabled = isDrawerOpen || isSettingsOpen || isFavoritesConfigOpen || isColorConfigOpen) {
                    if (isDrawerOpen) isDrawerOpen = false
                    else if (isFavoritesConfigOpen) isFavoritesConfigOpen = false
                    else if (isColorConfigOpen) isColorConfigOpen = false
                    else if (isSettingsOpen) isSettingsOpen = false
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
                                onToggleFavorite = { pkg ->
                                    val newFavs = LauncherLogic.toggleFavorite(favoritePackages, pkg)
                                    if (newFavs != favoritePackages) {
                                        saveFavorites(context, newFavs)
                                        favoritePackages = newFavs
                                    }
                                },
                                isFavorite = { pkg -> pkg in favoritePackages },
                                onClose = { isDrawerOpen = false }
                            )
                        } else {
                            HomeScreen(
                                favorites = favorites,
                                isSettingsOpen = isSettingsOpen,
                                onOpenDrawer = { isDrawerOpen = true },
                                onToggleSettings = { isSettingsOpen = !isSettingsOpen },
                                onOpenFavoritesConfig = { isFavoritesConfigOpen = true },
                                onOpenColorConfig = { isColorConfigOpen = true }
                            )
                        }
                    }

                    // Overlay Menüs
                    AnimatedVisibility(
                        visible = isFavoritesConfigOpen,
                        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300, easing = EaseOutCubic)) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300, easing = EaseInCubic)) + fadeOut()
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
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
                        visible = isColorConfigOpen,
                        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300, easing = EaseOutCubic)) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300, easing = EaseInCubic)) + fadeOut()
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
                            ColorConfigMenu(
                                selectedTheme = currentTheme,
                                onThemeSelected = { theme ->
                                    scope.launch { themeManager.setTheme(theme) }
                                },
                                onClose = { isColorConfigOpen = false }
                            )
                        }
                    }
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
    onOpenColorConfig: () -> Unit
) {
    val context = LocalContext.current
    val rotation by animateFloatAsState(
        targetValue = if (isSettingsOpen) 180f else 0f,
        animationSpec = tween(300, easing = EaseInOutCubic)
    )
    
    val settingsButtonSize by animateDpAsState(
        targetValue = if (isSettingsOpen) 72.dp else 56.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen")
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -50) onOpenDrawer()
                    else if (dragAmount > 50) expandNotifications(context)
                }
            }
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
                        Surface(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .bounceClick(intSrc)
                                .testTag("favorite_item_${app.packageName}")
                                .clickable(
                                    interactionSource = intSrc,
                                    indication = null
                                ) {
                                    context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it) }
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
}

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
            modifier = Modifier.bounceClick(intSrc).testTag("confirm_favorites")
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawer(
    apps: List<AppInfo>, 
    onToggleFavorite: (String) -> Unit,
    isFavorite: (String) -> Boolean,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val colorTheme = LocalColorTheme.current
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(apps.toList(), searchQuery) { LauncherLogic.filterApps(apps.toList(), searchQuery) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(modifier = Modifier.fillMaxSize().testTag("app_drawer")) {
        Box(modifier = Modifier.fillMaxSize().background(SolidColor(colorTheme.drawerBackground.copy(alpha = 0.85f))))
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Apps", fontSize = 24.sp, fontWeight = FontWeight.Light, color = Color.White)
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = null, tint = Color.White) }
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
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).testTag("search_field"),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
                        cursorBrush = SolidColor(Color.White), singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                        decorationBox = { if (searchQuery.isEmpty()) Text("Apps durchsuchen...", color = Color.White.copy(alpha = 0.4f), fontSize = 16.sp); it() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(32.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
                itemsIndexed(items = filteredApps, key = { _, app -> app.packageName }) { _, app ->
                    var showActions by remember { mutableStateOf(false) }
                    val intSrc = remember { MutableInteractionSource() }
                    Box(modifier = Modifier.width(80.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.bounceClick(intSrc).combinedClickable(
                            interactionSource = intSrc,
                            indication = null,
                            onClick = { context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it) } },
                            onLongClick = { showActions = true }
                        )) {
                            AppIconView(app)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = app.label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                        DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }, modifier = Modifier.background(Color(0xFF1A1F2B))) {
                            DropdownMenuItem(text = { Text(if (isFavorite(app.packageName)) "Vom Home entfernen" else "Als Favorit setzen", color = Color.White) }, onClick = { onToggleFavorite(app.packageName); showActions = false })
                            DropdownMenuItem(text = { Text("App-Info", color = Color.White) }, onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", app.packageName, null) }); showActions = false })
                            DropdownMenuItem(text = { Text("Deinstallieren", color = Color.Red) }, onClick = { context.startActivity(Intent(Intent.ACTION_DELETE).apply { data = Uri.fromParts("package", app.packageName, null) }); showActions = false })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppIconView(app: AppInfo) {
    val iconSize = 48.dp
    when {
        app.lucideIcon != null -> Icon(imageVector = app.lucideIcon, contentDescription = null, modifier = Modifier.size(iconSize), tint = Color.White)
        app.customIconResId != null -> Icon(painter = painterResource(id = app.customIconResId), contentDescription = null, modifier = Modifier.size(iconSize), tint = Color.White)
        app.iconBitmap != null -> Image(bitmap = app.iconBitmap, contentDescription = null, modifier = Modifier.size(iconSize), colorFilter = ColorFilter.tint(Color.White))
        else -> Box(modifier = Modifier.size(iconSize).background(Color.White.copy(alpha = 0.05f), CircleShape))
    }
}

@Composable
fun ClockHeader() {
    val context = LocalContext.current
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
            fontSize = 72.sp,
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
            fontSize = 18.sp,
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
    }.sortedBy { it.label.lowercase() }
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
