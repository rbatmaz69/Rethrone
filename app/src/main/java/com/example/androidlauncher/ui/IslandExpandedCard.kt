package com.example.androidlauncher.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.androidlauncher.MediaProgress
import com.example.androidlauncher.data.IslandContent
import com.example.androidlauncher.data.NotificationAction
import com.example.androidlauncher.extrapolateMediaPositionMs
import com.example.androidlauncher.formatMediaTime
import com.example.androidlauncher.isPauseActionTitle
import com.example.androidlauncher.isResumeActionTitle
import com.example.androidlauncher.ui.theme.LocalAnimationSpeed
import com.example.androidlauncher.ui.theme.LocalAnimationsEnabled
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.RethroneShapes
import com.example.androidlauncher.ui.theme.RethroneSprings
import kotlinx.coroutines.delay

/**
 * Bündelt die aus dem App-Theme abgeleiteten Farben der Insel-Karte, damit die Unterkarten
 * (Notification/Media/Timer) konsistent dieselbe themenbasierte Palette nutzen statt der früher
 * hartcodierten Dunkel-Farben. So passt sich die geöffnete Insel jedem Farbthema / Material You an.
 */
private data class IslandCardColors(
    val surface: Color,
    val text: Color,
    val subText: Color,
    val accent: Color
)

@Composable
private fun rememberIslandCardColors(islandColor: Color): IslandCardColors {
    val isDarkText = LocalDarkTextEnabled.current
    // Inhaltsfarbe per Kontrast aus der gewählten Insel-Farbe ableiten (dunkler Text auf heller
    // Insel, sonst weiß) – so bleibt der Text bei jeder Farbe lesbar. Akzent bleibt der Theme-
    // Akzent (Buttons/Chips), damit die Insel zum App-Design passt.
    val text = if (islandColor.luminance() > 0.5f) Color(0xFF010101) else Color.White
    return IslandCardColors(
        surface = islandColor,
        text = text,
        subText = text.copy(alpha = 0.6f),
        accent = LocalColorTheme.current.highlightColor(isDarkText)
    )
}

/**
 * Große, aufgeklappte Insel-Karte. Erscheint über dem restlichen UI (eigener `zIndex` in
 * `MainActivity`), zeigt den vollständigen Inhalt und – bei Benachrichtigungen – die
 * Aktions-Buttons zum direkten Reagieren. Tap auf den Text öffnet die App, Tap auf das Scrim
 * (oder Back) schließt die Karte. Stil & Farben folgen dem App-Theme (Material-3-Expressive).
 *
 * @param content der eingefrorene Inhalt (siehe `DynamicIslandManager.expandedContent`).
 * @param onAction Aktions-Button gedrückt (sendet den zugehörigen PendingIntent).
 * @param onReply Reply-Aktion abgeschickt (RemoteInput, z. B. WhatsApp): (Aktion, Text).
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
    onReply: (NotificationAction, String) -> Unit = { _, _ -> },
    onTimerControl: (NotificationAction) -> Unit = {},
    onMediaPlayPause: () -> Unit = {},
    onMediaNext: () -> Unit = {},
    onMediaPrev: () -> Unit = {},
    onMediaSeekTo: (Long) -> Unit = {},
    islandColor: Color = Color(0xFF0B0B0C)
) {
    val colors = rememberIslandCardColors(islandColor)
    val cardShape = RethroneShapes.extraLarge
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
                // Weiche, RUNDE Tiefe (Default `clip = true` ⇒ Schatten folgt der Form). Das frühere
                // `designSurface(clip = false)` projizierte dagegen eine rechteckige Schatten-Box,
                // die beim Erscheinen hinter der runden Karte aufblitzte.
                .shadow(16.dp, cardShape)
                .clip(cardShape)
                // Solide, frei wählbare Insel-Farbe (Default Schwarz) – gilt für Pille & Karte.
                .background(colors.surface)
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
                ActivitySwitcher(tabs, content, colors, onSwitch)
            }
            val animationsEnabled = LocalAnimationsEnabled.current
            val speed = LocalAnimationSpeed.current
            // Aktivitäts-Wechsel (Tabs) gleitet richtungsabhängig (Reihenfolge in allContents).
            // Key = Klasse: Timer-/Media-Live-Updates aktualisieren in-place, ohne Transition.
            AnimatedContent(
                targetState = content,
                contentKey = { it::class },
                transitionSpec = {
                    // Kartenhöhe (Media vs. Timer vs. Notification) ohne Nachschwingen morphen,
                    // damit sie während des horizontalen Slides smooth wächst/schrumpft statt zu
                    // springen. clip=false verhindert das Anschneiden des einblendenden Inhalts.
                    val sizeSpec = SizeTransform(clip = false) { _, _ ->
                        if (animationsEnabled) RethroneSprings.morph(speed) else snap()
                    }
                    if (!animationsEnabled) {
                        ContentTransform(fadeIn(), fadeOut(), sizeTransform = sizeSpec)
                    } else {
                        val fromIdx = tabs.indexOfFirst { it::class == initialState::class }
                        val toIdx = tabs.indexOfFirst { it::class == targetState::class }
                        val dir = if (fromIdx >= 0 && toIdx >= 0) toIdx.compareTo(fromIdx) else 0
                        if (dir == 0) {
                            ContentTransform(
                                targetContentEnter = fadeIn(RethroneSprings.effects(speed)),
                                initialContentExit = fadeOut(RethroneSprings.effects(speed)),
                                sizeTransform = sizeSpec
                            )
                        } else {
                            ContentTransform(
                                targetContentEnter = slideInHorizontally(RethroneSprings.spatial(speed)) { w -> dir * w } +
                                    fadeIn(RethroneSprings.effects(speed)),
                                initialContentExit = slideOutHorizontally(RethroneSprings.effects(speed)) { w -> -dir * w } +
                                    fadeOut(RethroneSprings.effects(speed)),
                                sizeTransform = sizeSpec
                            )
                        }
                    }
                },
                label = "islandCardContent"
            ) { c ->
                // Column: die Karten emittieren mehrere vertikale Geschwister (Titel/Text/Buttons);
                // ohne eigenen Column-Container würden sie im AnimatedContent-Box überlappen.
                Column {
                    when (c) {
                        is IslandContent.Notification -> NotificationCard(c, colors, onAction, onReply)
                        is IslandContent.Media ->
                            MediaCard(c, colors, onMediaPlayPause, onMediaNext, onMediaPrev, onMediaSeekTo)
                        is IslandContent.Timer -> TimerCard(c, colors, onTimerControl)
                        is IslandContent.Battery -> SimpleCard(
                            if (c.charging) "Wird geladen" else "Netzteil getrennt",
                            "${c.level}%",
                            colors
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
    colors: IslandCardColors,
    onAction: (NotificationAction) -> Unit,
    onReply: (NotificationAction, String) -> Unit
) {
    if (content.title.isNotBlank()) {
        Text(
            text = content.title,
            color = colors.text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
    if (content.text.isNotBlank()) {
        Text(
            text = content.text,
            color = colors.subText,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
    // Inline-Antwort: eine angetippte Reply-Aktion (RemoteInput) klappt unten ein Textfeld auf,
    // statt sofort einen leeren Intent zu feuern.
    var replyingTo by remember(content) { mutableStateOf<NotificationAction?>(null) }
    if (content.actions.isNotEmpty()) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content.actions.forEach { action ->
                val selected = replyingTo === action
                ActionChip(action.title, colors, selected = selected) {
                    if (action.isReply) {
                        replyingTo = if (selected) null else action
                    } else {
                        onAction(action)
                    }
                }
            }
        }
    }
    replyingTo?.let { action ->
        ReplyField(
            colors = colors,
            onSend = { text ->
                onReply(action, text)
                replyingTo = null
            }
        )
    }
}

/**
 * Inline-Antwortfeld für RemoteInput-Aktionen. Fokussiert sich automatisch (öffnet die Tastatur),
 * `Senden` über die IME-Action oder den Button. Themenbasiert gestaltet (kein Material-Default).
 */
