package com.example.androidlauncher.ui

import android.appwidget.AppWidgetHostView
import android.content.Intent
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.androidlauncher.R
import com.example.androidlauncher.data.AppAccessMode
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.FavoritesBorderStyle
import com.example.androidlauncher.data.GestureAction
import com.example.androidlauncher.data.HomeLayout
import com.example.androidlauncher.data.HostedWidget
import com.example.androidlauncher.launchShortcut
import com.example.androidlauncher.ui.LiquidGlass.designSurface
import com.example.androidlauncher.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Verschiebbares Element auf dem Startbildschirm. Sealed statt Enum, damit gehostete
 * System-Widgets (B1) mit ihrer dynamischen ID als gleichberechtigte Drag-Targets
 * durch dieselbe Mechanik (Offsets, Clamping, Hit-Test, Edit-Rahmen) laufen.
 */
internal sealed interface HomeEditTarget {
    data object Clock : HomeEditTarget
    data object Date : HomeEditTarget
    data object Weather : HomeEditTarget
    data object Favorites : HomeEditTarget

    /** Gehostetes System-Widget, identifiziert über seine AppWidget-ID. */
    data class Widget(val appWidgetId: Int) : HomeEditTarget
}

/**
 * Homescreen des Launchers mit intuitivem Bearbeitungsmodus für Element-Positionen.
 */
