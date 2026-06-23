package com.example.androidlauncher.ui

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import com.example.androidlauncher.data.IslandContent
import com.example.androidlauncher.data.IslandState
import com.example.androidlauncher.data.activityId
import com.example.androidlauncher.data.iconPackage
import com.example.androidlauncher.ui.theme.LocalAnimationSpeed
import com.example.androidlauncher.ui.theme.LocalAnimationsEnabled
import com.example.androidlauncher.ui.theme.RethroneSprings
import java.util.concurrent.TimeUnit
import kotlin.math.min
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
@Composable
fun DynamicIsland(
    state: IslandState,
    onTap: (IslandContent) -> Unit,
    loadIcon: suspend (String) -> ImageBitmap?,
    verticalOffsetDp: Float = 0f,
    editMode: Boolean = false,
    onSwipeNext: () -> Unit = {},
    onSwipePrevious: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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

    val animationsEnabled = LocalAnimationsEnabled.current
    val speed = LocalAnimationSpeed.current
    val cutout = rememberCutoutInfo()
    val density = LocalDensity.current
    val cutoutWidth = cutout?.let { with(density) { it.widthPx.toDp() } } ?: 0.dp
    // Start-/Zielgröße der „Notch wächst"-Animation ≈ Kamera-Cutout (Fallback: kleine Größe).
    val notchWidthPx = cutout?.widthPx ?: with(density) { 28.dp.roundToPx() }
    val notchHeightPx = cutout?.heightPx ?: with(density) { IslandPillHeight.roundToPx() }
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
                val clusterCenterScreen = if (cameraAnchorPx >= 0f && clusterWidthPx > 0) {
                    cutout.centerX - (cameraAnchorPx - clusterWidthPx / 2f)
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
        // „Wächst aus der Notch": von Kamera-Größe (oben-mittig) auf volle Größe expandieren.
        enter = if (animationsEnabled) {
            expandIn(
                RethroneSprings.container(speed),
                expandFrom = Alignment.TopCenter,
                initialSize = { full -> IntSize(min(notchWidthPx, full.width), min(notchHeightPx, full.height)) }
            ) + fadeIn(RethroneSprings.effects(speed))
        } else {
            fadeIn()
        },
        exit = if (animationsEnabled) {
            shrinkOut(
                RethroneSprings.container(speed),
                shrinkTowards = Alignment.TopCenter,
                targetSize = { full -> IntSize(min(notchWidthPx, full.width), min(notchHeightPx, full.height)) }
            ) + fadeOut(RethroneSprings.effects(speed))
        } else {
            fadeOut()
        }
    ) {
        if (editMode) {
            // Vorschau ohne Tap/Swipe (Höhe via separate Steuerleiste).
            IslandPill(
                shownContent,
                cutoutWidth,
                loadIcon,
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
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
                                if (animationsEnabled) {
                                    (scaleIn(RethroneSprings.spatial(speed), initialScale = 0.85f) +
                                        fadeIn(RethroneSprings.effects(speed))) togetherWith
                                        (scaleOut(RethroneSprings.effects(speed), targetScale = 0.85f) +
                                            fadeOut(RethroneSprings.effects(speed)))
                                } else {
                                    fadeIn() togetherWith fadeOut()
                                }
                            },
                            label = "islandActivePill"
                        ) { c ->
                            IslandPill(
                                c, cutoutWidth, loadIcon, editMode = false,
                                balanced = others.isEmpty(),
                                onCameraSlotPositioned = { slot ->
                                    rowCoords?.let { r ->
                                        if (r.isAttached && slot.isAttached) {
                                            cameraAnchorPx = r.localPositionOf(
                                                slot, Offset(slot.size.width / 2f, 0f)
                                            ).x
                                        }
                                    }
                                }
                            )
                        }
                    }
                    // Slots rechts: übrige Aktivitäten als Kreise (stabile Reihenfolge), Inhalt per Crossfade.
                    others.forEachIndexed { index, other ->
                        Spacer(Modifier.width(6.dp))
                        AnimatedContent(
                            targetState = other,
                            contentKey = { activityId(it) },
                            transitionSpec = {
                                if (animationsEnabled) {
                                    fadeIn(RethroneSprings.effects(speed)) togetherWith
                                        fadeOut(RethroneSprings.effects(speed))
                                } else {
                                    fadeIn() togetherWith fadeOut()
                                }
                            },
                            label = "islandCircle$index"
                        ) { o ->
                            AppCircle(o, loadIcon)
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

/** Kleiner runder App-Icon-Indikator der anderen aktiven Aktivität (Swipe-Ziel, immer rechts). */
@Composable
private fun AppCircle(content: IslandContent, loadIcon: suspend (String) -> ImageBitmap?) {
    val pkg = iconPackage(content)
    val icon by produceState<ImageBitmap?>(initialValue = null, pkg) {
        value = pkg?.let { loadIcon(it) }
    }
    val img = icon
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(IslandSurface),
        contentAlignment = Alignment.Center
    ) {
        if (img != null) {
            Image(
                bitmap = img,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
            )
        } else {
            Dot(IslandSubText)
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
        .background(IslandSurface)
    if (editMode) {
        rowModifier = rowModifier.border(1.dp, IslandText.copy(alpha = 0.45f), shape)
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
                    is IslandContent.Notification -> AppIcon(c.pkg, loadIcon)
                    // Kein Wort: Live-Zeit wenn ableitbar, sonst nur das App-Icon der Uhr.
                    is IslandContent.Timer ->
                        if (c.displayMs != null) TimerLabel(formatRemaining(c.displayMs)) else AppIcon(c.pkg, loadIcon)
                    is IslandContent.Battery -> ShortLabel("${c.level}%")
                    is IslandContent.Media -> MediaArt(c.art)
                }
            },
            // Kleiner Statuspunkt rechts der Kamera.
            trailing = {
                when (c) {
                    is IslandContent.Notification -> Dot(AccentBlue)
                    is IslandContent.Timer -> Dot(if (c.paused) TimerAmber else ChargingGreen)
                    is IslandContent.Battery -> Dot(if (c.charging) ChargingGreen else IslandSubText)
                    is IslandContent.Media -> Dot(if (c.isPlaying) ChargingGreen else IslandSubText)
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
private fun ShortLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = IslandText,
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
private fun TimerLabel(text: String) {
    val placeholder = if (text.count { it == ':' } == 2) "00:00:00" else "00:00"
    Box(contentAlignment = Alignment.CenterStart) {
        ShortLabel(placeholder, Modifier.alpha(0f))
        ShortLabel(text)
    }
}

@Composable
private fun MediaArt(art: Bitmap?) {
    if (art != null) {
        Image(
            bitmap = art.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(5.dp))
        )
    } else {
        Dot(AccentBlue)
    }
}

@Composable
private fun AppIcon(pkg: String, loadIcon: suspend (String) -> ImageBitmap?) {
    val icon by produceState<ImageBitmap?>(initialValue = null, pkg) { value = loadIcon(pkg) }
    val img = icon
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
