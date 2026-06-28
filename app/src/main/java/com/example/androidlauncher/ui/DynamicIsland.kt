package com.example.androidlauncher.ui

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.data.IslandAnimationStyle
import com.example.androidlauncher.data.IslandContent
import androidx.compose.ui.graphics.luminance
import com.example.androidlauncher.data.IslandState
import com.example.androidlauncher.data.activityId
import com.example.androidlauncher.data.iconPackage
import com.example.androidlauncher.ui.theme.LocalAnimationSpeed
import com.example.androidlauncher.ui.theme.LocalAnimationsEnabled
import com.example.androidlauncher.ui.theme.RethroneSprings
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

internal val IslandSurface = Color(0xFF0B0B0C)
internal val IslandText = Color.White
internal val IslandSubText = Color(0xFFB5B5BA)
private val ChargingGreen = Color(0xFF34C759)
private val AccentBlue = Color(0xFF0A84FF)
private val TimerAmber = Color(0xFFFF9F0A)

/** Schrittweite (dp) pro Tipp auf die Höhen-Buttons im Layout-Editor. */
private const val EDIT_NUDGE_DP = 2f

/** Einheitliche Höhe der kompakten Pille (Medien-Look) – unabhängig vom Inhalt. */
private val IslandPillHeight = 30.dp

/**
 * Kompakte Dynamic-Island-Pille an der Front-Kamera. Erscheint **nur**, wenn gerade etwas
 * aktiv ist ([IslandState.content] != null) – im Leerlauf wird nichts gezeichnet. Tap löst
 * [onTap] aus (klappt die große Karte auf, siehe [IslandExpandedCard]).
 *
 * Dunkle Oberfläche (themenunabhängig) für maximalen Kontrast wie beim iOS-Original.
 */
/**
 * Enter-Transition der Insel je nach [IslandAnimationStyle]. Alle „Wachs"-Stile starten opak aus
 * Breite 0 (nur Breite animiert, Höhe konstant); [IslandAnimationStyle.SOFT] blendet dezent ein.
 */
private fun islandEnter(style: IslandAnimationStyle, speed: Float): EnterTransition = when (style) {
    IslandAnimationStyle.FROM_NOTCH -> expandIn(RethroneSprings.island(speed), Alignment.TopCenter) { full -> IntSize(0, full.height) }
    IslandAnimationStyle.BOUNCE -> expandIn(RethroneSprings.islandBouncy(speed), Alignment.TopCenter) { full -> IntSize(0, full.height) }
    IslandAnimationStyle.SNAPPY -> expandIn(RethroneSprings.effects(speed), Alignment.TopCenter) { full -> IntSize(0, full.height) }
    IslandAnimationStyle.SOFT -> fadeIn(RethroneSprings.effects(speed)) +
        scaleIn(RethroneSprings.effects(speed), initialScale = 0.92f, transformOrigin = TransformOrigin(0.5f, 0f))
}

/** Exit-Transition der Insel je nach [IslandAnimationStyle] (Spiegelbild des Enters). */
private fun islandExit(style: IslandAnimationStyle, speed: Float): ExitTransition = when (style) {
    IslandAnimationStyle.SOFT -> fadeOut(RethroneSprings.effects(speed)) +
        scaleOut(RethroneSprings.effects(speed), targetScale = 0.92f, transformOrigin = TransformOrigin(0.5f, 0f))
    IslandAnimationStyle.SNAPPY -> shrinkOut(RethroneSprings.effects(speed), Alignment.TopCenter) { full -> IntSize(0, full.height) } +
        fadeOut(RethroneSprings.effects(speed))
    // FROM_NOTCH & BOUNCE: sauber ohne Bounce zur Notch zusammenziehen (morph) + kurzes Fade.
    else -> shrinkOut(RethroneSprings.morph(speed), Alignment.TopCenter) { full -> IntSize(0, full.height) } +
        fadeOut(RethroneSprings.effects(speed))
}

/** Start-Stauchung des vertikalen Jelly-Squash je Stil (1f = kein Jelly). */
private fun islandJellyFromScaleY(style: IslandAnimationStyle): Float = when (style) {
    IslandAnimationStyle.FROM_NOTCH -> 0.8f
    IslandAnimationStyle.BOUNCE -> 0.7f
    IslandAnimationStyle.SOFT, IslandAnimationStyle.SNAPPY -> 1f
}

