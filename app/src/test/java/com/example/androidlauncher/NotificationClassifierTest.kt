package com.example.androidlauncher

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Prüft die reine Klassifizierungs-Logik der Benachrichtigungen für die Dynamic Island.
 */
class NotificationClassifierTest {

    private val now = 1_000_000L

    @Test
    fun `media notification is classified as media`() {
        val kind = classifyNotificationKind(
            isMedia = true,
            isCountDownChronometer = false,
            isClockApp = false,
            isClockActivityChannel = false,
            isOngoing = false,
            whenMs = now + 60_000,
            nowMs = now
        )
        assertEquals(NotificationKind.MEDIA, kind)
    }

    @Test
    fun `countdown chronometer with future end is a timer`() {
        val kind = classifyNotificationKind(
            isMedia = false,
            isCountDownChronometer = true,
            isClockApp = false,
            isClockActivityChannel = false,
            isOngoing = true,
            whenMs = now + 60_000,
            nowMs = now
        )
        assertEquals(NotificationKind.TIMER, kind)
    }

    @Test
    fun `countdown already elapsed is not a timer`() {
        val kind = classifyNotificationKind(
            isMedia = false,
            isCountDownChronometer = true,
            isClockApp = false,
            isClockActivityChannel = false,
            isOngoing = false,
            whenMs = now - 1,
            nowMs = now
        )
        assertEquals(NotificationKind.NORMAL, kind)
    }

    @Test
    fun `ongoing clock-app notification is a timer`() {
        val kind = classifyNotificationKind(
            isMedia = false,
            isCountDownChronometer = false,
            isClockApp = true,
            isClockActivityChannel = false,
            isOngoing = true,
            whenMs = 0,
            nowMs = now
        )
        assertEquals(NotificationKind.TIMER, kind)
    }

    @Test
    fun `clock-app timer-channel notification is a timer (Google Clock RemoteViews case)`() {
        // Google Clock: nicht ongoing, kein Chronometer-Extra, when ~ jetzt – aber Channel "Timers".
        val kind = classifyNotificationKind(
            isMedia = false,
            isCountDownChronometer = false,
            isClockApp = true,
            isClockActivityChannel = true,
            isOngoing = false,
            whenMs = now,
            nowMs = now
        )
        assertEquals(NotificationKind.TIMER, kind)
    }

    @Test
    fun `clock-app non-timer-channel notification stays normal`() {
        val kind = classifyNotificationKind(
            isMedia = false,
            isCountDownChronometer = false,
            isClockApp = true,
            isClockActivityChannel = false,
            isOngoing = false,
            whenMs = now,
            nowMs = now
        )
        assertEquals(NotificationKind.NORMAL, kind)
    }

    @Test
    fun `plain notification is normal`() {
        val kind = classifyNotificationKind(
            isMedia = false,
            isCountDownChronometer = false,
            isClockApp = false,
            isClockActivityChannel = false,
            isOngoing = false,
            whenMs = now,
            nowMs = now
        )
        assertEquals(NotificationKind.NORMAL, kind)
    }

    @Test
    fun `media takes precedence`() {
        val kind = classifyNotificationKind(
            isMedia = true,
            isCountDownChronometer = true,
            isClockApp = true,
            isClockActivityChannel = true,
            isOngoing = true,
            whenMs = now + 60_000,
            nowMs = now
        )
        assertEquals(NotificationKind.MEDIA, kind)
    }

    @Test
    fun `clock package detection`() {
        assertEquals(true, isClockPackage("com.google.android.deskclock"))
        assertEquals(true, isClockPackage("com.android.deskclock"))
        assertEquals(false, isClockPackage("com.whatsapp"))
        assertEquals(false, isClockPackage(null))
    }

    @Test
    fun `clock activity channel detection (timer or stopwatch)`() {
        assertEquals(true, isClockActivityChannel("Timers"))
        assertEquals(true, isClockActivityChannel("timer_channel"))
        assertEquals(true, isClockActivityChannel("Stopwatch v2"))
        assertEquals(false, isClockActivityChannel("Alarms"))
        assertEquals(false, isClockActivityChannel(null))
    }

    @Test
    fun `stopwatch channel detection`() {
        assertEquals(true, isStopwatchChannel("Stopwatch v2"))
        assertEquals(false, isStopwatchChannel("Timers"))
        assertEquals(false, isStopwatchChannel(null))
    }

