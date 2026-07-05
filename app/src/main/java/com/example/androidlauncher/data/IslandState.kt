package com.example.androidlauncher.data

import android.app.PendingIntent
import android.graphics.Bitmap
import com.example.androidlauncher.MediaProgress

/**
 * Eine einzelne Aktion einer Benachrichtigung (z. B. „Antworten", „Markieren"),
 * die in der aufgeklappten Insel-Karte als Button erscheint.
 */
data class NotificationAction(
    val title: String,
    val intent: PendingIntent,
    /**
     * RemoteInputs der Aktion (z. B. WhatsApp „Antworten"). Nicht leer ⇒ Reply-Aktion: der
     * [intent] darf **nicht** bar gefeuert werden, sondern braucht einen mit RemoteInput-Ergebnissen
     * gefüllten Intent (siehe `sendNotificationReply`).
     */
    val remoteInputs: List<android.app.RemoteInput> = emptyList()
) {
    /** `true` ⇒ Aktion erwartet eine Texteingabe (Reply); zeigt in der Karte ein Inline-Feld. */
    val isReply: Boolean get() = remoteInputs.isNotEmpty()
}

/**
 * Inhalt, den die Dynamic Island gerade darstellt. Genau eine Variante ist aktiv;
 * welche gewinnt, entscheidet [resolveIslandContent] anhand einer festen Priorität.
 *
 * Es gibt **keinen** Ruhe-/Uhr-Inhalt mehr: ist nichts aktiv, liefert [resolveIslandContent]
 * `null` und die Insel wird gar nicht gezeichnet.
 */
sealed interface IslandContent {
    /** Aktive Medienwiedergabe (via MediaSession). Persistent solange Session aktiv. */
    data class Media(
        val title: String,
        val artist: String,
        val isPlaying: Boolean,
        val art: Bitmap? = null,
        val packageName: String = "",
        /** Fortschritt für die Seek-Leiste der Karte (`null` = keine Dauer bekannt). */
        val progress: MediaProgress? = null
    ) : IslandContent

    /**
     * Laufender Timer/Stoppuhr der Uhr-App. Persistent solange aktiv.
     * @param displayMs fertige Anzeigezeit (Rest- bzw. verstrichene Zeit), jede Sekunde neu
     *        berechnet; `null`, wenn keine Zeit ableitbar ist (dann zeigt die Pille kein Wort,
     *        sondern nur App-Icon + Punkt).
     * @param countUp `true` ⇒ hochzählende Stoppuhr, `false` ⇒ Countdown-Timer.
     * @param actions App-eigene Notification-Aktionen (Pause/Resume/…) zum Steuern in der Karte.
     */
    data class Timer(
        val label: String,
        val displayMs: Long?,
        val countUp: Boolean = false,
        val pkg: String = "",
        val contentIntent: PendingIntent? = null,
        val actions: List<NotificationAction> = emptyList(),
        /** `true` ⇒ Timer pausiert/gestoppt (steuert u. a. die Farbe des Pillen-Punkts). */
        val paused: Boolean = false
    ) : IslandContent

    /** Frisch eingetroffene Benachrichtigung. Transient (klingt nach kurzer Zeit aus). */
    data class Notification(
        val pkg: String,
        val title: String,
        val text: String,
        val actions: List<NotificationAction> = emptyList(),
        val contentIntent: PendingIntent? = null
    ) : IslandContent

    /** Lade-/Akkustatus. Transient (nur kurz nach An-/Abstecken). */
    data class Battery(
        val level: Int,
        val charging: Boolean
    ) : IslandContent
}

/**
 * Vollständiger Zustand der Insel.
 *
 * @param content `null` ⇒ die Insel rendert nichts (Leerlauf, vollständig verborgen).
 * @param expanded `true` ⇒ aufgeklappte Karte (z. B. nach Tap) wird angezeigt.
 */
data class IslandState(
    val content: IslandContent? = null,
    val expanded: Boolean = false,
    /** Alle aktiven Aktivitäten, prioritätssortiert (für Auto-Rotation/Umschalten). */
    val all: List<IslandContent> = emptyList()
)

/**
 * Rohdaten der einzelnen Quellen, die der [DynamicIslandManager] zusammenführt.
 * Bewusst als reine Datenklasse gehalten, damit [resolveIslandContent] ohne
 * Android-Abhängigkeiten unit-testbar bleibt.
 */
data class IslandSources(
    val media: IslandContent.Media? = null,
    val timer: IslandContent.Timer? = null,
    val notification: IslandContent.Notification? = null,
    val battery: IslandContent.Battery? = null
)

/**
 * Prioritäts-Logik der Insel:
 * **spielende** Medien > Timer/Stoppuhr > Benachrichtigung > **pausierte** Medien > Laden.
 * Pausierte Medien rutschen bewusst unter Timer/Benachrichtigung, damit sie eine laufende
 * Uhr-Aktivität nicht verdecken, bleiben aber im Leerlauf sichtbar (Fortsetzen via Karte).
 * Reine Funktion ⇒ direkt testbar. Gibt `null` zurück, wenn keine Quelle aktiv ist (Leerlauf).
 */
fun resolveIslandContent(sources: IslandSources): IslandContent? =
    orderedIslandContents(sources).firstOrNull()

/**
 * Alle aktiven Inhalte in Prioritätsreihenfolge (spielende Medien > Timer/Stoppuhr >
 * Benachrichtigung > pausierte Medien > Laden). Für Auto-Rotation/Umschalten der Insel.
 */
fun orderedIslandContents(sources: IslandSources): List<IslandContent> {
    val media = sources.media
    return buildList {
        if (media != null && media.isPlaying) add(media)
        sources.timer?.let { add(it) }
        sources.notification?.let { add(it) }
        if (media != null && !media.isPlaying) add(media)
        sources.battery?.let { add(it) }
    }
}

/**
 * Stabile Kennung einer Aktivität (Typ + Paket) zum Merken der Nutzer-Auswahl über
 * Listenänderungen hinweg.
 */
fun activityId(content: IslandContent): String = when (content) {
    is IslandContent.Media -> "media:${content.packageName}"
    is IslandContent.Timer -> "timer:${content.pkg}:${content.label}"
    is IslandContent.Notification -> "notif:${content.pkg}"
    is IslandContent.Battery -> "battery"
}

/** App-Paket einer Aktivität (für das App-Icon im Indikator-Kreis), oder `null`. */
fun iconPackage(content: IslandContent): String? = when (content) {
    is IslandContent.Media -> content.packageName.ifBlank { null }
    is IslandContent.Timer -> content.pkg.ifBlank { null }
    is IslandContent.Notification -> content.pkg.ifBlank { null }
    is IslandContent.Battery -> null
}