@Composable
fun DynamicIsland(
    state: IslandState,
    onTap: (IslandContent) -> Unit,
    loadIcon: suspend (String) -> ImageBitmap?,
    verticalOffsetDp: Float = 0f,
    islandColor: Color = IslandSurface,
    editMode: Boolean = false,
    onSwipeNext: () -> Unit = {},
    onSwipePrevious: () -> Unit = {},
    animationStyle: IslandAnimationStyle = IslandAnimationStyle.FROM_NOTCH,
    modifier: Modifier = Modifier
) {
    // Kontrast-Inhaltsfarbe: dunkler Text auf heller Insel, sonst weiß – damit auch frei gewählte
    // helle Farben lesbar bleiben. Subtext etwas gedämpft.
    val islandContent = if (islandColor.luminance() > 0.5f) Color(0xFF0B0B0C) else Color.White
    val islandSubContent = islandContent.copy(alpha = 0.7f)
    // „Main" = vom Manager gewählte Aktivität. Alle aktiven werden als Einheiten gezeigt: die
    // gewählte als breite Pille (umschließt die Kamera), die übrigen als kleine App-Kreise.
    // Beim Wechsel tauschen sie Form & Platz (Rollen-Tausch-Morph).
    val content = state.content
    // Letzten Zustand merken, damit die Ausblende-Animation noch etwas anzuzeigen hat.
    var lastContent by remember { mutableStateOf<IslandContent?>(null) }
    var lastAll by remember { mutableStateOf<List<IslandContent>>(emptyList()) }
    LaunchedEffect(content, state.all) {
        if (content != null) lastContent = content
        if (state.all.isNotEmpty()) lastAll = state.all
    }
    val shownContent = content ?: lastContent
    val shownAll = if (state.all.isNotEmpty()) state.all else lastAll

    // Geteilter Icon-Cache über die ganze Insel: überlebt App-Wechsel (Swipe), sodass ein erneut
    // erscheinender Kreis/Icon sein Bitmap ohne `null`-Frame zeigt (kein Dot→Icon-Flash beim Swap).
    val iconCache = remember { androidx.compose.runtime.mutableStateMapOf<String, ImageBitmap?>() }

    val animationsEnabled = LocalAnimationsEnabled.current
    val speed = LocalAnimationSpeed.current
    val cutout = rememberCutoutInfo()
    val density = LocalDensity.current
    val cutoutWidth = cutout?.let { with(density) { it.widthPx.toDp() } } ?: 0.dp
    // Statusleisten-Höhe für den Fallback ohne Cutout (notchlose Geräte / Tablets).
    val statusBarTopPx = WindowInsets.statusBars.getTop(density)
    // Gemessene Cluster-Größe + Mitte der aktiven Pille (für Kamera-Zentrierung beim Switch).
    var clusterWidthPx by remember { mutableStateOf(0) }
    var clusterHeightPx by remember { mutableStateOf(0) }
    var rowCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // Mitte des Kamera-Slots (= die Lücke in der aktiven Pille), die unter die Kamera gelegt wird.
    // Bewusst NICHT die geometrische Pillen-Mitte: bei langem Timer ist die Pille asymmetrisch
    // (breiter Text links, kleiner Punkt rechts) → die Mitte läge im Text und die Kamera über
    // den letzten Sekundenziffern.
    var cameraAnchorPx by remember { mutableStateOf(-1f) }
    // Im Offset KONSUMIERTER Anker: gleitet beim Swipe im selben Tempo wie der Breiten-Morph
    // (clusterWidthPx) von alt→neu. Würde der rohe `cameraAnchorPx` direkt benutzt, schnappte er
    // sofort auf den Zielwert (die eingehende Pille ist durch `clip = false` ab Frame 1 in
    // Zielgröße ausgelegt), während die Breite noch morpht → das Kamera-Loch driftet über den
    // Inhalt = „flackernd". Erste gültige Messung (und bei deaktivierten Animationen) hart setzen,
    // damit beim Erscheinen kein Sweep vom Sentinel −1 entsteht.
    val animatedAnchor = remember { androidx.compose.animation.core.Animatable(-1f) }
    LaunchedEffect(cameraAnchorPx, animationsEnabled, speed) {
        val target = cameraAnchorPx
        if (target < 0f) return@LaunchedEffect
        if (animatedAnchor.value < 0f || !animationsEnabled) {
            animatedAnchor.snapTo(target)
        } else {
            animatedAnchor.animateTo(target, RethroneSprings.morph(speed))
        }
    }

    AnimatedVisibility(
        // Im Edit-Modus immer sichtbar (Platzhalter zum Ziehen), sonst nur bei aktivem Inhalt.
        visible = content != null || editMode,
        modifier = modifier.offset {
            // Vertikal IMMER im Statusleisten-Band zentrieren (geräteadaptiv auf Kamerahöhe).
            val targetCenterY = statusBarTopPx / 2f + verticalOffsetDp.dp.toPx()
            val y = (targetCenterY - clusterHeightPx / 2f).coerceAtLeast(0f).roundToInt()
            // Horizontal: Mitte der AKTIVEN Pille unter die Kamera legen, Cluster auf dem Schirm
            // geclampt. Beim Switch wandert die Pille im Cluster → der Cluster gleitet so, dass
            // die neu gewählte App unter die Kamera rückt (Rollen-Tausch).
            val x = if (cutout != null) {
                val margin = 6.dp.toPx()
                val half = clusterWidthPx / 2f
                val lo = half + margin
                val hi = cutout.screenWidth - half - margin
                val anchor = animatedAnchor.value
                // Im Edit-Modus den (schriftabhängigen) Live-Anker ignorieren und die Pille fest
                // unter dem Cutout zentrieren – dort wird nur die vertikale Position eingestellt,
                // horizontal soll sie unabhängig von der Schriftart stabil bleiben.
                val clusterCenterScreen = if (!editMode && anchor >= 0f && clusterWidthPx > 0) {
                    cutout.centerX - (anchor - clusterWidthPx / 2f)
                } else {
                    cutout.centerX
                }
                val clamped = if (lo <= hi) clusterCenterScreen.coerceIn(lo, hi) else cutout.screenWidth / 2f
                (clamped - cutout.screenWidth / 2f).roundToInt()
            } else {
                0
            }
            IntOffset(x, y)
        },
        // Öffnungs-/Schließanimation je nach gewähltem Stil (siehe islandEnter/islandExit).
        // Bei deaktivierten Animationen reines Fade.
        enter = if (animationsEnabled) islandEnter(animationStyle, speed) else fadeIn(),
        exit = if (animationsEnabled) islandExit(animationStyle, speed) else fadeOut()
    ) {
        if (editMode) {
            // Vorschau ohne Tap/Swipe (Höhe via separate Steuerleiste).
            IslandPill(
                shownContent,
                cutoutWidth,
                loadIcon,
                iconCache,
                islandColor = islandColor,
                contentColor = islandContent,
                subContentColor = islandSubContent,
                editMode = true,
                modifier = Modifier.onGloballyPositioned {
                    clusterWidthPx = it.size.width
                    clusterHeightPx = it.size.height
                }
            )
        } else {
            val swipeThresholdPx = with(density) { 40.dp.toPx() }
            // Press-Feedback: kurzes Einsinken der Pille beim Drücken (taktiles „Drauf drücken").
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            val pressScale by animateFloatAsState(
                targetValue = if (pressed && animationsEnabled) 0.94f else 1f,
                animationSpec = RethroneSprings.spatial(speed),
                label = "islandPress"
            )
            // „Jelly": vertikaler Mitfeder-Fortschritt aus der Enter/Exit-Transition der ganzen Insel.
            // 0→1 beim Erscheinen, 1→0 beim Schließen. Triggert NUR beim Auf-/Abtauchen – nicht beim
            // Inhalts-/App-Wechsel (dann bleibt AnimatedVisibility = Visible → appear = 1).
            // Start-Stauchung & Feder hängen vom Stil ab; bei jellyFrom == 1f ist der Jelly inaktiv.
            val jellyFrom = islandJellyFromScaleY(animationStyle)
            val appear by transition.animateFloat(
                transitionSpec = {
                    if (!animationsEnabled) snap()
                    else if (targetState == EnterExitState.Visible) when (animationStyle) {
                        IslandAnimationStyle.BOUNCE -> RethroneSprings.islandBouncy(speed)
                        IslandAnimationStyle.FROM_NOTCH -> RethroneSprings.island(speed)
                        else -> RethroneSprings.effects(speed)
                    }
                    else RethroneSprings.morph(speed)
                },
                label = "islandJelly"
            ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    // Dezentes Squash→Stretch→Settle von der Kameralinie aus; rein zeichnerisch, kein
                    // Layout-Einfluss → Kamera-Zentrierung/Anker (onGloballyPositioned) bleibt stabil.
                    // Die Breite macht weiterhin der Clip (expandIn) – hier nur die zweite Achse.
                    .graphicsLayer {
                        scaleY = jellyFrom + (1f - jellyFrom) * appear
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { shownContent?.let(onTap) }
                    )
                    // Horizontaler Swipe (im Bereich knapp unter der Statusleiste) wechselt die Aktivität.
                    .pointerInput(Unit) {
                        var total = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = {
                                if (total <= -swipeThresholdPx) {
                                    onSwipeNext()
                                } else if (total >= swipeThresholdPx) {
                                    onSwipePrevious()
                                }
                            }
                        ) { _, drag -> total += drag }
                    }
            ) {
                // Feste Positions-Slots: aktive Pille IMMER links (unter der Kamera), die übrigen
                // App-Kreise IMMER rechts. Beim Switch wechselt nur der INHALT (richtungsneutraler
                // Scale+Fade-Crossfade) – nichts springt die Seite, das zweite Logo bleibt rechts.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                        .onGloballyPositioned {
                            rowCoords = it
                            clusterWidthPx = it.size.width
                            clusterHeightPx = it.size.height
                        }
                ) {
                    // Übrige Aktivitäten (Kreise rechts). Vorab berechnet, damit die Pille weiß,
                    // ob sie symmetrisch (allein) oder kompakt (mit Kreisen) layouten soll.
                    val others = shownAll.filter {
                        shownContent == null || activityId(it) != activityId(shownContent)
                    }
                    // Slot 0: aktive Pille. Die Pille meldet die Mitte ihres Kamera-Slots (Lücke),
                    // die unter die Kamera gelegt wird – so bleibt das Loch frei vom Text.
                    Box {
                        AnimatedContent(
                            targetState = shownContent,
                            contentKey = { it?.let(::activityId) },
                            transitionSpec = {
                                // Reiner Crossfade (kein Scale-Pop) + EIN koordinierter Größen-Morph:
                                // Breite/Höhe gleiten ohne Nachschwingen in einem Zug mit der
                                // Überblendung. clip=false verhindert das Anschneiden des
                                // einblendenden Inhalts während des Morphs.
                                val sizeSpec = SizeTransform(clip = false) { _, _ ->
                                    if (animationsEnabled) RethroneSprings.morph(speed) else snap()
                                }
                                if (animationsEnabled) {
                                    ContentTransform(
                                        targetContentEnter = fadeIn(RethroneSprings.effects(speed)),
                                        initialContentExit = fadeOut(RethroneSprings.effects(speed)),
                                        sizeTransform = sizeSpec
                                    )
                                } else {
                                    ContentTransform(
                                        targetContentEnter = fadeIn(),
                                        initialContentExit = fadeOut(),
                                        sizeTransform = sizeSpec
                                    )
                                }
                            },
                            label = "islandActivePill"
                        ) { c ->
                            // Anker nur von der EINGEHENDEN Pille treiben: während des Übergangs
                            // sind alte + neue gleichzeitig komponiert; würden beide melden, spränge
                            // cameraAnchorPx zwischen den Slot-Mitten → horizontales Zittern.
                            val isTarget = c != null && shownContent != null &&
                                activityId(c) == activityId(shownContent)
                            IslandPill(
                                c, cutoutWidth, loadIcon, iconCache,
                                islandColor = islandColor,
                                contentColor = islandContent,
                                subContentColor = islandSubContent,
                                editMode = false,
                                balanced = others.isEmpty(),
                                onCameraSlotPositioned = if (isTarget) {
                                    { slot ->
                                        rowCoords?.let { r ->
                                            if (r.isAttached && slot.isAttached) {
                                                cameraAnchorPx = r.localPositionOf(
                                                    slot, Offset(slot.size.width / 2f, 0f)
                                                ).x
                                            }
                                        }
                                    }
                                } else {
                                    {}
                                }
                            )
                        }
                    }
                    // Slots rechts: übrige Aktivitäten als Kreise. GEHALTENE, animierte Slot-Liste,
                    // damit ein dazukommender Kreis weich rechts einwächst (statt hart zu poppen) und
                    // ein verschwindender weich ausblendet. Das Ein-/Ausblenden passiert auf
                    // LAYOUT-Ebene (expand/shrinkHorizontally) → die Row-Breite morpht sanft, was die
                    // (rohe) clusterWidthPx und damit das Re-Centern kohärent mitführt.
                    val circleOrder = remember { mutableStateListOf<String>() }
                    val circleContent = remember { mutableStateMapOf<String, IslandContent>() }
                    val circleVisible = remember { mutableStateMapOf<String, MutableTransitionState<Boolean>>() }
                    val currentIds = others.map(::activityId)
                    SideEffect {
                        others.forEach { o ->
                            val id = activityId(o)
                            circleContent[id] = o
                            if (id !in circleOrder) circleOrder.add(id)
                            circleVisible.getOrPut(id) { MutableTransitionState(false) }.targetState = true
                        }
                        circleVisible.forEach { (id, st) -> if (id !in currentIds) st.targetState = false }
                    }
                    circleOrder.toList().forEach { id ->
                        val st = circleVisible[id] ?: return@forEach
                        val content = circleContent[id]
                        key(id) {
                            AnimatedVisibility(
                                visibleState = st,
                                enter = if (animationsEnabled) {
                                    expandHorizontally(RethroneSprings.morph(speed), Alignment.Start) +
                                        scaleIn(RethroneSprings.morph(speed), initialScale = 0.6f) +
                                        fadeIn(RethroneSprings.effects(speed))
                                } else {
                                    EnterTransition.None
                                },
                                exit = if (animationsEnabled) {
                                    shrinkHorizontally(RethroneSprings.effects(speed), Alignment.Start) +
                                        scaleOut(RethroneSprings.effects(speed), targetScale = 0.6f) +
                                        fadeOut(RethroneSprings.effects(speed))
                                } else {
                                    ExitTransition.None
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(Modifier.width(6.dp))
                                    if (content != null) {
                                        AppCircle(content, loadIcon, iconCache, islandColor, islandSubContent)
                                    }
                                }
                            }
                            // Vollständig ausgeblendete Slots aufräumen (nach Abschluss der Exit-Anim).
                            if (!st.targetState && st.isIdle && !st.currentState) {
                                LaunchedEffect(id) {
                                    circleOrder.remove(id)
                                    circleVisible.remove(id)
                                    circleContent.remove(id)
                                }
                            }
                        }
                    }
                }
                // Unsichtbare Tipp-/Swipe-Erweiterung UNTER der Statusleiste, damit Gesten ankommen.
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                )
            }
        }
    }
}