    @Test
    fun `remaining is clamped to zero`() {
        assertEquals(0L, timerRemainingMs(whenMs = now - 5_000, nowMs = now))
        assertEquals(5_000L, timerRemainingMs(whenMs = now + 5_000, nowMs = now))
    }

    @Test
    fun `parseClockTimeMs reads m ss and h mm ss`() {
        assertEquals(12 * 60_000L + 34_000L, parseClockTimeMs("12:34"))
        assertEquals(3_600_000L + 2 * 60_000L + 3_000L, parseClockTimeMs("1:02:03"))
        assertEquals(59_000L, parseClockTimeMs("Timer • 0:59 verbleibend"))
    }

    @Test
    fun `parseClockTimeMs rejects non-time text`() {
        assertEquals(null, parseClockTimeMs("Timer läuft"))
        assertEquals(null, parseClockTimeMs(""))
        assertEquals(null, parseClockTimeMs(null))
        // Keine Teil-Treffer aus längeren Ziffernfolgen (z. B. Datums-/ID-artige Strings).
        assertEquals(null, parseClockTimeMs("12345"))
    }

    private val horizon = 48L * 60 * 60 * 1000

    @Test
    fun `anchor for countdown chronometer is the future end`() {
        val a = resolveTimerAnchor(
            showChronometer = true, isCountDownChronometer = true,
            isStopwatchChannel = false, isTimerChannel = true,
            whenMs = now + 60_000, nowMs = now, horizonMs = horizon, candidateText = null
        )
        assertEquals(TimerAnchor(now + 60_000, countUp = false), a)
    }

    @Test
    fun `anchor for count-up chronometer is the past start`() {
        val a = resolveTimerAnchor(
            showChronometer = true, isCountDownChronometer = false,
            isStopwatchChannel = false, isTimerChannel = false,
            whenMs = now - 30_000, nowMs = now, horizonMs = horizon, candidateText = null
        )
        assertEquals(TimerAnchor(now - 30_000, countUp = true), a)
    }

    @Test
    fun `anchor for stopwatch channel without extras uses when as start`() {
        val a = resolveTimerAnchor(
            showChronometer = false, isCountDownChronometer = false,
            isStopwatchChannel = true, isTimerChannel = false,
            whenMs = now - 90_000, nowMs = now, horizonMs = horizon, candidateText = null
        )
        assertEquals(TimerAnchor(now - 90_000, countUp = true), a)
    }

    @Test
    fun `anchor for timer channel falls back to parsed text end`() {
        val a = resolveTimerAnchor(
            showChronometer = false, isCountDownChronometer = false,
            isStopwatchChannel = false, isTimerChannel = true,
            whenMs = now, nowMs = now, horizonMs = horizon, candidateText = "0:59"
        )
        assertEquals(TimerAnchor(now + 59_000, countUp = false), a)
    }

    @Test
    fun `anchor is null when nothing is parseable`() {
        val a = resolveTimerAnchor(
            showChronometer = false, isCountDownChronometer = false,
            isStopwatchChannel = false, isTimerChannel = true,
            whenMs = now, nowMs = now, horizonMs = horizon, candidateText = "Timer läuft"
        )
        assertEquals(TimerAnchor(null, countUp = false), a)
    }

    @Test
    fun `pause and resume action titles are detected`() {
        assertEquals(true, isPauseActionTitle("Pause"))
        assertEquals(true, isPauseActionTitle("Pausieren"))
        assertEquals(false, isPauseActionTitle("Fortsetzen"))
        assertEquals(true, isResumeActionTitle("Resume"))
        assertEquals(true, isResumeActionTitle("Fortsetzen"))
        assertEquals(true, isResumeActionTitle("Weiter"))
        assertEquals(false, isResumeActionTitle("Zurücksetzen"))
    }

    @Test
    fun `timerIsRunning prefers pause then resume then unknown`() {
        assertEquals(true, timerIsRunning(listOf("Pausieren", "+ 1:00")))
        assertEquals(false, timerIsRunning(listOf("Fortsetzen", "Zurücksetzen")))
        assertEquals(null, timerIsRunning(listOf("Zurücksetzen", "Runde")))
        assertEquals(null, timerIsRunning(emptyList()))
    }
}
