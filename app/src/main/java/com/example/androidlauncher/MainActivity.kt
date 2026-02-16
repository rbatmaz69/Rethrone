package com.example.androidlauncher

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.composables.icons.lucide.*
import java.text.SimpleDateFormat
import java.util.*

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val lucideIcon: ImageVector? = null,
    val customIconResId: Int? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidLauncherTheme(dynamicColor = false) {
                val context = LocalContext.current
                var isDrawerOpen by remember { mutableStateOf(false) }
                var isSettingsOpen by remember { mutableStateOf(false) }
                var isFavoritesConfigOpen by remember { mutableStateOf(false) }
                
                var allApps by remember { mutableStateOf(emptyList<AppInfo>()) }
                var favoritePackages by remember { mutableStateOf(getSavedFavorites(context)) }
                
                // Favoriten basierend auf der gespeicherten Liste (Reihenfolge!) laden
                val favorites = remember(allApps, favoritePackages) { 
                    favoritePackages.mapNotNull { pkg -> allApps.find { it.packageName == pkg } }.take(8)
                }

                LaunchedEffect(Unit) {
                    allApps = getInstalledApps(context)
                }

                LaunchedEffect(isDrawerOpen) {
                    if (isDrawerOpen) {
                        isSettingsOpen = false
                        isFavoritesConfigOpen = false
                    }
                }

                BackHandler(enabled = isDrawerOpen || isSettingsOpen || isFavoritesConfigOpen) {
                    if (isDrawerOpen) isDrawerOpen = false
                    else if (isFavoritesConfigOpen) isFavoritesConfigOpen = false
                    else if (isSettingsOpen) isSettingsOpen = false
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    SystemWallpaperView()

                    AnimatedContent(
                        targetState = isDrawerOpen,
                        transitionSpec = {
                            if (targetState) {
                                (slideInVertically(initialOffsetY = { it }, animationSpec = tween(500, easing = EaseInOutCubic)) + fadeIn())
                                    .togetherWith(fadeOut(animationSpec = tween(300)))
                            } else {
                                fadeIn(animationSpec = tween(300))
                                    .togetherWith(slideOutVertically(targetOffsetY = { it }, animationSpec = tween(500, easing = EaseInOutCubic)) + fadeOut())
                            }
                        },
                        label = "DrawerTransition"
                    ) { targetIsDrawerOpen ->
                        if (targetIsDrawerOpen) {
                            AppDrawer(
                                apps = allApps,
                                onToggleFavorite = { pkg ->
                                    val isFav = pkg in favoritePackages
                                    val newFavs = if (isFav) favoritePackages - pkg else {
                                        if (favoritePackages.size < 8) favoritePackages + pkg else favoritePackages
                                    }
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
                                onOpenFavoritesConfig = { isFavoritesConfigOpen = true }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isFavoritesConfigOpen,
                        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(500, easing = EaseInOutCubic)) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(500, easing = EaseInOutCubic)) + fadeOut()
                    ) {
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
    val wallpaperManager = WallpaperManager.getInstance(context)
    val wallpaper = remember { try { wallpaperManager.drawable } catch (e: Exception) { null } }

    Box(modifier = Modifier.fillMaxSize()) {
        if (wallpaper != null) {
            Image(
                bitmap = wallpaper.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1A0B2E), Color(0xFF4A148C)))))
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )
    }
}

