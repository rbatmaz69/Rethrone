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

    /** Gestubbter Wiedergabe-Fortschritt für [controller]. */
    private data class ProgressStub(
        val positionMs: Long = -1L,
        val durationMs: Long = 0L,
        val speed: Float = 0f,
        val updatedAtMs: Long = 0L,
    )

    private fun controller(
        state: Int,
        title: String = "Song",
        artist: String = "Artist",
        pkg: String = "com.music",
        token: MediaSession.Token = mockk(),
        progress: ProgressStub = ProgressStub(),
    ): MediaController = mockk(relaxed = true) {
        every { playbackState } returns mockk(relaxed = true) {
            every { this@mockk.state } returns state
            every { position } returns progress.positionMs
            every { playbackSpeed } returns progress.speed
            every { lastPositionUpdateTime } returns progress.updatedAtMs
        }
        every { sessionToken } returns token
        every { packageName } returns pkg
        every { metadata } returns mockk(relaxed = true) {
            every { getString(MediaMetadata.METADATA_KEY_TITLE) } returns title
            every { getString(MediaMetadata.METADATA_KEY_ARTIST) } returns artist
            every { getLong(MediaMetadata.METADATA_KEY_DURATION) } returns progress.durationMs
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
    fun `progress is mapped from playback state and metadata duration`() {
        val playing = controller(
            PlaybackState.STATE_PLAYING,
            progress = ProgressStub(positionMs = 42_000L, durationMs = 180_000L, speed = 1.5f, updatedAtMs = 99_000L),
        )
        every { msm.getActiveSessions(any()) } returns listOf(playing)

        monitor.recomputeMedia()

        val progress = received.last().first?.progress
        assertEquals(42_000L, progress?.positionMs)
        assertEquals(180_000L, progress?.durationMs)
        assertEquals(1.5f, progress?.playbackSpeed)
        assertEquals(99_000L, progress?.positionUpdatedAtElapsedMs)
    }

    @Test
    fun `missing duration or position yields no progress`() {
        // Session ohne Dauer (z. B. Livestream) → keine Seek-Leiste.
        val noDuration =
            controller(PlaybackState.STATE_PLAYING, progress = ProgressStub(positionMs = 10_000L, durationMs = 0L))
        every { msm.getActiveSessions(any()) } returns listOf(noDuration)
        monitor.recomputeMedia()
        assertNull(received.last().first?.progress)

        // Unbekannte Position → ebenfalls keine Seek-Leiste.
        val noPosition =
            controller(PlaybackState.STATE_PLAYING, progress = ProgressStub(positionMs = -1L, durationMs = 180_000L))
        every { msm.getActiveSessions(any()) } returns listOf(noPosition)
        monitor.recomputeMedia()
        assertNull(received.last().first?.progress)
    }

    @Test
    fun `zero playback speed falls back to normal tempo`() {
        val paused = controller(
            PlaybackState.STATE_PLAYING,
            progress = ProgressStub(positionMs = 1_000L, durationMs = 60_000L, speed = 0f),
        )
        every { msm.getActiveSessions(any()) } returns listOf(paused)

        monitor.recomputeMedia()

        assertEquals(1f, received.last().first?.progress?.playbackSpeed)
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
