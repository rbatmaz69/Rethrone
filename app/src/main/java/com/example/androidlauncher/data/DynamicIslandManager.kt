package com.example.androidlauncher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.androidlauncher.timerRemainingMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

/**
 * Führt die Live-Quellen der Dynamic Island (Timer, Benachrichtigungen, Laden; Medien folgt
 * in v2.4) zu einem [IslandState] zusammen.
 *
 * **Ereignisgesteuert:** Ist nichts aktiv, ist `state.content == null` und die Insel wird gar
 * nicht gezeichnet. Benachrichtigungen & Laden sind transient (klingen aus), Timer & Medien
 * bleiben sichtbar solange aktiv. Die Prioritäts-Logik lebt in der reinen Funktion
 * [resolveIslandContent].
 */
class DynamicIslandManager(
    context: Context,
    private val notifications: NotificationStateStore,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val appContext = context.applicationContext

    /** Eingefrorener Inhalt der aufgeklappten Karte (oder `null`, wenn nicht aufgeklappt). */
    private val _expandedContent = MutableStateFlow<IslandContent?>(null)
    val expandedContent: StateFlow<IslandContent?> = _expandedContent.asStateFlow()

    /**
     * Einmal-Puls bei einer **neu eingetroffenen** Benachrichtigung (Wert = `postTimeMs`). Treibt
     * z. B. das Edge Lighting. Feuert nur bei echter Neu-Benachrichtigung (streng steigende
     * `postTimeMs`); beim App-Start zählen nur Notifications, die **nach** der Erzeugung kommen.
     */
    private val _notificationPulse = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val notificationPulse: SharedFlow<Long> = _notificationPulse.asSharedFlow()

    init {
        scope.launch {
            // Basis = Erzeugungszeit, damit bereits aktive (alte) Benachrichtigungen nicht pulsen.
            var lastMax = System.currentTimeMillis()
            notifications.activeNotificationDetails
                .map { list -> list.maxByOrNull { it.postTimeMs }?.postTimeMs }
                .collect { max ->
                    if (max != null && max > lastMax) {
                        lastMax = max
                        _notificationPulse.tryEmit(max)
                    }
                }
        }
    }

    /** Wie lange Lade-/Benachrichtigungs-Pillen sichtbar bleiben, bevor sie ausklingen. */
    private val batteryVisibleMs = 5_000L
    private val notificationVisibleMs = 6_000L

    /** 1-Sekunden-Tick, um die Timer-Restzeit fortlaufend neu zu berechnen. */
    private val secondTick: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(1_000L)
        }
    }

    /** Zuletzt angezeigte Timer-/Stoppuhr-Zeit – wird im Pausenzustand statisch gehalten. */
    private var lastTimerDisplayMs: Long? = null

    /** Laufender Countdown/Stoppuhr der Uhr-App; Zeit wird jede Sekunde neu berechnet. */
    private val timerFlow: Flow<IslandContent.Timer?> =
        combine(notifications.activeTimer, secondTick) { timer, _ ->
            if (timer == null) {
                lastTimerDisplayMs = null
                null
            } else {
                val now = System.currentTimeMillis()
                val live = timer.anchorMs?.let { anchor ->
                    if (timer.countUp) (now - anchor).coerceAtLeast(0) else timerRemainingMs(anchor, now)
                }
                val display = if (timer.paused) {
                    // Pausiert: einmal den aktuellen Wert schnappen, danach konstant halten.
                    if (lastTimerDisplayMs == null) lastTimerDisplayMs = live
                    lastTimerDisplayMs
                } else {
                    if (live != null) lastTimerDisplayMs = live
                    live
                }
                IslandContent.Timer(
                    label = timer.label,
                    displayMs = display,
                    countUp = timer.countUp,
                    pkg = timer.pkg,
                    contentIntent = timer.contentIntent,
                    actions = timer.actions,
                    paused = timer.paused
                )
            }
        }

    /**
     * Neueste Benachrichtigung, transient sichtbar (≈[notificationVisibleMs]), dann ausgeklungen.
     * `transformLatest` verwirft das Ausblende-`delay`, sobald eine neue Benachrichtigung kommt.
     */
    @Suppress("OPT_IN_USAGE")
    private val notificationFlow: Flow<IslandContent.Notification?> =
        notifications.activeNotificationDetails
            .map { list -> list.maxByOrNull { it.postTimeMs } }
            .distinctUntilChanged { a, b -> a?.pkg == b?.pkg && a?.postTimeMs == b?.postTimeMs }
            .transformLatest { info ->
                if (info == null) {
                    emit(null)
                } else {
                    emit(
                        IslandContent.Notification(
                            pkg = info.pkg,
                            title = info.title,
                            text = info.text,
                            actions = info.actions,
                            contentIntent = info.contentIntent
                        )
                    )
                    delay(notificationVisibleMs)
                    emit(null)
                }
            }

    /**
     * Lade-Status, der nur transient (≈[batteryVisibleMs]) nach An-/Abstecken erscheint.
     */
    @Suppress("OPT_IN_USAGE")
    private val batteryFlow: Flow<IslandContent.Battery?> = callbackFlow {
        trySend(null)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val charging = intent?.action == Intent.ACTION_POWER_CONNECTED
                trySend(IslandContent.Battery(level = readBatteryLevel(), charging = charging))
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        appContext.registerReceiver(receiver, filter)
        awaitClose { appContext.unregisterReceiver(receiver) }
    }.transformLatest { battery ->
        emit(battery)
        if (battery != null) {
            delay(batteryVisibleMs)
            emit(null)
        }
    }

    /** Aktive Medienwiedergabe aus der MediaSession (oder `null`). */
    private val mediaFlow: Flow<IslandContent.Media?> =
        notifications.activeMedia.map { media ->
            media?.let {
                IslandContent.Media(it.title, it.artist, it.isPlaying, it.art, it.packageName, it.progress)
            }
        }

    /** Vom Nutzer gewählte „Haupt"-Aktivität (activityId) oder `null` = höchste Priorität. */
    private val _selectedId = MutableStateFlow<String?>(null)

    fun mediaPlayPause() = notifications.mediaPlayPause()
    fun mediaNext() = notifications.mediaNext()
    fun mediaPrevious() = notifications.mediaPrevious()
    fun mediaSeekTo(positionMs: Long) = notifications.mediaSeekTo(positionMs)

    /** Alle aktiven Aktivitäten, prioritätssortiert. */
    private val contentsFlow: Flow<List<IslandContent>> =
        combine(mediaFlow, timerFlow, notificationFlow, batteryFlow) { media, timer, notification, battery ->
            orderedIslandContents(
                IslandSources(media = media, timer = timer, notification = notification, battery = battery)
            )
        }

    /** Zusammengeführter, prozessweiter Insel-Zustand (main = gewählte oder höchste Priorität). */
    val state: StateFlow<IslandState> = combine(
        contentsFlow,
        _expandedContent,
        _selectedId
    ) { all, expanded, selectedId ->
        val main = all.firstOrNull { activityId(it) == selectedId } ?: all.firstOrNull()
        IslandState(content = main, expanded = expanded != null, all = all)
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = IslandState()
    )

    /** Wählt die angegebene Aktivität als „Haupt" (für Karten-Switcher). */
    fun selectActivity(content: IslandContent) {
        _selectedId.value = activityId(content)
    }

    /** Zyklisch zur nächsten aktiven Aktivität wechseln (Swipe). */
    fun selectNext() = step(1)

    /** Zyklisch zur vorherigen aktiven Aktivität wechseln (Swipe). */
    fun selectPrevious() = step(-1)

    private fun step(direction: Int) {
        val all = state.value.all
        if (all.size < 2) return
        val currentId = activityId(state.value.content ?: all.first())
        val idx = all.indexOfFirst { activityId(it) == currentId }.coerceAtLeast(0)
        val next = all[((idx + direction) % all.size + all.size) % all.size]
        _selectedId.value = activityId(next)
    }

    /** Klappt die Karte mit dem angegebenen Inhalt auf (friert ihn ein, kein Auto-Dismiss). */
    fun expand(content: IslandContent) {
        _expandedContent.value = content
    }

    /** Schließt die aufgeklappte Karte. */
    fun dismissExpanded() {
        _expandedContent.value = null
    }

    private fun readBatteryLevel(): Int {
        val bm = appContext.getSystemService(BatteryManager::class.java)
        return bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
    }
}
