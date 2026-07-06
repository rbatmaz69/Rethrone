package com.example.androidlauncher.data

/**
 * Framework-freie Entscheidungslogik rund um das Binden und Aufraeumen gehosteter
 * System-Widgets (B1). Die Seiteneffekte (AppWidgetHost-/AppWidgetManager-Aufrufe,
 * Activity-Ergebnisse) liegen in [WidgetHostManager] bzw. der MainActivity – hier
 * werden nur IDs und Booleans in den naechsten Schritt uebersetzt, damit der
 * komplette Flow als Unit-Test abgedeckt werden kann.
 */

/** Ein laufender Bind-Vorgang: ID ist bereits allokiert, aber noch nicht persistiert. */
data class PendingWidgetBind(
    val appWidgetId: Int,
    /** Provider als `ComponentName.flattenToString()`. */
    val provider: String,
    /** Ob der Provider eine Configure-Activity deklariert (`AppWidgetProviderInfo.configure`). */
    val needsConfigure: Boolean,
)

/** Naechster Schritt im Bind-Flow. */
sealed interface WidgetBindStep {
    /** Bind wurde verweigert – System-Consent-Dialog (`ACTION_APPWIDGET_BIND`) starten. */
    data class RequestBindPermission(val pending: PendingWidgetBind) : WidgetBindStep

    /** Configure-Activity des Providers starten (via `startAppWidgetConfigureActivityForResult`). */
    data class LaunchConfigure(val pending: PendingWidgetBind) : WidgetBindStep

    /** Widget persistieren und anzeigen – der Flow ist erfolgreich abgeschlossen. */
    data class Commit(val pending: PendingWidgetBind) : WidgetBindStep

    /** Abbruch: die allokierte ID muss wieder freigegeben werden (`deleteAppWidgetId`). */
    data class Abort(val appWidgetId: Int) : WidgetBindStep
}

/** Zustandsmaschine des Bind-/Configure-Flows – ein Schritt pro Activity-Ergebnis. */
object WidgetBindFlow {

    /** Nach `allocateAppWidgetId` + `bindAppWidgetIdIfAllowed`. */
    fun afterAllocate(pending: PendingWidgetBind, bindSucceeded: Boolean): WidgetBindStep = when {
        !bindSucceeded -> WidgetBindStep.RequestBindPermission(pending)
        pending.needsConfigure -> WidgetBindStep.LaunchConfigure(pending)
        else -> WidgetBindStep.Commit(pending)
    }

    /** Nach dem System-Consent-Dialog (`ACTION_APPWIDGET_BIND`). */
    fun afterBindPermission(pending: PendingWidgetBind, granted: Boolean): WidgetBindStep = when {
        !granted -> WidgetBindStep.Abort(pending.appWidgetId)
        pending.needsConfigure -> WidgetBindStep.LaunchConfigure(pending)
        else -> WidgetBindStep.Commit(pending)
    }

    /** Nach der Configure-Activity des Providers. */
    fun afterConfigure(pending: PendingWidgetBind, resultOk: Boolean): WidgetBindStep =
        if (resultOk) WidgetBindStep.Commit(pending) else WidgetBindStep.Abort(pending.appWidgetId)
}

/**
 * Ergebnis des Orphan-Cleanups beim Start: [idsToDelete] werden im Host freigegeben,
 * [idsToRemoveFromPersistence] zusaetzlich aus dem persistierten Bestand entfernt.
 */
data class WidgetCleanup(
    val idsToDelete: List<Int>,
    val idsToRemoveFromPersistence: List<Int>,
)

/**
 * Gleicht persistierte Widgets mit den im Host allokierten IDs ab:
 * - allokiert, aber nicht persistiert → geleakte ID (Prozess-Tod mitten im Bind-Flow) → freigeben;
 * - persistiert, aber Provider existiert nicht mehr (deinstalliert) → freigeben + aus Bestand entfernen.
 */
fun resolveWidgetCleanup(
    persistedIds: List<Int>,
    allocatedIds: List<Int>,
    infoExists: (Int) -> Boolean,
): WidgetCleanup {
    val leaked = allocatedIds.filterNot { it in persistedIds }
    val stale = persistedIds.filterNot(infoExists)
    return WidgetCleanup(
        idsToDelete = leaked + stale,
        idsToRemoveFromPersistence = stale,
    )
}

/** Anzeigegroesse eines gehosteten Widgets in dp. */
data class WidgetSizeDp(val widthDp: Int, val heightDp: Int)

/** Rastergroesse einer Launcher-Zelle, mit der `targetCellWidth/Height` multipliziert wird. */
private const val WIDGET_CELL_SIZE_DP = 70

/** Untergrenzen, damit auch "0dp"-Provider-Angaben ein greifbares Widget ergeben. */
private const val MIN_WIDGET_WIDTH_DP = 110
private const val MIN_WIDGET_HEIGHT_DP = 40

/** Seitenrand, den ein Widget in der Breite nie ueberdecken darf. */
private const val SCREEN_EDGE_MARGIN_DP = 48

/** Hoehen-Kappung, damit ein einzelnes Widget nicht den ganzen Screen einnimmt. */
private const val MAX_HEIGHT_SCREEN_FRACTION = 0.6f

/**
 * Default-Groesse beim Binden (MVP ohne Resize): bevorzugt die Zell-Angaben des Providers
 * (API 31+, `targetCellWidth/Height`, 0 = nicht gesetzt), sonst `minWidth/minHeight` –
 * geclampt auf sinnvolle Unter-/Obergrenzen relativ zur Screen-Groesse.
 */
fun defaultWidgetSizeDp(
    targetCellWidth: Int,
    targetCellHeight: Int,
    minWidthDp: Int,
    minHeightDp: Int,
    screenWidthDp: Int,
    screenHeightDp: Int,
): WidgetSizeDp {
    val rawWidth = if (targetCellWidth > 0) targetCellWidth * WIDGET_CELL_SIZE_DP else minWidthDp
    val rawHeight = if (targetCellHeight > 0) targetCellHeight * WIDGET_CELL_SIZE_DP else minHeightDp
    val maxWidth = (screenWidthDp - SCREEN_EDGE_MARGIN_DP).coerceAtLeast(MIN_WIDGET_WIDTH_DP)
    val maxHeight = (screenHeightDp * MAX_HEIGHT_SCREEN_FRACTION).toInt().coerceAtLeast(MIN_WIDGET_HEIGHT_DP)
    return WidgetSizeDp(
        widthDp = rawWidth.coerceIn(MIN_WIDGET_WIDTH_DP, maxWidth),
        heightDp = rawHeight.coerceIn(MIN_WIDGET_HEIGHT_DP, maxHeight),
    )
}