@Composable
fun HomeScreen(
    favorites: List<AppInfo>,
    allApps: List<AppInfo> = emptyList(),
    appAccessMode: AppAccessMode = AppAccessMode.DRAWER_GRID,
    isSettingsOpen: Boolean,
    isSearchOpen: Boolean,
    isEditMode: Boolean = false,
    homeLayout: HomeLayout = HomeLayout(),
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    doubleTapAction: GestureAction = GestureAction.LOCK_SCREEN,
    doubleTapAppPackage: String? = null,
    onGestureAction: (GestureAction, String?) -> Unit = { _, _ -> },
    onToggleSettings: () -> Unit,
    onToggleEditMode: () -> Unit,
    onOpenFavoritesConfig: () -> Unit,
    onOpenColorConfig: () -> Unit,
    onOpenSizeConfig: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onOpenInfo: () -> Unit,
    onSaveHomeLayout: (HomeLayout) -> Unit,
    onLaunchApp: (String, Intent, Rect?) -> Unit,
    returnIconPackage: String?,
    searchButtonBounceToken: Int = 0,
    onSearchButtonBoundsChanged: (Rect?) -> Unit = {},
    isPreview: Boolean = false,
    // B1: gehostete System-Widgets als frei verschiebbare Home-Objekte.
    hostedWidgets: List<HostedWidget> = emptyList(),
    widgetViewProvider: (Int) -> AppWidgetHostView? = { null },
    onSaveWidgetOffsets: (Map<Int, Offset>) -> Unit = {},
    onRemoveWidget: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val colorTheme = LocalColorTheme.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val showLabels = LocalShowFavoriteLabels.current
    val fontSize = LocalFontSize.current
    val favoriteSpacing = LocalFavoriteSpacing.current
    val designStyle = LocalDesignStyle.current
    val surfaceAccent = colorTheme.menuSurfaceColor(isDarkTextEnabled)
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val hapticEnabled = LocalHapticFeedbackEnabled.current
    val animationsEnabled = LocalAnimationsEnabled.current
    val menuAnimationsEnabled = LocalMenuAnimationEnabled.current
    val density = androidx.compose.ui.platform.LocalDensity.current

    // Schriftfarbe nur auf dem Startbildschirm frei wählbar.
    val mainTextColor = LocalHomeTextColor.current
    var rootSize by remember { mutableStateOf(IntSize.Zero) }

    // --- Bearbeitungs-States (Lokal für Live-Vorschau) ---
    // A6-Split: gebündelt im HomeDragStateHolder; die Delegates/Aliase darunter
    // halten die bestehenden Lese-/Schreib-Stellen im Composable stabil.
    val dragState = rememberHomeDragState(homeLayout, hostedWidgets)
    val offsets = dragState.offsets
    val neutralBounds = dragState.neutralBounds
    val lastBlockedHapticMs = dragState.lastBlockedHapticMs

    // Hält die Live-Offsets außerhalb des Edit-Modus an der gespeicherten Position;
    // beim Betreten des Edit-Modus werden sie 1:1 daraus geseedet, Abbrechen revertiert.
    LaunchedEffect(homeLayout, hostedWidgets, isEditMode) {
        dragState.seedFrom(homeLayout, hostedWidgets)
    }

    // Tickt die Uhrzeit für die getrennten Uhr-/Datums-Elemente.
    var currentTime by remember { mutableStateOf(Calendar.getInstance().time) }
    val clockLifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance().time
            // Exakt bis zum nächsten Minutenwechsel warten, damit die Anzeige
            // minutengenau mit der echten Uhr umspringt (statt bis zu 30s versetzt).
            val now = System.currentTimeMillis()
            delay(60_000L - (now % 60_000L))
        }
    }
    // Sofort aktualisieren, sobald der Launcher wieder in den Vordergrund kommt –
    // sonst zeigt er nach längerer Pause kurz die alte Zeit.
    DisposableEffect(clockLifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentTime = Calendar.getInstance().time
            }
        }
        clockLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { clockLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var searchButtonBounds by dragState::searchButtonBounds
    var settingsButtonBounds by dragState::settingsButtonBounds
    var editControlsBounds by dragState::editControlsBounds
    var collisionHapticWasTriggered by dragState::collisionHapticWasTriggered

    val favoritesFramePaddingPx = with(density) { 10.dp.toPx() }
    val navCollisionPaddingPx = favoritesFramePaddingPx
    val bottomControlsPaddingPx = with(density) { 8.dp.toPx() }

    val navigationBarHeightPx =
        with(density) { WindowInsets.systemBars.asPaddingValues().calculateBottomPadding().toPx() }
    val editSelectionHitPaddingPx = with(density) { 12.dp.toPx() }
    val dragVisualDeadzonePx = 1f
    val reachabilityProbeStepPx = 1f
    val blockedDragHapticMinIntervalMs = 45L
    val blockedDragHapticMinDeltaPx = 0.25f
    val clockTopLimitPx = with(density) { 32.dp.toPx() }

    var selectedEditTarget by dragState::selectedEditTarget
    var isEditTargetUserPinned by dragState::isEditTargetUserPinned
    var selectedShortcutApp by remember { mutableStateOf<AppInfo?>(null) }
    var shortcutMenuBounds by remember { mutableStateOf<Rect?>(null) }

    // translateRect/expandRect/intersects liegen jetzt framework-frei in HomeGeometry.kt
    // (unit-getestet); die Aufrufe loesen auf die Top-Level-Funktionen auf.

    // rectContains(...) liegt jetzt framework-frei in HomeGeometry.kt (unit-getestet); die
    // Aufrufe unten loesen auf die Top-Level-Funktion auf.

    // Sammel-Padding je Element: Favoriten behalten ihren Rahmenabstand, Text-Elemente
    // (Uhr/Datum/Wetter) liegen im Neutralzustand bündig gestapelt → kein Inter-Padding.
    fun framePadding(target: HomeEditTarget): Float =
        if (target == HomeEditTarget.Favorites) favoritesFramePaddingPx else 0f

    fun baseRect(target: HomeEditTarget, o: Offset): Rect? =
        neutralBounds[target]?.let { translateRect(it, o.x, o.y) }

    fun navRect(target: HomeEditTarget, o: Offset): Rect? =
        baseRect(target, o)?.let { expandRect(it, navCollisionPaddingPx) }

    fun clampOffset(target: HomeEditTarget, x: Float, y: Float): Offset {
        val bounds = neutralBounds[target] ?: return Offset(x, y)
        val topLimit = if (target == HomeEditTarget.Clock) clockTopLimitPx else 0f
        // Reine Clamping-Mathematik liegt in HomeGeometry.kt (unit-getestet); die
        // target->bounds/topLimit-Aufloesung bleibt hier (haengt am neutralBounds-State).
        return clampOffsetToBounds(
            bounds = bounds,
            rootWidth = rootSize.width,
            rootHeight = rootSize.height,
            topLimit = topLimit,
            navigationBarHeightPx = navigationBarHeightPx,
            x = x,
            y = y,
        )
    }

    fun getNavigationBarForbiddenZone(): Rect =
        navigationBarForbiddenZone(rootSize.width, rootSize.height, navigationBarHeightPx)

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
            listOfNotNull(searchZone, settingsZone)
        }
    }

    fun updateCollisionFeedback(blocked: Boolean) {
        if (blocked && !collisionHapticWasTriggered && hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            collisionHapticWasTriggered = true
        }
        if (!blocked) {
            collisionHapticWasTriggered = false
        }
    }

    /**
     * Harte Constraints für [target] an [pos]: innerhalb des Bildschirms (kein Clamp nötig)
     * und keine Kollision mit Navigationsleiste oder unteren Steuer-Buttons.
     */
    fun hardOk(target: HomeEditTarget, pos: Offset): Boolean {
        val clamped = clampOffset(target, pos.x, pos.y)
        if (clamped.x != pos.x || clamped.y != pos.y) return false
        val rect = navRect(target, pos) ?: return true
        if (intersects(rect, getNavigationBarForbiddenZone())) return false
        if (bottomControlsForbiddenZones.any { intersects(rect, it) }) return false
        return true
    }

    /**
     * Sucht eine erreichbare Offset-Position zwischen [baseOffset] und [desiredOffset].
     *
     * Widgets dürfen sich frei überlappen – es gibt keine Kollisionssperre zwischen den
     * Home-Elementen mehr. Begrenzt wird nur durch die harten Constraints ([hardOkAt]):
     * Bildschirmrand, System-Navigationsleiste und untere Steuer-Buttons. Das Probing entlang
     * der Achse sorgt dafür, dass ein Zug Richtung Sperrzone so weit wie erlaubt gleitet.
     */
    fun reachableOffset(
        baseOffset: Float,
        desiredOffset: Float,
        hardOkAt: (Float) -> Boolean
    ): Float? {
        fun acceptable(offset: Float): Boolean = hardOkAt(offset)

        if (abs(desiredOffset - baseOffset) < 0.5f) {
            return if (acceptable(baseOffset)) baseOffset else null
        }

        val stepPx = reachabilityProbeStepPx
        val direction = if (desiredOffset > baseOffset) 1f else -1f
        var probe = desiredOffset

        while (true) {
            if (acceptable(probe)) return probe
            val next = probe - (direction * stepPx)
            val crossedBase = (direction > 0f && next < baseOffset) || (direction < 0f && next > baseOffset)
            if (crossedBase) break
            probe = next
        }

        return if (acceptable(baseOffset)) baseOffset else null
    }

    /**
     * Wendet eine 2D-Ziehbewegung auf [target] an. Achsen werden getrennt aufgelöst
     * (dominante Achse zuerst, mindert Eck-Klemmen); je Achse sorgt [reachableOffset]
     * dafür, dass keine neue Überlappung mit anderen Elementen oder Sperrzonen entsteht.
     */
    fun applyDrag(target: HomeEditTarget, drag: Offset) {
        val cur = offsets[target] ?: Offset.Zero

        fun resolveX(fromX: Float, fromY: Float): Float {
            val desired = clampOffset(target, fromX + drag.x, fromY).x
            return reachableOffset(
                baseOffset = fromX,
                desiredOffset = desired,
                hardOkAt = { x -> hardOk(target, Offset(x, fromY)) }
            ) ?: fromX
        }
        fun resolveY(fromX: Float, fromY: Float): Float {
            val desired = clampOffset(target, fromX, fromY + drag.y).y
            return reachableOffset(
                baseOffset = fromY,
                desiredOffset = desired,
                hardOkAt = { y -> hardOk(target, Offset(fromX, y)) }
            ) ?: fromY
        }

        val newX: Float
        val newY: Float
        if (abs(drag.x) >= abs(drag.y)) {
            newX = resolveX(cur.x, cur.y)
            newY = resolveY(newX, cur.y)
        } else {
            newY = resolveY(cur.x, cur.y)
            newX = resolveX(cur.x, newY)
        }

        val moved = abs(newX - cur.x) + abs(newY - cur.y)
        if (moved >= dragVisualDeadzonePx) {
            offsets[target] = Offset(newX, newY)
            lastBlockedHapticMs[target] = 0L
            updateCollisionFeedback(false)
        } else {
            val hasIntent = abs(drag.x) > 0f || abs(drag.y) > 0f
            val strongEnough = abs(drag.x) >= blockedDragHapticMinDeltaPx ||
                abs(drag.y) >= blockedDragHapticMinDeltaPx
            if (hasIntent && strongEnough && hapticEnabled) {
                val now = SystemClock.uptimeMillis()
                if (now - (lastBlockedHapticMs[target] ?: 0L) >= blockedDragHapticMinIntervalMs) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastBlockedHapticMs[target] = now
                }
            } else {
                lastBlockedHapticMs[target] = 0L
            }
            updateCollisionFeedback(hasIntent)
        }
    }

    fun editZIndex(target: HomeEditTarget): Float = when {
        selectedEditTarget == target -> 1600f
        selectedEditTarget == null -> 1500f
        else -> 1400f
    }

    fun testTagFor(target: HomeEditTarget): String = when (target) {
        HomeEditTarget.Clock -> "home_edit_target_clock"
        HomeEditTarget.Date -> "home_edit_target_date"
        HomeEditTarget.Weather -> "home_edit_target_weather"
        HomeEditTarget.Favorites -> "home_edit_target_favorites"
        is HomeEditTarget.Widget -> "home_edit_target_widget_${target.appWidgetId}"
    }

    // Wackel-Richtung im Edit-Modus: benachbarte Elemente rotieren gegenläufig
    // (früher `ordinal % 2`; Widgets alternieren über ihre AppWidget-ID).
    fun wiggleSign(target: HomeEditTarget): Float = when (target) {
        HomeEditTarget.Clock, HomeEditTarget.Weather -> 1f
        HomeEditTarget.Date, HomeEditTarget.Favorites -> -1f
        is HomeEditTarget.Widget -> if (target.appWidgetId % 2 == 0) 1f else -1f
    }

    // Material-3-Expressive: dezentes „Jiggle" der bewegbaren Home-Elemente im Edit-Modus,
    // damit klar wird, dass sie verschoben werden können (iOS-artig, aber zurückhaltend).
    val editWiggle = rememberInfiniteTransition(label = "homeEditWiggle")
    val editWiggleAngle by editWiggle.animateFloat(
        initialValue = -1.1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(170, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "homeEditWiggleAngle"
    )

    // Offset + Neutralbounds gelten in beiden Modi (gespeichertes Layout wird auch normal angezeigt).
    fun Modifier.targetLayout(target: HomeEditTarget): Modifier = this
        .zIndex(if (!isEditMode) 0f else editZIndex(target))
        .offset {
            val o = offsets[target] ?: Offset.Zero
            IntOffset(o.x.roundToInt(), o.y.roundToInt())
        }
        .onGloballyPositioned { coords ->
            val o = offsets[target] ?: Offset.Zero
            neutralBounds[target] = translateRect(coords.boundsInRoot(), -o.x, -o.y)
        }

    // Auswahlrahmen + Tap/Drag-Gesten – nur im Edit-Modus.
    fun Modifier.targetEditModifier(target: HomeEditTarget): Modifier {
        if (!isEditMode) return this
        val isSelected = selectedEditTarget == target
        val showFrame = selectedEditTarget == null || isSelected
        return this
            .graphicsLayer {
                if (animationsEnabled) {
                    // Ausgewähltes (= bewegtes) Element hebt sich an; die übrigen
                    // bewegbaren „wackeln" dezent, abwechselnd in Gegenrichtung.
                    if (isSelected) {
                        scaleX = 1.04f
                        scaleY = 1.04f
                    } else if (showFrame) {
                        rotationZ = editWiggleAngle * wiggleSign(target)
                    }
                }
            }
            .then(
                if (showFrame) {
                    Modifier.border(
                        BorderStroke(
                            1.dp,
                            mainTextColor.copy(alpha = if (isSelected) 0.35f else 0.2f)
                        ),
                        RoundedCornerShape(20.dp)
                    )
                } else {
                    Modifier
                }
            )
            .testTag(testTagFor(target))
            .pointerInput(isEditMode, target) {
                detectTapGestures(
                    onTap = {
                        selectedEditTarget = target
                        isEditTargetUserPinned = true
                    }
                )
            }
            .pointerInput(isEditMode, target) {
                var owns = false
                detectDragGestures(
                    onDragStart = {
                        owns = selectedEditTarget == null || selectedEditTarget == target
                        if (owns) {
                            selectedEditTarget = target
                            isEditTargetUserPinned = false
                            lastBlockedHapticMs[target] = 0L
                        }
                    },
                    onDragEnd = {
                        if (owns) {
                            lastBlockedHapticMs[target] = 0L
                            updateCollisionFeedback(false)
                        }
                        owns = false
                    },
                    onDragCancel = {
                        if (owns) {
                            lastBlockedHapticMs[target] = 0L
                            updateCollisionFeedback(false)
                        }
                        owns = false
                    }
                ) { change, drag ->
                    if (!owns || selectedEditTarget != target) return@detectDragGestures
                    change.consume()
                    applyDrag(target, drag)
                }
            }
    }

    LaunchedEffect(isSettingsOpen) {
        if (isSettingsOpen) {
            searchButtonBounds = null
            onSearchButtonBoundsChanged(null)
        }
    }

    LaunchedEffect(isEditMode) {
        if (!isEditMode) {
            editControlsBounds = null
        }
        selectedEditTarget = null
        isEditTargetUserPinned = false
    }

    // Aktuelle Werte für die Doppeltipp-Geste: der pointerInput-Block unten wird nur
    // bei isEditMode neu gestartet, würde sonst aber veraltete Werte „einfrieren".
    // rememberUpdatedState sorgt dafür, dass die Geste immer den aktuellen Wert liest.
    val currentDoubleTapAction by rememberUpdatedState(doubleTapAction)
    val currentDoubleTapAppPackage by rememberUpdatedState(doubleTapAppPackage)
    val currentOnGestureAction by rememberUpdatedState(onGestureAction)

    val rotation by animateFloatAsState(
        targetValue = if (isSettingsOpen) 180f else 0f,
        animationSpec = tween(if (menuAnimationsEnabled) 300 else 0, easing = EaseInOutCubic),
        label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen")
            .onGloballyPositioned { rootSize = it.size }
            .then(
                if (!isPreview) {
                    Modifier
                        .pointerInput(isEditMode, appAccessMode) {
                            if (!isEditMode) {
                                // Untere System-Gestenzone (Home-Wisch) aussparen, sonst öffnet ein Wisch
                                // von ganz unten versehentlich den Drawer. Zudem die Gesamtstrecke
                                // akkumulieren (robuster als der fragile Pro-Frame-Delta).
                                val deadZonePx = 48.dp.toPx()
                                val thresholdPx = 60f
                                var startY = 0f
                                var totalDy = 0f
                                var handled = false
                                detectVerticalDragGestures(
                                    onDragStart = { offset ->
                                        startY = offset.y
                                        totalDy = 0f
                                        handled = false
                                    },
                                    onDragCancel = { handled = false }
                                ) { _, dragAmount ->
                                    val startedInBottomGestureZone = startY > size.height - deadZonePx
                                    if (!handled && !startedInBottomGestureZone) {
                                        totalDy += dragAmount
                                        // Im HOME_LIST-Modus gibt es keinen hochziehbaren Drawer – der App-Zugriff
                                        // läuft über die seitliche Randleiste (HomeAppScrubber).
                                        if (totalDy < -thresholdPx && appAccessMode != AppAccessMode.HOME_LIST) {
                                            handled = true
                                            onOpenDrawer()
                                        } else if (totalDy > thresholdPx) {
                                            handled = true
                                            expandNotifications(context)
                                        }
                                    }
                                }
                            }
                        }
                        .pointerInput(isEditMode) {
                            detectTapGestures(
                                onTap = { tapOffset ->
                                    if (isEditMode && selectedEditTarget != null) {
                                        // Alle gemessenen Targets (eingebaute + Widgets) prüfen.
                                        val hitTarget = neutralBounds.keys.toList().any { target ->
                                            val rect = neutralBounds[target]?.let {
                                                translateRect(it, offsets[target]?.x ?: 0f, offsets[target]?.y ?: 0f)
                                            } ?: return@any false
                                            val hitRect = expandRect(
                                                rect,
                                                framePadding(target) + editSelectionHitPaddingPx
                                            )
                                            rectContains(hitRect, tapOffset)
                                        }
                                        val hitEditControls = editControlsBounds?.let { rectContains(it, tapOffset) } == true

                                        if (!hitTarget && !hitEditControls) {
                                            selectedEditTarget = null
                                            isEditTargetUserPinned = false
                                        }
                                    }
                                },
                                onDoubleTap = {
                                    if (isEditMode) return@detectTapGestures
                                    currentOnGestureAction(currentDoubleTapAction, currentDoubleTapAppPackage)
                                }
                            )
                        }
                } else {
                    Modifier
                }
            )
    ) {
        // --- Haupt-Layout ---
        // Zwei voneinander unabhängige, bildschirmfüllende Ebenen: die obere Gruppe
        // (Uhr/Datum/Wetter) ist oben verankert, die Favoriten sind mittig zentriert.
        // Dadurch verschiebt das Ein-/Ausschalten des Wetters die Favoriten NICHT mehr –
        // jedes Element folgt seiner festen Neutralposition plus gespeichertem Offset.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // Gemeinsamer Rückkehr-Launch-Callback für die Uhr-/Datums-Elemente.
            val launchForReturn: (String, Rect?) -> Unit = { pkg, bounds ->
                onLaunchApp(pkg, context.packageManager.getLaunchIntentForPackage(pkg)!!, bounds)
            }

            // Obere Gruppe (oben verankert): nur die Uhr. Das Datum liegt bewusst auf einer
            // eigenen Ebene (siehe unten), damit es NICHT von der – je nach Schriftart
            // unterschiedlich hohen – Uhr-Box mitgeschoben wird.
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(30.dp))

                // 1. Uhr (unabhängig verschiebbar, nur wenn aktiviert). Natürliche Höhe
                // (wrapContent), damit hohe/dekorative Schriften nicht abgeschnitten werden.
                if (LocalClockWidgetEnabled.current) {
                    Box(
                        modifier = Modifier
                            .wrapContentWidth(Alignment.Start)
                            .targetLayout(HomeEditTarget.Clock)
                            .targetEditModifier(HomeEditTarget.Clock)
                    ) {
                        ClockText(
                            time = currentTime,
                            isPreview = isPreview || isEditMode,
                            returnIconPackage = returnIconPackage,
                            onAppLaunchForReturn = launchForReturn
                        )
                    }
                }
            }

            // 2. Datum: eigene Ebene mit fester, schriftunabhängiger Verankerung unter der
            // Standard-Uhrposition. Dadurch springt das Datum beim Schriftartwechsel nicht mehr
            // weg (es hängt nicht mehr an der gemessenen Uhr-Box-Höhe). Frei verschiebbar.
            if (LocalCalendarWidgetEnabled.current) {
                val dateTopAnchor = 30.dp + with(density) { (72.sp * fontSize.scale).toDp() } + 8.dp
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = dateTopAnchor)
                        .targetLayout(HomeEditTarget.Date)
                        .targetEditModifier(HomeEditTarget.Date)
                ) {
                    DateText(
                        time = currentTime,
                        isPreview = isPreview || isEditMode,
                        returnIconPackage = returnIconPackage,
                        onAppLaunchForReturn = launchForReturn
                    )
                }
            }

            // 3. Wetter: standardmäßig rechts oben in der Ecke (unter der Statusleiste,
            // auf Höhe der Uhr), weiterhin frei verschiebbar. Eigene Ebene, damit es
            // unabhängig von Uhr/Datum positioniert ist.
            if (LocalWeatherWidgetEnabled.current) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 30.dp)
                        .targetLayout(HomeEditTarget.Weather)
                        .targetEditModifier(HomeEditTarget.Weather)
                ) {
                    WeatherRow(isPreview = isPreview || isEditMode)
                }
            }

            // 5. Gehostete System-Widgets (B1): frei verschiebbar wie die eingebauten
            // Elemente. Natürliche Ankerposition unterhalb der Uhr-Zone, pro Widget leicht
            // gestaffelt, damit neu hinzugefügte nicht exakt übereinander liegen.
            hostedWidgets.forEachIndexed { index, widget ->
                key(widget.appWidgetId) {
                    val widgetTarget = HomeEditTarget.Widget(widget.appWidgetId)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 160.dp + (index * 24).dp)
                            .targetLayout(widgetTarget)
                            .targetEditModifier(widgetTarget)
                    ) {
                        HostedWidgetView(
                            widget = widget,
                            hostView = if (isPreview) null else widgetViewProvider(widget.appWidgetId),
                            isEditMode = isEditMode,
                        )
                        // Entfernen-Badge am ausgewählten Widget (nur im Edit-Modus).
                        if (isEditMode && selectedEditTarget == widgetTarget) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .zIndex(10f)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .designSurface(
                                        designStyle,
                                        CircleShape,
                                        isDarkTextEnabled,
                                        surfaceAccent,
                                        fillAlpha = 0.5f
                                    )
                                    .clickable { onRemoveWidget(widget.appWidgetId) }
                                    .testTag("home_widget_remove_${widget.appWidgetId}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.cd_remove_widget),
                                    tint = mainTextColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Favoriten-Ebene (unabhängig von der oberen Gruppe). Standardmäßig im unteren
            // Drittel verankert (mehr Abstand zu den Widgets) – feste Gewichte, daher
            // unabhängig vom Wetter-Schalter.
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.weight(1.9f))

                // 4. Favoriten-Liste (unabhängig verschiebbar)
                Box(
                    modifier = Modifier
                        .wrapContentWidth(Alignment.Start)
                        .targetLayout(HomeEditTarget.Favorites)
                        .targetEditModifier(HomeEditTarget.Favorites)
                ) {
                    // Optionale Umrandung der Favoriten-Box (reiner Umriss, innen transparent).
                    val favoritesBorderColor = when (LocalFavoritesBorderStyle.current) {
                        FavoritesBorderStyle.NONE -> null
                        FavoritesBorderStyle.BLACK -> Color.Black.copy(alpha = 0.85f)
                        FavoritesBorderStyle.WHITE -> Color.White.copy(alpha = 0.85f)
                        FavoritesBorderStyle.ACCENT -> colorTheme.accentColor(isDarkTextEnabled)
                        FavoritesBorderStyle.SUBTLE -> mainTextColor.copy(alpha = 0.25f)
                    }
                    val favoritesBoxModifier = if (favoritesBorderColor != null) {
                        Modifier
                            .border(BorderStroke(1.5.dp, favoritesBorderColor), RoundedCornerShape(28.dp))
                            .padding(10.dp)
                    } else {
                        Modifier
                    }
                    // --- Niagara-artiges „Rüberfahren" über die Favoriten-Leiste ---
                    // Finger vertikal über die Leiste ziehen → App unter dem Finger wird
                    // hervorgehoben (+ Vibration), beim Loslassen öffnet sie sich.
                    val favView = LocalView.current
                    val touchSlop = LocalViewConfiguration.current.touchSlop
                    var hoveredFavIndex by remember { mutableIntStateOf(-1) }
                    val favItemBounds = remember(favorites) { mutableStateMapOf<Int, Rect>() }
                    var favColumnTop by remember { mutableFloatStateOf(0f) }
                    val favScrubEnabled = !isPreview && !isEditMode && favorites.isNotEmpty()
                    val favScrubModifier = if (favScrubEnabled) {
                        Modifier
                            .onGloballyPositioned { favColumnTop = it.boundsInRoot().top }
                            .pointerInput(favorites) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val down = awaitFirstDown(
                                            requireUnconsumed = false,
                                            pass = PointerEventPass.Initial
                                        )
                                        val start = down.position
                                        var scrubbing = false
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            if (!change.pressed) {
                                                if (scrubbing && hoveredFavIndex in favorites.indices) {
                                                    val app = favorites[hoveredFavIndex]
                                                    context.packageManager
                                                        .getLaunchIntentForPackage(app.packageName)
                                                        ?.let { intent ->
                                                            onLaunchApp(
                                                                app.packageName,
                                                                intent,
                                                                favItemBounds[hoveredFavIndex]
                                                            )
                                                        }
                                                }
                                                break
                                            }
                                            val dy = change.position.y - start.y
                                            val dx = change.position.x - start.x
                                            if (!scrubbing && abs(dy) > touchSlop && abs(dy) > abs(dx)) {
                                                scrubbing = true
                                            }
                                            if (scrubbing) {
                                                // Finger weit außerhalb der Leiste (oben/unten ODER
                                                // seitlich) → Scrubben abbrechen.
                                                val marginY = 56.dp.toPx()
                                                val marginX = 120.dp.toPx()
                                                if (change.position.y < -marginY ||
                                                    change.position.y > size.height + marginY ||
                                                    change.position.x < -marginX ||
                                                    change.position.x > size.width + marginX
                                                ) {
                                                    hoveredFavIndex = -1
                                                    break
                                                }
                                                val rootY = favColumnTop + change.position.y
                                                // Nächstgelegene App wählen (auch in den Lücken zwischen den Items).
                                                val idx = favorites.indices.minByOrNull { i ->
                                                    favItemBounds[i]?.let { abs(rootY - (it.top + it.bottom) / 2f) }
                                                        ?: Float.MAX_VALUE
                                                } ?: -1
                                                if (idx != hoveredFavIndex) {
                                                    hoveredFavIndex = idx
                                                    if (idx >= 0 && hapticEnabled) {
                                                        favView.performHapticFeedback(
                                                            HapticFeedbackConstants.KEYBOARD_TAP,
                                                            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                                                        )
                                                    }
                                                }
                                                change.consume()
                                            }
                                        }
                                        hoveredFavIndex = -1
                                    }
                                }
                            }
                    } else {
                        Modifier
                    }
                    Column(
                        modifier = favoritesBoxModifier.then(favScrubModifier),
                        verticalArrangement = Arrangement.spacedBy(favoriteSpacing.spacing)
                    ) {
                        if (favorites.isEmpty()) {
                            val intSrc = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .designSurface(designStyle, CircleShape, isDarkTextEnabled, surfaceAccent)
                                    .clip(CircleShape)
                                    .then(
                                        if (!isPreview) {
                                            Modifier
                                                .bounceClick(intSrc)
                                                .clickable(
                                                    interactionSource = intSrc,
                                                    indication = null,
                                                    enabled = !isEditMode
                                                ) { onOpenFavoritesConfig() }
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = mainTextColor)
                            }
                        } else {
                            favorites.forEachIndexed { index, app ->
                                FavoriteItem(
                                    app = app,
                                    index = index,
                                    showLabels = showLabels,
                                    fontSize = fontSize.scale,
                                    mainTextColor = mainTextColor,
                                    returnIconPackage = returnIconPackage,
                                    isHovered = index == hoveredFavIndex,
                                    onBoundsChanged = { favItemBounds[index] = it },
                                    onAppLaunchForReturn = { pkg, bounds ->
                                        onLaunchApp(
                                            pkg,
                                            context.packageManager.getLaunchIntentForPackage(pkg)!!,
                                            bounds
                                        )
                                    },
                                    onShortcutRequested = { shortcutApp, bounds ->
                                        selectedShortcutApp = shortcutApp
                                        shortcutMenuBounds = bounds
                                    },
                                    isPreview = isPreview || isEditMode
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
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
                        editControlsBounds = it.boundsInRoot()
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditControlButton(
                        icon = Icons.Rounded.Close,
                        onClick = {
                            // Abbrechen: Live-Offsets (inkl. Widgets) auf gespeicherten Stand zurücksetzen.
                            dragState.seedFrom(homeLayout, hostedWidgets)
                            selectedEditTarget = null
                            isEditTargetUserPinned = false
                            updateCollisionFeedback(false)
                            onToggleEditMode()
                        },
                        sizeDp = 56.dp,
                        tint = mainTextColor.copy(alpha = 0.6f),
                        testTag = "home_edit_cancel"
                    )

                    EditControlButton(
                        icon = Icons.Rounded.Check,
                        onClick = {
                            updateCollisionFeedback(false)
                            onSaveHomeLayout(dragState.toHomeLayout())
                            onSaveWidgetOffsets(dragState.toWidgetOffsets())
                            Toast.makeText(
                                context,
                                context.getString(R.string.position_saved),
                                Toast.LENGTH_SHORT
                            ).show()
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

                val animSlideDuration = if (animationsEnabled) 300 else 0
                val animFadeDuration = if (animationsEnabled) 150 else 0
                val animTweenDuration = if (animationsEnabled) 300 else 0

                LaunchedEffect(searchButtonBounceToken) {
                    if (searchButtonBounceToken > 0) {
                        isSearchButtonBouncing = true
                        delay(240)
                        isSearchButtonBouncing = false
                    }
                }

                val rotation by animateFloatAsState(
                    targetValue = if (isSettingsOpen) 180f else 0f,
                    animationSpec = tween(if (animationsEnabled) 300 else 0, easing = EaseInOutCubic),
                    label = ""
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedVisibility(
                        visible = !isSettingsOpen,
                        enter = scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            initialScale = 0.0f
                        ) + fadeIn(animationSpec = tween(if (animationsEnabled) 150 else 0)),
                        exit = scaleOut(animationSpec = tween(if (animationsEnabled) 150 else 0)) + fadeOut(animationSpec = tween(if (animationsEnabled) 150 else 0))
                    ) {
                        val searchButtonScale by animateFloatAsState(
                            targetValue = when {
                                isSearchOpen -> 0.8f
                                isSearchButtonBouncing -> 1.06f
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
                                .designSurface(
                                    designStyle,
                                    CircleShape,
                                    isDarkTextEnabled,
                                    surfaceAccent,
                                    fillAlpha = 0.15f
                                )
                                .clip(CircleShape)
                                .testTag("home_search_button")
                                .onGloballyPositioned {
                                    val bounds = it.boundsInRoot()
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
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = stringResource(R.string.cd_search),
                                tint = mainTextColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    val settingsBtnScale by animateFloatAsState(
                        targetValue = if (isSettingsOpen) 1.2f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "SettingsBtnScale"
                    )

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = settingsBtnScale
                                scaleY = settingsBtnScale
                            }
                            .size(56.dp)
                            .designSurface(
                                designStyle,
                                CircleShape,
                                isDarkTextEnabled,
                                surfaceAccent,
                                fillAlpha = if (isSettingsOpen) 0.1f else 0.15f
                            )
                            .clip(CircleShape)
                            .testTag("settings_button")
                            .onGloballyPositioned {
                                settingsButtonBounds = it.boundsInRoot()
                            }
                            .then(
                                Modifier
                                    .bounceClick(intSrc)
                                    .clickable(
                                        interactionSource = intSrc,
                                        indication = null,
                                        enabled = !isEditMode
                                    ) { onToggleSettings() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.rotate(rotation)) {
                            Icon(
                                imageVector = if (isSettingsOpen) Icons.Rounded.Close else Icons.Rounded.Settings,
                                contentDescription = null,
                                tint = mainTextColor,
                                modifier = Modifier.size(if (isSettingsOpen) 32.dp else 28.dp)
                            )
                        }
                    }
                }
            }
        }

        if (!isPreview && !isEditMode) {
            selectedShortcutApp?.let { app ->
                AppShortcutsMenu(
                    packageName = app.packageName,
                    targetBounds = shortcutMenuBounds,
                    onDismiss = {
                        selectedShortcutApp = null
                        shortcutMenuBounds = null
                    },
                    onShortcutClick = { shortcut -> launchShortcut(context, app.packageName, shortcut.id) }
                )
            }
        }

        // Niagara-Schnellzugriff: A–Z-Randleiste direkt auf der Startseite (nur im HOME_LIST-Modus).
        if (!isPreview && !isEditMode && appAccessMode == AppAccessMode.HOME_LIST) {
            HomeAppScrubber(
                apps = allApps,
                onLaunchApp = onLaunchApp,
                returnIconPackage = returnIconPackage,
                modifier = Modifier.zIndex(2500f)
            )
        }
    }
}
