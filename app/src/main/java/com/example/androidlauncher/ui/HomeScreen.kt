package com.example.androidlauncher.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.LauncherAccessibilityService
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.ui.LiquidGlass.conditionalGlass
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalFontSize
import com.example.androidlauncher.ui.theme.LocalFontWeight
import com.example.androidlauncher.ui.theme.LocalLiquidGlassEnabled
import com.example.androidlauncher.ui.theme.LocalShowFavoriteLabels
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

/**
 * Homescreen des Launchers.
 *
 * Zeigt die Uhr, Favoriten-Apps, den Settings-Button und den Such-Button an.
 * Unterstützt vertikale Swipe-Gesten zum Öffnen des App-Drawers (hoch)
 * und der Benachrichtigungsleiste (runter).
 *
 * @param favorites Liste der favorisierten Apps.
 * @param isSettingsOpen Ob das Einstellungsmenü gerade sichtbar ist.
 * @param isSearchOpen Ob die Suche gerade sichtbar ist.
 * @param onOpenDrawer Callback zum Öffnen des App-Drawers.
 * @param onOpenSearch Callback zum Öffnen der Suche.
 * @param onToggleSettings Callback zum Umschalten des Einstellungsmenüs.
 * @param onOpenFavoritesConfig Callback zum Öffnen der Favoriten-Konfiguration.
 * @param onOpenColorConfig Callback zum Öffnen der Farb-Konfiguration.
 * @param onOpenSizeConfig Callback zum Öffnen der Größen-Konfiguration.
 * @param onOpenSystemSettings Callback zum Öffnen der System-Einstellungen.
 * @param onOpenInfo Callback zum Öffnen des Info-Dialogs.
 * @param onLaunchApp Callback zum Starten einer App.
 * @param returnIconPackage Paketname der App, die gerade zurückkehrt (für Bounce-Animation).
 * @param isPreview Ob der Screen im Vorschau-Modus (z.B. Crop-Screen) angezeigt wird.
 */
