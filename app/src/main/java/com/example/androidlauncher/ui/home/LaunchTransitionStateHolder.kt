package com.example.androidlauncher.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.androidlauncher.ReturnAnimation

/**
 * Bündelt die **transienten** Zustände der App-Start- und Rückkehr-Animationen
 * (A2-Split aus der MainActivity). Bewusst `remember`-basiert und NICHT im
 * ViewModel: diese Zustände beschreiben laufende Übergänge und sollen einen
 * Konfigurationswechsel gerade nicht überleben. Die Entscheidungslogik (ob eine
 * Rückkehr-Animation läuft) bleibt in `ReturnAnimationGate`/`ReturnResumeGuard`;
 * dieser Holder besitzt nur die veränderlichen Felder.
 */
@Stable
class LaunchTransitionStateHolder(defaultLaunchBackground: Color) {

    // ── Rückkehr-Animation (App → Launcher) ──────────────────────────
    var pendingReturnAnimation by mutableStateOf<ReturnAnimation?>(null)
    var pendingReturnAnimationStartedWallClockMs by mutableStateOf(0L)
    var activeReturnAnimation by mutableStateOf<ReturnAnimation?>(null)
    var returnIconPackage by mutableStateOf<String?>(null)

    // ── Bounce-Trigger ───────────────────────────────────────────────
    var searchButtonBounceToken by mutableStateOf(0)

    // Eigener Trigger für den Rückkehr-Bounce, entkoppelt von
    // activeReturnAnimation. Letzteres wird vom Schließen-Overlay nach
    // ~260ms auf null gesetzt; würde der Bounce darauf gekeyt sein, würde
    // die delay(270)-Coroutine ~10ms vor dem Feuern abgebrochen -> der
    // Bounce käme nur „manchmal". Das Token steigt nur bei einer neuen
    // Aktivierung, sodass die Coroutine zuverlässig zu Ende läuft.
    var returnBounceToken by mutableStateOf(0)
    var returnBounceTargetPackage by mutableStateOf<String?>(null)

    // Beim Öffnen poppt das gedrückte Icon kurz, bevor das Panel es
    // verdeckt – symmetrisch zum Rückkehr-Bounce.
    var launchIconPackage by mutableStateOf<String?>(null)

    // ── Start-Animation (Launcher → App) ─────────────────────────────
    var isSearchLaunching by mutableStateOf(false)
    var isAppLaunchAnimating by mutableStateOf(false)
    var activeLaunchBounds by mutableStateOf<Rect?>(null)
    var activeLaunchBackground by mutableStateOf(defaultLaunchBackground)
    var activeLaunchBackgroundBrush by mutableStateOf<Brush?>(null)
}

/** Merkt sich den Holder über Recompositions hinweg (nicht über Config-Wechsel). */
@Composable
fun rememberLaunchTransitionState(defaultLaunchBackground: Color): LaunchTransitionStateHolder =
    remember { LaunchTransitionStateHolder(defaultLaunchBackground) }
