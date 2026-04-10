package com.example.androidlauncher.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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

private enum class HomeEditTarget {
    CLOCK,
    FAVORITES
}

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
    clockOffsetX: Float = 0f,
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
    onSaveClockOffset: (Float, Float) -> Unit,
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
    val haptic = LocalHapticFeedback.current
    val hapticEnabled = LocalHapticFeedbackEnabled.current
    val density = LocalDensity.current
    val mainTextColor = LiquidGlass.mainTextColor(isDarkTextEnabled)
    var rootSize by remember { mutableStateOf(IntSize.Zero) }

    // --- Bearbeitungs-States (Lokal für Live-Vorschau) ---
    // Favoriten-Offset wird lokal gehalten und erst bei "Speichern" persistiert.
    // Keine isEditMode Abhängigkeit: States sollten nur neu initialisiert werden, wenn sich die persistierten Werte ändern, nicht wenn der Edit-Modus wechselt.
    var currentFavOffsetX by remember { mutableStateOf(0f) }
    var currentFavOffsetY by remember(favoritesOffsetY) { mutableStateOf(favoritesOffsetY) }
    // Uhrbereich (Uhr + Datum) ist eine Einheit und wird frei auf X/Y verschoben.
    var currentClockOffsetX by remember { mutableStateOf(0f) }
    var currentClockOffsetY by remember(clockOffsetY) { mutableStateOf(clockOffsetY) }

    // Letzte gültige Positionen: Bei Kollision wird darauf zurückgesetzt.
    var lastValidFavOffsetX by remember { mutableStateOf(0f) }
    var lastValidFavOffsetY by remember(favoritesOffsetY) { mutableStateOf(favoritesOffsetY) }
    var lastValidClockOffsetX by remember { mutableStateOf(0f) }
    var lastValidClockOffsetY by remember(clockOffsetY) { mutableStateOf(clockOffsetY) }

    // Neutral-Bounds sind Layout-Bounds ohne aktuelle Offsets; damit können wir sauber Kandidaten prüfen.
    var clockNeutralBounds by remember { mutableStateOf<Rect?>(null) }
    var favoritesNeutralBounds by remember { mutableStateOf<Rect?>(null) }
    // Live-Bounds der UI-Controls, die im Edit-Mode nicht überlappt werden dürfen.
    var searchButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var settingsButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var editControlsBounds by remember { mutableStateOf<Rect?>(null) }

    // Visuelles Kollision-Feedback (Option A) + einmaliges Haptic pro Blockadephase.
    var isClockCollisionBlocked by remember { mutableStateOf(false) }
    var isFavoritesCollisionBlocked by remember { mutableStateOf(false) }
    var isClockNavigationBarBlocked by remember { mutableStateOf(false) }
    var isFavoritesNavigationBarBlocked by remember { mutableStateOf(false) }
    var collisionHapticWasTriggered by remember { mutableStateOf(false) }

    // Die Favoriten-Markierung wird visuell um diesen Wert nach außen gezeichnet.
    val favoritesFramePaddingPx = with(density) { 10.dp.toPx() }
    // Für Nav-Bar-Kollision soll die Uhr den gleichen "gefühlten" Abstand haben wie die Favoriten.
    val clockNavCollisionPaddingPx = favoritesFramePaddingPx
    // Kleine Schutzkante um UI-Controls, damit die Container visuell nicht "ankleben".
    val bottomControlsPaddingPx = with(density) { 8.dp.toPx() }
    
    // Systemnavigation Bar Höhe ermitteln, um eine Sperrzone am unteren Bildschirmrand zu definieren.
    // Die Navigationgleiste darf nicht überlagert werden.
    val navigationBarHeightPx = with(density) { WindowInsets.systemBars.asPaddingValues().calculateBottomPadding().toPx() }
    val arrowProbeStepPx = with(density) { 8.dp.toPx() }
    val editHintContentPadding = 14.dp
    val editHintArrowInset = 4.dp
    val editHintArrowSize = 18.dp
    val editHintClockDownInset = 2.dp
    val editHintContentPaddingPx = with(density) { editHintContentPadding.toPx() }
    
    // Launch Request State
    val launchRequestState = remember { mutableStateOf<HomeLaunchRequest?>(null) }
    var launchRequest by launchRequestState

    var selectedEditTarget by remember { mutableStateOf<HomeEditTarget?>(null) }
    var selectedShortcutApp by remember { mutableStateOf<AppInfo?>(null) }
    var shortcutMenuBounds by remember { mutableStateOf<Rect?>(null) }

    // Lokale Hilfsfunktion: Verschiebt ein Rechteck um X/Y-Pixel.
    fun translateRect(rect: Rect, x: Float, y: Float): Rect {
        return Rect(
            left = rect.left + x,
            top = rect.top + y,
            right = rect.right + x,
            bottom = rect.bottom + y
        )
    }

    // Lokale Hilfsfunktion: Erweitert ein Rechteck um eine visuelle Außenkante.
    fun expandRect(rect: Rect, padding: Float): Rect {
        return Rect(
            left = rect.left - padding,
            top = rect.top - padding,
            right = rect.right + padding,
            bottom = rect.bottom + padding
        )
    }

    // Lokale Hilfsfunktion: Rechteck-Überlappung ohne Sonderlogik.
    fun intersects(first: Rect, second: Rect): Boolean {
        return first.left < second.right &&
            first.right > second.left &&
            first.top < second.bottom &&
            first.bottom > second.top
    }

    // Lokale Hilfsfunktion: Punkt innerhalb eines Rechtecks prüfen.
    fun rectContains(rect: Rect, point: Offset): Boolean {
        return point.x >= rect.left &&
            point.x <= rect.right &&
            point.y >= rect.top &&
            point.y <= rect.bottom
    }

    // Lokale Hilfsfunktion: Kandidat innerhalb des sichtbaren Screens halten und Systemnavigation nicht überlagern.
    fun clampToRoot(candidateX: Float, candidateY: Float, neutralBounds: Rect?): Pair<Float, Float> {
        val bounds = neutralBounds ?: return candidateX to candidateY
        if (rootSize.width <= 0 || rootSize.height <= 0) return candidateX to candidateY

        val minX = -bounds.left
        val maxX = rootSize.width.toFloat() - bounds.right
        val minY = -bounds.top
        // Max Y wird reduziert, um Überlappung mit der Systemnavigation (unten) zu verhindern.
        val maxY = (rootSize.height.toFloat() - bounds.bottom - navigationBarHeightPx).coerceAtLeast(-bounds.top)

        return candidateX.coerceIn(minX, maxX) to candidateY.coerceIn(minY, maxY)
    }

    // Lokale Hilfsfunktion: Erstellt die Sperrzone der Systemnavigation unten am Bildschirm.
    fun getNavigationBarForbiddenZone(): Rect {
        if (rootSize.width <= 0 || rootSize.height <= 0) {
            return Rect(0f, 0f, 0f, 0f)
        }
        return Rect(
            left = 0f,
            top = rootSize.height.toFloat() - navigationBarHeightPx,
            right = rootSize.width.toFloat(),
            bottom = rootSize.height.toFloat()
        )
    }

    // Reaktive Sperrzonen für sichtbar platzierte Controls (Lupe/Zahnrad + Edit-Buttons).
    // Wichtig: Als State geführt, damit pointerInput bei Bounds-Änderung neu gestartet wird.
    val bottomControlsForbiddenZones by remember(
        searchButtonBounds,
        settingsButtonBounds,
        isEditMode,
        isSettingsOpen,
        bottomControlsPaddingPx
    ) {
        derivedStateOf {
            val searchZone = if (!isSettingsOpen) {
                searchButtonBounds?.let { expandRect(it, bottomControlsPaddingPx) }
            } else {
                null
            }
            val settingsZone = settingsButtonBounds?.let { expandRect(it, bottomControlsPaddingPx) }
            // Edit-Controls sind temporär und dürfen die Positionsbearbeitung nicht blockieren.
            listOfNotNull(searchZone, settingsZone)
        }
    }

    // Lokale Hilfsfunktion: Kollisionstint/Haptic steuern, ohne dauerhaft zu triggern.
    fun updateCollisionFeedback(clockBlocked: Boolean, favoritesBlocked: Boolean) {
        isClockCollisionBlocked = clockBlocked
        isFavoritesCollisionBlocked = favoritesBlocked

        val anyBlocked = clockBlocked || favoritesBlocked
        if (anyBlocked && !collisionHapticWasTriggered && hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            collisionHapticWasTriggered = true
        }
        if (!anyBlocked) {
            collisionHapticWasTriggered = false
        }
    }

    // Lokale Hilfsfunktion: Prüft einen vollständigen Layoutzustand auf Screen-Grenzen und Kollisionen.
    fun isValidLayoutState(
        favoritesX: Float,
        favoritesY: Float,
        clockX: Float,
        clockY: Float
    ): Boolean {
        val adjustedFavorites = clampToRoot(favoritesX, favoritesY, favoritesNeutralBounds)
        val adjustedClock = clampToRoot(clockX, clockY, clockNeutralBounds)

        // Bereits geklemmte Werte gelten als ungültig für Speichern, weil nicht exakt der gewünschte Zustand vorliegt.
        val favoritesWereClamped = adjustedFavorites.first != favoritesX || adjustedFavorites.second != favoritesY
        val clockWereClamped = adjustedClock.first != clockX || adjustedClock.second != clockY
        if (favoritesWereClamped || clockWereClamped) return false

        val favoritesRect = favoritesNeutralBounds?.let {
            // Für Kollisionen zählen die sichtbaren Container-Kanten, nicht nur der nackte Content.
            expandRect(
                rect = translateRect(it, favoritesX, favoritesY),
                padding = favoritesFramePaddingPx
            )
        }
        val clockRect = clockNeutralBounds?.let {
            translateRect(it, clockX, clockY)
        }
        val clockNavRect = clockRect?.let {
            expandRect(it, clockNavCollisionPaddingPx)
        }
        val bottomControlsZones = bottomControlsForbiddenZones

        // Solange Bounds fehlen, blockieren wir Speichern nicht unnötig.
        if (favoritesRect == null || clockRect == null) return true

        // Prüfe auf Überlappung zwischen Container
        if (intersects(favoritesRect, clockRect)) return false

        // Prüfe auf Überlappung mit der Systemnavigation
        val navigationBarZone = getNavigationBarForbiddenZone()
        if (intersects(favoritesRect, navigationBarZone) || (clockNavRect != null && intersects(clockNavRect, navigationBarZone))) return false

        // Prüfe auf Überlappung mit sichtbaren UI-Controls.
        if (bottomControlsZones.any { zone ->
                intersects(favoritesRect, zone) || (clockNavRect != null && intersects(clockNavRect, zone))
            }) return false

        return true
    }

    // Prüft, ob die Uhr-Einheit in die gewünschte Richtung real verschoben werden kann.
    fun canMoveClockBy(deltaY: Float): Boolean {
        if (deltaY == 0f) return false
        val (_, candidateY) = clampToRoot(0f, currentClockOffsetY + deltaY, clockNeutralBounds)
        if (candidateY == currentClockOffsetY) return false
        return isValidLayoutState(
            favoritesX = 0f,
            favoritesY = currentFavOffsetY,
            clockX = 0f,
            clockY = candidateY
        )
    }

    // Prüft, ob die Favoriten-Einheit in die gewünschte Richtung real verschoben werden kann.
    fun canMoveFavoritesBy(deltaY: Float): Boolean {
        if (deltaY == 0f) return false
        val (_, candidateY) = clampToRoot(0f, currentFavOffsetY + deltaY, favoritesNeutralBounds)
        if (candidateY == currentFavOffsetY) return false
        return isValidLayoutState(
            favoritesX = 0f,
            favoritesY = candidateY,
            clockX = 0f,
            clockY = currentClockOffsetY
        )
    }

    // Persistenter Überlappungsstatus: hält die Rahmen eingefärbt, solange die Container aktuell kollidieren oder sich mit der Systemnavigation überlagern.
    val hasActiveContainerOverlap by remember(
        isEditMode,
        currentFavOffsetX,
        currentFavOffsetY,
        currentClockOffsetX,
        currentClockOffsetY,
        favoritesNeutralBounds,
        clockNeutralBounds,
        rootSize
    ) {
        derivedStateOf {
            if (!isEditMode) return@derivedStateOf false

            val currentFavoritesRect = favoritesNeutralBounds?.let {
                expandRect(
                    rect = translateRect(it, currentFavOffsetX, currentFavOffsetY),
                    padding = favoritesFramePaddingPx
                )
            }
            val currentClockRect = clockNeutralBounds?.let {
                translateRect(it, currentClockOffsetX, currentClockOffsetY)
            }
            val currentClockNavRect = currentClockRect?.let {
                expandRect(it, clockNavCollisionPaddingPx)
            }

            if (currentFavoritesRect == null || currentClockRect == null) return@derivedStateOf false

            // Prüfe auf Überlappung zwischen den beiden Container
            val containerOverlap = intersects(currentFavoritesRect, currentClockRect)

            // Prüfe auf Überlappung mit der Systemnavigation
            val navigationBarZone = getNavigationBarForbiddenZone()
            val navigationOverlap = intersects(currentFavoritesRect, navigationBarZone) ||
                (currentClockNavRect != null && intersects(currentClockNavRect, navigationBarZone))

            // Prüfe auf Überlappung mit sichtbaren UI-Controls.
            val bottomControlsOverlap = bottomControlsForbiddenZones.any { zone ->
                intersects(currentFavoritesRect, zone) ||
                    (currentClockNavRect != null && intersects(currentClockNavRect, zone))
            }

            containerOverlap || navigationOverlap || bottomControlsOverlap
        }
    }

    // Wenn die Lupe ausgeblendet ist (Settings offen), deren Sperrzone sofort entfernen.
    LaunchedEffect(isSettingsOpen) {
        if (isSettingsOpen) {
            searchButtonBounds = null
            onSearchButtonBoundsChanged(null)
        }
    }

    // Wenn Edit-Mode endet, die Sperrzone der Edit-Buttons sofort entfernen.
    LaunchedEffect(isEditMode) {
        if (!isEditMode) {
            editControlsBounds = null
            selectedEditTarget = null
        } else {
            // Vertikalmodus: bestehende Legacy-X-Verschiebungen beim Einstieg neutralisieren.
            currentFavOffsetX = 0f
            currentClockOffsetX = 0f
            lastValidFavOffsetX = 0f
            lastValidClockOffsetX = 0f
            // Beim Einstieg in den Edit-Mode wird Content vertikal gepolstert; Y daher lokal kompensieren.
            currentFavOffsetY = favoritesOffsetY - editHintContentPaddingPx
            currentClockOffsetY = clockOffsetY - editHintContentPaddingPx
            lastValidFavOffsetY = currentFavOffsetY
            lastValidClockOffsetY = currentClockOffsetY
            selectedEditTarget = null
        }
    }

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
                    .pointerInput(isEditMode) {
                        detectTapGestures(
                            onPress = { tapOffset ->
                                if (isEditMode && selectedEditTarget != null) {
                                    val clockRect = clockNeutralBounds?.let {
                                        translateRect(it, currentClockOffsetX, currentClockOffsetY)
                                    }
                                    val favoritesRect = favoritesNeutralBounds?.let {
                                        translateRect(it, currentFavOffsetX, currentFavOffsetY)
                                    }

                                    val hitClock = clockRect?.let { rectContains(it, tapOffset) } == true
                                    val hitFavorites = favoritesRect?.let { rectContains(it, tapOffset) } == true
                                    val hitEditControls = editControlsBounds?.let { rectContains(it, tapOffset) } == true

                                    if (!hitClock && !hitFavorites && !hitEditControls) {
                                        selectedEditTarget = null
                                    }
                                }
                                tryAwaitRelease()
                            },
                            onDoubleTap = {
                                if (isEditMode) return@detectTapGestures
                                if (LauncherAccessibilityService.isAccessibilityServiceEnabled(context)) {
                                    LauncherAccessibilityService.requestLockScreen(context)
                                } else {
                                    Toast.makeText(context, "Accessibility Service erforderlich", Toast.LENGTH_SHORT).show()
                                }
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
                    // Im Edit-Modus bleibt der Drag-Container kompakt für präzises vertikales Draggen.
                    .then(if (isEditMode) Modifier.wrapContentWidth(Alignment.Start) else Modifier.fillMaxWidth())
                    .zIndex(if (isEditMode) 1500f else 0f)
                    // Uhrbereich wird als Einheit auf X/Y verschoben.
                    .offset { IntOffset(0, currentClockOffsetY.roundToInt()) }
                    // Neutral-Bounds aus aktuellen Bounds ableiten, damit Kandidatenberechnung stabil bleibt.
                    .onGloballyPositioned { coordinates ->
                        val currentBounds = coordinates.boundsInRoot()
                        clockNeutralBounds = translateRect(
                            rect = currentBounds,
                            x = -currentClockOffsetX,
                            y = -currentClockOffsetY
                        )
                    }
                    .then(if (isEditMode) {
                        val isClockSelected = selectedEditTarget == HomeEditTarget.CLOCK
                        val showClockFrame = selectedEditTarget == null || isClockSelected
                        Modifier
                            .then(
                                if (showClockFrame) {
                                    Modifier.border(
                                        BorderStroke(
                                            1.dp,
                                            mainTextColor.copy(alpha = if (isClockSelected) 0.35f else 0.2f)
                                        ),
                                        RoundedCornerShape(16.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .testTag("home_edit_target_clock")
                            .pointerInput(isEditMode) {
                                if (!isEditMode) return@pointerInput
                                detectTapGestures(
                                    onTap = { selectedEditTarget = HomeEditTarget.CLOCK }
                                )
                            }
                            .pointerInput(isEditMode, selectedEditTarget) {
                                if (!isEditMode) return@pointerInput
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                    },
                                    onDragCancel = {
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                    }
                                ) { change, dragAmount ->
                                    if (selectedEditTarget != HomeEditTarget.CLOCK) return@detectVerticalDragGestures
                                    change.consume()
                                    val (_, candidateY) = clampToRoot(0f, currentClockOffsetY + dragAmount, clockNeutralBounds)
                                    val isBlockedAtTopEdge = dragAmount < 0f && candidateY == currentClockOffsetY
                                    if (isBlockedAtTopEdge) {
                                        updateCollisionFeedback(clockBlocked = true, favoritesBlocked = false)
                                        return@detectVerticalDragGestures
                                    }
                                    val canApply = isValidLayoutState(
                                        favoritesX = 0f,
                                        favoritesY = currentFavOffsetY,
                                        clockX = 0f,
                                        clockY = candidateY
                                    )

                                    if (canApply) {
                                        currentClockOffsetX = 0f
                                        currentClockOffsetY = candidateY
                                        lastValidClockOffsetX = 0f
                                        lastValidClockOffsetY = candidateY
                                        isClockNavigationBarBlocked = false
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                    } else {
                                        updateCollisionFeedback(clockBlocked = true, favoritesBlocked = false)
                                    }
                                }
                            }
                            .pointerInput(isEditMode) {
                                if (!isEditMode) return@pointerInput
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        selectedEditTarget = HomeEditTarget.CLOCK
                                    },
                                    onDragEnd = {
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                    },
                                    onDragCancel = {
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    val (_, candidateY) = clampToRoot(0f, currentClockOffsetY + dragAmount.y, clockNeutralBounds)
                                    val isBlockedAtTopEdge = dragAmount.y < 0f && candidateY == currentClockOffsetY
                                    if (isBlockedAtTopEdge) {
                                        updateCollisionFeedback(clockBlocked = true, favoritesBlocked = false)
                                        return@detectDragGesturesAfterLongPress
                                    }
                                    val canApply = isValidLayoutState(
                                        favoritesX = 0f,
                                        favoritesY = currentFavOffsetY,
                                        clockX = 0f,
                                        clockY = candidateY
                                    )

                                    if (canApply) {
                                        currentClockOffsetX = 0f
                                        currentClockOffsetY = candidateY
                                        lastValidClockOffsetX = 0f
                                        lastValidClockOffsetY = candidateY
                                        isClockNavigationBarBlocked = false
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                    } else {
                                        updateCollisionFeedback(clockBlocked = true, favoritesBlocked = false)
                                    }
                                }
                            }
                    } else Modifier)
            ) {
                val isClockSelected = isEditMode && selectedEditTarget == HomeEditTarget.CLOCK
                val canMoveClockUp = isClockSelected && canMoveClockBy(-arrowProbeStepPx)
                val canMoveClockDown = isClockSelected && canMoveClockBy(arrowProbeStepPx)

                Box(
                    modifier = if (isEditMode) Modifier.padding(vertical = editHintContentPadding) else Modifier
                ) {
                    ClockHeader(
                        onAppLaunchForReturn = { pkg, bounds -> onLaunchApp(pkg, context.packageManager.getLaunchIntentForPackage(pkg)!!, bounds) },
                        onLaunchRequest = { launchRequest = it },
                        returnIconPackage = returnIconPackage,
                        // Kompaktmodus hält Uhr+Datum als gemeinsamen Block ohne volle Breite.
                        isCompact = isEditMode,
                        isPreview = isPreview || isEditMode
                    )
                }

                if (isClockSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                    ) {
                        if (canMoveClockUp) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = mainTextColor.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = editHintArrowInset)
                                    .size(editHintArrowSize)
                                    .testTag("home_edit_hint_clock_up")
                            )
                        }
                        if (canMoveClockDown) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = mainTextColor.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = editHintClockDownInset)
                                    .size(editHintArrowSize)
                                    .testTag("home_edit_hint_clock_down")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 2. Favoriten-Liste (Verschiebbar im Edit-Mode)
            Box(
                modifier = Modifier
                    .zIndex(if (isEditMode) 1500f else 0f)
                    .offset { IntOffset(0, currentFavOffsetY.roundToInt()) }
                    // Neutral-Bounds für Favoriten als Referenz ohne aktuelle Offsets pflegen.
                    .onGloballyPositioned { coordinates ->
                        val currentBounds = coordinates.boundsInRoot()
                        favoritesNeutralBounds = translateRect(
                            rect = currentBounds,
                            x = -currentFavOffsetX,
                            y = -currentFavOffsetY
                        )
                    }
                    .then(if (isEditMode) {
                        val isFavoritesSelected = selectedEditTarget == HomeEditTarget.FAVORITES
                        val showFavoritesFrame = selectedEditTarget == null || isFavoritesSelected
                        Modifier
                            .then(
                                if (showFavoritesFrame) {
                                    Modifier.border(
                                        BorderStroke(
                                            1.dp,
                                            mainTextColor.copy(alpha = if (isFavoritesSelected) 0.35f else 0.2f)
                                        ),
                                        RoundedCornerShape(16.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .testTag("home_edit_target_favorites")
                            .pointerInput(isEditMode) {
                                if (!isEditMode) return@pointerInput
                                detectTapGestures(
                                    onTap = { selectedEditTarget = HomeEditTarget.FAVORITES }
                                )
                            }
                            .pointerInput(isEditMode, selectedEditTarget) {
                                if (!isEditMode) return@pointerInput
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                    },
                                    onDragCancel = {
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                    }
                                ) { change, dragAmount ->
                                    if (selectedEditTarget != HomeEditTarget.FAVORITES) return@detectVerticalDragGestures
                                    change.consume()
                                    val (_, candidateY) = clampToRoot(0f, currentFavOffsetY + dragAmount, favoritesNeutralBounds)
                                    val canApply = isValidLayoutState(
                                        favoritesX = 0f,
                                        favoritesY = candidateY,
                                        clockX = 0f,
                                        clockY = currentClockOffsetY
                                    )

                                    if (canApply) {
                                        currentFavOffsetX = 0f
                                        currentFavOffsetY = candidateY
                                        lastValidFavOffsetX = 0f
                                        lastValidFavOffsetY = candidateY
                                        isFavoritesNavigationBarBlocked = false
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                    } else {
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = true)
                                    }
                                }
                            }
                            .pointerInput(isEditMode) {
                                if (!isEditMode) return@pointerInput
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        selectedEditTarget = HomeEditTarget.FAVORITES
                                    },
                                    onDragEnd = {
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                    },
                                    onDragCancel = {
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    val (_, candidateY) = clampToRoot(0f, currentFavOffsetY + dragAmount.y, favoritesNeutralBounds)
                                    val canApply = isValidLayoutState(
                                        favoritesX = 0f,
                                        favoritesY = candidateY,
                                        clockX = 0f,
                                        clockY = currentClockOffsetY
                                    )

                                    if (canApply) {
                                        currentFavOffsetX = 0f
                                        currentFavOffsetY = candidateY
                                        lastValidFavOffsetX = 0f
                                        lastValidFavOffsetY = candidateY
                                        isFavoritesNavigationBarBlocked = false
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                    } else {
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = true)
                                    }
                                }
                            }
                    } else Modifier)
            ) {
                val isFavoritesSelected = isEditMode && selectedEditTarget == HomeEditTarget.FAVORITES
                val canMoveFavoritesUp = isFavoritesSelected && canMoveFavoritesBy(-arrowProbeStepPx)
                val canMoveFavoritesDown = isFavoritesSelected && canMoveFavoritesBy(arrowProbeStepPx)

                Column(
                    // Kein einseitiger Start-Padding mehr, damit der Container visuell zentriert wirkt.
                    modifier = if (isEditMode) Modifier.padding(vertical = editHintContentPadding) else Modifier,
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

                if (isFavoritesSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                    ) {
                        if (canMoveFavoritesUp) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = mainTextColor.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = editHintArrowInset)
                                    .size(editHintArrowSize)
                                    .testTag("home_edit_hint_favorites_up")
                            )
                        }
                        if (canMoveFavoritesDown) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = mainTextColor.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = editHintArrowInset)
                                    .size(editHintArrowSize)
                                    .testTag("home_edit_hint_favorites_down")
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
                    .testTag("home_edit_controls")
                    .wrapContentWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 24.dp)
                    .onGloballyPositioned {
                        // Gesamte Edit-Controls als Sperrzone erfassen.
                        editControlsBounds = it.boundsInRoot()
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Kontroll-Buttons (Abbrechen, Speichern)
                Row(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Abbrechen
                    EditControlButton(
                        icon = Icons.Default.Close,
                        onClick = {
                            // Abbrechen: lokalen Bearbeitungszustand auf persistierten Stand zurücksetzen.
                            currentFavOffsetX = 0f
                            currentFavOffsetY = favoritesOffsetY
                            currentClockOffsetX = 0f
                            currentClockOffsetY = clockOffsetY
                            lastValidFavOffsetX = 0f
                            lastValidFavOffsetY = favoritesOffsetY
                            lastValidClockOffsetX = 0f
                            lastValidClockOffsetY = clockOffsetY
                            selectedEditTarget = null
                            isClockNavigationBarBlocked = false
                            isFavoritesNavigationBarBlocked = false
                            updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                            onToggleEditMode()
                        },
                        sizeDp = 56.dp,
                        tint = mainTextColor.copy(alpha = 0.6f),
                        testTag = "home_edit_cancel"
                    )


                    // Speichern
                    EditControlButton(
                        icon = Icons.Default.Check,
                        onClick = {
                            // WYSIWYG-Speichern: Sichtbare Edit-Position 1:1 in den Normalmodus uebertragen.
                            // Im Edit-Mode wird der Content vertikal gepolstert, daher Y um diesen Betrag kompensieren.
                            val savedFavoritesX = 0f
                            val savedFavoritesY = currentFavOffsetY + editHintContentPaddingPx
                            val savedClockX = 0f
                            val savedClockY = currentClockOffsetY + editHintContentPaddingPx

                            // UI auf die gespeicherten Werte synchronisieren.
                            currentFavOffsetX = savedFavoritesX
                            currentFavOffsetY = savedFavoritesY
                            currentClockOffsetX = savedClockX
                            currentClockOffsetY = savedClockY
                            lastValidFavOffsetX = savedFavoritesX
                            lastValidFavOffsetY = savedFavoritesY
                            lastValidClockOffsetX = savedClockX
                            lastValidClockOffsetY = savedClockY
                            isClockNavigationBarBlocked = false
                            isFavoritesNavigationBarBlocked = false
                            updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)

                            onSaveFavoritesOffset(savedFavoritesX, savedFavoritesY)
                            onSaveClockOffset(savedClockX, savedClockY)
                            Toast.makeText(context, "Position gespeichert", Toast.LENGTH_SHORT).show()
                            onToggleEditMode()
                        },
                        sizeDp = 56.dp,
                        containerColor = mainTextColor,
                        tint = if (isDarkTextEnabled) Color.White else Color(0xFF0F172A),
                        testTag = "home_edit_save"
                    )
                }
            }
        }

        // --- Standard UI (Settings & Search) - im Echtbetrieb immer sichtbar, auch im Edit-Mode ---
        if (!isPreview) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Fixe Position bleibt unten rechts, auch im Edit-Mode.
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
                                .onGloballyPositioned {
                                    val bounds = it.boundsInRoot()
                                    // Bounds lokal halten (Kollision) und zusätzlich an MainActivity melden (Return-Flow).
                                    searchButtonBounds = bounds
                                    onSearchButtonBoundsChanged(bounds)
                                }
                                .bounceClick(searchIntSrc)
                                .clickable(
                                    interactionSource = searchIntSrc,
                                    indication = null,
                                    enabled = !isEditMode
                                ) { onOpenSearch() },
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
                            .onGloballyPositioned {
                                // Settings-Bounds lokal halten, damit die Sperrzone im Edit-Mode aktiv ist.
                                settingsButtonBounds = it.boundsInRoot()
                            }
                            .then(
                                if (!isPreview) {
                                    Modifier
                                        .bounceClick(intSrc)
                                        .clickable(
                                            interactionSource = intSrc,
                                            indication = null,
                                            enabled = !isEditMode
                                        ) { onToggleSettings() }
                                } else {
                                    Modifier
                                }
                            ),
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
    sizeDp: androidx.compose.ui.unit.Dp = 56.dp,
    containerColor: Color = Color.Transparent,
    testTag: String? = null
) {
    val intSrc = remember { MutableInteractionSource() }
    val isLiquidGlassEnabled = LocalLiquidGlassEnabled.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current

    Box(
        modifier = Modifier
            .size(sizeDp)
            .then(if (containerColor == Color.Transparent) {
                Modifier.conditionalGlass(CircleShape, isDarkTextEnabled, isLiquidGlassEnabled, fallbackAlpha = 0.1f)
            } else {
                Modifier.background(containerColor, CircleShape)
            })
            .clip(CircleShape)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
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
    isCompact: Boolean = false,
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

    // Kompaktmodus nutzt Inhaltsbreite, damit der Edit-Container nicht unnötig breit wird.
    val headerModifier = if (isCompact) Modifier.wrapContentWidth(Alignment.Start) else Modifier.fillMaxWidth()

    Column(modifier = headerModifier) {
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