@Composable
fun HomeScreen(
    favorites: List<AppInfo>,
    isSettingsOpen: Boolean,
    isSearchOpen: Boolean,
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    onToggleSettings: () -> Unit,
    onOpenFavoritesConfig: () -> Unit,
    onOpenColorConfig: () -> Unit,
    onOpenSizeConfig: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onOpenInfo: () -> Unit,
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

    // Shortcut-Zustand
    var selectedShortcutApp by remember { mutableStateOf<AppInfo?>(null) }
    var shortcutMenuBounds by remember { mutableStateOf<Rect?>(null) }

    val rotation by animateFloatAsState(
        targetValue = if (isSettingsOpen) 180f else 0f,
        animationSpec = tween(300, easing = EaseInOutCubic), label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen")
            .onGloballyPositioned { rootSize = it.size }
            .then(if (!isPreview) {
                Modifier
                    .pointerInput(Unit) {
                        // Bestehende vertikale Gesten (Drawer/Notifications)
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -50) onOpenDrawer()
                            else if (dragAmount > 50) expandNotifications(context)
                        }
                    }
                    .pointerInput(Unit) {
                        // NEU: Doppelklick-Geste zum Sperren
                        detectTapGestures(
                            onDoubleTap = {
                                if (LauncherAccessibilityService.isAccessibilityServiceEnabled(context)) {
                                    // Service ist aktiv -> Befehl senden
                                    LauncherAccessibilityService.requestLockScreen(context)
                                } else {
                                    // Service fehlt -> Nutzer informieren und zu Einstellungen leiten
                                    Toast.makeText(context, "Bitte aktiviere den Accessibility Service in den Einstellungen", Toast.LENGTH_LONG).show()
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    } catch (_: Exception) {}
                                }
                            }
                        )
                    }
            } else Modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(30.dp))
                ClockHeader(
                    onLaunchApp = onLaunchApp,
                    returnIconPackage = returnIconPackage,
                    isPreview = isPreview
                )

                Spacer(modifier = Modifier.weight(1f))

                // Favoriten-Liste
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    if (favorites.isEmpty()) {
                        // "Favoriten hinzufügen"-Button
                        val intSrc = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .conditionalGlass(CircleShape, isDarkTextEnabled, isLiquidGlassEnabled)
                                .clip(CircleShape)
                                .then(if (!isPreview) Modifier.bounceClick(intSrc).clickable(interactionSource = intSrc, indication = null) {
                                    onOpenFavoritesConfig()
                                } else Modifier),
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
                                onLaunchApp = onLaunchApp,
                                onShortcutRequested = { shortcutApp, bounds ->
                                    selectedShortcutApp = shortcutApp
                                    shortcutMenuBounds = bounds
                                },
                                isPreview = isPreview
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            // Shortcuts-Menü Overlay
            selectedShortcutApp?.let { app ->
                AppShortcutsMenu(
                    packageName = app.packageName,
                    targetBounds = shortcutMenuBounds,
                    onDismiss = {
                        selectedShortcutApp = null
                        shortcutMenuBounds = null
                    },
                    onShortcutClick = { shortcut ->
                        launchShortcut(context, app.packageName, shortcut.id)
                    }
                )
            }

            // Settings- und Such-Buttons
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
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

                val settingsBtnScale by animateFloatAsState(
                    targetValue = if (isSettingsOpen) 1.2f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "SettingsBtnScale"
                )

                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Such-Button (nur sichtbar wenn Settings geschlossen)
                    AnimatedVisibility(
                        visible = !isSettingsOpen,
                        enter = scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            initialScale = 0.0f
                        ) + fadeIn(animationSpec = tween(150)),
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
                        val searchButtonRotation by animateFloatAsState(
                            targetValue = if (isSearchOpen) 90f else 0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "SearchButtonRotation"
                        )

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = searchButtonScale
                                    scaleY = searchButtonScale
                                    rotationZ = searchButtonRotation
                                }
                                .size(56.dp)
                                .conditionalGlass(CircleShape, isDarkTextEnabled, isLiquidGlassEnabled, fallbackAlpha = 0.15f)
                                .clip(CircleShape)
                                .testTag("home_search_button")
                                .onGloballyPositioned { onSearchButtonBoundsChanged(it.boundsInRoot()) }
                                .then(if (!isPreview) Modifier.bounceClick(searchIntSrc).clickable(interactionSource = searchIntSrc, indication = null) { onOpenSearch() } else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = mainTextColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Settings-Button
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
                            .then(if (!isPreview) Modifier.bounceClick(intSrc).clickable(interactionSource = intSrc, indication = null) { onToggleSettings() } else Modifier),
                        contentAlignment = Alignment.Center
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
        }
    }
}

// ── Favoriten-Item ───────────────────────────────────────────────

/**
 * Einzelnes Favoriten-Element auf dem Homescreen.
 * Unterstützt Bounce-Animation bei Rückkehr und Swipe-Geste für Shortcuts.
 */
