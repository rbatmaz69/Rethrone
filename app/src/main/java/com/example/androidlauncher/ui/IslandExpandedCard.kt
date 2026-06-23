package com.example.androidlauncher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.data.IslandContent
import com.example.androidlauncher.data.NotificationAction
import com.example.androidlauncher.isPauseActionTitle
import com.example.androidlauncher.isResumeActionTitle
import com.example.androidlauncher.ui.theme.LocalAnimationSpeed
import com.example.androidlauncher.ui.theme.LocalAnimationsEnabled
import com.example.androidlauncher.ui.theme.RethroneSprings

/**
 * Große, aufgeklappte Insel-Karte. Erscheint über dem restlichen UI (eigener `zIndex` in
 * `MainActivity`), zeigt den vollständigen Inhalt und – bei Benachrichtigungen – die
 * Aktions-Buttons zum direkten Reagieren. Tap auf den Text öffnet die App, Tap auf das Scrim
 * (oder Back) schließt die Karte.
 *
 * @param content der eingefrorene Inhalt (siehe `DynamicIslandManager.expandedContent`).
 * @param onAction Aktions-Button gedrückt (sendet den zugehörigen PendingIntent).
 * @param onOpen Tap auf den Inhalt (öffnet die App via contentIntent).
 * @param onDismiss Karte schließen.
 */
