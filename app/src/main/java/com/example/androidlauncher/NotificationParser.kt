package com.example.androidlauncher

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.example.androidlauncher.data.NotificationAction

/**
 * Wandelt [StatusBarNotification]s in die schlanken Island-Modelle um (A5-Split aus
 * dem NotificationService). Reine Feld-/Extras-Auswertung ohne Context oder Views –
 * die eigentliche Klassifikations- und Zeit-Logik liegt framework-frei in
 * `NotificationClassifier.kt`; die Chronometer-Verfeinerung (View-basiert, Main-Thread)
 * bleibt im Service.
 */
object NotificationParser {

    /** Plausibilitätsgrenze für eine Timer-Zielzeit (48 h). */
    const val MAX_TIMER_HORIZON_MS = 48L * 60 * 60 * 1000

    /** Ordnet eine [StatusBarNotification] in [NotificationKind] ein (siehe [classifyNotificationKind]). */
    fun kindOf(sbn: StatusBarNotification, nowMs: Long): NotificationKind {
        val n = sbn.notification ?: return NotificationKind.NORMAL
        val extras = n.extras
        val isCountDown = extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) == true &&
            extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN)
        val isOngoing = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0
        return classifyNotificationKind(
            isMedia = isMediaNotification(sbn),
            isCountDownChronometer = isCountDown,
            isClockApp = isClockPackage(sbn.packageName),
            isClockActivityChannel = isClockActivityChannel(n.channelId),
            isOngoing = isOngoing,
            whenMs = n.`when`,
            nowMs = nowMs
        )
    }

    /**
     * Erkennt Medien-/Wiedergabe-Benachrichtigungen (Mediensteuerung). EXTRA_MEDIA_SESSION
     * (MediaSession-Token) ist der robusteste Marker für MediaStyle-Benachrichtigungen,
     * unabhängig vom Play/Pause-Zustand; CATEGORY_TRANSPORT deckt zusätzliche Transport-Steuerungen ab.
     */
    fun isMediaNotification(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification ?: return false
        if (n.category == Notification.CATEGORY_TRANSPORT) return true
        return n.extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true
    }

    /**
     * Wandelt eine [StatusBarNotification] in die schlanke [NotificationInfo] um (inkl. Aktionen).
     * Liefert `null`, wenn weder Titel noch Text vorhanden sind (z. B. reine Gruppen-Header).
     */
    fun toInfo(sbn: StatusBarNotification): NotificationInfo? {
        val n = sbn.notification ?: return null
        val extras = n.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return null
        return NotificationInfo(
            pkg = sbn.packageName ?: return null,
            title = title,
            text = text,
            postTimeMs = sbn.postTime,
            actions = parseActions(n),
            contentIntent = n.contentIntent
        )
    }

    /**
     * Baut [TimerInfo] aus einer Timer-/Stoppuhr-Benachrichtigung. Die Bezugszeit kommt
     * **reflection-frei** aus Standard-Feldern via [resolveTimerAnchor]: Countdown-Chronometer,
     * Count-up-Chronometer/Stoppuhr (`when` = Start) oder Regex über Title/Text/SubText. Ist
     * nichts ableitbar (z. B. reine Custom-RemoteViews ohne Zeit), bleibt `anchorMs = null`.
     */
    fun toTimerInfo(sbn: StatusBarNotification, nowMs: Long): TimerInfo? {
        val n = sbn.notification ?: return null
        val extras = n.extras
        val showChrono = extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) == true
        val isCountDown = showChrono && extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN)
        val stopwatch = isStopwatchChannel(n.channelId)
        // Kandidaten-Text für den Regex-Fallback (Title/Text/SubText der Uhr-App).
        val candidateText = listOfNotNull(
            extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        ).joinToString(" ").ifBlank { null }
        // Reflection-freie Zeit-Verankerung aus Standard-Feldern.
        val anchor = resolveTimerAnchor(
            showChronometer = showChrono,
            isCountDownChronometer = isCountDown,
            isStopwatchChannel = stopwatch,
            isTimerChannel = isClockActivityChannel(n.channelId),
            whenMs = n.`when`,
            nowMs = nowMs,
            horizonMs = MAX_TIMER_HORIZON_MS,
            candidateText = candidateText
        )
        // Label bleibt als reine Aktivitätsart erhalten (für Switcher-Chip/Accessibility),
        // wird in der Pille aber nicht mehr als Text gezeigt.
        val label = when {
            stopwatch -> "Stoppuhr"
            else -> extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        }
        val actions = parseActions(n)
        // Pausiert (laut Aktions-Titeln)? Anchor bleibt erhalten – das Einfrieren übernimmt
        // der Manager, der den zuletzt gezeigten Wert hält.
        val paused = timerIsRunning(actions.map { it.title }) == false
        return TimerInfo(
            pkg = sbn.packageName ?: return null,
            label = label,
            anchorMs = anchor.anchorMs,
            countUp = anchor.countUp,
            paused = paused,
            contentIntent = n.contentIntent,
            actions = actions
        )
    }

    /** Extrahiert die Inline-Aktionen (Titel + PendingIntent + RemoteInputs). */
    private fun parseActions(n: Notification): List<NotificationAction> =
        n.actions?.mapNotNull { action ->
            val actionTitle = action?.title?.toString()
            val actionIntent = action?.actionIntent
            if (actionTitle.isNullOrBlank() || actionIntent == null) {
                null
            } else {
                NotificationAction(actionTitle, actionIntent, action.remoteInputs?.toList().orEmpty())
            }
        }.orEmpty()
}