@Composable
private fun ReplyField(colors: IslandCardColors, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val submit = {
        val t = text.trim()
        if (t.isNotEmpty()) {
            keyboard?.hide()
            onSend(t)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .clip(RethroneShapes.large)
            .background(colors.text.copy(alpha = 0.10f))
            .padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (text.isEmpty()) {
                Text(
                    text = "Antworten…",
                    color = colors.subText,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text),
                singleLine = true,
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit() })
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.accent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { submit() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Send,
                contentDescription = "Senden",
                tint = colors.surface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    colors: IslandCardColors,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = if (selected) colors.surface else colors.text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RethroneShapes.small)
            .background(if (selected) colors.accent else colors.text.copy(alpha = 0.10f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActivitySwitcher(
    all: List<IslandContent>,
    current: IslandContent,
    colors: IslandCardColors,
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
                color = if (selected) colors.surface else colors.text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RethroneShapes.extraSmall)
                    .background(if (selected) colors.accent else colors.text.copy(alpha = 0.10f))
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
    colors: IslandCardColors,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeekTo: (Long) -> Unit
) {
    // Cover-Slot konstant halten: Beim Track-Wechsel kommt das neue Album-Bitmap
    // asynchron (kurzzeitig null) nach. Würde das Bild bedingt gerendert, schrumpft
    // und wächst die Karte – die Größen-Animation des umschließenden AnimatedContent
    // staucht/clippt dann den Inhalt für ein paar Frames. Letztes Cover puffern und
    // immer eine 56.dp-Box (Bild oder Platzhalter) zeichnen.
    var lastArt by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    if (content.art != null) lastArt = content.art
    val art = content.art ?: lastArt
    // Album-Akzent: dominanter Farbton des Covers (1×1-Downscale), zur Lesbarkeit mit
    // der Kartentextfarbe aufgehellt/abgedunkelt. Ohne Cover bleibt der Theme-Akzent.
    val albumAccent = remember(art, colors) { art?.dominantColor()?.readableOn(colors) ?: colors.accent }
    Row(verticalAlignment = Alignment.CenterVertically) {
        val bmp = remember(art) { art?.asImageBitmap() }
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RethroneShapes.small)
                .background(colors.text.copy(alpha = 0.10f))
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
                color = colors.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (content.artist.isNotBlank()) {
                Text(
                    text = content.artist,
                    color = colors.subText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
    content.progress?.let { progress ->
        MediaSeekBar(
            progress = progress,
            isPlaying = content.isPlaying,
            colors = colors,
            accent = albumAccent,
            onSeekTo = onSeekTo
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TransportButton(Icons.Rounded.SkipPrevious, colors, onPrev)
        TransportButton(
            if (content.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            colors,
            onPlayPause,
            emphasized = true,
            accent = albumAccent
        )
        TransportButton(Icons.Rounded.SkipNext, colors, onNext)
    }
}

/**
 * Seek-Leiste der Media-Karte (Island Media v2.4). Die angezeigte Position wird
 * **clientseitig** aus dem letzten PlaybackState hochgerechnet
 * ([extrapolateMediaPositionMs]) – getickt wird nur, solange die Karte offen ist
 * und Wiedergabe läuft; die Session selbst wird nicht gepollt. Während des
 * Ziehens gewinnt die Finger-Position; losgelassen wird via [onSeekTo] gesprungen.
 */
@Composable
private fun MediaSeekBar(
    progress: MediaProgress,
    isPlaying: Boolean,
    colors: IslandCardColors,
    accent: Color,
    onSeekTo: (Long) -> Unit
) {
    // Tick nur bei offener Karte + laufender Wiedergabe (Composable verlässt die
    // Composition beim Schließen → Coroutine endet automatisch).
    var nowElapsedMs by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(progress, isPlaying) {
        nowElapsedMs = SystemClock.elapsedRealtime()
        while (isPlaying) {
            delay(500L)
            nowElapsedMs = SystemClock.elapsedRealtime()
        }
    }
    val livePositionMs = extrapolateMediaPositionMs(progress, isPlaying, nowElapsedMs)

    // Während des Ziehens zeigt die Leiste die Finger-Position statt der Live-Zeit.
    var dragPositionMs by remember { mutableStateOf<Long?>(null) }
    val shownMs = dragPositionMs ?: livePositionMs

    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Slider(
            value = shownMs.toFloat(),
            onValueChange = { dragPositionMs = it.toLong() },
            onValueChangeFinished = {
                dragPositionMs?.let(onSeekTo)
                dragPositionMs = null
            },
            valueRange = 0f..progress.durationMs.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = colors.text.copy(alpha = 0.15f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatMediaTime(shownMs),
                color = colors.subText,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = formatMediaTime(progress.durationMs),
                color = colors.subText,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/** Dominanter Farbton eines Covers via 1×1-Downscale (billig, ohne Palette-Abhängigkeit). */
private fun android.graphics.Bitmap.dominantColor(): Color = try {
    val pixel = android.graphics.Bitmap.createScaledBitmap(this, 1, 1, true).getPixel(0, 0)
    Color(pixel)
} catch (_: Exception) {
    Color.Unspecified
}

/**
 * Macht den Album-Akzent auf der Kartenfläche lesbar: zu dunkle/zu helle Töne werden
 * Richtung Kartentextfarbe gemischt; `Unspecified` fällt auf den Theme-Akzent zurück.
 */
private fun Color.readableOn(colors: IslandCardColors): Color {
    if (this == Color.Unspecified) return colors.accent
    val cardIsDark = colors.surface.luminance() <= 0.5f
    val tooDim = cardIsDark && luminance() < 0.25f
    val tooBright = !cardIsDark && luminance() > 0.75f
    return if (tooDim || tooBright) lerp(this, colors.text, 0.5f) else this
}

/**
 * Transport-/Steuer-Button. [emphasized] hebt die primäre Aktion (Play/Pause) mit einem
 * dezenten Akzent-Hintergrundkreis hervor (Material-3-Expressive), wie der Icon-Kreis im
 * [InfoDialog].
 */
@Composable
private fun TransportButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colors: IslandCardColors,
    onClick: () -> Unit,
    emphasized: Boolean = false,
    accent: Color = colors.accent
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .then(if (emphasized) Modifier.background(accent.copy(alpha = 0.16f)) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (emphasized) accent else colors.text,
            modifier = Modifier.size(32.dp)
        )
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
private fun TimerCard(
    content: IslandContent.Timer,
    colors: IslandCardColors,
    onControl: (NotificationAction) -> Unit
) {
    Text(
        text = content.displayMs?.let { formatRemaining(it) } ?: "--:--",
        color = colors.text,
        style = MaterialTheme.typography.displaySmall,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
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
                if (isResumeActionTitle(toggle.title)) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                colors,
                { onControl(toggle) },
                emphasized = true
            )
        }
    }
    if (others.isNotEmpty()) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            others.forEach { action -> ActionChip(action.title, colors) { onControl(action) } }
        }
    }
}

@Composable
private fun SimpleCard(primary: String, secondary: String, colors: IslandCardColors) {
    Text(
        primary,
        color = colors.text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
    )
    if (secondary.isNotBlank()) {
        Text(
            text = secondary,
            color = colors.subText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
