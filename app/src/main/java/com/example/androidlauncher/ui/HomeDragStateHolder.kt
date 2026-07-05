package com.example.androidlauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.androidlauncher.data.HomeLayout

/**
 * Bündelt die transienten Zustände des Edit-Modus auf dem Startbildschirm
 * (A6-Split aus dem HomeScreen-Composable): Live-Offsets der verschiebbaren
 * Elemente, Kollisions-/Haptik-Buchführung und die aktuelle Auswahl. Bewusst
 * `remember`-basiert – der persistierte Stand lebt in `HomeLayoutSettings`,
 * dieser Holder hält nur die Live-Vorschau während des Bearbeitens.
 * Geometrie-Entscheidungen bleiben framework-frei in `HomeGeometry.kt`.
 */
@Stable
internal class HomeDragStateHolder(initialLayout: HomeLayout) {

    /** Live-Offsets je Element; im Edit-Modus folgt das Element dem Finger. */
    val offsets = mutableStateMapOf(
        HomeEditTarget.CLOCK to initialLayout.clock,
        HomeEditTarget.DATE to initialLayout.date,
        HomeEditTarget.WEATHER to initialLayout.weather,
        HomeEditTarget.FAVORITES to initialLayout.favorites,
    )

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
     * Seedet die Live-Offsets aus dem persistierten Layout – beim Betreten des
     * Edit-Modus 1:1 übernehmen, Abbrechen revertiert damit automatisch.
     */
    fun seedFrom(layout: HomeLayout) {
        offsets[HomeEditTarget.CLOCK] = layout.clock
        offsets[HomeEditTarget.DATE] = layout.date
        offsets[HomeEditTarget.WEATHER] = layout.weather
        offsets[HomeEditTarget.FAVORITES] = layout.favorites
    }

    /** Aktuelle Live-Offsets als persistierbares [HomeLayout] (Speichern-Knopf). */
    fun toHomeLayout(): HomeLayout = HomeLayout(
        clock = offsets[HomeEditTarget.CLOCK] ?: Offset.Zero,
        date = offsets[HomeEditTarget.DATE] ?: Offset.Zero,
        weather = offsets[HomeEditTarget.WEATHER] ?: Offset.Zero,
        favorites = offsets[HomeEditTarget.FAVORITES] ?: Offset.Zero,
    )
}

/** Merkt sich den Holder über Recompositions hinweg (nicht über Config-Wechsel). */
@Composable
internal fun rememberHomeDragState(initialLayout: HomeLayout): HomeDragStateHolder =
    remember { HomeDragStateHolder(initialLayout) }