/**
 * Lädt ein App-Icon über den geteilten, Swap-überlebenden [cache]. Liefert sofort das gecachte
 * Bitmap (kein `null`-Frame), wenn das Paket schon einmal geladen wurde → kein Dot→Icon-Flash beim
 * Wechsel. Erst beim allerersten Anfordern eines Pakets wird einmalig nachgeladen.
 */
@Composable
private fun rememberIslandIcon(
    pkg: String?,
    loadIcon: suspend (String) -> ImageBitmap?,
    cache: SnapshotStateMap<String, ImageBitmap?>
): ImageBitmap? {
    if (pkg == null) return null
    LaunchedEffect(pkg) {
        if (!cache.containsKey(pkg)) cache[pkg] = loadIcon(pkg)
    }
    return cache[pkg]
}

/** Kleiner runder App-Icon-Indikator der anderen aktiven Aktivität (Swipe-Ziel, immer rechts). */
@Composable
private fun AppCircle(
    content: IslandContent,
    loadIcon: suspend (String) -> ImageBitmap?,
    cache: SnapshotStateMap<String, ImageBitmap?>,
    islandColor: Color,
    fallbackColor: Color
) {
    val img = rememberIslandIcon(iconPackage(content), loadIcon, cache)
    Box(
        modifier = Modifier
            // Gleiche Höhe wie die Pille, damit der Kreis nicht kleiner als die Pille wirkt.
            .size(IslandPillHeight)
            .clip(CircleShape)
            .background(islandColor),
        contentAlignment = Alignment.Center
    ) {
        if (img != null) {
            Image(
                bitmap = img,
                contentDescription = null,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
            )
        } else {
            Dot(fallbackColor)
        }
    }
}