@Composable
private fun FavoriteItem(
    app: AppInfo,
    showLabels: Boolean,
    fontSize: Float,
    mainTextColor: Color,
    returnIconPackage: String?,
    onLaunchApp: (String, Intent, Rect?) -> Unit,
    onShortcutRequested: (AppInfo, Rect?) -> Unit,
    isPreview: Boolean = false
) {
    val context = LocalContext.current
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
    val iconBounds = remember(app.packageName) { mutableStateOf<Rect?>(null) }

    // Horizontaler Swipe-Zustand für Shortcuts
    var horizontalOffset by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "SwipeOffset",
        targetValue = horizontalOffset
    )

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                itemBounds.value = coordinates.boundsInRoot()
            }
            .graphicsLayer { translationX = animatedOffset }
            .then(if (!isPreview) {
                Modifier
                    .pointerInput(app.packageName) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (horizontalOffset > 150f) {
                                    onShortcutRequested(app, itemBounds.value)
                                }
                                horizontalOffset = 0f
                            },
                            onDragCancel = { horizontalOffset = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                // Nur Swipe nach rechts erlauben
                                horizontalOffset = (horizontalOffset + dragAmount).coerceIn(0f, 300f)
                            }
                        )
                    }
                    .bounceClick(intSrc)
                    .clickable(interactionSource = intSrc, indication = null) {
                        val launchBounds = iconBounds.value ?: itemBounds.value ?: return@clickable
                        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName) ?: return@clickable
                        onLaunchApp(app.packageName, intent, launchBounds)
                    }
            } else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.graphicsLayer {
                scaleX = bounceScale
                scaleY = bounceScale
            }.onGloballyPositioned { coordinates ->
                iconBounds.value = coordinates.boundsInRoot()
             }) {
                AppIconView(app, showBadge = true)
            }
            if (showLabels) {
                Text(
                    text = app.label,
                    color = mainTextColor,
                    fontSize = 18.sp * fontSize,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── ClockHeader ──────────────────────────────────────────────────

/**
 * Zeigt die aktuelle Uhrzeit und das Datum auf dem Homescreen an.
 *
 * - Klick auf die Uhrzeit öffnet die System-Uhr-App.
 * - Klick auf das Datum öffnet den Kalender.
 *
 * **Energieeffizienz:** Aktualisiert die Anzeige im 30-Sekunden-Takt
 * statt jede Sekunde, da Sekunden nicht angezeigt werden.
 */
@Composable
fun ClockHeader(
    onLaunchApp: (String, Intent, Rect?) -> Unit,
    returnIconPackage: String?,
    isPreview: Boolean = false
) {
    val context = LocalContext.current
    val fontSize = LocalFontSize.current
    val appFontWeight = LocalFontWeight.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val mainTextColor = LiquidGlass.mainTextColor(isDarkTextEnabled)

    var currentTime by remember { mutableStateOf(Calendar.getInstance().time) }

    // Optimierung: 30-Sekunden-Intervall statt 1-Sekunden-Intervall,
    // da nur HH:mm angezeigt wird – spart CPU und Akku
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance().time
            delay(30_000L)
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, d. MMMM", Locale.getDefault()) }
    val intSrcTime = remember { MutableInteractionSource() }
    val intSrcDate = remember { MutableInteractionSource() }
    val clockBounds = remember { mutableStateOf<Rect?>(null) }
    val calendarBounds = remember { mutableStateOf<Rect?>(null) }

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

    Column(modifier = Modifier.fillMaxWidth()) {
        // Uhrzeit
        Text(
            text = timeFormat.format(currentTime),
            fontSize = 72.sp * fontSize.scale,
            fontWeight = appFontWeight.weight,
            letterSpacing = (-2).sp,
            color = mainTextColor,
            modifier = Modifier
                .onGloballyPositioned { clockBounds.value = it.boundsInRoot() }
                .graphicsLayer {
                    scaleX = bounceScaleTime
                    scaleY = bounceScaleTime
                }
                .clip(RoundedCornerShape(8.dp))
                .then(if (!isPreview) Modifier.bounceClick(intSrcTime).clickable(interactionSource = intSrcTime, indication = null) {
                    launchClockApp(context, clockBounds.value, onLaunchApp) { clockPackage = it }
                } else Modifier)
        )
        // Datum
        Text(
            text = dateFormat.format(currentTime),
            fontSize = 18.sp * fontSize.scale,
            fontWeight = appFontWeight.weight,
            color = mainTextColor.copy(alpha = 0.7f),
            modifier = Modifier
                .onGloballyPositioned { calendarBounds.value = it.boundsInRoot() }
                .graphicsLayer {
                    scaleX = bounceScaleDate
                    scaleY = bounceScaleDate
                }
                .clip(RoundedCornerShape(8.dp))
                .then(if (!isPreview) Modifier
                    .bounceClick(intSrcDate)
                    .clickable(interactionSource = intSrcDate, indication = null) {
                        launchCalendarApp(context, calendarBounds.value, onLaunchApp) { calendarPackage = it }
                    } else Modifier)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

/**
 * Sucht und startet die System-Uhr-App.
 * Probiert mehrere bekannte Paketnamen und Intent-Strategien durch.
 */
private fun launchClockApp(
    context: Context,
    bounds: Rect?,
    onLaunchApp: (String, Intent, Rect?) -> Unit,
    onPackageFound: (String) -> Unit
) {
    val pm = context.packageManager
    var foundPkg: String? = null

    // Bekannte Uhr-Paketnamen verschiedener Hersteller
    val clockPackages = listOf(
        "cn.nubia.deskclock.preset", "cn.nubia.deskclock", "cn.nubia.clock",
        "com.android.deskclock", "com.google.android.deskclock",
        "com.sec.android.app.clockpackage", "com.huawei.android.clock",
        "com.miui.clock", "com.zte.deskclock", "com.android.clock"
    )

    for (pkg in clockPackages) {
        try {
            if (pm.getLaunchIntentForPackage(pkg) != null) {
                foundPkg = pkg
                break
            }
        } catch (_: Exception) { }
    }

    // Fallback: Standard-Uhr-Kategorie
    if (foundPkg == null) {
        try {
            val stdIntent = Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.APP_CLOCK")
            val res = pm.resolveActivity(stdIntent, 0)
            if (res != null) foundPkg = res.activityInfo.packageName
        } catch (_: Exception) { }
    }

    // Fallback: Alarm-Intent
    if (foundPkg == null) {
        try {
            val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            val res = pm.resolveActivity(alarmIntent, 0)
            if (res != null) foundPkg = res.activityInfo.packageName
        } catch (_: Exception) { }
    }

    // Fallback: Installierte Apps durchsuchen
    if (foundPkg == null) {
        try {
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val foundApp = apps.find {
                val pkg = it.packageName.lowercase()
                (pkg.contains("deskclock") || pkg.contains("uhr") ||
                    (pkg.contains("clock") && !pkg.contains("widget"))) &&
                    pm.getLaunchIntentForPackage(it.packageName) != null
            }
            foundPkg = foundApp?.packageName
        } catch (_: Exception) { }
    }

    // Fallback: Launcher-Apps nach Label durchsuchen
    if (foundPkg == null) {
        try {
            val apps = pm.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
            )
            val clockApp = apps.find {
                val labelText = it.loadLabel(pm).toString().lowercase()
                val appPkg = it.activityInfo.packageName.lowercase()
                (appPkg.contains("clock") || appPkg.contains("deskclock") ||
                    labelText.contains("uhr") || labelText.contains("clock")) &&
                    pm.getLaunchIntentForPackage(it.activityInfo.packageName) != null
            }
            foundPkg = clockApp?.activityInfo?.packageName
        } catch (_: Exception) { }
    }

    if (foundPkg != null) {
        onPackageFound(foundPkg)
        val launchIntent = pm.getLaunchIntentForPackage(foundPkg)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            onLaunchApp(foundPkg, launchIntent, bounds)
            return
        }
    }

    // Letzter Fallback: Standard-Intent
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

/**
 * Sucht und startet die System-Kalender-App.
 */
private fun launchCalendarApp(
    context: Context,
    bounds: Rect?,
    onLaunchApp: (String, Intent, Rect?) -> Unit,
    onPackageFound: (String) -> Unit
) {
    val pm = context.packageManager
    var calendarIntent = Intent(Intent.ACTION_VIEW).apply {
        data = CalendarContract.CONTENT_URI.buildUpon()
            .appendPath("time")
            .appendPath(System.currentTimeMillis().toString())
            .build()
    }

    val res = pm.resolveActivity(calendarIntent, 0)
    if (res == null) {
        calendarIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
    }

    val foundPkg = pm.resolveActivity(calendarIntent, 0)?.activityInfo?.packageName
    if (foundPkg != null) {
        onPackageFound(foundPkg)
        val launchIntent = pm.getLaunchIntentForPackage(foundPkg)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            onLaunchApp(foundPkg, launchIntent, bounds)
        } else {
            calendarIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            onLaunchApp(foundPkg, calendarIntent, bounds)
        }
        return
    }

    // Fallback
    try {
        calendarIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(calendarIntent)
    } catch (_: Exception) {
        val selectorIntent = Intent.makeMainSelectorActivity(
            Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR
        ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        try {
            context.startActivity(selectorIntent)
        } catch (_: Exception) { }
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
    } catch (_: Exception) {
    }
}
