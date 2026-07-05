package com.example.androidlauncher.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Sun
import com.example.androidlauncher.FavoritesEntranceTracker
import com.example.androidlauncher.R
import com.example.androidlauncher.data.AppAccessMode
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.FavoritesBorderStyle
import com.example.androidlauncher.data.GestureAction
import com.example.androidlauncher.data.HomeLayout
import com.example.androidlauncher.data.WeatherData
import com.example.androidlauncher.data.WeatherRepository
import com.example.androidlauncher.launchShortcut
import com.example.androidlauncher.ui.LiquidGlass.designSurface
import com.example.androidlauncher.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

internal enum class HomeEditTarget {
    CLOCK,
    DATE,
    WEATHER,
    FAVORITES
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
    isPreview: Boolean = false
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
    // Live-Offsets, Neutralpositionen und Haptik-Zeitstempel je verschiebbarem Element.
    val offsets = remember {
        mutableStateMapOf(
            HomeEditTarget.CLOCK to homeLayout.clock,
            HomeEditTarget.DATE to homeLayout.date,
            HomeEditTarget.WEATHER to homeLayout.weather,
            HomeEditTarget.FAVORITES to homeLayout.favorites
        )
    }
    val neutralBounds = remember { mutableStateMapOf<HomeEditTarget, Rect>() }
    val lastBlockedHapticMs = remember { mutableStateMapOf<HomeEditTarget, Long>() }

