package com.example.androidlauncher.data

import android.media.session.MediaController
import com.example.androidlauncher.MediaInfo
import com.example.androidlauncher.NotificationInfo
import com.example.androidlauncher.TimerInfo
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Prozessweiter Zustand der Benachrichtigungs-/Timer-/Media-Quellen für die Dynamic
 * Island und die Notification-Dots (A5-Split: ersetzt die statischen Companion-
 * StateFlows im NotificationService). Der Service (Schreiber) und die Konsumenten
 * (DynamicIslandManager, App-Icons) teilen sich das Hilt-Singleton – dadurch sind
 * beide Seiten gegen Fakes testbar statt an globalen statischen Zustand gebunden.
 */
class NotificationStateStore {

    private val _activeNotificationPackages = MutableStateFlow<Set<String>>(emptySet())
    val activeNotificationPackages: StateFlow<Set<String>> = _activeNotificationPackages.asStateFlow()

    /** Detail-Flow (Paket/Titel/Text/Aktionen) für die Dynamic Island. Media + Timer ausgefiltert. */
    private val _activeNotificationDetails = MutableStateFlow<List<NotificationInfo>>(emptyList())
    val activeNotificationDetails: StateFlow<List<NotificationInfo>> = _activeNotificationDetails.asStateFlow()

    /** Aktuell laufender Countdown-Timer der Uhr-App (oder `null`). */
    private val _activeTimer = MutableStateFlow<TimerInfo?>(null)
    val activeTimer: StateFlow<TimerInfo?> = _activeTimer.asStateFlow()

    /** Aktuelle Medienwiedergabe (oder `null`). */
    private val _activeMedia = MutableStateFlow<MediaInfo?>(null)
    val activeMedia: StateFlow<MediaInfo?> = _activeMedia.asStateFlow()

    /** Transport-Steuerung der aktiven Session (für Play/Pause/Skip aus der Insel-Karte). */
    @Volatile
    private var transportControls: MediaController.TransportControls? = null

    /** Schreibt die einsortierten Benachrichtigungen (Aufrufer: NotificationService). */
    fun updateNotifications(packages: Set<String>, details: List<NotificationInfo>) {
        _activeNotificationPackages.value = packages
        _activeNotificationDetails.value = details
    }

    fun updateTimer(timer: TimerInfo?) {
        _activeTimer.value = timer
    }

    /**
     * Aktualisiert den Timer nur, wenn noch [expected] anliegt – für die nachgereichte
     * Chronometer-Verfeinerung vom Main-Thread (verhindert das Überschreiben eines
     * inzwischen neu einsortierten Timers).
     */
    fun refineTimerIfCurrent(expected: TimerInfo, refined: TimerInfo) {
        if (refined != _activeTimer.value && _activeTimer.value === expected) {
            _activeTimer.value = refined
        }
    }

    /** Schreibt die aktive Medienwiedergabe samt zugehöriger Transport-Steuerung. */
    fun updateMedia(media: MediaInfo?, controls: MediaController.TransportControls?) {
        transportControls = controls
        _activeMedia.value = media
    }

    fun mediaPlayPause() {
        val tc = transportControls ?: return
        if (_activeMedia.value?.isPlaying == true) tc.pause() else tc.play()
    }

    fun mediaNext() {
        transportControls?.skipToNext()
    }

    fun mediaPrevious() {
        transportControls?.skipToPrevious()
    }

    /** Springt in der aktiven Wiedergabe an die gewünschte Position (Seek-Leiste der Karte). */
    fun mediaSeekTo(positionMs: Long) {
        transportControls?.seekTo(positionMs.coerceAtLeast(0L))
    }
}

/**
 * Hilt-Zugang für Aufrufer ohne Injection-Möglichkeit (z. B. freistehende Composables
 * wie AppIconView), aufgelöst via `EntryPointAccessors.fromApplication(...)`.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationStateStoreEntryPoint {
    fun notificationStateStore(): NotificationStateStore
}
