package com.example.androidlauncher.ui

import android.content.Intent
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.ui.theme.LocalAppFont
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalFontSize
import com.example.androidlauncher.ui.theme.LocalFontWeight
import com.example.androidlauncher.ui.theme.LocalHapticFeedbackEnabled
import kotlin.math.roundToInt

/** Buchstabenschlüssel einer App fürs Gruppieren (#, A–Z). */
private fun scrubberLetterKey(label: String): String {
    val c = label.trim().firstOrNull()?.uppercaseChar()
    return if (c != null && c in 'A'..'Z') c.toString() else "#"
}

/** Gruppiert Apps nach Anfangsbuchstabe, Reihenfolge: '#' zuerst, dann A–Z (nur vorhandene). */
private fun groupAppsForScrubber(apps: List<AppInfo>): LinkedHashMap<String, List<AppInfo>> {
    val grouped = apps.groupBy { scrubberLetterKey(it.label) }
    val ordered = LinkedHashMap<String, List<AppInfo>>()
    grouped["#"]?.let { ordered["#"] = it }
    ('A'..'Z').forEach { l -> grouped[l.toString()]?.let { ordered[l.toString()] = it } }
    return ordered
}

/**
 * Niagara-artige A–Z-Schnellleiste am rechten Rand der Startseite.
 *
 * Die Leiste ist im Ruhezustand unsichtbar. Berührt man den rechten Bildschirmrand, erscheint sie und
 * der aktuelle Buchstabe wird durch die Finger-Y-Position bestimmt; die Apps dieses Buchstabens werden
 * als schwebende Liste links daneben gezeigt. Gleitet der Finger nach links in die Liste, wählt die
 * Finger-Y die App aus. Beim Loslassen über einer App wird diese gestartet.
 */
