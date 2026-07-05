package com.example.androidlauncher

/**
 * Wiedergabe-Fortschritt einer MediaSession zum Zeitpunkt der letzten
 * PlaybackState-Änderung. Die Insel-Karte rechnet die aktuelle Position daraus
 * **clientseitig** hoch ([extrapolateMediaPositionMs]) – es wird also nicht
 * gepollt, sondern nur bei State-Änderungen neu gelesen (Island Media v2.4).
 *
 * @param positionMs Position zum Zeitpunkt [positionUpdatedAtElapsedMs].
 * @param durationMs Gesamtlänge (> 0, sonst wird kein Fortschritt angeboten).
 * @param playbackSpeed Wiedergabegeschwindigkeit (1.0 = normal).
 * @param positionUpdatedAtElapsedMs Zeitstempel der Positionsmessung in der
 *        `SystemClock.elapsedRealtime()`-Domäne (wie von PlaybackState geliefert).
 */
data class MediaProgress(
    val positionMs: Long,
    val durationMs: Long,
    val playbackSpeed: Float,
    val positionUpdatedAtElapsedMs: Long,
)

/**
 * Rechnet die aktuelle Wiedergabeposition aus dem letzten bekannten Fortschritt
 * hoch: bei laufender Wiedergabe wächst sie mit `playbackSpeed` seit der letzten
 * Messung, pausiert bleibt sie stehen. Ergebnis ist auf `0..durationMs` begrenzt.
 * Reine Logik – ohne Framework unit-testbar.
 */
fun extrapolateMediaPositionMs(
    progress: MediaProgress,
    isPlaying: Boolean,
    nowElapsedMs: Long,
): Long {
    val base = progress.positionMs
    val advanced = if (isPlaying) {
        val elapsed = (nowElapsedMs - progress.positionUpdatedAtElapsedMs).coerceAtLeast(0L)
        base + (elapsed * progress.playbackSpeed).toLong()
    } else {
        base
    }
    return advanced.coerceIn(0L, progress.durationMs)
}

/** Formatiert eine Wiedergabezeit als `m:ss` bzw. `h:mm:ss` (für die Seek-Leiste). */
fun formatMediaTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L)) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
