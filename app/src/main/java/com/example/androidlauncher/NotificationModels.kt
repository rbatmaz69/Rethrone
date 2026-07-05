package com.example.androidlauncher

import android.graphics.Bitmap
import com.example.androidlauncher.data.NotificationAction

/**
 * Kompakte Detail-Repräsentation einer aktiven Benachrichtigung für die Dynamic Island.
 *
 * @param postTimeMs Zeitpunkt des Postings (zum Ermitteln der „neuesten" Benachrichtigung).
 * @param actions Inline-Aktionen (z. B. Antworten) für die aufgeklappte Karte.
 * @param contentIntent Öffnet die zugehörige App beim Tap auf den Text.
 */
data class NotificationInfo(
    val pkg: String,
    val title: String,
    val text: String,
    val postTimeMs: Long,
    val actions: List<NotificationAction> = emptyList(),
    val contentIntent: android.app.PendingIntent? = null
)

/**
 * Laufender Timer/Stoppuhr der Uhr-App. Zeit-Bezug via [resolveTimerAnchor] (reflection-frei).
 */
data class TimerInfo(
    val pkg: String,
    val label: String,
    /** Bezugszeit: bei Countdown das Zielende, bei Stoppuhr die Startzeit; `null` = keine Zeit. */
    val anchorMs: Long?,
    /** `true` ⇒ hochzählende Stoppuhr (verstrichene Zeit), `false` ⇒ Countdown. */
    val countUp: Boolean = false,
    /** `true` ⇒ Aktivität ist pausiert; der Manager hält dann den zuletzt gezeigten Wert. */
    val paused: Boolean = false,
    val contentIntent: android.app.PendingIntent? = null,
    val actions: List<NotificationAction> = emptyList()
)

/**
 * Aktuelle Medienwiedergabe (aus der aktiven MediaSession) für die Dynamic Island.
 *
 * @param progress Wiedergabe-Fortschritt für die Seek-Leiste; `null`, wenn die
 *        Session keine (plausible) Dauer/Position meldet.
 */
data class MediaInfo(
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val art: Bitmap?,
    val packageName: String,
    val progress: MediaProgress? = null
)
