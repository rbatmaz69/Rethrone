package com.example.androidlauncher.data

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import com.example.androidlauncher.MediaInfo

/**
 * Beobachtet die aktiven MediaSessions und meldet die relevante Wiedergabe als
 * [MediaInfo] an [onMediaChanged] (A5-Split aus dem NotificationService). Der
 * Service bleibt ein dünner Listener; Auswahl-Logik (spielend vor pausiert,
 * Pausiert nur nach gesehener Wiedergabe) liegt hier und ist mit MockK testbar.
 */
class MediaSessionMonitor(
    private val mediaSessionManager: MediaSessionManager?,
    private val listenerComponent: ComponentName,
    private val handler: Handler,
    private val onMediaChanged: (MediaInfo?, MediaController.TransportControls?) -> Unit,
) {

    private val controllerCallbacks = mutableMapOf<MediaController, MediaController.Callback>()

    /**
     * Sessions, die wir in dieser Prozess-Lebenszeit **schon einmal spielend** gesehen haben.
     * Nur solche dürfen pausiert eine Pille zeigen – so taucht eine von Spotify spontan
     * aufgeweckte, nie gespielte Session nicht von selbst in der Insel auf.
     */
    private val seenPlayingTokens = mutableSetOf<MediaSession.Token>()

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers -> onSessionsChanged(controllers) }

    fun start() {
        val msm = mediaSessionManager ?: return
        try {
            msm.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, handler)
            onSessionsChanged(msm.getActiveSessions(listenerComponent))
        } catch (_: Exception) {
            // Sicherheitsnetz: bei fehlendem Zugriff bleibt die Mediensteuerung einfach leer.
        }
    }

    fun stop() {
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsListener)
        } catch (_: Exception) {
            // ignore
        }
        controllerCallbacks.forEach { (controller, cb) -> controller.unregisterCallback(cb) }
        controllerCallbacks.clear()
        onMediaChanged(null, null)
    }

    internal fun onSessionsChanged(controllers: List<MediaController>?) {
        controllerCallbacks.forEach { (controller, cb) -> controller.unregisterCallback(cb) }
        controllerCallbacks.clear()
        for (controller in controllers.orEmpty()) {
            val cb = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) = recomputeMedia()
                override fun onMetadataChanged(metadata: MediaMetadata?) = recomputeMedia()
                override fun onSessionDestroyed() {
                    // Tote Session sofort abmelden + aus dem Cache werfen, sonst meldet sie evtl.
                    // weiter STATE_PAUSED und die Media-Pille bliebe hängen.
                    controllerCallbacks.remove(controller)?.let { controller.unregisterCallback(it) }
                    recomputeMedia()
                }
            }
            controller.registerCallback(cb, handler)
            controllerCallbacks[controller] = cb
        }
        recomputeMedia()
    }

    /** Wählt die aktive Session (bevorzugt spielend) und meldet sie via [onMediaChanged]. */
    internal fun recomputeMedia() {
        // Autoritative, frische Liste vom System abfragen – nicht den (evtl. veralteten) Cache. So
        // können tote/geschlossene Sessions die Pille nicht mehr künstlich am Leben halten.
        val controllers = try {
            mediaSessionManager?.getActiveSessions(listenerComponent).orEmpty()
        } catch (_: Exception) {
            controllerCallbacks.keys.toList()
        }
        // Tote Sessions vergessen, lebende mit aktueller Wiedergabe als „schon gespielt" merken.
        val currentTokens = controllers.mapNotNull { it.sessionToken }.toSet()
        seenPlayingTokens.retainAll(currentTokens)
        controllers.forEach { c ->
            if (c.playbackState?.state == PlaybackState.STATE_PLAYING) {
                c.sessionToken?.let { seenPlayingTokens.add(it) }
            }
        }
        val active = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull {
                val s = it.playbackState?.state
                (s == PlaybackState.STATE_PAUSED || s == PlaybackState.STATE_BUFFERING) &&
                    it.sessionToken in seenPlayingTokens
            }
        if (active == null) {
            onMediaChanged(null, null)
            return
        }
        val md = active.metadata
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty()
        if (title.isBlank() && artist.isBlank()) {
            onMediaChanged(null, active.transportControls)
            return
        }
        val art = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        // Buffering/Skip als „spielend" werten: beim Track-Wechsel durchläuft der Player
        // kurz diese Zustände. Würden sie als Pause gelten, flackerte `isPlaying` und die
        // Insel-Reihenfolge (spielend vs. pausiert) würde springen.
        val playbackState = active.playbackState?.state
        val isPlaying = playbackState == PlaybackState.STATE_PLAYING ||
            playbackState == PlaybackState.STATE_BUFFERING ||
            playbackState == PlaybackState.STATE_SKIPPING_TO_NEXT ||
            playbackState == PlaybackState.STATE_SKIPPING_TO_PREVIOUS
        onMediaChanged(
            MediaInfo(
                title = title,
                artist = artist,
                isPlaying = isPlaying,
                art = art,
                packageName = active.packageName ?: ""
            ),
            active.transportControls
        )
    }
}
