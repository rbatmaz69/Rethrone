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
}
