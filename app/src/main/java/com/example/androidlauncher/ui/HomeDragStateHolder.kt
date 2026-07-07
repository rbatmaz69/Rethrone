package com.example.androidlauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.androidlauncher.data.HomeLayout
import com.example.androidlauncher.data.HostedWidget
import com.example.androidlauncher.data.WidgetSizeDp

/**
 * Bündelt die transienten Zustände des Edit-Modus auf dem Startbildschirm
 * (A6-Split aus dem HomeScreen-Composable): Live-Offsets der verschiebbaren
 * Elemente, Kollisions-/Haptik-Buchführung und die aktuelle Auswahl. Bewusst
 * `remember`-basiert – der persistierte Stand lebt in `HomeLayoutSettings`,
 * dieser Holder hält nur die Live-Vorschau während des Bearbeitens.
 * Geometrie-Entscheidungen bleiben framework-frei in `HomeGeometry.kt`.
 *
 * Neben den vier eingebauten Elementen verwaltet der Holder auch die Offsets
 * gehosteter System-Widgets (B1) – gleiche Drag-Mechanik, dynamische Targets.
 */
@Stable
internal class HomeDragStateHolder(
    initialLayout: HomeLayout,
    initialWidgets: List<HostedWidget> = emptyList(),
) {

    /** Live-Offsets je Element; im Edit-Modus folgt das Element dem Finger. */
    val offsets = mutableStateMapOf(
        HomeEditTarget.Clock to initialLayout.clock,
        HomeEditTarget.Date to initialLayout.date,
        HomeEditTarget.Weather to initialLayout.weather,
        HomeEditTarget.Favorites to initialLayout.favorites,
    ).apply {
        initialWidgets.forEach {
            put(HomeEditTarget.Widget(it.appWidgetId), Offset(it.offsetX, it.offsetY))
        }
    }

    /** Live-Größen der gehosteten Widgets während des Resize-Drags (B1-PR4). */
    val widgetSizes = mutableStateMapOf<Int, WidgetSizeDp>().apply {
        initialWidgets.forEach { put(it.appWidgetId, WidgetSizeDp(it.widthDp, it.heightDp)) }
    }

    /** Neutralposition (Bounds ohne Offset) je Element – Basis der Kollisionsprüfung. */
    val neutralBounds = mutableStateMapOf<HomeEditTarget, Rect>()

    /** Zeitstempel der letzten Blockier-Haptik je Element (Drossel gegen Dauervibration). */
    val lastBlockedHapticMs = mutableStateMapOf<HomeEditTarget, Long>()

    // Bounds der festen UI-Elemente, mit denen verschobene Elemente kollidieren können.
    var searchButtonBounds by mutableStateOf<Rect?>(null)
    var settingsButtonBounds by mutableStateOf<Rect?>(null)
    var editControlsBounds by mutableStateOf<Rect?>(null)

    var collisionHapticWasTriggered by mutableStateOf(false)

    /** Aktuell ausgewähltes Element im Edit-Modus (oder `null`). */
    var selectedEditTarget by mutableStateOf<HomeEditTarget?>(null)

    /** `true`, wenn der Nutzer die Auswahl explizit angetippt hat (nicht nur Auto-Auswahl). */
    var isEditTargetUserPinned by mutableStateOf(false)

    /**
     * Zum Entfernen vorgemerkte Widgets (U3): das X-Badge markiert nur, erst der
     * Speichern-Knopf loescht wirklich – Abbrechen (via [seedFrom]) und die
     * Rueckgaengig-Pille stellen markierte Widgets verlustfrei wieder her.
     */
    val pendingWidgetRemovals = mutableStateListOf<Int>()

    /** Merkt ein Widget zum Entfernen vor und hebt ggf. dessen Auswahl auf. */
    fun markWidgetForRemoval(appWidgetId: Int) {
        if (appWidgetId !in pendingWidgetRemovals) pendingWidgetRemovals.add(appWidgetId)
        if (selectedEditTarget == HomeEditTarget.Widget(appWidgetId)) {
            selectedEditTarget = null
            isEditTargetUserPinned = false
        }
    }

    /** Nimmt die Entfernen-Vormerkung zurueck (Rueckgaengig-Pille). */
    fun undoWidgetRemoval(appWidgetId: Int) {
        pendingWidgetRemovals.remove(appWidgetId)
    }

    fun isWidgetPendingRemoval(appWidgetId: Int): Boolean =
        appWidgetId in pendingWidgetRemovals

    /** Snapshot der Vormerkungen fuer den Speichern-Commit. */
    fun pendingRemovalsSnapshot(): Set<Int> = pendingWidgetRemovals.toSet()

    /**
     * Seedet die Live-Offsets aus dem persistierten Stand – beim Betreten des
     * Edit-Modus 1:1 übernehmen, Abbrechen revertiert damit automatisch.
     * Widget-Einträge werden synchronisiert: entfernte Widgets verschwinden
     * mitsamt Buchführung (und ggf. Auswahl) aus dem Holder.
     */
    fun seedFrom(layout: HomeLayout, widgets: List<HostedWidget> = emptyList()) {
        pendingWidgetRemovals.clear()
        offsets[HomeEditTarget.Clock] = layout.clock
        offsets[HomeEditTarget.Date] = layout.date
        offsets[HomeEditTarget.Weather] = layout.weather
        offsets[HomeEditTarget.Favorites] = layout.favorites

        val widgetTargets = widgets.map { HomeEditTarget.Widget(it.appWidgetId) }.toSet()
        offsets.keys.filterIsInstance<HomeEditTarget.Widget>()
            .filterNot { it in widgetTargets }
            .forEach { stale ->
                offsets.remove(stale)
                neutralBounds.remove(stale)
                lastBlockedHapticMs.remove(stale)
                widgetSizes.remove(stale.appWidgetId)
                if (selectedEditTarget == stale) {
                    selectedEditTarget = null
                    isEditTargetUserPinned = false
                }
            }
        widgets.forEach {
            offsets[HomeEditTarget.Widget(it.appWidgetId)] = Offset(it.offsetX, it.offsetY)
            widgetSizes[it.appWidgetId] = WidgetSizeDp(it.widthDp, it.heightDp)
        }
    }

    /** Aktuelle Live-Offsets als persistierbares [HomeLayout] (Speichern-Knopf). */
    fun toHomeLayout(): HomeLayout = HomeLayout(
        clock = offsets[HomeEditTarget.Clock] ?: Offset.Zero,
        date = offsets[HomeEditTarget.Date] ?: Offset.Zero,
        weather = offsets[HomeEditTarget.Weather] ?: Offset.Zero,
        favorites = offsets[HomeEditTarget.Favorites] ?: Offset.Zero,
    )

    /**
     * Aktuelle Live-Offsets der gehosteten Widgets, keyed per appWidgetId
     * (Speichern-Knopf). Zum Entfernen vorgemerkte Widgets sind ausgenommen –
     * fuer sie wird im selben Write keine Platzierung mehr persistiert.
     */
    fun toWidgetOffsets(): Map<Int, Offset> = offsets.entries
        .mapNotNull { (target, offset) ->
            (target as? HomeEditTarget.Widget)
                ?.takeUnless { it.appWidgetId in pendingWidgetRemovals }
                ?.let { it.appWidgetId to offset }
        }
        .toMap()

    /** Aktuelle Live-Größen der gehosteten Widgets (Speichern-Knopf, B1-PR4); ohne Vormerkungen. */
    fun toWidgetSizes(): Map<Int, WidgetSizeDp> = widgetSizes
        .filterKeys { it !in pendingWidgetRemovals }
}

/** Merkt sich den Holder über Recompositions hinweg (nicht über Config-Wechsel). */
@Composable
internal fun rememberHomeDragState(
    initialLayout: HomeLayout,
    initialWidgets: List<HostedWidget> = emptyList(),
): HomeDragStateHolder = remember { HomeDragStateHolder(initialLayout, initialWidgets) }
