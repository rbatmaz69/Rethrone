package com.example.androidlauncher

/**
 * Art einer Benachrichtigung aus Sicht der Dynamic Island.
 */
enum class NotificationKind {
    /** Mediensteuerung (MediaStyle / Transport) – wird separat über MediaSession behandelt. */
    MEDIA,

    /** Laufender Countdown der Uhr-App. */
    TIMER,

    /** Normale Benachrichtigung. */
    NORMAL
}

/**
 * Reine Entscheidungs-Logik (ohne Android-Abhängigkeiten ⇒ unit-testbar), die eine
 * Benachrichtigung anhand bereits extrahierter Merkmale einordnet.
 *
 * @param isMedia true, wenn es sich um eine MediaStyle-/Transport-Benachrichtigung handelt.
 * @param isCountDownChronometer true, wenn die Benachrichtigung einen rückwärts laufenden
 *        Chronometer anzeigt (EXTRA_SHOW_CHRONOMETER && EXTRA_CHRONOMETER_COUNT_DOWN).
 * @param isClockApp true, wenn die Benachrichtigung von einer bekannten Uhr-App stammt
 *        (siehe [isClockPackage]).
 * @param isClockActivityChannel true, wenn die Benachrichtigung aus einem Timer- ODER
 *        Stoppuhr-Channel der Uhr-App stammt (Channel-ID enthält „timer"/„stopwatch").
 *        Zuverlässiges Signal für die Google-/AOSP-Uhr, die Timer/Stoppuhr über eine eigene
 *        RemoteViews-Ansicht OHNE Chronometer-Extras und NICHT als ongoing rendert.
 * @param isOngoing true, wenn die Benachrichtigung dauerhaft ist (FLAG_ONGOING_EVENT).
 * @param whenMs der `when`-Zeitstempel der Benachrichtigung (bei Countdown das Zielende).
 * @param nowMs aktuelle Zeit.
 */
fun classifyNotificationKind(
    isMedia: Boolean,
    isCountDownChronometer: Boolean,
    isClockApp: Boolean,
    isClockActivityChannel: Boolean,
    isOngoing: Boolean,
    whenMs: Long,
    nowMs: Long
): NotificationKind = when {
    isMedia -> NotificationKind.MEDIA
    isCountDownChronometer && whenMs > nowMs -> NotificationKind.TIMER
    isClockApp && (isClockActivityChannel || isOngoing) -> NotificationKind.TIMER
    else -> NotificationKind.NORMAL
}

/** Erkennt einen Timer- oder Stoppuhr-Channel einer Uhr-App (z. B. „Timers", „Stopwatch v2"). */
fun isClockActivityChannel(channelId: String?): Boolean {
    if (channelId == null) return false
    return channelId.contains("timer", ignoreCase = true) ||
        channelId.contains("stopwatch", ignoreCase = true)
}

/** True, wenn die Channel-ID auf eine Stoppuhr hindeutet (für die Label-Wahl). */
fun isStopwatchChannel(channelId: String?): Boolean =
    channelId != null && channelId.contains("stopwatch", ignoreCase = true)

/** Bekannte Uhr-App-Pakete (Google-/AOSP-Uhr und Varianten). */
fun isClockPackage(pkg: String?): Boolean {
    if (pkg == null) return false
    return pkg == "com.google.android.deskclock" ||
        pkg == "com.android.deskclock" ||
        pkg.contains("deskclock")
}

/**
 * Restzeit eines Countdown-Timers in Millisekunden, nie negativ.
 */
fun timerRemainingMs(whenMs: Long, nowMs: Long): Long = (whenMs - nowMs).coerceAtLeast(0)
