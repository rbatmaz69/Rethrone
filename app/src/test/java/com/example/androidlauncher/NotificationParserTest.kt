package com.example.androidlauncher

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testet die Umwandlung von StatusBarNotifications in die Island-Modelle.
 * `Notification` wird real instanziiert (öffentliche Felder), `Bundle`-Zugriffe
 * werden gemockt, weil die android.jar-Stubs in Plain-JVM-Tests nur Defaults liefern.
 */
class NotificationParserTest {

    private fun sbn(
        pkg: String = "com.example.app",
        postTime: Long = 42L,
        notification: Notification?,
    ): StatusBarNotification = mockk {
        every { this@mockk.notification } returns notification
        every { packageName } returns pkg
        every { this@mockk.postTime } returns postTime
    }

    private fun extras(title: String? = null, text: String? = null): Bundle = mockk(relaxed = true) {
        every { getCharSequence(Notification.EXTRA_TITLE) } returns title
        every { getCharSequence(Notification.EXTRA_TEXT) } returns text
        every { getBoolean(any()) } returns false
        every { containsKey(Notification.EXTRA_MEDIA_SESSION) } returns false
    }

    @Test
    fun `toInfo maps title text package and post time`() {
        val n = Notification().apply { extras = extras(title = "Nachricht", text = "Hallo") }

        val info = NotificationParser.toInfo(sbn(pkg = "com.whatsapp", postTime = 7L, notification = n))

        assertEquals("com.whatsapp", info?.pkg)
        assertEquals("Nachricht", info?.title)
        assertEquals("Hallo", info?.text)
        assertEquals(7L, info?.postTimeMs)
        assertTrue(info?.actions.orEmpty().isEmpty())
    }

    @Test
    fun `toInfo returns null for empty group headers`() {
        val n = Notification().apply { extras = extras(title = "", text = " ") }

        assertNull(NotificationParser.toInfo(sbn(notification = n)))
    }

    @Test
    fun `kindOf detects media via transport category`() {
        val n = Notification().apply {
            extras = extras()
            category = Notification.CATEGORY_TRANSPORT
        }

        assertEquals(NotificationKind.MEDIA, NotificationParser.kindOf(sbn(notification = n), nowMs = 1_000L))
    }

    @Test
    fun `kindOf treats plain notification as normal`() {
        val n = Notification().apply { extras = extras(title = "T") }

        assertEquals(NotificationKind.NORMAL, NotificationParser.kindOf(sbn(notification = n), nowMs = 1_000L))
    }

    @Test
    fun `toTimerInfo without derivable time keeps null anchor`() {
        // Uhr-App-Notification ohne Chronometer/Zeit-Text → Timer ohne Anker.
        val n = Notification().apply { extras = extras(title = "Timer") }

        val timer = NotificationParser.toTimerInfo(sbn(pkg = "com.google.android.deskclock", notification = n), 1_000L)

        assertEquals("com.google.android.deskclock", timer?.pkg)
        assertNull(timer?.anchorMs)
    }
}