@Composable
fun HomeScreen(
    favorites: List<AppInfo>, 
    isSettingsOpen: Boolean,
    onOpenDrawer: () -> Unit, 
    onToggleSettings: () -> Unit,
    onOpenFavoritesConfig: () -> Unit
) {
    val context = LocalContext.current
    val rotation by animateFloatAsState(
        targetValue = if (isSettingsOpen) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = EaseInOutCubic)
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
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
                    // Ein "+"-Symbol wird nur dann auf der Startseite angezeigt, wenn noch keine einzige Favoriten-App gesetzt wurde.
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(56.dp)
                            .clickable { onOpenFavoritesConfig() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        }
                    }
                } else {
                    favorites.forEach { app ->
                        Surface(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable {
                                val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                if (intent != null) context.startActivity(intent)
                            }
                        ) {
                            Box(modifier = Modifier.padding(6.dp)) {
                                AppIconView(app)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }

        Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp, end = 8.dp), contentAlignment = Alignment.BottomEnd) {
            AnimatedVisibility(visible = isSettingsOpen, enter = scaleIn(transformOrigin = TransformOrigin(1f, 1f)) + fadeIn(), exit = scaleOut(transformOrigin = TransformOrigin(1f, 1f)) + fadeOut()) {
                Surface(color = Color(0xFF1A1F2B).copy(alpha = 0.98f), shape = RoundedCornerShape(24.dp), modifier = Modifier.width(220.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Einstellungen", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                        SettingsItemView(icon = Icons.Default.Star, label = "Favoriten konfigurieren", onClick = { onOpenFavoritesConfig(); onToggleSettings() })
                        SettingsItemView(icon = Icons.Default.Settings, label = "System", onClick = { context.startActivity(Intent(Settings.ACTION_SETTINGS)) })
                        SettingsItemView(icon = Icons.Default.Info, label = "Info", onClick = { /* Action */ })
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().navigationBarsPadding(), contentAlignment = Alignment.BottomEnd) {
            Surface(modifier = Modifier.padding(8.dp).size(56.dp).clip(CircleShape).clickable { onToggleSettings() }, color = Color.White.copy(alpha = 0.15f), shape = CircleShape) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.rotate(rotation)) {
                    Icon(imageVector = if (isSettingsOpen) Icons.Default.Close else Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
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
    
    val filteredApps = apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    val focusRequester = remember { FocusRequester() }

    Box(modifier = Modifier.fillMaxSize()) {
        SystemWallpaperView()
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A).copy(alpha = 0.95f)))

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Favoriten", fontSize = 24.sp, fontWeight = FontWeight.Light, color = Color.White)
                    Text("${selectedPackages.size} von 8 ausgewählt", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))
                }
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = null, tint = Color.White) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            // Suche
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        focusRequester.requestFocus()
                    }
            ) {
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
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Apps suchen...", color = Color.White.copy(alpha = 0.4f), fontSize = 15.sp)
                            }
                            innerTextField()
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // SEKTION: Sortierung
                if (selectedPackages.isNotEmpty()) {
                    item {
                        Text("Reihenfolge (Halten zum Verschieben oder Pfeile nutzen)", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    itemsIndexed(selectedPackages) { index, pkg ->
                        val app = apps.find { it.packageName == pkg }
                        if (app != null) {
                            Surface(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${index + 1}.", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, modifier = Modifier.width(24.dp))
                                    AppIconView(app)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(app.label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                    
                                    // Sortier-Buttons
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val newList = selectedPackages.toMutableList()
                                                val item = newList.removeAt(index)
                                                newList.add(index - 1, item)
                                                selectedPackages = newList
                                            }
                                        },
                                        enabled = index > 0
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = if (index > 0) Color.White else Color.White.copy(alpha = 0.2f))
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < selectedPackages.size - 1) {
                                                val newList = selectedPackages.toMutableList()
                                                val item = newList.removeAt(index)
                                                newList.add(index + 1, item)
                                                selectedPackages = newList
                                            }
                                        },
                                        enabled = index < selectedPackages.size - 1
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = if (index < selectedPackages.size - 1) Color.White else Color.White.copy(alpha = 0.2f))
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }

                // SEKTION: Alle Apps
                item {
                    Text("Alle Apps", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
                items(filteredApps) { app ->
                    val isFav = app.packageName in selectedPackages
                    Surface(
                        color = if (isFav) Color.White.copy(alpha = 0.05f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (isFav) {
                                selectedPackages = selectedPackages - app.packageName
                            } else {
                                if (selectedPackages.size < 8) {
                                    selectedPackages = selectedPackages + app.packageName
                                } else {
                                    Toast.makeText(context, "Maximal 8 Favoriten erlaubt", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconView(app)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(app.label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            Checkbox(
                                checked = isFav,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (selectedPackages.size < 8) {
                                            selectedPackages = selectedPackages + app.packageName
                                        } else {
                                            Toast.makeText(context, "Maximal 8 Favoriten erlaubt", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        selectedPackages = selectedPackages - app.packageName
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color.White,
                                    uncheckedColor = Color.White.copy(alpha = 0.4f),
                                    checkmarkColor = Color(0xFF0F172A)
                                )
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = {
                    if (selectedPackages.isEmpty()) {
                        Toast.makeText(context, "Keine Favoriten-App ausgewählt", Toast.LENGTH_SHORT).show()
                    } else {
                        onConfirm(selectedPackages)
                    }
                },
                containerColor = Color.White,
                contentColor = Color(0xFF0F172A),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Check, contentDescription = "Bestätigen")
            }
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
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(modifier = Modifier.fillMaxSize()) {
        SystemWallpaperView()
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A).copy(alpha = 0.85f)))

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Apps", fontSize = 24.sp, fontWeight = FontWeight.Light, color = Color.White)
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = null, tint = Color.White) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        focusRequester.requestFocus()
                    }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
                        cursorBrush = SolidColor(Color.White),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.None,
                            keyboardType = KeyboardType.Text,
                            autoCorrectEnabled = false
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { keyboardController?.hide() }
                        ),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Apps durchsuchen...", color = Color.White.copy(alpha = 0.4f), fontSize = 16.sp)
                            }
                            innerTextField()
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.spacedBy(32.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(filteredApps) { app ->
                    var showAppActions by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.width(80.dp), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { 
                                        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                        if (intent != null) context.startActivity(intent)
                                    },
                                    onLongClick = { showAppActions = true }
                                )
                        ) {
                            AppIconView(app)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = app.label,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showAppActions,
                            onDismissRequest = { showAppActions = false },
                            modifier = Modifier.background(Color(0xFF1A1F2B))
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isFavorite(app.packageName)) "Vom Home entfernen" else "Als Favorit setzen", color = Color.White) },
                                onClick = { onToggleFavorite(app.packageName); showAppActions = false }
                            )
                            DropdownMenuItem(
                                text = { Text("App-Info", color = Color.White) },
                                onClick = { 
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", app.packageName, null) }
                                    context.startActivity(intent); showAppActions = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Deinstallieren", color = Color.Red) },
                                onClick = { 
                                    val intent = Intent(Intent.ACTION_DELETE).apply { data = Uri.fromParts("package", app.packageName, null) }
                                    context.startActivity(intent); showAppActions = false
                                }
                            )
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
        app.lucideIcon != null -> {
            Icon(imageVector = app.lucideIcon, contentDescription = null, modifier = Modifier.size(iconSize), tint = Color.White)
        }
        app.customIconResId != null -> {
            Icon(painter = painterResource(id = app.customIconResId), contentDescription = null, modifier = Modifier.size(iconSize), tint = Color.White)
        }
        else -> {
            // Standardmäßig originales Logo (Vordergrund, weiß gefärbt)
            val drawable = app.icon
            val foregroundDrawable = if (drawable is AdaptiveIconDrawable) {
                drawable.foreground ?: drawable
            } else {
                drawable
            }
            Image(
                bitmap = foregroundDrawable.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                colorFilter = ColorFilter.tint(Color.White)
            )
        }
    }
}

@Composable
fun SettingsItemView(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
fun ClockHeader() {
    var currentTime by remember { mutableStateOf(Calendar.getInstance().time) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance().time
            kotlinx.coroutines.delay(1000)
        }
    }
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEEE, d. MMMM", Locale.getDefault())
    Column {
        Text(text = timeFormat.format(currentTime), fontSize = 72.sp, fontWeight = FontWeight.Normal, letterSpacing = (-2).sp, color = Color.White)
        Text(text = dateFormat.format(currentTime), fontSize = 18.sp, fontWeight = FontWeight.Normal, color = Color.White.copy(alpha = 0.7f))
    }
}

private fun getInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    return pm.queryIntentActivities(intent, 0).map { resolveInfo ->
        val icon = resolveInfo.loadIcon(pm)
        
        // Keine automatischen Lucide-Zuweisungen mehr
        AppInfo(
            label = resolveInfo.loadLabel(pm).toString(),
            packageName = resolveInfo.activityInfo.packageName,
            icon = icon,
            lucideIcon = null,
            customIconResId = null
        )
    }.sortedBy { it.label.lowercase() }
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
