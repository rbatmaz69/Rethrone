package com.example.androidlauncher.ui

import android.content.Intent
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.androidlauncher.data.IconManager
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
    // Custom-Icons genau einmal pro Leiste sammeln (nicht pro Zeile in AppIconView), sonst würde beim
    // schnellen Scrubben pro Buchstabenwechsel je Zeile ein neuer IconManager + DataStore-Flow aufgesetzt.
    val iconManager = remember { IconManager(context) }
    val customIcons by iconManager.customIcons.collectAsState(initial = emptyMap())
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    // Direkt berechnet (nicht via remember(colorTheme,…)): bei CUSTOM/DYNAMIC ändern sich
    // die Farben über den Holder, ohne dass sich der Enum-Key ändert – sonst bliebe die
    // Startseiten-Liste stale, bis ein Layout-Wechsel sie neu zusammensetzt.
    val menuSurfaceColor = colorTheme.menuSurfaceColor(isDarkTextEnabled)
    val highlightColor = colorTheme.highlightColor(isDarkTextEnabled)

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
    // Schrittweite (px pro Buchstabe) für relatives Scrubbing – kleinere Werte = empfindlicher.
    val minStepPx = with(density) { 10.dp.toPx() }
    val maxStepPx = with(density) { 28.dp.toPx() }
    // App-Auswahl per Bewegungs-Absicht statt fester X-Distanz: eine bewusst nach links gerichtete,
    // eher waagerechte Bewegung wechselt schnell in die Auswahl; die senkrechte Bogenbewegung beim
    // Scrubben nicht. Werte sind UX-Tuning (kleiner = reaktiver).
    val commitEnterPx = with(density) { 28.dp.toPx() } // Links-Bewegung bis App-Auswahl aktiv
    val scrubFreezePx = with(density) { 12.dp.toPx() } // ab hier Buchstabe einfrieren (kein Wechsel mehr)
    val exitListThresholdPx = with(density) { 32.dp.toPx() } // wieder so nah an der Leiste → Scrubben

    var active by remember { mutableStateOf(false) }
    var stripBounds by remember { mutableStateOf(Rect.Zero) }
    var rootHeightPx by remember { mutableStateOf(0f) }
    var currentLetterIndex by remember { mutableStateOf(0) }
    var hoveredIndex by remember { mutableStateOf(-1) }
    var inListMode by remember { mutableStateOf(false) }
    var listTopYPx by remember { mutableStateOf(0f) }
    // Relatives Scrubbing: letzte Finger-Y + aufaddierte Bewegung; thumbY für die Anzeige.
    var lastScrubY by remember { mutableStateOf(0f) }
    var accumPx by remember { mutableStateOf(0f) }
    var thumbY by remember { mutableStateOf(0f) }
    // Bewegungs-Absicht nach links (px) + letzte Finger-X, um Scrubben von „in die Liste ziehen" zu trennen.
    var lastX by remember { mutableStateOf(0f) }
    var commitAccum by remember { mutableStateOf(0f) }
    // -1 = oberste Grenze signalisiert, 1 = unterste, 0 = keine / wieder freigegeben
    var edgeSignaled by remember { mutableStateOf(0) }

    val barAlpha by animateFloatAsState(targetValue = if (active) 1f else 0f, label = "ScrubberBarAlpha")

    val currentApps: List<AppInfo> = if (letters.isEmpty()) {
        emptyList()
    } else {
        ordered[letters[currentLetterIndex.coerceIn(0, letters.size - 1)]] ?: emptyList()
    }

    fun tick() {
        if (hapticEnabled) {
            @Suppress("DEPRECATION")
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    // Deutlich anderes Feedback an der obersten/untersten Listengrenze („geht nicht weiter").
    fun edgeTick() {
        if (!hapticEnabled) return
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT // API 30+: „geht nicht weiter"
        } else {
            HapticFeedbackConstants.LONG_PRESS // Fallback (kräftiger als KEYBOARD_TAP)
        }
        @Suppress("DEPRECATION")
        view.performHapticFeedback(constant)
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
                    .clip(RoundedCornerShape(24.dp))
                    .background(menuSurfaceColor.copy(alpha = 0.96f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    currentApps.forEachIndexed { index, app ->
                        val isHovered = inListMode && index == hoveredIndex
                        // Animierte „Auswahl-Pille": gefüllt, leicht vergrößert, fettes Label – hebt klar
                        // hervor, welche App beim Loslassen gestartet wird.
                        val hl by animateFloatAsState(
                            targetValue = if (isHovered) 1f else 0f,
                            label = "ScrubberHover"
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (isHovered) 1.06f else 1f,
                            label = "ScrubberHoverScale"
                        )
                        // Äußere Box bleibt 52 dp hoch – die Treffer-Erkennung rechnet mit rowHeightPx.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(highlightColor.copy(alpha = 0.85f * hl))
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIconView(app, customIcons = customIcons)
                                Spacer(modifier = Modifier.width(12.dp))
                                androidx.compose.material3.Text(
                                    text = app.label,
                                    fontSize = 15.sp * fontSize.scale,
                                    fontFamily = appFont.fontFamily,
                                    fontWeight = if (isHovered) FontWeight.Bold else fontWeight.weight,
                                    color = mainTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                            // Relatives Scrubbing: ab Druckpunkt zählen, aktueller Buchstabe bleibt.
                            lastScrubY = stripBounds.top + down.position.y
                            lastX = stripBounds.left + down.position.x
                            accumPx = 0f
                            commitAccum = 0f
                            edgeSignaled = 0
                            thumbY = stripBounds.top + down.position.y

                            fun handle(localPos: Offset) {
                                val sb = stripBounds
                                if (sb.height <= 0f) return
                                val rootX = sb.left + localPos.x
                                val rootY = sb.top + localPos.y
                                val dx = rootX - lastX
                                val dy = rootY - lastScrubY
                                lastX = rootX
                                lastScrubY = rootY
                                thumbY = rootY

                                // Bewegungs-Absicht erkennen: eine nach links gerichtete, eher waagerechte
                                // Bewegung ist der Wunsch nach App-Auswahl → baut commitAccum auf. Senkrechtes
                                // Scrubben oder Bewegung nach rechts baut ihn wieder ab. Dadurch unterbricht die
                                // (senkrechte) Bogenbewegung das Scrubbing nicht, ein bewusster Links-Wisch
                                // wechselt aber sofort – ohne vorher den Buchstaben zu ändern.
                                if (dx < 0f && -dx >= kotlin.math.abs(dy)) {
                                    commitAccum = (commitAccum - dx).coerceAtMost(commitEnterPx)
                                } else {
                                    commitAccum = (commitAccum - kotlin.math.abs(dy) - dx.coerceAtLeast(0f))
                                        .coerceAtLeast(0f)
                                }

                                // Eintritt in die App-Auswahl über die Absicht, Austritt über die Position.
                                if (!inListMode && commitAccum >= commitEnterPx) {
                                    inListMode = true
                                } else if (inListMode && rootX >= sb.left - exitListThresholdPx) {
                                    inListMode = false
                                    commitAccum = 0f
                                    accumPx = 0f
                                }

                                if (!inListMode && commitAccum < scrubFreezePx) {
                                    // Buchstaben-Scrubbing (kein Links-Commit aktiv).
                                    accumPx += dy
                                    val denom = (letters.size - 1).coerceAtLeast(1)
                                    val stepPx = (0.32f * rootHeightPx / denom).coerceIn(minStepPx, maxStepPx)
                                    var idx = currentLetterIndex
                                    while (accumPx >= stepPx) {
                                        idx++
                                        accumPx -= stepPx
                                    }
                                    while (accumPx <= -stepPx) {
                                        idx--
                                        accumPx += stepPx
                                    }
                                    val last = letters.size - 1
                                    val clamped = idx.coerceIn(0, last)
                                    if (clamped != currentLetterIndex) {
                                        currentLetterIndex = clamped
                                        tick()
                                    }
                                    when {
                                        idx < 0 && currentLetterIndex == 0 ->
                                            if (edgeSignaled != -1) {
                                                edgeTick()
                                                edgeSignaled = -1
                                            }
                                        idx > last && currentLetterIndex == last ->
                                            if (edgeSignaled != 1) {
                                                edgeTick()
                                                edgeSignaled = 1
                                            }
                                        else -> edgeSignaled = 0 // weg von der Grenze → wieder scharf
                                    }
                                    hoveredIndex = -1
                                    val count = (ordered[letters[currentLetterIndex]] ?: emptyList()).size
                                    val listH = count * rowHeightPx
                                    val maxTop = (rootHeightPx - listH).coerceAtLeast(0f)
                                    listTopYPx = (rootY - listH / 2f).coerceIn(0f, maxTop)
                                } else if (inListMode) {
                                    // App-Auswahl-Modus → App in der Liste über die Y-Position wählen
                                    val count = (ordered[letters[currentLetterIndex]] ?: emptyList()).size
                                    val rel = rootY - listTopYPx
                                    val idx = (rel / rowHeightPx).toInt()
                                    val newHover = if (idx in 0 until count) idx else -1
                                    if (newHover != hoveredIndex) {
                                        hoveredIndex = newHover
                                        if (newHover >= 0) tick()
                                    }
                                } else {
                                    // Links-Commit im Gange: Buchstabe eingefroren, noch keine App gewählt.
                                    hoveredIndex = -1
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
                                            context.packageManager.getLaunchIntentForPackage(
                                                app.packageName
                                            )?.let { intent ->
                                                val sb = stripBounds
                                                val rowTop = listTopYPx + hoveredIndex * rowHeightPx
                                                val listLeft = (sb.left - gapPx - listWidthPx).coerceAtLeast(0f)
                                                val bounds =
                                                    Rect(listLeft, rowTop, sb.left - gapPx, rowTop + rowHeightPx)
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
        ) { /* unsichtbarer Touch-Streifen – das Feedback liefert die Buchstaben-Walze am Daumen */ }

        // Daumen-folgende Buchstaben-Walze: aktueller Buchstabe groß, Nachbarn blass.
        if (active && letters.isNotEmpty()) {
            val idx = currentLetterIndex.coerceIn(0, letters.size - 1)
            val wheelWidthPx = with(density) { 96.dp.toPx() }
            val wheelHeightPx = with(density) { 168.dp.toPx() }
            val wheelXPx = (stripBounds.left - wheelWidthPx).coerceAtLeast(0f)
            Box(
                modifier = Modifier
                    .offset { IntOffset(wheelXPx.roundToInt(), (thumbY - wheelHeightPx / 2f).roundToInt()) }
                    .width(96.dp)
                    .height(168.dp)
                    .alpha(barAlpha),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (d in -2..2) {
                        val li = idx + d
                        if (li in letters.indices) {
                            if (d == 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(menuSurfaceColor.copy(alpha = 0.96f))
                                        .padding(horizontal = 18.dp, vertical = 6.dp)
                                ) {
                                    androidx.compose.material3.Text(
                                        text = letters[li],
                                        fontSize = 30.sp,
                                        fontFamily = appFont.fontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = mainTextColor,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                androidx.compose.material3.Text(
                                    text = letters[li],
                                    fontSize = 14.sp,
                                    fontFamily = appFont.fontFamily,
                                    fontWeight = fontWeight.weight,
                                    color = mainTextColor.copy(alpha = if (kotlin.math.abs(d) == 1) 0.5f else 0.25f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