/**
 * Schmale, kompakte Pille, die das Kamera-Loch umschließt: ein minimaler Hinweis links und
 * rechts, dazwischen eine Lücke für die Kamera. Die **volle** Nachricht erscheint bewusst erst
 * in der aufgeklappten Karte ([IslandExpandedCard]) – live bleibt es klein und aufgeräumt.
 */
@Composable
private fun IslandPill(
    content: IslandContent?,
    cutoutWidth: Dp,
    loadIcon: suspend (String) -> ImageBitmap?,
    iconCache: SnapshotStateMap<String, ImageBitmap?>,
    islandColor: Color,
    contentColor: Color,
    subContentColor: Color,
    editMode: Boolean,
    modifier: Modifier = Modifier,
    balanced: Boolean = true,
    onCameraSlotPositioned: (LayoutCoordinates) -> Unit = {}
) {
    if (content == null && !editMode) return
    val shape = RoundedCornerShape(28.dp)
    // Lücke = Kamera-Slot. Knapp über der Notch-Breite gehalten (kleiner Sicherheitsabstand),
    // damit die Pille kompakt bleibt und Inhalt nah – aber frei – an der Notch sitzt.
    val gap = if (cutoutWidth > 0.dp) cutoutWidth + 4.dp else 8.dp

    // Tap wird vom umschließenden Container (mit Erweiterung unter die Statusleiste) gehandhabt.
    var rowModifier = modifier
        .clip(shape)
        .background(islandColor)
    if (editMode) {
        rowModifier = rowModifier.border(1.dp, contentColor.copy(alpha = 0.45f), shape)
    }

    val styleModifier = rowModifier
        .height(IslandPillHeight)
        .padding(horizontal = 10.dp, vertical = 5.dp)

    if (editMode) {
        // Reine Vorschau-Pille an der echten Position (umschließt die Kamera).
        Box(modifier = styleModifier, contentAlignment = Alignment.Center) {
            CameraBalancedRow(
                gap = gap,
                balanced = balanced,
                onCameraSlotPositioned = onCameraSlotPositioned,
                leading = { Dot(AccentBlue) },
                trailing = { Dot(AccentBlue) }
            )
        }
        return
    }

    val c = content ?: return
    Box(modifier = styleModifier, contentAlignment = Alignment.Center) {
        CameraBalancedRow(
            gap = gap,
            balanced = balanced,
            onCameraSlotPositioned = onCameraSlotPositioned,
            // Führender, gut lesbarer Inhalt (links der Kamera) – nie ins Punch-Hole.
            leading = {
                when (c) {
                    is IslandContent.Notification -> AppIcon(c.pkg, loadIcon, iconCache)
                    // Kein Wort: Live-Zeit wenn ableitbar, sonst nur das App-Icon der Uhr.
                    is IslandContent.Timer ->
                        if (c.displayMs != null) TimerLabel(formatRemaining(c.displayMs), contentColor) else AppIcon(c.pkg, loadIcon, iconCache)
                    is IslandContent.Battery -> ShortLabel("${c.level}%", contentColor)
                    is IslandContent.Media -> MediaArt(c.art, islandColor)
                }
            },
            // Kleiner Statuspunkt rechts der Kamera.
            trailing = {
                when (c) {
                    is IslandContent.Notification -> Dot(AccentBlue)
                    is IslandContent.Timer -> Dot(if (c.paused) TimerAmber else ChargingGreen)
                    is IslandContent.Battery -> Dot(if (c.charging) ChargingGreen else subContentColor)
                    is IslandContent.Media -> MediaBars(c.isPlaying)
                }
            }
        )
    }
}

