package com.example.androidlauncher.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.androidlauncher.LauncherAccessibilityService
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.ui.LiquidGlass.conditionalGlass
import com.example.androidlauncher.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Datenklasse für einen App-Start-Request vom Homescreen.
 */
data class HomeLaunchRequest(
    val packageName: String,
    val bounds: Rect?,
    val intent: Intent?
)

/**
 * Homescreen des Launchers mit intuitivem Bearbeitungsmodus für Element-Positionen.
 */
@Composable
fun HomeScreen(
    favorites: List<AppInfo>,
    isSettingsOpen: Boolean,
    isSearchOpen: Boolean,
    isEditMode: Boolean = false,
    favoritesOffsetX: Float = 0f,
    favoritesOffsetY: Float = 0f,
    clockOffsetY: Float = 0f,
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    onToggleSettings: () -> Unit,
    onToggleEditMode: () -> Unit,
    onOpenFavoritesConfig: () -> Unit,
    onOpenColorConfig: () -> Unit,
    onOpenSizeConfig: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onOpenInfo: () -> Unit,
    onSaveFavoritesOffset: (Float, Float) -> Unit,
    onSaveClockOffset: (Float) -> Unit,
    onLaunchApp: (String, Intent, Rect?) -> Unit,
    returnIconPackage: String?,
    searchButtonBounceToken: Int = 0,
    onSearchButtonBoundsChanged: (Rect?) -> Unit = {},
    isPreview: Boolean = false
) {
    val context = LocalContext.current
    val colorTheme = LocalColorTheme.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val showLabels = LocalShowFavoriteLabels.current
    val fontSize = LocalFontSize.current
    val isLiquidGlassEnabled = LocalLiquidGlassEnabled.current
    val mainTextColor = LiquidGlass.mainTextColor(isDarkTextEnabled)
    var rootSize by remember { mutableStateOf(IntSize.Zero) }

    // --- Bearbeitungs-States (Lokal für Live-Vorschau) ---
    var currentFavOffsetX by remember(favoritesOffsetX, isEditMode) { mutableStateOf(favoritesOffsetX) }
    var currentFavOffsetY by remember(favoritesOffsetY, isEditMode) { mutableStateOf(favoritesOffsetY) }
    var currentClockOffsetY by remember(clockOffsetY, isEditMode) { mutableStateOf(clockOffsetY) }
    
    // Launch Request State
    val launchRequestState = remember { mutableStateOf<HomeLaunchRequest?>(null) }
    var launchRequest by launchRequestState

    var selectedShortcutApp by remember { mutableStateOf<AppInfo?>(null) }
    var shortcutMenuBounds by remember { mutableStateOf<Rect?>(null) }

    val rotation by animateFloatAsState(
        targetValue = if (isSettingsOpen) 180f else 0f,
        animationSpec = tween(300, easing = EaseInOutCubic), label = ""
    )

    // App-Start verzögert ausführen für Animationen
    LaunchedEffect(launchRequest) {
        if (isPreview) return@LaunchedEffect
        val request = launchRequest ?: return@LaunchedEffect
        delay(280)
        request.intent?.let { onLaunchApp(request.packageName, it, request.bounds) }
        launchRequest = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen")
            .onGloballyPositioned { rootSize = it.size }
            .then(if (!isPreview) {
                Modifier
                    .pointerInput(isEditMode) {
                        if (!isEditMode) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount < -50) onOpenDrawer()
                                else if (dragAmount > 50) expandNotifications(context)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (LauncherAccessibilityService.isAccessibilityServiceEnabled(context)) {
                                    LauncherAccessibilityService.requestLockScreen(context)
                                } else {
                                    Toast.makeText(context, "Accessibility Service erforderlich", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onLongPress = {
                                if (!isEditMode) onToggleEditMode()
                            }
                        )
                    }
            } else Modifier)
    ) {
        // --- Haupt-Layout ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding() 
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(30.dp))
            
            // 1. Uhr / Widget Bereich (Verschiebbar im Edit-Mode)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, currentClockOffsetY.roundToInt()) }
                    .then(if (isEditMode) {
                        Modifier
                            .border(BorderStroke(1.dp, mainTextColor.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
                            .background(mainTextColor.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    currentClockOffsetY += dragAmount.y
                                }
                            }
                    } else Modifier)
            ) {
                ClockHeader(
                    onAppLaunchForReturn = { pkg, bounds -> onLaunchApp(pkg, context.packageManager.getLaunchIntentForPackage(pkg)!!, bounds) },
                    onLaunchRequest = { launchRequest = it },
                    returnIconPackage = returnIconPackage,
                    isPreview = isPreview || isEditMode
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 2. Favoriten-Liste (Verschiebbar im Edit-Mode)
            Box(
                modifier = Modifier
                    .offset { IntOffset(currentFavOffsetX.roundToInt(), currentFavOffsetY.roundToInt()) }
                    .then(if (isEditMode) {
                        Modifier
                            .border(BorderStroke(1.dp, mainTextColor.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
                            .background(mainTextColor.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    currentFavOffsetX += dragAmount.x
                                    currentFavOffsetY += dragAmount.y
                                }
                            }
                    } else Modifier)
                    .padding(if (isEditMode) 12.dp else 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (favorites.isEmpty()) {
                        val intSrc = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .conditionalGlass(CircleShape, isDarkTextEnabled, isLiquidGlassEnabled)
                                .clip(CircleShape)
                                .then(if (!isPreview) Modifier.bounceClick(intSrc).clickable(interactionSource = intSrc, indication = null, enabled = !isEditMode) { onOpenFavoritesConfig() } else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = mainTextColor)
                        }
                    } else {
                        favorites.forEach { app ->
                            FavoriteItem(
                                app = app,
                                showLabels = showLabels,
                                fontSize = fontSize.scale,
                                mainTextColor = mainTextColor,
                                returnIconPackage = returnIconPackage,
                                onAppLaunchForReturn = { pkg, bounds -> onLaunchApp(pkg, context.packageManager.getLaunchIntentForPackage(pkg)!!, bounds) },
                                onLaunchRequest = { launchRequest = it },
                                onShortcutRequested = { shortcutApp, bounds ->
                                    selectedShortcutApp = shortcutApp
                                    shortcutMenuBounds = bounds
                                },
                                launchRequest = launchRequest,
                                isPreview = isPreview || isEditMode
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // --- Edit Mode Kontrollen (Minimalistisches Menü am unteren Rand) ---
        AnimatedVisibility(
            visible = isEditMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(3000f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info-Badge
                Surface(
                    color = mainTextColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, mainTextColor.copy(alpha = 0.2f))
                ) {
                    Text(
                        "Position anpassen",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        color = mainTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Kontroll-Buttons (Abbrechen, Zurücksetzen, Speichern)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Abbrechen
                    EditControlButton(
                        icon = Icons.Default.Close,
                        onClick = { onToggleEditMode() }, // Beendet Modus ohne Speichern (lokaler State verworfen)
                        tint = mainTextColor.copy(alpha = 0.6f)
                    )
                    
                    Spacer(modifier = Modifier.width(20.dp))

                    // Zurücksetzen auf Standard
                    EditControlButton(
                        icon = Icons.Default.Refresh,
                        onClick = {
                            currentFavOffsetX = 0f
                            currentFavOffsetY = 0f
                            currentClockOffsetY = 0f
                        },
                        tint = mainTextColor
                    )

                    Spacer(modifier = Modifier.width(20.dp))

                    // Speichern
                    EditControlButton(
                        icon = Icons.Default.Check,
                        onClick = {
                            onSaveFavoritesOffset(currentFavOffsetX, currentFavOffsetY)
                            onSaveClockOffset(currentClockOffsetY)
                            onToggleEditMode()
                            Toast.makeText(context, "Position gespeichert", Toast.LENGTH_SHORT).show()
                        },
                        containerColor = mainTextColor,
                        tint = if (isDarkTextEnabled) Color.White else Color(0xFF0F172A)
                    )
                }
            }
        }

        // --- Standard UI (Settings & Search) - nur sichtbar wenn nicht im Edit-Mode ---
        if (!isPreview && !isEditMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(24.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                SettingsPaletteMenu(
                    isSettingsOpen = isSettingsOpen,
                    onToggleSettings = onToggleSettings,
                    onOpenFavoritesConfig = onOpenFavoritesConfig,
                    onOpenColorConfig = onOpenColorConfig,
                    onOpenSizeConfig = onOpenSizeConfig,
                    onOpenSystemSettings = onOpenSystemSettings,
                    onOpenInfo = onOpenInfo
                )

                val intSrc = remember { MutableInteractionSource() }
                val searchIntSrc = remember { MutableInteractionSource() }
                
                var isSearchButtonBouncing by remember { mutableStateOf(false) }
                LaunchedEffect(searchButtonBounceToken) {
                    if (searchButtonBounceToken <= 0) return@LaunchedEffect
                    isSearchButtonBouncing = true
                    delay(240)
                    isSearchButtonBouncing = false
                }

                val settingsBtnScale by animateFloatAsState(targetValue = if (isSettingsOpen) 1.2f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "SettingsBtnScale")

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedVisibility(
                        visible = !isSettingsOpen,
                        enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), initialScale = 0.0f) + fadeIn(animationSpec = tween(150)),
                        exit = scaleOut(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150))
                    ) {
                        val searchButtonScale by animateFloatAsState(
                            targetValue = when {
                                isSearchOpen -> 0.8f
                                isSearchButtonBouncing -> 1.12f
                                else -> 1f
                            },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "SearchButtonScale"
                        )
                        val searchButtonRotation by animateFloatAsState(targetValue = if (isSearchOpen) 90f else 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "SearchButtonRotation")

                        Box(
                            modifier = Modifier
                                .graphicsLayer { scaleX = searchButtonScale; scaleY = searchButtonScale; rotationZ = searchButtonRotation }
                                .size(56.dp)
                                .conditionalGlass(CircleShape, isDarkTextEnabled, isLiquidGlassEnabled, fallbackAlpha = 0.15f)
                                .clip(CircleShape)
                                .testTag("home_search_button")
                                .onGloballyPositioned { onSearchButtonBoundsChanged(it.boundsInRoot()) }
                                .bounceClick(searchIntSrc)
                                .clickable(interactionSource = searchIntSrc, indication = null) { onOpenSearch() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = mainTextColor, modifier = Modifier.size(28.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = settingsBtnScale
                                scaleY = settingsBtnScale
                            }
                            .size(56.dp)
                            .conditionalGlass(
                                CircleShape, isDarkTextEnabled, isLiquidGlassEnabled,
                                fallbackAlpha = if (isSettingsOpen) 0.1f else 0.15f
                            )
                            .clip(CircleShape)
                            .testTag("settings_button")
                            .then(if (!isPreview) Modifier.bounceClick(intSrc).clickable(interactionSource = intSrc, indication = null) { onToggleSettings() } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.rotate(rotation)) {
                            Icon(imageVector = if (isSettingsOpen) Icons.Default.Close else Icons.Default.Settings, contentDescription = null, tint = mainTextColor, modifier = Modifier.size(if (isSettingsOpen) 32.dp else 28.dp))
                        }
                    }
                }
            }
        }

        // Shortcut Overlay (Nur im Echtbetrieb)
        if (!isPreview && !isEditMode) {
            selectedShortcutApp?.let { app ->
                AppShortcutsMenu(
                    packageName = app.packageName,
                    targetBounds = shortcutMenuBounds,
                    onDismiss = { selectedShortcutApp = null; shortcutMenuBounds = null },
                    onShortcutClick = { shortcut -> launchShortcut(context, app.packageName, shortcut.id) }
                )
            }
        }

        // App-Start-Overlay
        HomeLaunchOverlay(launchRequest = launchRequest, rootSize = rootSize, backgroundColor = colorTheme.drawerBackground)
    }
}

/**
 * Hilfs-Button für den Bearbeitungsmodus.
 */
@Composable
private fun EditControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    tint: Color,
    containerColor: Color = Color.Transparent
) {
    val intSrc = remember { MutableInteractionSource() }
    val isLiquidGlassEnabled = LocalLiquidGlassEnabled.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current

    Box(
        modifier = Modifier
            .size(56.dp)
            .then(if (containerColor == Color.Transparent) {
                Modifier.conditionalGlass(CircleShape, isDarkTextEnabled, isLiquidGlassEnabled, fallbackAlpha = 0.1f)
            } else {
                Modifier.background(containerColor, CircleShape)
            })
            .clip(CircleShape)
            .bounceClick(intSrc)
            .clickable(interactionSource = intSrc, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

// ── Favoriten-Item ───────────────────────────────────────────────

@Composable
private fun FavoriteItem(
    app: AppInfo,
    showLabels: Boolean,
    fontSize: Float,
    mainTextColor: Color,
    returnIconPackage: String?,
    onAppLaunchForReturn: (String, Rect?) -> Unit,
    onLaunchRequest: (HomeLaunchRequest) -> Unit,
    onShortcutRequested: (AppInfo, Rect?) -> Unit,
    launchRequest: HomeLaunchRequest?,
    isPreview: Boolean = false
) {
    val context = LocalContext.current
    val intSrc = remember { MutableInteractionSource() }
    val bounceScale by animateFloatAsState(targetValue = if (returnIconPackage == app.packageName) 1.12f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "HomeReturnBounce")
    val itemBounds = remember(app.packageName) { mutableStateOf<Rect?>(null) }
    val iconBounds = remember(app.packageName) { mutableStateOf<Rect?>(null) }
    
    var horizontalOffset by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = horizontalOffset, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "SwipeOffset")

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .onGloballyPositioned { coordinates -> itemBounds.value = coordinates.boundsInRoot() }
            .graphicsLayer { translationX = animatedOffset }
            .then(if (!isPreview) {
                Modifier
                    .pointerInput(app.packageName) {
                        detectHorizontalDragGestures(
                            onDragEnd = { if (horizontalOffset > 150f) { onShortcutRequested(app, itemBounds.value) }; horizontalOffset = 0f },
                            onDragCancel = { horizontalOffset = 0f },
                            onHorizontalDrag = { _, dragAmount -> horizontalOffset = (horizontalOffset + dragAmount).coerceIn(0f, 300f) }
                        )
                    }
                    .bounceClick(intSrc)
                    .clickable(interactionSource = intSrc, indication = null) {
                        if (launchRequest == null) {
                            val targetBounds = iconBounds.value ?: itemBounds.value
                            onAppLaunchForReturn(app.packageName, targetBounds)
                            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            onLaunchRequest(HomeLaunchRequest(app.packageName, targetBounds, intent))
                        }
                    }
            } else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier
                .graphicsLayer { scaleX = bounceScale; scaleY = bounceScale }
                .onGloballyPositioned { iconBounds.value = it.boundsInRoot() }
            ) { 
                AppIconView(app, showBadge = true) 
            }
            if (showLabels) {
                Text(text = app.label, color = mainTextColor, fontSize = 18.sp * fontSize, fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ── App-Start-Overlay ────────────────────────────────────────────

@Composable
private fun HomeLaunchOverlay(
    launchRequest: HomeLaunchRequest?,
    rootSize: IntSize,
    backgroundColor: Color
) {
    val launchTransition = updateTransition(targetState = launchRequest != null, label = "HomeLaunchTransition")
    val launchProgress by launchTransition.animateFloat(transitionSpec = { spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium) }, label = "HomeLaunchProgress") { if (it) 1f else 0f }
    val launchOverlayAlpha by launchTransition.animateFloat(transitionSpec = { tween(durationMillis = 220, easing = LinearEasing) }, label = "HomeLaunchOverlayAlpha") { if (it) 1f else 0f }
    val launchBounds = launchRequest?.bounds
    val launchTranslation = remember(launchBounds, rootSize) { if (launchBounds != null && rootSize.width > 0 && rootSize.height > 0) { val centerX = rootSize.width / 2f; val centerY = rootSize.height / 2f; Offset(launchBounds.center.x - centerX, launchBounds.center.y - centerY) } else Offset.Zero }
    val launchStartScale = remember(launchBounds, rootSize) { if (launchBounds != null && rootSize.width > 0 && rootSize.height > 0) { val wScale = launchBounds.width.toFloat() / rootSize.width.toFloat(); val hScale = launchBounds.height.toFloat() / rootSize.height.toFloat(); max(wScale, hScale).coerceIn(0.06f, 0.35f) } else 0.08f }

    if (launchProgress > 0f) {
        val scale = launchStartScale + (1f - launchStartScale) * launchProgress
        val translationX = launchTranslation.x * (1f - launchProgress)
        val translationY = launchTranslation.y * (1f - launchProgress)
        Box(modifier = Modifier.fillMaxSize().zIndex(2000f).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})) {
            Box(modifier = Modifier.fillMaxSize().graphicsLayer { this.scaleX = scale; this.scaleY = scale; this.translationX = translationX; this.translationY = translationY; this.transformOrigin = TransformOrigin.Center; this.alpha = launchOverlayAlpha }.background(backgroundColor))
        }
    }
}

// ── Hilfsfunktionen ──────────────────────────────────────────────

@SuppressLint("WrongConstant")
internal fun expandNotifications(context: Context) {
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val method = statusBarManager.getMethod("expandNotificationsPanel")
        method.invoke(statusBarService)
    } catch (_: Exception) { }
}

// ── ClockHeader ──────────────────────────────────────────────────

@Composable
fun ClockHeader(
    onAppLaunchForReturn: (String, Rect?) -> Unit,
    onLaunchRequest: (HomeLaunchRequest) -> Unit,
    returnIconPackage: String?,
    isPreview: Boolean = false
) {
    val context = LocalContext.current
    val fontSize = LocalFontSize.current
    val appFontWeight = LocalFontWeight.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val mainTextColor = LiquidGlass.mainTextColor(isDarkTextEnabled)
    var currentTime by remember { mutableStateOf(Calendar.getInstance().time) }

    LaunchedEffect(Unit) { while (true) { currentTime = Calendar.getInstance().time; delay(30_000L) } }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, d. MMMM", Locale.getDefault()) }
    val intSrcTime = remember { MutableInteractionSource() }
    val intSrcDate = remember { MutableInteractionSource() }
    val clockBounds = remember { mutableStateOf<Rect?>(null) }
    val calendarBounds = remember { mutableStateOf<Rect?>(null) }
    var clockPackage by remember { mutableStateOf<String?>(null) }
    var calendarPackage by remember { mutableStateOf<String?>(null) }

    val bounceScaleTime by animateFloatAsState(targetValue = if (returnIconPackage != null && returnIconPackage == clockPackage) 1.08f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "ClockReturnBounce")
    val bounceScaleDate by animateFloatAsState(targetValue = if (returnIconPackage != null && returnIconPackage == calendarPackage) 1.08f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "CalendarReturnBounce")

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = timeFormat.format(currentTime),
            fontSize = 72.sp * fontSize.scale,
            fontWeight = appFontWeight.weight,
            letterSpacing = (-2).sp,
            color = mainTextColor,
            modifier = Modifier
                .onGloballyPositioned { clockBounds.value = it.boundsInRoot() }
                .graphicsLayer { scaleX = bounceScaleTime; scaleY = bounceScaleTime }
                .clip(RoundedCornerShape(8.dp))
                .then(if (!isPreview) Modifier.bounceClick(intSrcTime).clickable(interactionSource = intSrcTime, indication = null) { launchClockApp(context, clockBounds.value, onAppLaunchForReturn, onLaunchRequest) { clockPackage = it } } else Modifier)
        )
        Text(
            text = dateFormat.format(currentTime),
            fontSize = 18.sp * fontSize.scale,
            fontWeight = appFontWeight.weight,
            color = mainTextColor.copy(alpha = 0.7f),
            modifier = Modifier
                .onGloballyPositioned { calendarBounds.value = it.boundsInRoot() }
                .graphicsLayer { scaleX = bounceScaleDate; scaleY = bounceScaleDate }
                .clip(RoundedCornerShape(8.dp))
                .then(if (!isPreview) Modifier.bounceClick(intSrcDate).clickable(interactionSource = intSrcDate, indication = null) { launchCalendarApp(context, calendarBounds.value, onAppLaunchForReturn, onLaunchRequest) { calendarPackage = it } } else Modifier)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

private fun launchClockApp(context: Context, bounds: Rect?, onAppLaunchForReturn: (String, Rect?) -> Unit, onLaunchRequest: (HomeLaunchRequest) -> Unit, onPackageFound: (String) -> Unit) {
    val pm = context.packageManager
    var foundPkg: String? = null
    val clockPackages = listOf("cn.nubia.deskclock.preset", "cn.nubia.deskclock", "cn.nubia.clock", "com.android.deskclock", "com.google.android.deskclock", "com.sec.android.app.clockpackage", "com.huawei.android.clock", "com.miui.clock", "com.zte.deskclock", "com.android.clock")
    for (pkg in clockPackages) { try { if (pm.getLaunchIntentForPackage(pkg) != null) { foundPkg = pkg; break } } catch (_: Exception) { } }
    if (foundPkg == null) { try { val stdIntent = Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.APP_CLOCK"); val res = pm.resolveActivity(stdIntent, 0); if (res != null) foundPkg = res.activityInfo.packageName } catch (_: Exception) { } }
    if (foundPkg == null) { try { val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS); val res = pm.resolveActivity(alarmIntent, 0); if (res != null) foundPkg = res.activityInfo.packageName } catch (_: Exception) { } }
    if (foundPkg == null) { try { val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA); val foundApp = apps.find { val pkg = it.packageName.lowercase(); (pkg.contains("deskclock") || pkg.contains("uhr") || (pkg.contains("clock") && !pkg.contains("widget"))) && pm.getLaunchIntentForPackage(it.packageName) != null }; foundPkg = foundApp?.packageName } catch (_: Exception) { } }
    if (foundPkg == null) { try { val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0); val clockApp = apps.find { val labelText = it.loadLabel(pm).toString().lowercase(); val appPkg = it.activityInfo.packageName.lowercase(); (appPkg.contains("clock") || appPkg.contains("deskclock") || labelText.contains("uhr") || labelText.contains("clock")) && pm.getLaunchIntentForPackage(it.activityInfo.packageName) != null }; foundPkg = clockApp?.activityInfo?.packageName } catch (_: Exception) { } }
    if (foundPkg != null) { onPackageFound(foundPkg); val launchIntent = pm.getLaunchIntentForPackage(foundPkg); if (launchIntent != null) { launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED); onAppLaunchForReturn(foundPkg, bounds); onLaunchRequest(HomeLaunchRequest(foundPkg, bounds, launchIntent)); return } }
    try { val intent = Intent(Intent.ACTION_MAIN).apply { addCategory("android.intent.category.APP_CLOCK"); flags = Intent.FLAG_ACTIVITY_NEW_TASK }; context.startActivity(intent) } catch (_: Exception) { Toast.makeText(context, "Uhr-App nicht gefunden", Toast.LENGTH_SHORT).show() }
}

private fun launchCalendarApp(context: Context, bounds: Rect?, onAppLaunchForReturn: (String, Rect?) -> Unit, onLaunchRequest: (HomeLaunchRequest) -> Unit, onPackageFound: (String) -> Unit) {
    val pm = context.packageManager
    var calendarIntent = Intent(Intent.ACTION_VIEW).apply { data = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").appendPath(System.currentTimeMillis().toString()).build() }
    val res = pm.resolveActivity(calendarIntent, 0)
    if (res == null) { calendarIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR) }
    val foundPkg = pm.resolveActivity(calendarIntent, 0)?.activityInfo?.packageName
    if (foundPkg != null) { onPackageFound(foundPkg); val launchIntent = pm.getLaunchIntentForPackage(foundPkg); if (launchIntent != null) { launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED); onAppLaunchForReturn(foundPkg, bounds); onLaunchRequest(HomeLaunchRequest(foundPkg, bounds, launchIntent)) } else { calendarIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED); onAppLaunchForReturn(foundPkg, bounds); onLaunchRequest(HomeLaunchRequest(foundPkg, bounds, calendarIntent)) }; return }
    try { calendarIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(calendarIntent) } catch (_: Exception) { val selectorIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }; try { context.startActivity(selectorIntent) } catch (_: Exception) { } }
}
