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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
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
    var currentFavOffsetX by remember(favoritesOffsetX) { mutableStateOf(favoritesOffsetX) }
    var currentFavOffsetY by remember(favoritesOffsetY) { mutableStateOf(favoritesOffsetY) }
    // Uhrbereich (Uhr + Datum) ist eine Einheit und wird frei auf X/Y verschoben.
    var currentClockOffsetX by remember(clockOffsetX) { mutableStateOf(clockOffsetX) }
    var currentClockOffsetY by remember(clockOffsetY) { mutableStateOf(clockOffsetY) }

    // Letzte gültige Positionen: Bei Kollision wird darauf zurückgesetzt.
    var lastValidFavOffsetX by remember(favoritesOffsetX) { mutableStateOf(favoritesOffsetX) }
    var lastValidFavOffsetY by remember(favoritesOffsetY) { mutableStateOf(favoritesOffsetY) }
    var lastValidClockOffsetX by remember(clockOffsetX) { mutableStateOf(clockOffsetX) }
    var lastValidClockOffsetY by remember(clockOffsetY) { mutableStateOf(clockOffsetY) }

    // Neutral-Bounds sind Layout-Bounds ohne aktuelle Offsets; damit können wir sauber Kandidaten prüfen.
    var clockNeutralBounds by remember { mutableStateOf<Rect?>(null) }
    var favoritesNeutralBounds by remember { mutableStateOf<Rect?>(null) }
    // Live-Bounds der Bottom-Right-Controls (Lupe/Zahnrad) für Kollisionserkennung im Edit-Mode.
    var searchButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var settingsButtonBounds by remember { mutableStateOf<Rect?>(null) }

    // Visuelles Kollision-Feedback (Option A) + einmaliges Haptic pro Blockadephase.
    var isClockCollisionBlocked by remember { mutableStateOf(false) }
    var isFavoritesCollisionBlocked by remember { mutableStateOf(false) }
    var isClockNavigationBarBlocked by remember { mutableStateOf(false) }
    var isFavoritesNavigationBarBlocked by remember { mutableStateOf(false) }
    var collisionHapticWasTriggered by remember { mutableStateOf(false) }

    // Drag-Session-Daten: stabilisieren die Fingerbindung und verhindern Sprünge bei Blockaden.
    var clockDragBaseX by remember { mutableStateOf(0f) }
    var clockDragBaseY by remember { mutableStateOf(0f) }
    var clockDragAccumX by remember { mutableStateOf(0f) }
    var clockDragAccumY by remember { mutableStateOf(0f) }
    var favoritesDragBaseX by remember { mutableStateOf(0f) }
    var favoritesDragBaseY by remember { mutableStateOf(0f) }
    var favoritesDragAccumX by remember { mutableStateOf(0f) }
    var favoritesDragAccumY by remember { mutableStateOf(0f) }

    // Sobald der Finger den Container verlässt, pausieren wir die Session bis zum Loslassen.
    var isClockDragSuspended by remember { mutableStateOf(false) }
    var isFavoritesDragSuspended by remember { mutableStateOf(false) }

    // Containergrößen für präzise Pointer-Grenzprüfung in lokalen Koordinaten.
    var clockDragContainerSize by remember { mutableStateOf(IntSize.Zero) }
    var favoritesDragContainerSize by remember { mutableStateOf(IntSize.Zero) }

    // Snap-to-Grid ist standardmäßig aktiv; "soft" vermeidet hakelige Sprünge.
    val gridStepPx = with(density) { 12.dp.toPx() }
    val snapThresholdPx = with(density) { 4.dp.toPx() }
    // Kleine Toleranz vermeidet ungewollte Pausen bei minimalem Finger-Jitter an Kanten.
    val pointerBoundaryTolerancePx = with(density) { 10.dp.toPx() }
    // Die Favoriten-Markierung wird visuell um diesen Wert nach außen gezeichnet.
    val favoritesFramePaddingPx = with(density) { 10.dp.toPx() }
    // Für Nav-Bar-Kollision soll die Uhr den gleichen "gefühlten" Abstand haben wie die Favoriten.
    val clockNavCollisionPaddingPx = favoritesFramePaddingPx
    // Kleine Schutzkante um Search/Settings, damit die Container visuell nicht "ankleben".
    val bottomControlsPaddingPx = with(density) { 8.dp.toPx() }
    
    // Systemnavigation Bar Höhe ermitteln, um eine Sperrzone am unteren Bildschirmrand zu definieren.
    // Die Navigationgleiste darf nicht überlagert werden.
    val navigationBarHeightPx = with(density) { WindowInsets.systemBars.asPaddingValues().calculateBottomPadding().toPx() }
    
    // Launch Request State
    val launchRequestState = remember { mutableStateOf<HomeLaunchRequest?>(null) }
    var launchRequest by launchRequestState

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

    // Lokale Hilfsfunktion: "Magnetisches" Snap-to-Grid für flüssiges Live-Dragging.
    fun softSnap(value: Float): Float {
        if (gridStepPx <= 0f) return value
        val nearestGrid = (value / gridStepPx).roundToInt() * gridStepPx
        return if (kotlin.math.abs(value - nearestGrid) <= snapThresholdPx) nearestGrid else value
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

    // Lokale Hilfsfunktion: Sperrzonen für unten rechts sichtbare Controls (Lupe/Zahnrad) erzeugen.
    fun getBottomControlsForbiddenZones(): List<Rect> {
        val searchZone = if (!isSettingsOpen) {
            searchButtonBounds?.let { expandRect(it, bottomControlsPaddingPx) }
        } else {
            null
        }
        val settingsZone = settingsButtonBounds?.let { expandRect(it, bottomControlsPaddingPx) }
        return listOfNotNull(searchZone, settingsZone)
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

    // Lokale Hilfsfunktion: Prüft, ob Pointer noch im Container liegt (lokale Pointer-Koordinaten).
    fun isPointerInsideContainer(position: Offset, size: IntSize, tolerancePx: Float): Boolean {
        if (size.width <= 0 || size.height <= 0) return false
        return position.x >= -tolerancePx &&
            position.x <= size.width + tolerancePx &&
            position.y >= -tolerancePx &&
            position.y <= size.height + tolerancePx
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
        val bottomControlsZones = getBottomControlsForbiddenZones()

        // Solange Bounds fehlen, blockieren wir Speichern nicht unnötig.
        if (favoritesRect == null || clockRect == null) return true

        // Prüfe auf Überlappung zwischen Container
        if (intersects(favoritesRect, clockRect)) return false

        // Prüfe auf Überlappung mit der Systemnavigation
        val navigationBarZone = getNavigationBarForbiddenZone()
        if (intersects(favoritesRect, navigationBarZone) || (clockNavRect != null && intersects(clockNavRect, navigationBarZone))) return false

        // Prüfe auf Überlappung mit den unten rechts sichtbaren Controls.
        if (bottomControlsZones.any { zone ->
                intersects(favoritesRect, zone) || (clockNavRect != null && intersects(clockNavRect, zone))
            }) return false

        return true
    }

    // Lokale Hilfsfunktion: Liefert garantiert speicherbare Offsets, niemals mit Überlappung.
    fun resolveSavableOffsets(): Pair<Pair<Float, Float>, Pair<Float, Float>> {
        val candidates = listOf(
            // Bevorzugt wird der aktuelle sichtbare Zustand.
            Pair(currentFavOffsetX to currentFavOffsetY, currentClockOffsetX to currentClockOffsetY),
            // Falls die Uhr die blockierte Einheit war, nehmen wir deren letzten gültigen Zustand.
            Pair(currentFavOffsetX to currentFavOffsetY, lastValidClockOffsetX to lastValidClockOffsetY),
            // Falls die Favoriten die blockierte Einheit waren, nehmen wir deren letzten gültigen Zustand.
            Pair(lastValidFavOffsetX to lastValidFavOffsetY, currentClockOffsetX to currentClockOffsetY),
            // Letzter gemeinsamer Fallback: beide Einheiten auf zuletzt bekannte gültige Werte.
            Pair(lastValidFavOffsetX to lastValidFavOffsetY, lastValidClockOffsetX to lastValidClockOffsetY),
            // Sicherheitsnetz: neutrale Standardposition ist als gültiger Basispunkt immer verfügbar.
            Pair(0f to 0f, 0f to 0f)
        )

        return candidates.firstOrNull { candidate ->
            isValidLayoutState(
                favoritesX = candidate.first.first,
                favoritesY = candidate.first.second,
                clockX = candidate.second.first,
                clockY = candidate.second.second
            )
        } ?: Pair(0f to 0f, 0f to 0f)
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

            // Prüfe auf Überlappung mit den unten rechts sichtbaren Controls.
            val bottomControlsOverlap = getBottomControlsForbiddenZones().any { zone ->
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
                    // Im Edit-Modus bleibt der Drag-Container kompakt, damit X-Verschiebung spürbar möglich ist.
                    .then(if (isEditMode) Modifier.wrapContentWidth(Alignment.Start) else Modifier.fillMaxWidth())
                    // Uhrbereich wird als Einheit auf X/Y verschoben.
                    .offset { IntOffset(currentClockOffsetX.roundToInt(), currentClockOffsetY.roundToInt()) }
                    // Neutral-Bounds aus aktuellen Bounds ableiten, damit Kandidatenberechnung stabil bleibt.
                    .onGloballyPositioned { coordinates ->
                        val currentBounds = coordinates.boundsInRoot()
                        // Größe des Draggable-Containers für lokale Pointer-Prüfung übernehmen.
                        clockDragContainerSize = coordinates.size
                        clockNeutralBounds = translateRect(
                            rect = currentBounds,
                            x = -currentClockOffsetX,
                            y = -currentClockOffsetY
                        )
                    }
                    .then(if (isEditMode) {
                        // Beim Blockieren färben wir den Rahmen warm ein, damit klar ist: nicht erlaubt.
                        val isClockHighlighted = isClockCollisionBlocked || isClockNavigationBarBlocked || hasActiveContainerOverlap
                        val borderTint = if (isClockHighlighted) Color(0xFFFF7043) else mainTextColor.copy(alpha = 0.2f)
                        val fillTint = if (isClockHighlighted) Color(0xFFFF7043).copy(alpha = 0.12f) else mainTextColor.copy(alpha = 0.05f)
                        Modifier
                            .border(BorderStroke(1.dp, borderTint), RoundedCornerShape(16.dp))
                            .background(fillTint, RoundedCornerShape(16.dp))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        // Drag startet relativ zur aktuellen Position; dadurch bleibt der Finger "gekoppelt".
                                        clockDragBaseX = currentClockOffsetX
                                        clockDragBaseY = currentClockOffsetY
                                        clockDragAccumX = 0f
                                        clockDragAccumY = 0f
                                        // Neue Session startet immer aktiv.
                                        isClockDragSuspended = false
                                        isClockNavigationBarBlocked = false
                                        // Startet ohne aktiven Blockadezustand, damit Feedback pro Session klar ist.
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                    },
                                    onDragEnd = {
                                        // Session sauber abschließen, damit die nächste Geste frisch startet.
                                        clockDragAccumX = 0f
                                        clockDragAccumY = 0f
                                        isClockDragSuspended = false
                                        isClockNavigationBarBlocked = false
                                        // Nach Drag-Ende Blockadefeedback zurücksetzen.
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                    },
                                    onDragCancel = {
                                        // Bei Abbruch identisches Reset-Verhalten für konsistente Gesten.
                                        clockDragAccumX = 0f
                                        clockDragAccumY = 0f
                                        isClockDragSuspended = false
                                        isClockNavigationBarBlocked = false
                                        // Auch bei Abbruch keine hängenden Highlight-States behalten.
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                    }
                                ) { change, dragAmount ->
                                    change.consume()

                                    // Wenn Finger den Container verlässt, wird diese Session bis zum Loslassen pausiert.
                                    if (!isPointerInsideContainer(change.position, clockDragContainerSize, pointerBoundaryTolerancePx)) {
                                        isClockDragSuspended = true
                                        // Finger außerhalb: Session pausieren und aktives Blockade-Feedback beenden.
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                    }
                                    if (isClockDragSuspended) {
                                        return@detectDragGestures
                                    }

                                    // Delta in der aktuellen Session akkumulieren statt direkt Offsets zu addieren.
                                    clockDragAccumX += dragAmount.x
                                    clockDragAccumY += dragAmount.y

                                    // Kandidat relativ zur Session-Basis erzeugen und live auf Raster ziehen.
                                    val snappedX = softSnap(clockDragBaseX + clockDragAccumX)
                                    val snappedY = softSnap(clockDragBaseY + clockDragAccumY)

                                    // Kandidat im sichtbaren Bereich halten.
                                    val (candidateX, candidateY) = clampToRoot(
                                        candidateX = snappedX,
                                        candidateY = snappedY,
                                        neutralBounds = clockNeutralBounds
                                    )

                                    // Kandidat-Rect gegen aktuelle Favoriten-Position prüfen.
                                    val candidateClockRect = clockNeutralBounds?.let { translateRect(it, candidateX, candidateY) }
                                    val currentFavoritesRect = favoritesNeutralBounds?.let {
                                        expandRect(
                                            rect = translateRect(it, currentFavOffsetX, currentFavOffsetY),
                                            padding = favoritesFramePaddingPx
                                        )
                                    }

                                    val blockedByFavorites = candidateClockRect != null && currentFavoritesRect != null && intersects(candidateClockRect, currentFavoritesRect)
                                    val candidateClockNavRect = candidateClockRect?.let {
                                        expandRect(it, clockNavCollisionPaddingPx)
                                    }
                                    // Prüfe auch, ob der Kandidat die Systemnavigation überlagern würde.
                                    val blockedByNavigation = candidateClockNavRect != null && intersects(candidateClockNavRect, getNavigationBarForbiddenZone())
                                    // Prüfe zusätzlich auf Überlappung mit unten rechts sichtbaren Controls.
                                    val blockedByBottomControls = candidateClockNavRect != null &&
                                        getBottomControlsForbiddenZones().any { zone -> intersects(candidateClockNavRect, zone) }
                                    val blocked = blockedByFavorites || blockedByNavigation || blockedByBottomControls

                                    if (!blocked) {
                                        // Gültiger Move: live anwenden und als "letzte gültige" Position merken.
                                        currentClockOffsetX = candidateX
                                        currentClockOffsetY = candidateY
                                        lastValidClockOffsetX = candidateX
                                        lastValidClockOffsetY = candidateY
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                        isClockNavigationBarBlocked = false

                                        // Bei Hard-Limit (Screenrand) Session neu ankern, damit kein Nachziehen entsteht.
                                        val wasClamped = candidateX != snappedX || candidateY != snappedY
                                        if (wasClamped) {
                                            clockDragBaseX = currentClockOffsetX
                                            clockDragBaseY = currentClockOffsetY
                                            clockDragAccumX = 0f
                                            clockDragAccumY = 0f
                                        }
                                    } else {
                                        // Ungültiger Move: auf letzte gültige Position zurückfallen.
                                        currentClockOffsetX = lastValidClockOffsetX
                                        currentClockOffsetY = lastValidClockOffsetY
                                        // Bei Container-Kollision beide Einheiten markieren; bei Nav-Kollision nur die Uhr.
                                        updateCollisionFeedback(
                                            clockBlocked = true,
                                            favoritesBlocked = blockedByFavorites
                                        )
                                        // Für Clock-Hervorhebung teilen sich Nav-Bar und Bottom-Controls denselben "System-Block"-Kanal.
                                        isClockNavigationBarBlocked = blockedByNavigation || blockedByBottomControls

                                        // Bei Kollision Session neu ankern, damit der Finger nicht "driften" kann.
                                        clockDragBaseX = currentClockOffsetX
                                        clockDragBaseY = currentClockOffsetY
                                        clockDragAccumX = 0f
                                        clockDragAccumY = 0f
                                    }
                                }
                            }
                    } else Modifier)
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

            Spacer(modifier = Modifier.weight(1f))

            // 2. Favoriten-Liste (Verschiebbar im Edit-Mode)
            Box(
                modifier = Modifier
                    .offset { IntOffset(currentFavOffsetX.roundToInt(), currentFavOffsetY.roundToInt()) }
                    // Neutral-Bounds für Favoriten als Referenz ohne aktuelle Offsets pflegen.
                    .onGloballyPositioned { coordinates ->
                        val currentBounds = coordinates.boundsInRoot()
                        // Größe des Draggable-Containers für lokale Pointer-Prüfung übernehmen.
                        favoritesDragContainerSize = coordinates.size
                        favoritesNeutralBounds = translateRect(
                            rect = currentBounds,
                            x = -currentFavOffsetX,
                            y = -currentFavOffsetY
                        )
                    }
                    .then(if (isEditMode) {
                        // Gleiche Rückmeldung wie beim Uhrbereich: warmes Tinting bei Blockade (Favoriten oder Navigation Bar).
                        val isFavoritesHighlighted = isFavoritesCollisionBlocked || isFavoritesNavigationBarBlocked || hasActiveContainerOverlap
                        val borderTint = if (isFavoritesHighlighted) Color(0xFFFF7043) else mainTextColor.copy(alpha = 0.2f)
                        val fillTint = if (isFavoritesHighlighted) Color(0xFFFF7043).copy(alpha = 0.12f) else mainTextColor.copy(alpha = 0.05f)
                        Modifier
                            // Der Edit-Rahmen wird nur gezeichnet und verändert nicht mehr das Layout.
                            .drawBehind {
                                val framePadding = 10.dp.toPx()
                                val cornerRadius = 16.dp.toPx()
                                val frameTopLeft = Offset(-framePadding, -framePadding)
                                val frameSize = Size(
                                    width = size.width + framePadding * 2,
                                    height = size.height + framePadding * 2
                                )

                                // Füllung des Edit-Frames außerhalb des eigentlichen Content-Bounds zeichnen.
                                drawRoundRect(
                                    color = fillTint,
                                    topLeft = frameTopLeft,
                                    size = frameSize,
                                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                                )

                                // Rahmen separat zeichnen, damit die Position stabil bleibt.
                                drawRoundRect(
                                    color = borderTint,
                                    topLeft = frameTopLeft,
                                    size = frameSize,
                                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        // Drag startet relativ zur aktuellen Position; dadurch bleibt der Finger "gekoppelt".
                                        favoritesDragBaseX = currentFavOffsetX
                                        favoritesDragBaseY = currentFavOffsetY
                                        favoritesDragAccumX = 0f
                                        favoritesDragAccumY = 0f
                                        // Neue Session startet immer aktiv.
                                        isFavoritesDragSuspended = false
                                        isFavoritesNavigationBarBlocked = false
                                        // Startet ohne aktiven Blockadezustand, damit Feedback pro Session klar ist.
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                    },
                                    onDragEnd = {
                                        // Session sauber abschließen, damit die nächste Geste frisch startet.
                                        favoritesDragAccumX = 0f
                                        favoritesDragAccumY = 0f
                                        isFavoritesDragSuspended = false
                                        isFavoritesNavigationBarBlocked = false
                                        // Nach Drag-Ende Blockadefeedback zurücksetzen.
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                    },
                                    onDragCancel = {
                                        // Bei Abbruch identisches Reset-Verhalten für konsistente Gesten.
                                        favoritesDragAccumX = 0f
                                        favoritesDragAccumY = 0f
                                        isFavoritesDragSuspended = false
                                        isFavoritesNavigationBarBlocked = false
                                        // Auch bei Abbruch keine hängenden Highlight-States behalten.
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                    }
                                ) { change, dragAmount ->
                                    change.consume()

                                    // Wenn Finger den Container verlässt, wird diese Session bis zum Loslassen pausiert.
                                    if (!isPointerInsideContainer(change.position, favoritesDragContainerSize, pointerBoundaryTolerancePx)) {
                                        isFavoritesDragSuspended = true
                                        // Finger außerhalb: Session pausieren und aktives Blockade-Feedback beenden.
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                    }
                                    if (isFavoritesDragSuspended) {
                                        return@detectDragGestures
                                    }

                                    // Delta in der aktuellen Session akkumulieren statt direkt Offsets zu addieren.
                                    favoritesDragAccumX += dragAmount.x
                                    favoritesDragAccumY += dragAmount.y

                                    // Kandidat relativ zur Session-Basis erzeugen und live auf Raster ziehen.
                                    val snappedX = softSnap(favoritesDragBaseX + favoritesDragAccumX)
                                    val snappedY = softSnap(favoritesDragBaseY + favoritesDragAccumY)

                                    // Kandidat im sichtbaren Bereich halten.
                                    val (candidateX, candidateY) = clampToRoot(
                                        candidateX = snappedX,
                                        candidateY = snappedY,
                                        neutralBounds = favoritesNeutralBounds
                                    )

                                    // Kandidat-Rect gegen aktuelle Uhrposition prüfen.
                                    val candidateFavoritesRect = favoritesNeutralBounds?.let {
                                        expandRect(
                                            rect = translateRect(it, candidateX, candidateY),
                                            padding = favoritesFramePaddingPx
                                        )
                                    }
                                    val currentClockRect = clockNeutralBounds?.let {
                                        translateRect(it, currentClockOffsetX, currentClockOffsetY)
                                    }

                                    val blockedByClock = candidateFavoritesRect != null && currentClockRect != null && intersects(candidateFavoritesRect, currentClockRect)
                                    // Prüfe auch, ob der Kandidat die Systemnavigation überlagern würde.
                                    val blockedByNavigation = candidateFavoritesRect != null && intersects(candidateFavoritesRect, getNavigationBarForbiddenZone())
                                    // Prüfe zusätzlich auf Überlappung mit unten rechts sichtbaren Controls.
                                    val blockedByBottomControls = candidateFavoritesRect != null &&
                                        getBottomControlsForbiddenZones().any { zone -> intersects(candidateFavoritesRect, zone) }
                                    val blocked = blockedByClock || blockedByNavigation || blockedByBottomControls

                                    if (!blocked) {
                                        // Gültiger Move: live anwenden und als "letzte gültige" Position merken.
                                        currentFavOffsetX = candidateX
                                        currentFavOffsetY = candidateY
                                        lastValidFavOffsetX = candidateX
                                        lastValidFavOffsetY = candidateY
                                        updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                                        isFavoritesNavigationBarBlocked = false

                                        // Bei Hard-Limit (Screenrand) Session neu ankern, damit kein Nachziehen entsteht.
                                        val wasClamped = candidateX != snappedX || candidateY != snappedY
                                        if (wasClamped) {
                                            favoritesDragBaseX = currentFavOffsetX
                                            favoritesDragBaseY = currentFavOffsetY
                                            favoritesDragAccumX = 0f
                                            favoritesDragAccumY = 0f
                                        }
                                    } else {
                                        // Ungültiger Move: auf letzte gültige Position zurückfallen.
                                        currentFavOffsetX = lastValidFavOffsetX
                                        currentFavOffsetY = lastValidFavOffsetY
                                        // Bei Container-Kollision beide Einheiten markieren; bei Nav-Kollision nur Favoriten.
                                        updateCollisionFeedback(
                                            clockBlocked = blockedByClock,
                                            favoritesBlocked = true
                                        )
                                        // Für Favoriten-Hervorhebung teilen sich Nav-Bar und Bottom-Controls denselben "System-Block"-Kanal.
                                        isFavoritesNavigationBarBlocked = blockedByNavigation || blockedByBottomControls

                                        // Bei Kollision Session neu ankern, damit der Finger nicht "driften" kann.
                                        favoritesDragBaseX = currentFavOffsetX
                                        favoritesDragBaseY = currentFavOffsetY
                                        favoritesDragAccumX = 0f
                                        favoritesDragAccumY = 0f
                                    }
                                }
                            }
                    } else Modifier)
            ) {
                Column(
                    // Kein einseitiger Start-Padding mehr, damit der Container visuell zentriert wirkt.
                    modifier = Modifier,
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
                            // Reset setzt beide Einheiten (Favoriten + Uhrbereich) auf den Standard zurück.
                            currentFavOffsetX = 0f
                            currentFavOffsetY = 0f
                            currentClockOffsetX = 0f
                            currentClockOffsetY = 0f
                            lastValidFavOffsetX = 0f
                            lastValidFavOffsetY = 0f
                            lastValidClockOffsetX = 0f
                            lastValidClockOffsetY = 0f
                            // Drag-Session ebenfalls zurücksetzen, damit der nächste Touch sauber startet.
                            clockDragBaseX = 0f
                            clockDragBaseY = 0f
                            clockDragAccumX = 0f
                            clockDragAccumY = 0f
                            favoritesDragBaseX = 0f
                            favoritesDragBaseY = 0f
                            favoritesDragAccumX = 0f
                            favoritesDragAccumY = 0f
                            isClockDragSuspended = false
                            isFavoritesDragSuspended = false
                            isClockNavigationBarBlocked = false
                            isFavoritesNavigationBarBlocked = false
                            updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)
                        },
                        tint = mainTextColor
                    )

                    Spacer(modifier = Modifier.width(20.dp))

                    // Speichern
                    EditControlButton(
                        icon = Icons.Default.Check,
                        onClick = {
                            // Vor dem Persistieren wird immer ein final gültiger Zustand ermittelt.
                            val resolvedOffsets = resolveSavableOffsets()
                            val resolvedFavorites = resolvedOffsets.first
                            val resolvedClock = resolvedOffsets.second

                            // UI aktiv auf den akzeptierten Zustand zurücksetzen, damit nichts Inkonsistentes bleibt.
                            currentFavOffsetX = resolvedFavorites.first
                            currentFavOffsetY = resolvedFavorites.second
                            currentClockOffsetX = resolvedClock.first
                            currentClockOffsetY = resolvedClock.second
                            lastValidFavOffsetX = resolvedFavorites.first
                            lastValidFavOffsetY = resolvedFavorites.second
                            lastValidClockOffsetX = resolvedClock.first
                            lastValidClockOffsetY = resolvedClock.second
                            isClockNavigationBarBlocked = false
                            isFavoritesNavigationBarBlocked = false
                            updateCollisionFeedback(clockBlocked = false, favoritesBlocked = false)

                            // Persistenz wird nur mit garantiert validen Offsets geschrieben.
                            // Die Mode wird NACH den Save-Callbacks toggled, damit die DataStore Updates
                            // die UI States aktualisiert haben, bevor wir den Edit Mode ausschalten.
                            onSaveFavoritesOffset(resolvedFavorites.first, resolvedFavorites.second)
                            onSaveClockOffset(resolvedClock.first, resolvedClock.second)
                            Toast.makeText(context, "Position gespeichert", Toast.LENGTH_SHORT).show()
                            onToggleEditMode()
                        },
                        containerColor = mainTextColor,
                        tint = if (isDarkTextEnabled) Color.White else Color(0xFF0F172A)
                    )
                }
            }
        }

        // --- Standard UI (Settings & Search) - im Echtbetrieb immer sichtbar, auch im Edit-Mode ---
        if (!isPreview) {
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
                                .onGloballyPositioned {
                                    val bounds = it.boundsInRoot()
                                    // Bounds lokal halten (Kollision) und zusätzlich an MainActivity melden (Return-Flow).
                                    searchButtonBounds = bounds
                                    onSearchButtonBoundsChanged(bounds)
                                }
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
                            .onGloballyPositioned {
                                // Settings-Bounds lokal halten, damit die Sperrzone im Edit-Mode aktiv ist.
                                settingsButtonBounds = it.boundsInRoot()
                            }
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
