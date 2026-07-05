package com.example.androidlauncher

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.media.session.MediaSessionManager
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
import com.example.androidlauncher.data.MediaSessionMonitor
import com.example.androidlauncher.data.NotificationStateStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Service that listens for notification events.
 * A5-Split: nur noch ein dünner Listener – die Flows leben im injizierten
 * [NotificationStateStore], das Parsen in [NotificationParser], die
 * MediaSession-Beobachtung im [MediaSessionMonitor]. Hier verbleibt einzig die
 * View-basierte Chronometer-Verfeinerung (braucht Context + Main-Thread).
 */
@AndroidEntryPoint
class NotificationService : NotificationListenerService() {

    @Inject
    lateinit var stateStore: NotificationStateStore

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listenerComponent by lazy { ComponentName(this, NotificationService::class.java) }
    private var mediaMonitor: MediaSessionMonitor? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateNotifications()
        mediaMonitor = MediaSessionMonitor(
            mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager,
            listenerComponent = listenerComponent,
            handler = mainHandler,
            onMediaChanged = { media, controls -> stateStore.updateMedia(media, controls) },
        ).also { it.start() }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        mediaMonitor?.stop()
        mediaMonitor = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateNotifications()
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
                when (NotificationParser.kindOf(sbn, now)) {
                    NotificationKind.MEDIA -> Unit // separat über MediaSession
                    NotificationKind.TIMER -> if (timer == null) {
                        timer = NotificationParser.toTimerInfo(sbn, now)
                        timerSbn = sbn
                    }
                    NotificationKind.NORMAL -> NotificationParser.toInfo(sbn)?.let {
                        details.add(it)
                        packages.add(it.pkg)
                    }
                }
            }

            stateStore.updateNotifications(packages, details)
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
            stateStore.updateTimer(resolved)
            if (!onMain && provisional != null && sbn != null) {
                mainHandler.post {
                    stateStore.refineTimerIfCurrent(
                        expected = provisional,
                        refined = refineTimerWithChronometer(sbn, provisional),
                    )
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
                "when=${n?.`when`} now=$now kind=${NotificationParser.kindOf(sbn, now)}"
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