/**
 * Layoutet die Pille um den Kamera-Slot. Der Inhalt sitzt jeweils **direkt an der Lücke**
 * (führend rechtsbündig, folgend linksbündig), sodass Timer und Punkt die Notch eng umschließen.
 *
 * @param balanced `true` ⇒ beide Seiten gleich breit (= max. der Inhalte) → das Kamera-Loch sitzt
 *   **mittig** in der Pille (Ausgleich/Leerraum wandert an die Außenränder). Sinnvoll, wenn die
 *   Pille allein steht. `false` ⇒ **kompakt** (Breite = Inhalt + Lücke + Inhalt), kein Leerraum →
 *   nachfolgende App-Kreise sitzen dicht an der Notch (statt ans Status-Icon-Band gedrückt zu
 *   werden). In beiden Fällen hugt der Inhalt die Notch und die Slot-Mitte bleibt der Kamera-Anker.
 *
 * Die Mitte des Slots wird via [onCameraSlotPositioned] gemeldet (Zentrierung unter der Kamera).
 */
@Composable
private fun CameraBalancedRow(
    gap: Dp,
    balanced: Boolean,
    onCameraSlotPositioned: (LayoutCoordinates) -> Unit,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit
) {
    Layout(
        content = {
            Box(contentAlignment = Alignment.Center) { leading() }
            Spacer(
                Modifier
                    .width(gap)
                    .onGloballyPositioned(onCameraSlotPositioned)
            )
            Box(contentAlignment = Alignment.Center) { trailing() }
        }
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val lead = measurables[0].measure(loose)
        val cam = measurables[1].measure(loose)
        val trail = measurables[2].measure(loose)
        val side = maxOf(lead.width, trail.width)
        val leftSide = if (balanced) side else lead.width
        val rightSide = if (balanced) side else trail.width
        val width = leftSide + cam.width + rightSide
        val height = maxOf(lead.height, cam.height, trail.height)
        layout(width, height) {
            // Führend rechtsbündig (rechte Kante an der Lücke = an der Notch).
            lead.place(leftSide - lead.width, (height - lead.height) / 2)
            cam.place(leftSide, (height - cam.height) / 2)
            // Folgend: balanced ⇒ an den äußeren Pillen-Rand (spiegelt den Timer links →
            // Notch mittig zwischen beiden); kompakt ⇒ dicht an die Notch.
            val trailX = if (balanced) leftSide + cam.width + rightSide - trail.width
            else leftSide + cam.width
            trail.place(trailX, (height - trail.height) / 2)
        }
    }
}