    // Hält die Live-Offsets außerhalb des Edit-Modus an der gespeicherten Position;
    // beim Betreten des Edit-Modus werden sie 1:1 daraus geseedet, Abbrechen revertiert.
    LaunchedEffect(homeLayout, isEditMode) {
        offsets[HomeEditTarget.CLOCK] = homeLayout.clock
        offsets[HomeEditTarget.DATE] = homeLayout.date
        offsets[HomeEditTarget.WEATHER] = homeLayout.weather
        offsets[HomeEditTarget.FAVORITES] = homeLayout.favorites
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

    var searchButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var settingsButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var editControlsBounds by remember { mutableStateOf<Rect?>(null) }
    var collisionHapticWasTriggered by remember { mutableStateOf(false) }

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

    var selectedEditTarget by remember { mutableStateOf<HomeEditTarget?>(null) }
    var isEditTargetUserPinned by remember { mutableStateOf(false) }
    var selectedShortcutApp by remember { mutableStateOf<AppInfo?>(null) }
    var shortcutMenuBounds by remember { mutableStateOf<Rect?>(null) }

    // translateRect/expandRect/intersects liegen jetzt framework-frei in HomeGeometry.kt
    // (unit-getestet); die Aufrufe loesen auf die Top-Level-Funktionen auf.

    // rectContains(...) liegt jetzt framework-frei in HomeGeometry.kt (unit-getestet); die
    // Aufrufe unten loesen auf die Top-Level-Funktion auf.

    // Sammel-Padding je Element: Favoriten behalten ihren Rahmenabstand, Text-Elemente
    // (Uhr/Datum/Wetter) liegen im Neutralzustand bündig gestapelt → kein Inter-Padding.
    fun framePadding(target: HomeEditTarget): Float =
        if (target == HomeEditTarget.FAVORITES) favoritesFramePaddingPx else 0f

    fun baseRect(target: HomeEditTarget, o: Offset): Rect? =
        neutralBounds[target]?.let { translateRect(it, o.x, o.y) }

    fun navRect(target: HomeEditTarget, o: Offset): Rect? =
        baseRect(target, o)?.let { expandRect(it, navCollisionPaddingPx) }

    fun clampOffset(target: HomeEditTarget, x: Float, y: Float): Offset {
        val bounds = neutralBounds[target] ?: return Offset(x, y)
        val topLimit = if (target == HomeEditTarget.CLOCK) clockTopLimitPx else 0f
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
        HomeEditTarget.CLOCK -> "home_edit_target_clock"
        HomeEditTarget.DATE -> "home_edit_target_date"
        HomeEditTarget.WEATHER -> "home_edit_target_weather"
        HomeEditTarget.FAVORITES -> "home_edit_target_favorites"
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
                        rotationZ = editWiggleAngle * if (target.ordinal % 2 == 0) 1f else -1f
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
                                        val hitTarget = HomeEditTarget.entries.any { target ->
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
                            .targetLayout(HomeEditTarget.CLOCK)
                            .targetEditModifier(HomeEditTarget.CLOCK)
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
                        .targetLayout(HomeEditTarget.DATE)
                        .targetEditModifier(HomeEditTarget.DATE)
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
                        .targetLayout(HomeEditTarget.WEATHER)
                        .targetEditModifier(HomeEditTarget.WEATHER)
                ) {
                    WeatherRow(isPreview = isPreview || isEditMode)
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
                        .targetLayout(HomeEditTarget.FAVORITES)
                        .targetEditModifier(HomeEditTarget.FAVORITES)
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
                            // Abbrechen: Live-Offsets auf gespeicherten Stand zurücksetzen.
                            offsets[HomeEditTarget.CLOCK] = homeLayout.clock
                            offsets[HomeEditTarget.DATE] = homeLayout.date
                            offsets[HomeEditTarget.WEATHER] = homeLayout.weather
                            offsets[HomeEditTarget.FAVORITES] = homeLayout.favorites
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
                            onSaveHomeLayout(
                                HomeLayout(
                                    clock = offsets[HomeEditTarget.CLOCK] ?: Offset.Zero,
                                    date = offsets[HomeEditTarget.DATE] ?: Offset.Zero,
                                    weather = offsets[HomeEditTarget.WEATHER] ?: Offset.Zero,
                                    favorites = offsets[HomeEditTarget.FAVORITES] ?: Offset.Zero
                                )
                            )
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
    val designStyle = LocalDesignStyle.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val surfaceAccent = LocalColorTheme.current.menuSurfaceColor(isDarkTextEnabled)

    Box(
        modifier = Modifier
            .size(sizeDp)
            .then(
                if (containerColor == Color.Transparent) {
                    Modifier.designSurface(designStyle, CircleShape, isDarkTextEnabled, surfaceAccent, fillAlpha = 0.1f)
                } else {
                    Modifier.background(containerColor, CircleShape)
                }
            )
            .clip(CircleShape)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .bounceClick(intSrc)
            .clickable(interactionSource = intSrc, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun FavoriteItem(
    app: AppInfo,
    showLabels: Boolean,
    fontSize: Float,
    mainTextColor: Color,
    returnIconPackage: String?,
    index: Int = 0,
    isHovered: Boolean = false,
    onBoundsChanged: (Rect) -> Unit = {},
    onAppLaunchForReturn: (String, Rect?) -> Unit,
    onShortcutRequested: (AppInfo, Rect?) -> Unit,
    isPreview: Boolean = false
) {
    val context = LocalContext.current
    val intSrc = remember { MutableInteractionSource() }
    // Hervorhebung beim Rüberfahren: nur Vergrößerung, kein Hintergrund.
    // Abschaltbar über die Animationseinstellungen (Favoriten-Leiste).
    val favoritesAnimationEnabled = LocalFavoritesAnimationEnabled.current
    val hoverScale by animateFloatAsState(
        targetValue = if (isHovered && favoritesAnimationEnabled) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "FavHoverScale"
    )
    val bounceScale by animateFloatAsState(
        targetValue = if (!LocalAppCloseAnimationEnabled.current) 1f else if (returnIconPackage == app.packageName) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "HomeReturnBounce"
    )
    val itemBounds = remember(app.packageName) { mutableStateOf<Rect?>(null) }
    val iconBounds = remember(app.packageName) { mutableStateOf<Rect?>(null) }
    // Icon-Größe ist (anders als die Icon-Position) unabhängig von der Swipe-Verschiebung
    // und dient als stabiler Anker für das Shortcut-Menü.
    var iconSize by remember(app.packageName) { mutableStateOf(IntSize.Zero) }

    var horizontalOffset by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = if (!LocalAnimationsEnabled.current) 0f else horizontalOffset,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "SwipeOffset"
    )

    // Material-3-Expressive: gestaffelter Eingang der Favoriten (federndes Aufpoppen),
    // einmalig pro Prozess-Lebenszeit beim ersten Erscheinen. Läuft NICHT erneut beim
    // Zurückkommen aus dem App-Drawer (HomeScreen wird dann neu komponiert).
    // Respektiert die Favoriten-Animationseinstellung.
    val playEntrance = favoritesAnimationEnabled && !isPreview &&
        !FavoritesEntranceTracker.hasAppeared(app.packageName)
    val appear = remember(app.packageName) { Animatable(if (playEntrance) 0f else 1f) }
    LaunchedEffect(app.packageName, favoritesAnimationEnabled) {
        if (playEntrance) {
            appear.snapTo(0f)
            delay((index.coerceAtMost(8) * 40).toLong())
            appear.animateTo(1f, RethroneSprings.spatial())
            FavoritesEntranceTracker.markAppeared(app.packageName)
        } else {
            appear.snapTo(1f)
        }
    }

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                val b = coordinates.boundsInRoot()
                itemBounds.value = b
                onBoundsChanged(b)
            }
            .graphicsLayer {
                translationX = animatedOffset
                // Eingang (0.9 → 1.0) mit Hover-Scale kombiniert.
                val entranceScale = 0.9f + 0.1f * appear.value
                scaleX = hoverScale * entranceScale
                scaleY = hoverScale * entranceScale
                alpha = appear.value
                // Linksbündig vergrößern, damit das Icon nicht zur Seite wandert.
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .then(
                if (!isPreview) {
                    Modifier
                        .pointerInput(app.packageName) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (horizontalOffset > 150f) {
                                        // Stabilen Anker nur fürs Icon bauen: Position aus dem
                                        // (unverschobenen) itemBounds, Breite/Höhe aus der Icon-Größe.
                                        // So verdeckt das Menü das Logo nicht – egal ob Labels an sind.
                                        val ib = itemBounds.value
                                        val anchor = if (ib != null && iconSize.width > 0) {
                                            Rect(
                                                left = ib.left,
                                                top = ib.center.y - iconSize.height / 2f,
                                                right = ib.left + iconSize.width,
                                                bottom = ib.center.y + iconSize.height / 2f
                                            )
                                        } else {
                                            ib
                                        }
                                        onShortcutRequested(app, anchor)
                                    }
                                    horizontalOffset = 0f
                                },
                                onDragCancel = { horizontalOffset = 0f },
                                onHorizontalDrag = { _, dragAmount ->
                                    horizontalOffset = (horizontalOffset + dragAmount).coerceIn(0f, 300f)
                                }
                            )
                        }
                        .bounceClick(intSrc)
                        .clickable(interactionSource = intSrc, indication = null) {
                            val targetBounds = iconBounds.value ?: itemBounds.value
                            onAppLaunchForReturn(app.packageName, targetBounds)
                        }
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            // Nur vertikaler Innenabstand, damit die Icons links bündig mit Uhr/Datum stehen.
            modifier = Modifier.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = bounceScale
                        scaleY = bounceScale
                    }
                    .onGloballyPositioned {
                        iconBounds.value = it.boundsInRoot()
                        iconSize = it.size
                    }
            ) {
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

/**
 * Großes Uhrzeit-Element (eigenständig verschiebbar). [time] wird vom Aufrufer getickt.
 */
@Composable
fun ClockText(
    time: java.util.Date,
    isPreview: Boolean,
    returnIconPackage: String?,
    onAppLaunchForReturn: (String, Rect?) -> Unit
) {
    val context = LocalContext.current
    val fontSize = LocalFontSize.current
    val appFontWeight = LocalFontWeight.current
    val mainTextColor = LocalHomeTextColor.current

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val intSrcTime = remember { MutableInteractionSource() }
    val clockBounds = remember { mutableStateOf<Rect?>(null) }
    var clockPackage by remember { mutableStateOf<String?>(null) }

    val bounceScaleTime by animateFloatAsState(
        targetValue = if (!LocalAppCloseAnimationEnabled.current) 1f else if (returnIconPackage != null && returnIconPackage == clockPackage) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "ClockReturnBounce"
    )

    // Material-3-Expressive: Minutenwechsel per kurzem Crossfade statt hartem Swap.
    val animationsEnabled = LocalAnimationsEnabled.current
    Crossfade(
        targetState = timeFormat.format(time),
        animationSpec = tween(if (animationsEnabled) 220 else 0),
        label = "clockCrossfade",
        modifier = Modifier
            .onGloballyPositioned { clockBounds.value = it.boundsInRoot() }
            .graphicsLayer {
                scaleX = bounceScaleTime
                scaleY = bounceScaleTime
            }
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (!isPreview) {
                    Modifier
                        .bounceClick(intSrcTime)
                        .clickable(interactionSource = intSrcTime, indication = null) {
                            launchClockApp(context, clockBounds.value, onAppLaunchForReturn) { clockPackage = it }
                        }
                } else {
                    Modifier
                }
            )
    ) { timeStr ->
        Text(
            text = timeStr,
            // Seitliches/vertikales Polster INNERHALB des Clips, damit Glyphen-Überhänge
            // (kursive/dekorative Fonts, negatives letterSpacing) nicht abgeschnitten werden.
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 72.sp * fontSize.scale,
            fontWeight = appFontWeight.weight,
            letterSpacing = (-2).sp,
            lineHeight = 72.sp * fontSize.scale,
            // Entfernt das zusätzliche Font-Padding (enger Rahmen), behält aber die gewählte
            // App-Schriftart bei, indem auf den aktuellen LocalTextStyle gemergt wird.
            // Trim.Both passt die Box an die echten Glyphen an → hohe/dekorative Schriften
            // werden NICHT vom Clip abgeschnitten.
            style = LocalTextStyle.current.merge(
                TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    )
                )
            ),
            color = mainTextColor
        )
    }
}

/**
 * Datums-Element (eigenständig verschiebbar). [time] wird vom Aufrufer getickt.
 */
@Composable
fun DateText(
    time: java.util.Date,
    isPreview: Boolean,
    returnIconPackage: String?,
    onAppLaunchForReturn: (String, Rect?) -> Unit
) {
    val context = LocalContext.current
    val fontSize = LocalFontSize.current
    val appFontWeight = LocalFontWeight.current
    val mainTextColor = LocalHomeTextColor.current

    val dateFormat = remember { SimpleDateFormat("EEEE, d. MMMM", Locale.getDefault()) }
    val intSrcDate = remember { MutableInteractionSource() }
    val calendarBounds = remember { mutableStateOf<Rect?>(null) }
    var calendarPackage by remember { mutableStateOf<String?>(null) }

    val bounceScaleDate by animateFloatAsState(
        targetValue = if (!LocalAppCloseAnimationEnabled.current) 1f else if (returnIconPackage != null && returnIconPackage == calendarPackage) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "CalendarReturnBounce"
    )

    // Material-3-Expressive: Datumswechsel (zur Mitternacht) per Crossfade.
    val animationsEnabled = LocalAnimationsEnabled.current
    Crossfade(
        targetState = dateFormat.format(time),
        animationSpec = tween(if (animationsEnabled) 300 else 0),
        label = "dateCrossfade",
        modifier = Modifier
            .onGloballyPositioned { calendarBounds.value = it.boundsInRoot() }
            .graphicsLayer {
                scaleX = bounceScaleDate
                scaleY = bounceScaleDate
            }
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (!isPreview) {
                    Modifier
                        .bounceClick(intSrcDate)
                        .clickable(interactionSource = intSrcDate, indication = null) {
                            launchCalendarApp(
                                context,
                                calendarBounds.value,
                                onAppLaunchForReturn
                            ) { calendarPackage = it }
                        }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) { dateStr ->
        Text(
            text = dateStr,
            fontSize = 18.sp * fontSize.scale,
            fontWeight = appFontWeight.weight,
            lineHeight = 18.sp * fontSize.scale,
            // Trim.Both passt die Box an die echten Glyphen an (kein Abschneiden).
            style = LocalTextStyle.current.merge(
                TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    )
                )
            ),
            color = mainTextColor.copy(alpha = 0.7f)
        )
    }
}

