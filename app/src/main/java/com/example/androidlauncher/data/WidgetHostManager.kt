package com.example.androidlauncher.data

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import androidx.compose.ui.geometry.Offset
import com.example.androidlauncher.data.settings.HomeLayoutSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Besitzt den prozessweiten [AppWidgetHost] fuer gehostete System-Widgets (B1):
 * ID-Allokation/-Freigabe, Bind-Versuche, Lifecycle (start/stopListening – von der
 * MainActivity an ON_START/ON_STOP gebunden), View-Cache sowie die Persistenz der
 * platzierten Widgets ueber [HomeLayoutSettings].
 *
 * System-Aufrufe sind in `runCatching` gekapselt: einzelne OEMs werfen aus
 * AppWidget-APIs SecurityExceptions, die hier als "nicht moeglich" behandelt werden.
 */
class WidgetHostManager(
    private val appContext: Context,
    private val host: AppWidgetHost,
    private val appWidgetManager: AppWidgetManager,
    private val settings: HomeLayoutSettings,
) {

    /** Produktiv-Konstruktor; der Test-Konstruktor injiziert Host/Manager/Settings direkt. */
    constructor(context: Context, settings: HomeLayoutSettings) : this(
        context.applicationContext,
        AppWidgetHost(context.applicationContext, WIDGET_HOST_ID),
        AppWidgetManager.getInstance(context.applicationContext),
        settings,
    )

    companion object {
        /** Feste Host-ID: muss nur ueber App-Starts stabil sein (Namensraum ist per-Package). */
        const val WIDGET_HOST_ID = 0x5254
    }

    /**
     * Cache der erzeugten Host-Views (eine pro Widget-ID): Recomposition und
     * Edit-Mode-Toggles duerfen die View nie neu erzeugen, sonst verliert das
     * Widget seinen internen Zustand (z. B. Scroll-Position).
     */
    private val hostViews = mutableMapOf<Int, AppWidgetHostView>()

    /** Persistierte, platzierte Widgets. */
    val widgets: Flow<List<HostedWidget>> get() = settings.hostedWidgets

    fun allocateWidgetId(): Int = host.allocateAppWidgetId()

    /** `true`, wenn der Bind ohne User-Consent gelang; Exceptions zaehlen als "nicht gebunden". */
    fun bindIfAllowed(appWidgetId: Int, provider: ComponentName): Boolean =
        runCatching { appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider) }
            .getOrDefault(false)

    /** Gibt die ID im Host frei und verwirft eine evtl. gecachte View. */
    fun deleteWidgetId(appWidgetId: Int) {
        hostViews.remove(appWidgetId)
        runCatching { host.deleteAppWidgetId(appWidgetId) }
    }

    fun startListening() {
        runCatching { host.startListening() }
    }

    fun stopListening() {
        runCatching { host.stopListening() }
    }

    fun providerInfo(appWidgetId: Int): AppWidgetProviderInfo? =
        runCatching { appWidgetManager.getAppWidgetInfo(appWidgetId) }.getOrNull()

    /**
     * Startet die Configure-Activity des Providers ueber den Host – die einzige auf
     * Android 12+ zulaessige Variante fuer nicht-exportierte Configure-Activities
     * (der Host vergibt eine temporaere Start-Berechtigung). `false` bei Startfehlern
     * (ActivityNotFound/SecurityException), der Aufrufer bricht den Flow dann ab.
     */
    fun startConfigureActivity(activity: Activity, appWidgetId: Int, requestCode: Int): Boolean =
        runCatching {
            host.startAppWidgetConfigureActivityForResult(activity, appWidgetId, 0, requestCode, null)
            true
        }.getOrDefault(false)

    /**
     * Liefert die (gecachte) Host-View fuer ein gebundenes Widget oder `null`,
     * wenn der Provider nicht mehr existiert. Erzeugt bewusst mit dem
     * Application-Context, damit keine Activity geleakt wird.
     */
    fun createView(appWidgetId: Int): AppWidgetHostView? {
        hostViews[appWidgetId]?.let { return it }
        val info = providerInfo(appWidgetId) ?: return null
        return runCatching { host.createView(appContext, appWidgetId, info) }
            .getOrNull()
            ?.also { hostViews[appWidgetId] = it }
    }

    /** Haengt ein fertig gebundenes/konfiguriertes Widget an den persistierten Bestand an. */
    suspend fun addWidget(widget: HostedWidget) {
        settings.setHostedWidgets(settings.hostedWidgets.first() + widget)
    }

    /** Entfernt ein Widget vollstaendig: Host-ID freigeben + aus der Persistenz loeschen. */
    suspend fun removeWidget(appWidgetId: Int) {
        deleteWidgetId(appWidgetId)
        settings.setHostedWidgets(
            settings.hostedWidgets.first().filterNot { it.appWidgetId == appWidgetId }
        )
    }

    /** Persistiert neue Edit-Mode-Offsets (ein atomarer Write fuer alle Widgets). */
    suspend fun updateOffsets(offsets: Map<Int, Offset>) {
        updateWidgetPlacement(offsets, emptyMap())
    }

    /**
     * Persistiert Offsets und Groessen aus dem Edit-Modus in einem einzigen
     * atomaren Write (B1-PR4: der Speichern-Knopf committet beides gemeinsam).
     */
    suspend fun updateWidgetPlacement(offsets: Map<Int, Offset>, sizes: Map<Int, WidgetSizeDp>) {
        if (offsets.isEmpty() && sizes.isEmpty()) return
        val updated = settings.hostedWidgets.first().map { widget ->
            var result = widget
            offsets[widget.appWidgetId]?.let { result = result.copy(offsetX = it.x, offsetY = it.y) }
            sizes[widget.appWidgetId]?.let { result = result.copy(widthDp = it.widthDp, heightDp = it.heightDp) }
            result
        }
        settings.setHostedWidgets(updated)
    }

    /**
     * Stellt Widgets aus einem Backup wieder her (U2): pro geplantem Eintrag wird eine
     * frische ID allokiert und still gebunden. Bewusst KEIN `ACTION_APPWIDGET_BIND`-
     * Consent-Dialog und keine Configure-Activity pro Widget – eine Dialog-Kaskade
     * direkt nach dem Import waere schlechtere UX; als Default-Launcher hat die App
     * i. d. R. die Bind-Permission. Verweigerte/kaputte Eintraege fallen still weg.
     * Alle Erfolge werden in EINEM atomaren Write angehaengt.
     */
    suspend fun restoreWidgets(
        backups: List<com.example.androidlauncher.data.backup.WidgetBackup>,
        resolveComponent: (String) -> ComponentName? = { ComponentName.unflattenFromString(it) },
    ): WidgetRestoreSummary {
        val existing = settings.hostedWidgets.first()
        val restored = mutableListOf<HostedWidget>()
        for (backup in planWidgetRestore(backups, existing)) {
            val component = resolveComponent(backup.provider) ?: continue
            val appWidgetId = allocateWidgetId()
            if (bindIfAllowed(appWidgetId, component)) {
                restored += HostedWidget(
                    appWidgetId = appWidgetId,
                    provider = backup.provider,
                    widthDp = backup.widthDp,
                    heightDp = backup.heightDp,
                    offsetX = backup.offsetX,
                    offsetY = backup.offsetY,
                )
            } else {
                deleteWidgetId(appWidgetId)
            }
        }
        if (restored.isNotEmpty()) {
            settings.setHostedWidgets(existing + restored)
        }
        return WidgetRestoreSummary(restored = restored.size, skipped = backups.size - restored.size)
    }

    /**
     * Raeumt beim Start Leichen auf (siehe [resolveWidgetCleanup]): geleakte IDs aus
     * abgebrochenen Bind-Flows und Widgets deinstallierter Provider.
     */
    suspend fun cleanupOrphans() {
        val persisted = settings.hostedWidgets.first()
        val allocated = runCatching { host.appWidgetIds.toList() }.getOrDefault(emptyList())
        val cleanup = resolveWidgetCleanup(
            persistedIds = persisted.map { it.appWidgetId },
            allocatedIds = allocated,
            infoExists = { providerInfo(it) != null },
        )
        cleanup.idsToDelete.forEach(::deleteWidgetId)
        if (cleanup.idsToRemoveFromPersistence.isNotEmpty()) {
            settings.setHostedWidgets(
                persisted.filterNot { it.appWidgetId in cleanup.idsToRemoveFromPersistence }
            )
        }
    }
}