@Composable
private fun ShortLabel(text: String, color: Color = IslandText, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = modifier,
        // Schrift exakt vertikal mittig in der Pille: Font-Padding entfernen und die Zeile
        // symmetrisch auf Schriftgröße trimmen/zentrieren (sonst sitzt der Text leicht tief).
        lineHeight = 14.sp,
        style = LocalTextStyle.current.merge(
            TextStyle(
                // Tabular Figures: gleich breite Ziffern → Timer/Prozent „atmen" nicht je Sekunde.
                fontFeatureSettings = "tnum",
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                )
            )
        )
    )
}

/**
 * Timer-Zeit mit **fester** Breite: ein unsichtbarer Platzhalter (max. Breite des Formats)
 * reserviert die Breite → die Pille wackelt nicht je Sekunde. Die echte Zeit liegt **linksbündig**
 * darüber, sodass ihr linker Rand konstant ist (gleicher Außen-Abstand wie der Punkt rechts).
 */
@Composable
private fun TimerLabel(text: String, color: Color = IslandText) {
    val placeholder = if (text.count { it == ':' } == 2) "00:00:00" else "00:00"
    Box(contentAlignment = Alignment.CenterStart) {
        ShortLabel(placeholder, color, Modifier.alpha(0f))
        ShortLabel(text, color)
    }
}

