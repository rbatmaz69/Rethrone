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

private val PAUSE_WORDS = listOf("pause", "pausier")
private val RESUME_WORDS = listOf("resume", "fortsetz", "weiter", "play")

/** True, wenn der Aktions-Titel einer Uhr-Notification eine **Pause**-Aktion meint. */
fun isPauseActionTitle(title: String): Boolean =
    title.lowercase().let { t -> PAUSE_WORDS.any { t.contains(it) } }

/** True, wenn der Aktions-Titel einer Uhr-Notification eine **Fortsetzen/Play**-Aktion meint. */
fun isResumeActionTitle(title: String): Boolean =
    title.lowercase().let { t -> RESUME_WORDS.any { t.contains(it) } }

/**
 * Leitet aus den Aktions-Titeln einer Timer-/Stoppuhr-Notification ab, ob sie gerade **läuft**:
 * eine Pause-Aktion ⇒ läuft (`true`), nur eine Resume-Aktion ⇒ pausiert (`false`), sonst
 * unbekannt (`null`). Bei `null` behandelt der Aufrufer die Aktivität als laufend
 * (Chronometer-Normalfall).
 */
fun timerIsRunning(actionTitles: List<String>): Boolean? = when {
    actionTitles.any { isPauseActionTitle(it) } -> true
    actionTitles.any { isResumeActionTitle(it) } -> false
    else -> null
}

/** Erkennt `m:ss` bzw. `h:mm:ss` (auch ohne führende Stunden-Null) irgendwo im Text. */
private val CLOCK_TIME_REGEX = Regex("""(?<!\d)(\d{1,2}):([0-5]\d)(?::([0-5]\d))?(?!\d)""")

/**
 * Liest die erste Uhrzeit-Angabe (`m:ss` oder `h:mm:ss`) aus einem Notification-Text und gibt
 * sie in Millisekunden zurück. Dient als reflection-freier Fallback, wenn die Uhr-App die
 * Restzeit nur als Text (statt als Chronometer) bereitstellt. `null`, wenn nichts passt.
 */
fun parseClockTimeMs(text: String?): Long? {
    if (text.isNullOrBlank()) return null
    val m = CLOCK_TIME_REGEX.find(text) ?: return null
    val a = m.groupValues[1].toLongOrNull() ?: return null
    val b = m.groupValues[2].toLongOrNull() ?: return null
    val c = m.groupValues[3].takeIf { it.isNotEmpty() }?.toLongOrNull()
    // Drei Gruppen ⇒ h:mm:ss, sonst m:ss.
    val totalSeconds = if (c != null) a * 3600 + b * 60 + c else a * 60 + b
    return totalSeconds * 1000
}

/**
 * Ergebnis der Zeit-Verankerung einer Timer-/Stoppuhr-Benachrichtigung.
 *
 * @param anchorMs Bezugszeit: bei Countdown das **Zielende**, bei Stoppuhr die **Startzeit**;
 *        `null`, wenn keine Zeit ableitbar ist (dann zeigt die Insel kein Wort, nur Icon+Punkt).
 * @param countUp `true` ⇒ hochzählende Stoppuhr (verstrichene Zeit), `false` ⇒ Countdown.
 */
data class TimerAnchor(val anchorMs: Long?, val countUp: Boolean)

/**
 * Bestimmt **reflection-frei** die Bezugszeit einer Timer-/Stoppuhr-Benachrichtigung aus
 * Standard-Feldern. Priorität:
 * 1. Countdown-Chronometer (`when` in der Zukunft) → Zielende.
 * 2. Count-up-Chronometer (`when` in der Vergangenheit) → Startzeit (Stoppuhr).
 * 3. Stoppuhr-Channel ohne Chronometer-Extras → `when` als Startzeit.
 * 4. Timer-Channel ohne Extras → Zeit aus dem Text ([parseClockTimeMs]) → `now + Rest` als Ende.
 * 5. sonst keine Zeit (`anchorMs == null`).
 *
 * @param candidateText zusammengeführter Title/Text/SubText für den Regex-Fallback.
 */
fun resolveTimerAnchor(
    showChronometer: Boolean,
    isCountDownChronometer: Boolean,
    isStopwatchChannel: Boolean,
    isTimerChannel: Boolean,
    whenMs: Long,
    nowMs: Long,
    horizonMs: Long,
    candidateText: String?
): TimerAnchor {
    val futureWhen = whenMs in (nowMs + 1) until (nowMs + horizonMs)
    val pastWhen = whenMs in (nowMs - horizonMs + 1)..nowMs
    return when {
        isCountDownChronometer && futureWhen -> TimerAnchor(whenMs, countUp = false)
        showChronometer && !isCountDownChronometer && pastWhen -> TimerAnchor(whenMs, countUp = true)
        isStopwatchChannel && pastWhen -> TimerAnchor(whenMs, countUp = true)
        isTimerChannel -> parseClockTimeMs(candidateText)
            ?.let { TimerAnchor(nowMs + it, countUp = false) }
            ?: TimerAnchor(null, countUp = false)
        else -> TimerAnchor(null, countUp = false)
    }
}
