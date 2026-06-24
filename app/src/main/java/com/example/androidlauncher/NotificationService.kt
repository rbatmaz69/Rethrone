package com.example.androidlauncher

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.FrameLayout
import com.example.androidlauncher.data.NotificationAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Kompakte Detail-Repräsentation einer aktiven Benachrichtigung für die Dynamic Island.
 *
 * @param postTimeMs Zeitpunkt des Postings (zum Ermitteln der „neuesten" Benachrichtigung).
 * @param actions Inline-Aktionen (z. B. Antworten) für die aufgeklappte Karte.
 * @param contentIntent Öffnet die zugehörige App beim Tap auf den Text.
 */
data class NotificationInfo(
    val pkg: String,
    val title: String,
    val text: String,
    val postTimeMs: Long,
    val actions: List<NotificationAction> = emptyList(),
    val contentIntent: android.app.PendingIntent? = null
)

/**
 * Laufender Timer/Stoppuhr der Uhr-App. Zeit-Bezug via [resolveTimerAnchor] (reflection-frei).
 */
data class TimerInfo(
    val pkg: String,
    val label: String,
    /** Bezugszeit: bei Countdown das Zielende, bei Stoppuhr die Startzeit; `null` = keine Zeit. */
    val anchorMs: Long?,
    /** `true` ⇒ hochzählende Stoppuhr (verstrichene Zeit), `false` ⇒ Countdown. */
    val countUp: Boolean = false,
    /** `true` ⇒ Aktivität ist pausiert; der Manager hält dann den zuletzt gezeigten Wert. */
    val paused: Boolean = false,
    val contentIntent: android.app.PendingIntent? = null,
    val actions: List<NotificationAction> = emptyList()
)

/**
 * Aktuelle Medienwiedergabe (aus der aktiven MediaSession) für die Dynamic Island.
 */
data class MediaInfo(
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val art: Bitmap?,
    val packageName: String
)

/**
 * Service that listens for notification events.
 * Stellt für die Dynamic Island getrennte Flows bereit: normale Benachrichtigungen und
 * laufende Timer. Mediensteuerung wird ausgefiltert (separat über MediaSession behandelt).
 */
class NotificationService : NotificationListenerService() {

    companion object {
        /** Plausibilitätsgrenze für eine Timer-Zielzeit (48 h). */
        private const val MAX_TIMER_HORIZON_MS = 48L * 60 * 60 * 1000

        private val _activeNotificationPackages = MutableStateFlow<Set<String>>(emptySet())
        val activeNotificationPackages = _activeNotificationPackages.asStateFlow()

        /** Detail-Flow (Paket/Titel/Text/Aktionen) für die Dynamic Island. Media + Timer ausgefiltert. */
        private val _activeNotificationDetails = MutableStateFlow<List<NotificationInfo>>(emptyList())
        val activeNotificationDetails = _activeNotificationDetails.asStateFlow()

        /** Aktuell laufender Countdown-Timer der Uhr-App (oder `null`). */
        private val _activeTimer = MutableStateFlow<TimerInfo?>(null)
        val activeTimer = _activeTimer.asStateFlow()

        /** Aktuelle Medienwiedergabe (oder `null`). */
        private val _activeMedia = MutableStateFlow<MediaInfo?>(null)
        val activeMedia = _activeMedia.asStateFlow()

        /** Transport-Steuerung der aktiven Session (für Play/Pause/Skip aus der Insel-Karte). */
        @Volatile
        private var transportControls: MediaController.TransportControls? = null

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
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var mediaSessionManager: MediaSessionManager? = null
    private val listenerComponent by lazy { ComponentName(this, NotificationService::class.java) }
    private val controllerCallbacks = mutableMapOf<MediaController, MediaController.Callback>()

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers -> onSessionsChanged(controllers) }

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateNotifications()
        setupMediaSessions()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        teardownMediaSessions()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateNotifications()
    }

    // ── MediaSession ─────────────────────────────────────────────────────────────────────

    private fun setupMediaSessions() {
        try {
            val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
            mediaSessionManager = msm
            msm.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, mainHandler)
            onSessionsChanged(msm.getActiveSessions(listenerComponent))
        } catch (e: Exception) {
            // Sicherheitsnetz: bei fehlendem Zugriff bleibt die Mediensteuerung einfach leer.
        }
    }

    private fun teardownMediaSessions() {
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsListener)
        } catch (e: Exception) {
            // ignore
        }
        controllerCallbacks.forEach { (controller, cb) -> controller.unregisterCallback(cb) }
        controllerCallbacks.clear()
        transportControls = null
        _activeMedia.value = null
    }

    private fun onSessionsChanged(controllers: List<MediaController>?) {
        controllerCallbacks.forEach { (controller, cb) -> controller.unregisterCallback(cb) }
        controllerCallbacks.clear()
        for (controller in controllers.orEmpty()) {
            val cb = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) = recomputeMedia()
                override fun onMetadataChanged(metadata: MediaMetadata?) = recomputeMedia()
                override fun onSessionDestroyed() = recomputeMedia()
            }
            controller.registerCallback(cb, mainHandler)
            controllerCallbacks[controller] = cb
        }
        recomputeMedia()
    }

    /** Wählt die aktive Session (bevorzugt spielend) und aktualisiert [activeMedia]. */
    private fun recomputeMedia() {
        val controllers = controllerCallbacks.keys.toList()
        val active = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull {
                val s = it.playbackState?.state
                s == PlaybackState.STATE_PAUSED || s == PlaybackState.STATE_BUFFERING
            }
        if (active == null) {
            transportControls = null
            _activeMedia.value = null
            return
        }
        transportControls = active.transportControls
        val md = active.metadata
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty()
        if (title.isBlank() && artist.isBlank()) {
            _activeMedia.value = null
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
        _activeMedia.value = MediaInfo(
            title = title,
            artist = artist,
            isPlaying = isPlaying,
            art = art,
            packageName = active.packageName ?: ""
        )
    }

    /**
     * Aktualisiert die Flows: jede aktive Benachrichtigung wird in Media (ignoriert),
     * Timer oder normale Benachrichtigung einsortiert.
     */
    private fun updateNotifications() {
        try {
            val notifications = activeNotifications ?: return
            val now = System.currentTimeMillis()
            val details = mutableListOf<NotificationInfo>()
            var timer: TimerInfo? = null
            var timerSbn: StatusBarNotification? = null
            val packages = mutableSetOf<String>()

            for (sbn in notifications.filterNotNull()) {
                // Gruppen-Zusammenfassungen sind Aggregate (kein echter Inhalt) → überspringen.
                if (((sbn.notification?.flags ?: 0) and Notification.FLAG_GROUP_SUMMARY) != 0) continue
                if (BuildConfig.DEBUG) logNotification(sbn, now)
                when (sbn.kind(now)) {
                    NotificationKind.MEDIA -> Unit // separat über MediaSession
                    NotificationKind.TIMER -> if (timer == null) {
                        timer = sbn.toTimerInfo()
                        timerSbn = sbn
                    }
                    NotificationKind.NORMAL -> sbn.toInfo()?.let {
                        details.add(it)
                        packages.add(it.pkg)
                    }
                }
            }

            _activeNotificationPackages.value = packages
            _activeNotificationDetails.value = details
            // Die echte Chronometer-Zeit braucht den Main-Thread (View-Erzeugung). Die
            // Listener-Callbacks laufen ohnehin auf Main → dann inline verfeinern und nur EINE
            // Emission setzen. Sonst (defensiv) provisorisch setzen und auf Main nachreichen.
            // Wichtig: keine Doppel-Emission (provisorisch → verfeinert), sonst springt die Zeit
            // einen Frame lang = Flackern (v. a. beim Timer-Pause/Start).
            val provisional = timer
            val sbn = timerSbn
            val onMain = Looper.myLooper() == Looper.getMainLooper()
            val resolved = if (provisional != null && sbn != null && onMain) {
                refineTimerWithChronometer(sbn, provisional)
            } else {
                provisional
            }
            _activeTimer.value = resolved
            if (!onMain && provisional != null && sbn != null) {
                mainHandler.post {
                    val refined = refineTimerWithChronometer(sbn, provisional)
                    if (refined != _activeTimer.value && _activeTimer.value === provisional) {
                        _activeTimer.value = refined
                    }
                }
            }
        } catch (e: Exception) {
            // In some cases (e.g. during binding) activeNotifications might not be available yet
        }
    }

    /** Diagnose-Log (nur Debug-Build): zeigt, warum eine Notification als TIMER/NORMAL/MEDIA gilt. */
    private fun logNotification(sbn: StatusBarNotification, now: Long) {
        val n = sbn.notification
        val extras = n?.extras
        val ongoing = ((n?.flags ?: 0) and Notification.FLAG_ONGOING_EVENT) != 0
        Log.d(
            "IslandNotif",
            "pkg=${sbn.packageName} ongoing=$ongoing clock=${isClockPackage(sbn.packageName)} " +
                "cat=${n?.category} showChrono=${extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)} " +
                "countDown=${extras?.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN)} " +
                "when=${n?.`when`} now=$now kind=${sbn.kind(now)}"
        )
    }

    /** Ordnet eine [StatusBarNotification] in [NotificationKind] ein (siehe [classifyNotificationKind]). */
    private fun StatusBarNotification.kind(nowMs: Long): NotificationKind {
        val n = notification ?: return NotificationKind.NORMAL
        val extras = n.extras
        val isCountDown = extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) == true &&
            extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN)
        val isOngoing = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0
        return classifyNotificationKind(
            isMedia = isMediaNotification(),
            isCountDownChronometer = isCountDown,
            isClockApp = isClockPackage(packageName),
            isClockActivityChannel = isClockActivityChannel(n.channelId),
            isOngoing = isOngoing,
            whenMs = n.`when`,
            nowMs = nowMs
        )
    }

    /**
     * Erkennt Medien-/Wiedergabe-Benachrichtigungen (Mediensteuerung). EXTRA_MEDIA_SESSION
     * (MediaSession-Token) ist der robusteste Marker für MediaStyle-Benachrichtigungen,
     * unabhängig vom Play/Pause-Zustand; CATEGORY_TRANSPORT deckt zusätzliche Transport-Steuerungen ab.
     */
    private fun StatusBarNotification.isMediaNotification(): Boolean {
        val n = notification ?: return false
        if (n.category == Notification.CATEGORY_TRANSPORT) return true
        return n.extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true
    }

    /**
     * Wandelt eine [StatusBarNotification] in die schlanke [NotificationInfo] um (inkl. Aktionen).
     * Liefert `null`, wenn weder Titel noch Text vorhanden sind (z. B. reine Gruppen-Header).
     */
    private fun StatusBarNotification.toInfo(): NotificationInfo? {
        val n = notification ?: return null
        val extras = n.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return null
        val actions = n.actions?.mapNotNull { action ->
            val actionTitle = action?.title?.toString()
            val actionIntent = action?.actionIntent
            if (actionTitle.isNullOrBlank() || actionIntent == null) {
                null
            } else {
                NotificationAction(actionTitle, actionIntent, action.remoteInputs?.toList().orEmpty())
            }
        }.orEmpty()
        return NotificationInfo(
            pkg = packageName ?: return null,
            title = title,
            text = text,
            postTimeMs = postTime,
            actions = actions,
            contentIntent = n.contentIntent
        )
    }

    /**
     * Baut [TimerInfo] aus einer Timer-/Stoppuhr-Benachrichtigung. Die Bezugszeit kommt
     * **reflection-frei** aus Standard-Feldern via [resolveTimerAnchor]: Countdown-Chronometer,
     * Count-up-Chronometer/Stoppuhr (`when` = Start) oder Regex über Title/Text/SubText. Ist
     * nichts ableitbar (z. B. reine Custom-RemoteViews ohne Zeit), bleibt `anchorMs = null`.
     */
    private fun StatusBarNotification.toTimerInfo(): TimerInfo? {
        val n = notification ?: return null
        val extras = n.extras
        val now = System.currentTimeMillis()
        val showChrono = extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) == true
        val isCountDown = showChrono && extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN)
        val stopwatch = isStopwatchChannel(n.channelId)
        // Kandidaten-Text für den Regex-Fallback (Title/Text/SubText der Uhr-App).
        val candidateText = listOfNotNull(
            extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        ).joinToString(" ").ifBlank { null }
        // Reflection-freie Zeit-Verankerung aus Standard-Feldern.
        val anchor = resolveTimerAnchor(
            showChronometer = showChrono,
            isCountDownChronometer = isCountDown,
            isStopwatchChannel = stopwatch,
            isTimerChannel = isClockActivityChannel(n.channelId),
            whenMs = n.`when`,
            nowMs = now,
            horizonMs = MAX_TIMER_HORIZON_MS,
            candidateText = candidateText
        )
        // Label bleibt als reine Aktivitätsart erhalten (für Switcher-Chip/Accessibility),
        // wird in der Pille aber nicht mehr als Text gezeigt.
        val label = when {
            stopwatch -> "Stoppuhr"
            else -> extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        }
        val actions = n.actions?.mapNotNull { action ->
            val actionTitle = action?.title?.toString()
            val actionIntent = action?.actionIntent
            if (actionTitle.isNullOrBlank() || actionIntent == null) {
                null
            } else {
                NotificationAction(actionTitle, actionIntent, action.remoteInputs?.toList().orEmpty())
            }
        }.orEmpty()
        // Pausiert (laut Aktions-Titeln)? Anchor bleibt erhalten – das Einfrieren übernimmt
        // der Manager, der den zuletzt gezeigten Wert hält.
        val paused = timerIsRunning(actions.map { it.title }) == false
        return TimerInfo(
            pkg = packageName ?: return null,
            label = label,
            anchorMs = anchor.anchorMs,
            countUp = anchor.countUp,
            paused = paused,
            contentIntent = n.contentIntent,
            actions = actions
        )
    }

    /** Aus einer Chronometer-View gelesene Zeit (Wanduhr-Bezug). */
    private data class ChronoTime(val anchorWallMs: Long, val countUp: Boolean)

    /**
     * Verfeinert eine provisorische [TimerInfo] mit der **echten** Zeit aus der Chronometer-View
     * der Notification-`RemoteViews` (reflection-frei via `RemoteViews.apply` + `Chronometer`).
     * Pausierte Aktivitäten (laut Aktions-Titeln) werden eingefroren statt weiterzuticken.
     * **Muss auf dem Main-Thread laufen** (View-Erzeugung). Bei Fehlern/ohne Treffer bleibt die
     * provisorische Info unverändert.
     */
    private fun refineTimerWithChronometer(sbn: StatusBarNotification, base: TimerInfo): TimerInfo {
        val chrono = extractChronometerTime(sbn) ?: return base
        // Anchor bleibt erhalten (auch wenn pausiert); das Einfrieren übernimmt der Manager.
        return base.copy(anchorMs = chrono.anchorWallMs, countUp = chrono.countUp)
    }

    /**
     * Inflated die `RemoteViews` der Notification und liest die erste [Chronometer]-View aus
     * (`base` liegt in der `elapsedRealtime`-Domäne). Gibt die in Wanduhr-Zeit umgerechnete
     * Bezugszeit zurück, oder `null`, wenn keine Chronometer-View existiert bzw. das Inflaten
     * fehlschlägt.
     */
    private fun extractChronometerTime(sbn: StatusBarNotification): ChronoTime? {
        val n = sbn.notification ?: return null
        val rv = n.bigContentView ?: n.contentView ?: n.headsUpContentView ?: return null
        val root: View = try {
            rv.apply(this, FrameLayout(this))
        } catch (e: Exception) {
            return null
        }
        val chrono = root.findChronometer() ?: return null
        val nowEr = SystemClock.elapsedRealtime()
        val wall = System.currentTimeMillis()
        return if (chrono.isCountDown) {
            val remaining = (chrono.base - nowEr).coerceAtLeast(0)
            ChronoTime(anchorWallMs = wall + remaining, countUp = false)
        } else {
            val elapsed = (nowEr - chrono.base).coerceAtLeast(0)
            ChronoTime(anchorWallMs = wall - elapsed, countUp = true)
        }
    }

    /** Rekursive Suche nach der ersten [Chronometer]-View im (inflateten) View-Baum. */
    private fun View.findChronometer(): Chronometer? {
        if (this is Chronometer) return this
        if (this is ViewGroup) {
            for (i in 0 until childCount) {
                getChildAt(i).findChronometer()?.let { return it }
            }
        }
        return null
    }
}