@Composable
fun HomeAppScrubber(
    apps: List<AppInfo>,
    onLaunchApp: (String, Intent, Rect?) -> Unit,
    returnIconPackage: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val colorTheme = LocalColorTheme.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val fontSize = LocalFontSize.current
    val fontWeight = LocalFontWeight.current
    val appFont = LocalAppFont.current
    val hapticEnabled = LocalHapticFeedbackEnabled.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val menuSurfaceColor = remember(colorTheme, isDarkTextEnabled) {
        colorTheme.menuSurfaceColor(isDarkTextEnabled)
    }
    val highlightColor = remember(colorTheme, isDarkTextEnabled) {
        colorTheme.highlightColor(isDarkTextEnabled)
    }

    // apps ist eine in-place mutierte SnapshotStateList – auf den Inhalt keyen (Kopie),
    // damit Deinstallationen sofort durchschlagen (sonst bliebe die Gruppierung stale).
    val appsSnapshot = apps.toList()
    val ordered = remember(appsSnapshot) { groupAppsForScrubber(appsSnapshot) }
    val letters = remember(ordered) { ordered.keys.toList() }

    // Geometrie (px)
    val stripWidthPx = with(density) { 32.dp.toPx() }
    val rowHeightPx = with(density) { 52.dp.toPx() }
    val listWidthPx = with(density) { 232.dp.toPx() }
    val gapPx = with(density) { 8.dp.toPx() }

    var active by remember { mutableStateOf(false) }
    var stripBounds by remember { mutableStateOf(Rect.Zero) }
    var rootHeightPx by remember { mutableStateOf(0f) }
    var currentLetterIndex by remember { mutableStateOf(0) }
    var hoveredIndex by remember { mutableStateOf(-1) }
    var inListMode by remember { mutableStateOf(false) }
    var listTopYPx by remember { mutableStateOf(0f) }

    val barAlpha by animateFloatAsState(targetValue = if (active) 1f else 0f, label = "ScrubberBarAlpha")

    val currentApps: List<AppInfo> = if (letters.isEmpty()) emptyList()
        else ordered[letters[currentLetterIndex.coerceIn(0, letters.size - 1)]] ?: emptyList()

    fun tick() {
        if (hapticEnabled) {
            @Suppress("DEPRECATION")
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_app_scrubber")
            .onGloballyPositioned { rootHeightPx = it.size.height.toFloat() }
    ) {
        // Schwebende App-Liste (nur sichtbar/aktiv während der Geste)
        if (active && currentApps.isNotEmpty()) {
            val listLeftPx = (stripBounds.left - gapPx - listWidthPx).coerceAtLeast(0f)
            Box(
                modifier = Modifier
                    .offset { IntOffset(listLeftPx.roundToInt(), listTopYPx.roundToInt()) }
                    .width(232.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(menuSurfaceColor.copy(alpha = 0.96f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    currentApps.forEachIndexed { index, app ->
                        val isHovered = inListMode && index == hoveredIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .background(if (isHovered) highlightColor.copy(alpha = 0.30f) else Color.Transparent)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconView(app, customIcons = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            androidx.compose.material3.Text(
                                text = app.label,
                                fontSize = 15.sp * fontSize.scale,
                                fontFamily = appFont.fontFamily,
                                fontWeight = fontWeight.weight,
                                color = mainTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Unsichtbarer Touch-Streifen + sichtbare A–Z-Leiste am rechten Rand
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(32.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 24.dp, bottom = 140.dp)
                .onGloballyPositioned { stripBounds = it.boundsInRoot() }
                .pointerInput(ordered) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (letters.isEmpty()) continue
                            down.consume()
                            active = true
                            inListMode = false

                            fun handle(localPos: Offset) {
                                val sb = stripBounds
                                if (sb.height <= 0f) return
                                val rootX = sb.left + localPos.x
                                val rootY = sb.top + localPos.y
                                if (rootX >= sb.left) {
                                    // Finger über der Leiste → Buchstabe wählen
                                    inListMode = false
                                    val frac = ((rootY - sb.top) / sb.height).coerceIn(0f, 0.9999f)
                                    val idx = (frac * letters.size).toInt().coerceIn(0, letters.size - 1)
                                    if (idx != currentLetterIndex) { currentLetterIndex = idx; tick() }
                                    hoveredIndex = -1
                                    val count = (ordered[letters[idx]] ?: emptyList()).size
                                    val listH = count * rowHeightPx
                                    val maxTop = (rootHeightPx - listH).coerceAtLeast(0f)
                                    listTopYPx = (rootY - listH / 2f).coerceIn(0f, maxTop)
                                } else {
                                    // Finger links der Leiste → App in der Liste wählen
                                    if (!inListMode) inListMode = true
                                    val count = (ordered[letters[currentLetterIndex]] ?: emptyList()).size
                                    val rel = rootY - listTopYPx
                                    val idx = (rel / rowHeightPx).toInt()
                                    val newHover = if (idx in 0 until count) idx else -1
                                    if (newHover != hoveredIndex) { hoveredIndex = newHover; if (newHover >= 0) tick() }
                                }
                            }

                            handle(down.position)

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    // Release → ggf. App starten
                                    if (inListMode && hoveredIndex >= 0) {
                                        val group = ordered[letters[currentLetterIndex]] ?: emptyList()
                                        group.getOrNull(hoveredIndex)?.let { app ->
                                            context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { intent ->
                                                val sb = stripBounds
                                                val rowTop = listTopYPx + hoveredIndex * rowHeightPx
                                                val listLeft = (sb.left - gapPx - listWidthPx).coerceAtLeast(0f)
                                                val bounds = Rect(listLeft, rowTop, sb.left - gapPx, rowTop + rowHeightPx)
                                                onLaunchApp(app.packageName, intent, bounds)
                                            }
                                        }
                                    }
                                    break
                                }
                                handle(change.position)
                                change.consume()
                            }

                            active = false
                            inListMode = false
                            hoveredIndex = -1
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .alpha(barAlpha),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                letters.forEachIndexed { index, letter ->
                    val isCurrent = active && index == currentLetterIndex
                    androidx.compose.material3.Text(
                        text = letter,
                        fontSize = if (isCurrent) 14.sp else 11.sp,
                        fontFamily = appFont.fontFamily,
                        fontWeight = if (isCurrent) FontWeight.Bold else fontWeight.weight,
                        color = if (isCurrent) mainTextColor else mainTextColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