/**
 * Schmale Wetterzeile unter Uhr/Datum: ein Symbol plus aktuelle Temperatur.
 *
 * Holt den groben Standort über [WeatherRepository] und ruft das Wetter von Open-Meteo ab.
 * Solange kein Standort/keine Daten vorliegen (z. B. Berechtigung noch nicht erteilt),
 * wird in kurzen Abständen erneut versucht; danach im 30-Minuten-Takt aktualisiert.
 * Im Vorschau-/Edit-Modus wird ein statischer Platzhalter gezeigt (kein Netzwerkzugriff).
 */
@Composable
fun WeatherRow(isPreview: Boolean = false) {
    val context = LocalContext.current
    val fontSize = LocalFontSize.current
    val appFontWeight = LocalFontWeight.current
    val mainTextColor = LocalHomeTextColor.current

    // Startwert aus dem prozessweiten Cache: zeigt beim Zurückkehren aus dem App-Drawer
    // sofort den letzten Wert, statt kurz zu verschwinden.
    val lifecycleOwner = LocalLifecycleOwner.current
    val weather by produceState<WeatherData?>(initialValue = WeatherRepository.cached, isPreview) {
        if (isPreview) return@produceState
        val repo = WeatherRepository(context)
        val refreshIntervalMs = 30 * 60_000L
        // Nur bei sichtbarem Launcher abrufen: im Hintergrund pausiert der Loop und
        // prüft beim nächsten ON_START zuerst die Drossel (WeatherRepository.shouldRefresh).
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                if (WeatherRepository.shouldRefresh(refreshIntervalMs)) {
                    // GPS nur, falls die Berechtigung ohnehin erteilt ist; sonst grobe Position
                    // über die IP-Adresse (ohne Standortdienst/Berechtigung).
                    val location = repo.awaitLocation() ?: repo.ipLocation()
                    if (location != null) {
                        repo.fetch(location.first, location.second)?.let { value = it }
                    }
                } else {
                    // Cache noch aktuell – übernehmen, kein erneuter Netzwerkabruf.
                    value = WeatherRepository.cached
                }
                // Schneller erneut versuchen, solange noch keine Daten vorliegen,
                // sonst regulär alle 30 Minuten aktualisieren.
                delay(if (value == null) 20_000L else refreshIntervalMs)
            }
        }
    }

    val icon: ImageVector
    val temperatureText: String
    if (isPreview) {
        icon = Lucide.Sun
        temperatureText = "21°"
    } else {
        val data = weather ?: return
        icon = WeatherRepository.iconFor(data.weatherCode)
        temperatureText = "${data.temperatureC}°"
    }

    // Material-3-Expressive: Wetterwert wechselt per Crossfade statt hartem Swap.
    val animationsEnabled = LocalAnimationsEnabled.current
    Crossfade(
        targetState = icon to temperatureText,
        animationSpec = tween(if (animationsEnabled) 300 else 0),
        label = "weatherCrossfade",
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) { (stateIcon, stateTemp) ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = stateIcon,
                contentDescription = null,
                tint = mainTextColor.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp * fontSize.scale)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stateTemp,
                fontSize = 18.sp * fontSize.scale,
                fontWeight = appFontWeight.weight,
                color = mainTextColor.copy(alpha = 0.7f)
            )
        }
    }
}

