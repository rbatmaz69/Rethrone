package com.example.androidlauncher.data

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import com.example.androidlauncher.MediaInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testet die Session-Auswahl des Monitors: spielend vor pausiert, und pausierte
 * Sessions nur, wenn sie in dieser Prozess-Lebenszeit schon einmal gespielt haben.
 */
class MediaSessionMonitorTest {

    private val received = mutableListOf<Pair<MediaInfo?, MediaController.TransportControls?>>()
    private val msm = mockk<MediaSessionManager>(relaxed = true)
    private val monitor = MediaSessionMonitor(
        mediaSessionManager = msm,
        listenerComponent = mockk<ComponentName>(),
        handler = mockk<Handler>(relaxed = true),
        onMediaChanged = { media, controls -> received += media to controls },
    )

    private fun controller(
        state: Int,
        title: String = "Song",
        artist: String = "Artist",
        pkg: String = "com.music",
        token: MediaSession.Token = mockk(),
    ): MediaController = mockk(relaxed = true) {
        every { playbackState } returns mockk { every { this@mockk.state } returns state }
        every { sessionToken } returns token
        every { packageName } returns pkg
        every { metadata } returns mockk(relaxed = true) {
            every { getString(MediaMetadata.METADATA_KEY_TITLE) } returns title
            every { getString(MediaMetadata.METADATA_KEY_ARTIST) } returns artist
            every { getBitmap(any()) } returns null
        }
    }

    @Test
    fun `playing session wins and is reported as playing`() {
        val playing = controller(PlaybackState.STATE_PLAYING, title = "Laut")
        val paused = controller(PlaybackState.STATE_PAUSED, title = "Leise")
        every { msm.getActiveSessions(any()) } returns listOf(paused, playing)

        monitor.recomputeMedia()

        val (media, controls) = received.last()
        assertEquals("Laut", media?.title)
        assertTrue(media?.isPlaying == true)
        assertEquals(playing.transportControls, controls)
    }

    @Test
    fun `paused session shows only after it was seen playing`() {
        val token = mockk<MediaSession.Token>()
        val paused = controller(PlaybackState.STATE_PAUSED, title = "Podcast", token = token)
        every { msm.getActiveSessions(any()) } returns listOf(paused)

        // Nie spielend gesehen → keine Pille.
        monitor.recomputeMedia()
        assertNull(received.last().first)

        // Einmal spielend gesehen …
        val playing = controller(PlaybackState.STATE_PLAYING, title = "Podcast", token = token)
        every { msm.getActiveSessions(any()) } returns listOf(playing)
        monitor.recomputeMedia()

        // … dann darf dieselbe Session auch pausiert erscheinen.
        every { msm.getActiveSessions(any()) } returns listOf(paused)
        monitor.recomputeMedia()
        val media = received.last().first
        assertEquals("Podcast", media?.title)
        assertEquals(false, media?.isPlaying)
    }

    @Test
    fun `stop unregisters callbacks and clears the media state`() {
        val playing = controller(PlaybackState.STATE_PLAYING)
        every { msm.getActiveSessions(any()) } returns listOf(playing)
        monitor.onSessionsChanged(listOf(playing))

        monitor.stop()

        verify { playing.unregisterCallback(any()) }
        verify { msm.removeOnActiveSessionsChangedListener(any()) }
        assertNull(received.last().first)
        assertNull(received.last().second)
    }
}