@Composable
private fun MediaArt(art: Bitmap?, placeholderColor: Color) {
    // Cover-Slot konstant 20.dp halten: Beim Track-Wechsel kommt das neue Album-Bitmap
    // asynchron (kurzzeitig null) nach. Fiele der Slot dann auf einen 8.dp-Dot zurück, schrumpfte
    // und wüchse die Pille und spränge horizontal (Re-Center) → Flackern. Letztes Cover puffern
    // und immer eine 20.dp-Box (Bild oder dezenter Platzhalter) zeichnen.
    var lastArt by remember { mutableStateOf<Bitmap?>(null) }
    if (art != null) lastArt = art
    val shown = art ?: lastArt
    val bmp = remember(shown) { shown?.asImageBitmap() }
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(placeholderColor),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Dot(AccentBlue)
        }
    }
}

@Composable
private fun AppIcon(
    pkg: String,
    loadIcon: suspend (String) -> ImageBitmap?,
    cache: SnapshotStateMap<String, ImageBitmap?>
) {
    val img = rememberIslandIcon(pkg, loadIcon, cache)
    if (img != null) {
        Image(
            bitmap = img,
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
        )
    } else {
        Dot(AccentBlue)
    }
}

@Composable
private fun Dot(color: Color) {
    Spacer(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/** Höhe des Balken-Bands (Equalizer) im Medien-Status rechts der Kamera. */
private val MediaBarsHeight = 14.dp

/**
 * Equalizer-Balken als Medien-Statusanzeige (ersetzt den Punkt): bei Wiedergabe „tanzen" die
 * Balken (mehrere phasenversetzte Endlos-Animationen), bei Pause stehen sie in festen Resthöhen
 * still – wie aus Now-Playing-Anzeigen bekannt. Grün bei Wiedergabe, gedämpft grau bei Pause.
 * Bei deaktivierten Animationen bleiben die Balken ebenfalls statisch.
 */
@Composable
private fun MediaBars(isPlaying: Boolean) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val color = if (isPlaying) ChargingGreen else IslandSubText
    // Pro Balken: Tempo (ms) der Endlos-Bewegung und Resthöhe (Anteil) im Stillstand.
    val durations = listOf(560, 420, 680, 500)
    val rests = listOf(0.5f, 0.8f, 0.35f, 0.6f)
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.height(MediaBarsHeight)
    ) {
        if (isPlaying && animationsEnabled) {
            val transition = rememberInfiniteTransition(label = "mediaBars")
            durations.forEachIndexed { i, dur ->
                val fraction by transition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(dur, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(i * 90)
                    ),
                    label = "mediaBar$i"
                )
                EqualizerBar(fraction, color)
            }
        } else {
            rests.forEach { EqualizerBar(it, color) }
        }
    }
}