private fun launchClockApp(
    context: Context,
    bounds: Rect?,
    onAppLaunchForReturn: (String, Rect?) -> Unit,
    onPackageFound: (String) -> Unit
) {
    val pm = context.packageManager
    // Bekannte Uhr-Pakete + Auswahl liegen framework-frei in LauncherLogic (unit-getestet).
    var foundPkg: String? = com.example.androidlauncher.LauncherLogic.firstLaunchablePackage(
        com.example.androidlauncher.LauncherLogic.KNOWN_CLOCK_PACKAGES
    ) { pkg -> pm.getLaunchIntentForPackage(pkg) != null }

    if (foundPkg == null) {
        try {
            val stdIntent = Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.APP_CLOCK")
            val res = pm.resolveActivity(stdIntent, 0)
            if (res != null) foundPkg = res.activityInfo.packageName
        } catch (_: Exception) {
        }
    }
    if (foundPkg == null) {
        try {
            val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            val res = pm.resolveActivity(alarmIntent, 0)
            if (res != null) foundPkg = res.activityInfo.packageName
        } catch (_: Exception) {
        }
    }

    if (foundPkg != null) {
        onPackageFound(foundPkg)
        val launchIntent = pm.getLaunchIntentForPackage(foundPkg)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            onAppLaunchForReturn(foundPkg, bounds)
            return
        }
    }

    try {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory("android.intent.category.APP_CLOCK")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, context.getString(R.string.clock_app_not_found), Toast.LENGTH_SHORT).show()
    }
}

private fun launchCalendarApp(
    context: Context,
    bounds: Rect?,
    onAppLaunchForReturn: (String, Rect?) -> Unit,
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
            onAppLaunchForReturn(foundPkg, bounds)
        } else {
            calendarIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            onAppLaunchForReturn(foundPkg, bounds)
        }
        return
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
        } catch (_: Exception) {
        }
    }
}