@Composable
fun IslandExpandedCard(
    content: IslandContent,
    onAction: (NotificationAction) -> Unit,
    onOpen: (IslandContent) -> Unit,
    onDismiss: () -> Unit,
    allContents: List<IslandContent> = emptyList(),
    onSwitch: (IslandContent) -> Unit = {},
    onTimerControl: (NotificationAction) -> Unit = {},
    onMediaPlayPause: () -> Unit = {},
    onMediaNext: () -> Unit = {},
    onMediaPrev: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        val swipeThresholdPx = with(LocalDensity.current) { 40.dp.toPx() }
        // Tabs in stabiler, klassenbasierter Reihenfolge: `allContents` (= Prioritäts-
        // Reihenfolge) springt sonst beim Track-Wechsel, weil `isPlaying` durch Buffering
        // kurz `false` wird und spielende/pausierte Medien die Position tauschen → die
        // Tab-Chips würden „teleportieren".
        val tabs = remember(allContents) { allContents.sortedBy { islandTabRank(it) } }
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(IslandSurface)
                // Klicks auf der Karte sollen nicht das Scrim-Dismiss auslösen.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onOpen(content) }
                )
                // Horizontaler Swipe wechselt – wie an der Pille – zwischen den Aktivitäten.
                .pointerInput(tabs, content) {
                    if (tabs.size > 1) {
                        var total = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = {
                                val cur = tabs.indexOfFirst { it::class == content::class }
                                if (cur >= 0) {
                                    val size = tabs.size
                                    val next = when {
                                        total <= -swipeThresholdPx -> (cur + 1) % size
                                        total >= swipeThresholdPx -> (cur - 1 + size) % size
                                        else -> cur
                                    }
                                    if (next != cur) onSwitch(tabs[next])
                                }
                            }
                        ) { _, drag -> total += drag }
                    }
                }
                .padding(20.dp)
        ) {
            if (tabs.size > 1) {
                ActivitySwitcher(tabs, content, onSwitch)
            }
            val animationsEnabled = LocalAnimationsEnabled.current
            val speed = LocalAnimationSpeed.current
            // Aktivitäts-Wechsel (Tabs) gleitet richtungsabhängig (Reihenfolge in allContents).
            // Key = Klasse: Timer-/Media-Live-Updates aktualisieren in-place, ohne Transition.
            AnimatedContent(
                targetState = content,
                contentKey = { it::class },
                transitionSpec = {
                    if (!animationsEnabled) {
                        fadeIn() togetherWith fadeOut()
                    } else {
                        val fromIdx = tabs.indexOfFirst { it::class == initialState::class }
                        val toIdx = tabs.indexOfFirst { it::class == targetState::class }
                        val dir = if (fromIdx >= 0 && toIdx >= 0) toIdx.compareTo(fromIdx) else 0
                        if (dir == 0) {
                            fadeIn(RethroneSprings.effects(speed)) togetherWith
                                fadeOut(RethroneSprings.effects(speed))
                        } else {
                            (slideInHorizontally(RethroneSprings.spatial(speed)) { w -> dir * w } +
                                fadeIn(RethroneSprings.effects(speed))) togetherWith
                                (slideOutHorizontally(RethroneSprings.effects(speed)) { w -> -dir * w } +
                                    fadeOut(RethroneSprings.effects(speed)))
                        }
                    }
                },
                label = "islandCardContent"
            ) { c ->
                // Column: die Karten emittieren mehrere vertikale Geschwister (Titel/Text/Buttons);
                // ohne eigenen Column-Container würden sie im AnimatedContent-Box überlappen.
                Column {
                    when (c) {
                        is IslandContent.Notification -> NotificationCard(c, onAction)
                        is IslandContent.Media -> MediaCard(c, onMediaPlayPause, onMediaNext, onMediaPrev)
                        is IslandContent.Timer -> TimerCard(c, onTimerControl)
                        is IslandContent.Battery -> SimpleCard(
                            if (c.charging) "Wird geladen" else "Netzteil getrennt",
                            "${c.level}%"
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotificationCard(
    content: IslandContent.Notification,
    onAction: (NotificationAction) -> Unit
) {
    if (content.title.isNotBlank()) {
        Text(
            text = content.title,
            color = IslandText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
    if (content.text.isNotBlank()) {
        Text(
            text = content.text,
            color = IslandSubText,
            fontSize = 15.sp,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
    if (content.actions.isNotEmpty()) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            content.actions.forEach { action ->
                ActionChip(action.title) { onAction(action) }
            }
        }
    }
}

@Composable
private fun ActionChip(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = IslandText,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 9.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActivitySwitcher(
    all: List<IslandContent>,
    current: IslandContent,
    onSwitch: (IslandContent) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        all.forEach { item ->
            val selected = item::class == current::class
            Text(
                text = activityChipLabel(item),
                color = if (selected) IslandSurface else IslandText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) IslandText else Color.White.copy(alpha = 0.12f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSwitch(item) }
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * Feste, klassenbasierte Reihenfolge der Tab-Chips im Switcher. Bewusst unabhängig von der
 * Prioritäts-Reihenfolge in `allContents` (die sich bei transienten Zustandswechseln – z. B.
 * Buffering beim Track-Skip – umsortiert), damit die Tabs nicht ihre Plätze tauschen.
 */
private fun islandTabRank(content: IslandContent): Int = when (content) {
    is IslandContent.Media -> 0
    is IslandContent.Timer -> 1
    is IslandContent.Notification -> 2
    is IslandContent.Battery -> 3
}

private fun activityChipLabel(content: IslandContent): String = when (content) {
    is IslandContent.Media -> "Medien"
    is IslandContent.Timer -> content.displayMs?.let { formatRemaining(it) } ?: "Uhr"
    is IslandContent.Notification -> content.title.ifBlank { "Mitteilung" }
    is IslandContent.Battery -> "Akku"
}

@Composable
private fun MediaCard(
    content: IslandContent.Media,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Cover-Slot konstant halten: Beim Track-Wechsel kommt das neue Album-Bitmap
        // asynchron (kurzzeitig null) nach. Würde das Bild bedingt gerendert, schrumpft
        // und wächst die Karte – die Größen-Animation des umschließenden AnimatedContent
        // staucht/clippt dann den Inhalt für ein paar Frames. Letztes Cover puffern und
        // immer eine 56.dp-Box (Bild oder Platzhalter) zeichnen.
        var lastArt by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
        if (content.art != null) lastArt = content.art
        val art = content.art ?: lastArt
        val bmp = remember(art) { art?.asImageBitmap() }
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = content.title.ifBlank { "Wiedergabe" },
                color = IslandText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (content.artist.isNotBlank()) {
                Text(
                    text = content.artist,
                    color = IslandSubText,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TransportButton(Icons.Rounded.SkipPrevious, onPrev)
        TransportButton(
            if (content.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            onPlayPause
        )
        TransportButton(Icons.Rounded.SkipNext, onNext)
    }
}

@Composable
private fun TransportButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = IslandText, modifier = Modifier.size(32.dp))
    }
}

/**
 * Timer-/Stoppuhr-Karte: große Live-Zeit plus Steuerung über die App-eigenen
 * Notification-Aktionen. Die Pause/Resume-Aktion wird als prominenter Play/Pause-Button
 * dargestellt, übrige Aktionen (+1:00, Zurücksetzen, …) als Chips. Klicks feuern den
 * jeweiligen PendingIntent via [onControl] – die Karte bleibt offen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimerCard(content: IslandContent.Timer, onControl: (NotificationAction) -> Unit) {
    Text(
        text = content.displayMs?.let { formatRemaining(it) } ?: "--:--",
        color = IslandText,
        fontSize = 40.sp,
        fontWeight = FontWeight.Bold
    )
    val toggle = content.actions.firstOrNull {
        isPauseActionTitle(it.title) || isResumeActionTitle(it.title)
    }
    val others = content.actions.filter { it !== toggle }
    if (toggle != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransportButton(
                if (isResumeActionTitle(toggle.title)) Icons.Rounded.PlayArrow else Icons.Rounded.Pause
            ) { onControl(toggle) }
        }
    }
    if (others.isNotEmpty()) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            others.forEach { action -> ActionChip(action.title) { onControl(action) } }
        }
    }
}

@Composable
private fun SimpleCard(primary: String, secondary: String) {
    Text(primary, color = IslandText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    if (secondary.isNotBlank()) {
        Text(
            text = secondary,
            color = IslandSubText,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
