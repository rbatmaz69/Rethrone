package com.example.androidlauncher.ui.theme

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Zentrale, expressive Haptik. Alle Methoden respektieren [LocalHapticFeedbackEnabled]
 * und nutzen die stabilen Plattform-Konstanten (mit API-Guards), damit das Feedback
 * auf allen unterstützten Geräten konsistent ist.
 *
 * Verwendung: `val haptics = rememberAppHaptics()` → `haptics.tap()` / `select()` / `toggle(on)` / `confirm()`.
 */
class AppHaptics(
    private val view: View,
    private val enabled: Boolean
) {
    private fun perform(constant: Int) {
        if (!enabled) return
        view.performHapticFeedback(constant, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
    }

    /** Leichter Tap (Buttons, Kacheln, allgemeine Klicks). */
    fun tap() = perform(HapticFeedbackConstants.KEYBOARD_TAP)

    /** Auswahlwechsel (Theme-/Stil-/Optionswahl). */
    fun select() = perform(HapticFeedbackConstants.CLOCK_TICK)

    /** Schalter umlegen – nutzt die dedizierten Toggle-Effekte ab Android 14. */
    fun toggle(on: Boolean) = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (on) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
    )

    /** Bestätigender Impuls (Menü öffnen, abschließende Aktion). */
    fun confirm() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.KEYBOARD_TAP
    )
}

/** Liefert ein [AppHaptics], das automatisch die Haptik-Einstellung des Nutzers berücksichtigt. */
@Composable
fun rememberAppHaptics(): AppHaptics {
    val view = LocalView.current
    val enabled = LocalHapticFeedbackEnabled.current
    return remember(view, enabled) { AppHaptics(view, enabled) }
}
