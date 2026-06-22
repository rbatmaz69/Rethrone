package com.example.androidlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.data.IslandContent
import com.example.androidlauncher.data.NotificationAction

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
    onMediaPlayPause: () -> Unit = {},
    onMediaNext: () -> Unit = {},
    onMediaPrev: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.TopCenter
    ) {
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
                .padding(20.dp)
        ) {
            if (allContents.size > 1) {
                ActivitySwitcher(allContents, content, onSwitch)
            }
            when (content) {
                is IslandContent.Notification -> NotificationCard(content, onAction)
                is IslandContent.Media -> MediaCard(content, onMediaPlayPause, onMediaNext, onMediaPrev)
                is IslandContent.Timer -> SimpleCard(
                    content.remainingMs?.let { formatRemaining(it) } ?: content.label.ifBlank { "Timer" },
                    if (content.remainingMs != null) content.label else ""
                )
                is IslandContent.Battery -> SimpleCard(
                    if (content.charging) "Wird geladen" else "Netzteil getrennt",
                    "${content.level}%"
                )
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

private fun activityChipLabel(content: IslandContent): String = when (content) {
    is IslandContent.Media -> "Medien"
    is IslandContent.Timer -> content.label.ifBlank { "Timer" }
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
        content.art?.let { art ->
            Image(
                bitmap = art.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(14.dp))
        }
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
