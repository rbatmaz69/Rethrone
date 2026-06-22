package com.example.androidlauncher.ui

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.roundToInt

internal val IslandSurface = Color(0xFF0B0B0C)
internal val IslandText = Color.White
internal val IslandSubText = Color(0xFFB5B5BA)
private val ChargingGreen = Color(0xFF34C759)
private val AccentBlue = Color(0xFF0A84FF)
private val TimerAmber = Color(0xFFFF9F0A)

/** Schrittweite (dp) pro Tipp auf die Höhen-Buttons im Layout-Editor. */
private const val EDIT_NUDGE_DP = 2f

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
    // „Main" = vom Manager gewählte Aktivität; die übrigen aktiven werden als App-Kreise daneben
    // gezeigt. Gewechselt wird per Swipe (keine Auto-Rotation mehr).
    val content = state.content
    val others = state.all.filter { content == null || activityId(it) != activityId(content) }
    // Letzten Inhalt merken, damit die Ausblende-Animation noch etwas anzuzeigen hat.
    var lastContent by remember { mutableStateOf<IslandContent?>(null) }
    LaunchedEffect(content) { if (content != null) lastContent = content }

    val animationsEnabled = LocalAnimationsEnabled.current
    val speed = LocalAnimationSpeed.current
    val cutout = rememberCutoutInfo()
    val density = LocalDensity.current
    val cutoutWidth = cutout?.let { with(density) { it.widthPx.toDp() } } ?: 0.dp
    // Statusleisten-Höhe für den Fallback ohne Cutout (notchlose Geräte / Tablets).
    val statusBarTopPx = WindowInsets.statusBars.getTop(density)
    // Gemessene Cluster-Größe (Pille + Kreise), um ihn auf der Kamera zu zentrieren / auf dem Schirm zu halten.
    var clusterWidthPx by remember { mutableStateOf(0) }
    var clusterHeightPx by remember { mutableStateOf(0) }

    AnimatedVisibility(
        // Im Edit-Modus immer sichtbar (Platzhalter zum Ziehen), sonst nur bei aktivem Inhalt.
        visible = content != null || editMode,
        modifier = modifier.offset {
            // Vertikal IMMER im Statusleisten-Band zentrieren (geräteadaptiv auf Kamerahöhe).
            val targetCenterY = statusBarTopPx / 2f + verticalOffsetDp.dp.toPx()
            val y = (targetCenterY - clusterHeightPx / 2f).coerceAtLeast(0f).roundToInt()
            // Horizontal: bei Cutout mittig auf die Kamera (auf dem Schirm geclampt), sonst mittig.
            val x = if (cutout != null) {
                val half = clusterWidthPx / 2f
                val margin = 6.dp.toPx()
                val lo = half + margin
                val hi = cutout.screenWidth - half - margin
                val centerX = if (lo <= hi) cutout.centerX.coerceIn(lo, hi) else cutout.screenWidth / 2f
                (centerX - cutout.screenWidth / 2f).roundToInt()
            } else {
                0
            }
            IntOffset(x, y)
        },
        enter = if (animationsEnabled) {
            scaleIn(RethroneSprings.spatial(speed), transformOrigin = TransformOrigin(0.5f, 0.5f)) +
                fadeIn(RethroneSprings.effects(speed))
        } else {
            fadeIn()
        },
        exit = if (animationsEnabled) {
            scaleOut(RethroneSprings.effects(speed), transformOrigin = TransformOrigin(0.5f, 0.5f)) +
                fadeOut(RethroneSprings.effects(speed))
        } else {
            fadeOut()
        }
    ) {
        if (editMode) {
            // Vorschau ohne Tap/Swipe (Höhe via separate Steuerleiste).
            IslandPill(
                content ?: lastContent,
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { (content ?: lastContent)?.let(onTap) }
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
                // Cluster: Haupt-Pille + App-Kreise der anderen aktiven Aktivitäten daneben.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.onGloballyPositioned {
                        clusterWidthPx = it.size.width
                        clusterHeightPx = it.size.height
                    }
                ) {
                    IslandPill(content ?: lastContent, cutoutWidth, loadIcon, editMode = false)
                    others.forEach { other ->
                        Spacer(Modifier.width(6.dp))
                        AppCircle(other, loadIcon)
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

/** Kleiner runder App-Icon-Indikator der anderen aktiven Aktivität (Swipe-Ziel). */
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
    modifier: Modifier = Modifier
) {
    if (content == null && !editMode) return
    val shape = RoundedCornerShape(28.dp)
    val gap = if (cutoutWidth > 0.dp) cutoutWidth + 8.dp else 10.dp

    // Tap wird vom umschließenden Container (mit Erweiterung unter die Statusleiste) gehandhabt.
    var rowModifier = modifier
        .clip(shape)
        .background(IslandSurface)
    if (editMode) {
        rowModifier = rowModifier.border(1.dp, IslandText.copy(alpha = 0.45f), shape)
    }

    Row(
        modifier = rowModifier.padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (editMode) {
            // Reine Vorschau-Pille an der echten Position (umschließt die Kamera).
            Dot(AccentBlue)
            Spacer(Modifier.width(gap))
            Dot(AccentBlue)
            return@Row
        }

        val c = content ?: return@Row

        // Führender Hinweis (links der Kamera).
        when (c) {
            is IslandContent.Notification -> AppIcon(c.pkg, loadIcon)
            is IslandContent.Timer -> Dot(TimerAmber)
            is IslandContent.Battery -> Dot(if (c.charging) ChargingGreen else IslandSubText)
            is IslandContent.Media -> MediaArt(c.art)
        }

        // Lücke, die das Kamera-Loch frei lässt (knapp gehalten für eine schmale Pille).
        Spacer(Modifier.width(gap))

        // Kurzer Hinweis rechts der Kamera.
        when (c) {
            is IslandContent.Notification -> Dot(AccentBlue)
            is IslandContent.Timer -> ShortLabel(
                c.remainingMs?.let { formatRemaining(it) } ?: c.label.ifBlank { "Timer" }
            )
            is IslandContent.Battery -> ShortLabel("${c.level}%")
            is IslandContent.Media -> Dot(if (c.isPlaying) ChargingGreen else IslandSubText)
        }
    }
}

@Composable
private fun ShortLabel(text: String) {
    Text(
        text = text,
        color = IslandText,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1
    )
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

/** Mittelpunkt + Breite des Display-Cutouts (Kamera) in Pixeln plus Bildschirmbreite. */
private data class CutoutInfo(
    val centerX: Float,
    val centerY: Float,
    val widthPx: Int,
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
            CutoutInfo(rect.centerX().toFloat(), rect.centerY().toFloat(), rect.width(), view.width)
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
