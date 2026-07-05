package com.example.androidlauncher.data

import android.media.session.MediaController
import com.example.androidlauncher.MediaInfo
import com.example.androidlauncher.NotificationInfo
import com.example.androidlauncher.TimerInfo
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationStateStoreTest {

    private val store = NotificationStateStore()

    @Test
    fun `updateNotifications feeds both flows`() {
        val info = NotificationInfo(pkg = "com.a", title = "T", text = "x", postTimeMs = 1L)

        store.updateNotifications(setOf("com.a"), listOf(info))

        assertEquals(setOf("com.a"), store.activeNotificationPackages.value)
        assertEquals(listOf(info), store.activeNotificationDetails.value)
    }

    @Test
    fun `refineTimerIfCurrent only overwrites the expected instance`() {
        val provisional = TimerInfo(pkg = "com.clock", label = "Timer", anchorMs = null)
        val refined = provisional.copy(anchorMs = 123L)
        store.updateTimer(provisional)

        store.refineTimerIfCurrent(expected = provisional, refined = refined)
        assertEquals(refined, store.activeTimer.value)

        // Ein inzwischen neu einsortierter Timer darf nicht überschrieben werden.
        val newer = TimerInfo(pkg = "com.clock", label = "Neu", anchorMs = 999L)
        store.updateTimer(newer)
        store.refineTimerIfCurrent(expected = provisional, refined = refined)
        assertEquals(newer, store.activeTimer.value)
    }

    @Test
    fun `media controls forward to the current transport controls`() {
        val controls = mockk<MediaController.TransportControls>(relaxed = true)
        val playing = MediaInfo(title = "Song", artist = "A", isPlaying = true, art = null, packageName = "com.music")

        store.updateMedia(playing, controls)
        store.mediaPlayPause()
        verify { controls.pause() }

        store.updateMedia(playing.copy(isPlaying = false), controls)
        store.mediaPlayPause()
        verify { controls.play() }

        store.mediaNext()
        store.mediaPrevious()
        verify { controls.skipToNext() }
        verify { controls.skipToPrevious() }
    }

    @Test
    fun `clearing media detaches the controls`() {
        val controls = mockk<MediaController.TransportControls>(relaxed = true)
        store.updateMedia(
            MediaInfo(title = "Song", artist = "A", isPlaying = true, art = null, packageName = "com.music"),
            controls
        )

        store.updateMedia(null, null)

        assertNull(store.activeMedia.value)
        // Ohne Session sind die Steuer-Aufrufe stille No-Ops.
        store.mediaPlayPause()
        verify(exactly = 0) { controls.play() }
    }
}
