package com.example.androidlauncher.data

import android.app.PendingIntent
import android.app.RemoteInput
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prüft die reinen Helfer-Funktionen rund um die Dynamic Island:
 * Reihenfolge mehrerer aktiver Inhalte ([orderedIslandContents]), stabile Kennung
 * ([activityId]), Icon-Paket ([iconPackage]) sowie [NotificationAction.isReply].
 * Die Prioritäts-Auswahl ([resolveIslandContent]) deckt bereits IslandContentResolverTest ab.
 */
class IslandStateHelpersTest {

    private val playingMedia = IslandContent.Media("Song", "Artist", isPlaying = true, packageName = "com.spotify")
    private val pausedMedia = playingMedia.copy(isPlaying = false)
    private val timer = IslandContent.Timer(label = "Eier", displayMs = 90_000, pkg = "com.clock")
    private val notification = IslandContent.Notification("com.app", "Titel", "Text")
    private val battery = IslandContent.Battery(level = 80, charging = true)

    @Test
    fun `ordered list empty when no source active`() {
        assertEquals(emptyList<IslandContent>(), orderedIslandContents(IslandSources()))
    }

    @Test
    fun `ordered list follows priority playing-media timer notification paused-media battery`() {
        val ordered = orderedIslandContents(
            IslandSources(media = playingMedia, timer = timer, notification = notification, battery = battery)
        )
        assertEquals(listOf(playingMedia, timer, notification, battery), ordered)
    }

    @Test
    fun `paused media sinks below timer and notification`() {
        val ordered = orderedIslandContents(
            IslandSources(media = pausedMedia, timer = timer, notification = notification, battery = battery)
        )
        assertEquals(listOf(timer, notification, pausedMedia, battery), ordered)
    }

    @Test
    fun `activityId is stable per content type`() {
        assertEquals("media:com.spotify", activityId(playingMedia))
        assertEquals("timer:com.clock:Eier", activityId(timer))
        assertEquals("notif:com.app", activityId(notification))
        assertEquals("battery", activityId(battery))
    }

    @Test
    fun `iconPackage returns package or null`() {
        assertEquals("com.spotify", iconPackage(playingMedia))
        assertEquals("com.clock", iconPackage(timer))
        assertEquals("com.app", iconPackage(notification))
        assertEquals(null, iconPackage(battery))
    }

    @Test
    fun `iconPackage is null for blank package`() {
        assertEquals(null, iconPackage(playingMedia.copy(packageName = "")))
        assertEquals(null, iconPackage(timer.copy(pkg = "")))
    }

    @Test
    fun `isReply true only when remoteInputs present`() {
        val intent = mockk<PendingIntent>(relaxed = true)
        val remoteInput = mockk<RemoteInput>(relaxed = true)
        val withInput = NotificationAction(
            title = "Antworten",
            intent = intent,
            remoteInputs = listOf(remoteInput)
        )
        val withoutInput = NotificationAction(title = "Markieren", intent = intent)
        assertTrue(withInput.isReply)
        assertFalse(withoutInput.isReply)
    }
}