/** Einzelner Equalizer-Balken; [heightFraction] = Anteil der vollen Bandhöhe (von unten wachsend). */
@Composable
private fun EqualizerBar(heightFraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .width(2.5.dp)
            .fillMaxHeight(heightFraction.coerceIn(0.15f, 1f))
            .clip(RoundedCornerShape(1.5.dp))
            .background(color)
    )
}

/**
 * Antippbare Steuerleiste zum Justieren der Insel-Höhe im Layout-Editor. Wird vom Aufrufer
 * UNTER der Statusleiste platziert (`statusBarsPadding`), damit die Taps ankommen – in der
 * Statusleiste selbst würde das System sie abfangen.
 */
@Composable
fun IslandEditControls(onNudge: (Float) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(IslandSurface)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NudgeButton(Icons.Rounded.KeyboardArrowUp) { onNudge(-EDIT_NUDGE_DP) }
        Spacer(Modifier.width(6.dp))
        NudgeButton(Icons.Rounded.KeyboardArrowDown) { onNudge(EDIT_NUDGE_DP) }
    }
}

/** Tipp-Button (hoch/runter) zum Justieren der Insel-Höhe im Layout-Editor. */
@Composable
private fun NudgeButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = IslandText, modifier = Modifier.size(22.dp))
    }
}

/** Mittelpunkt + Größe des Display-Cutouts (Kamera) in Pixeln plus Bildschirmbreite. */
private data class CutoutInfo(
    val centerX: Float,
    val centerY: Float,
    val widthPx: Int,
    val heightPx: Int,
    val screenWidth: Int
)

/**
 * Ermittelt Mitte und Breite der Front-Kamera (Display-Cutout) in Pixeln, damit die Pille die
 * Kamera umschließen kann. Gibt `null` zurück, wenn kein Cutout vorhanden ist (dann Fallback
 * nahe oben-mittig in [DynamicIsland]).
 */
@Composable
private fun rememberCutoutInfo(): CutoutInfo? {
    val view = LocalView.current
    val density = LocalDensity.current
    // Reaktiver Schlüssel: der Compose-Cutout-Inset ändert sich, sobald die Insets verfügbar
    // werden bzw. bei Rotation – dann werden die Platform-Bounds neu berechnet. Den
    // OnApplyWindowInsetsListener der View bewusst NICHT überschreiben (würde Compose stören).
    val cutoutTopInset = WindowInsets.displayCutout.getTop(density)
    return remember(view.width, view.height, cutoutTopInset) {
        val cutout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.rootWindowInsets?.displayCutout
        } else {
            null
        }
        // Gezielt den oberen Cutout (Front-Kamera); ignoriert Eck-/Seiten-/Doppel-Cutouts.
        val rect = when {
            cutout == null -> null
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> cutout.boundingRectTop
            else -> cutout.boundingRects.firstOrNull()
        }
        if (rect != null && !rect.isEmpty && view.width > 0) {
            CutoutInfo(
                rect.centerX().toFloat(),
                rect.centerY().toFloat(),
                rect.width(),
                rect.height(),
                view.width
            )
        } else {
            null
        }
    }
}

/** Formatiert Restzeit als `m:ss` bzw. `h:mm:ss`. */
internal fun formatRemaining(remainingMs: Long): String {
    val totalSeconds = remainingMs / 1000
    val hours = TimeUnit.SECONDS.toHours(totalSeconds)
    val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
