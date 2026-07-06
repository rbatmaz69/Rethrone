package com.example.androidlauncher.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParserFactory

/** Ein installiertes Icon-Pack (Auswahl-Eintrag im Icon-Menü). */
data class IconPack(
    val packageName: String,
    val label: String,
)

/**
 * Zugriff auf installierte Icon-Packs (B4): Discovery über die ADW-/Nova-Theme-Intents,
 * Laden + Parsen der `appfilter.xml` (einmal pro Prozess und Pack, Mutex-gecacht) und
 * Aufloesen einzelner Pack-Drawables zu Bitmaps. Alle System-Zugriffe sind defensiv in
 * `runCatching` gekapselt – ein fehlendes/kaputtes Pack degradiert zu System-Icons.
 */
class IconPackRepository(private val context: Context) {

    private val appFilterCache = mutableMapOf<String, Map<String, String>>()
    private val cacheMutex = Mutex()

    /** Installierte Icon-Packs, dedupliziert per Package und label-sortiert. */
    suspend fun installedIconPacks(): List<IconPack> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        THEME_INTENT_ACTIONS
            .flatMap { action ->
                runCatching { pm.queryIntentActivities(Intent(action), 0) }
                    .getOrDefault(emptyList())
            }
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                val label = runCatching { resolveInfo.loadLabel(pm)?.toString() }.getOrNull()
                IconPack(packageName = packageName, label = label ?: packageName)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * Geparster appfilter-Inhalt (Component → Drawable-Name) eines Packs; leere Map,
     * wenn das Pack fehlt oder keinen appfilter mitbringt. Der Mutex verhindert
     * Doppel-Parsen unter parallelen Icon-Loads.
     */
    suspend fun loadAppFilter(packPackage: String): Map<String, String> = cacheMutex.withLock {
        appFilterCache[packPackage]?.let { return@withLock it }
        val parsed = withContext(Dispatchers.IO) { parseAppFilter(packPackage) }
        appFilterCache[packPackage] = parsed
        parsed
    }

    /** Ob das Pack fuer diese App ein Mapping hat (steuert den Qualitaets-Heuristik-Skip). */
    suspend fun hasPackIcon(packPackage: String, packageName: String, componentFlat: String?): Boolean =
        IconPackAppFilterParser.resolveDrawableName(
            loadAppFilter(packPackage),
            packageName,
            componentFlat,
        ) != null

    /**
     * Laedt das Pack-Drawable einer App als quadratische Bitmap. Pack-Icons sind finale
     * Grafik – bewusst KEINE AdaptiveIcon-Foreground-Extraktion. `null` bei fehlendem
     * Mapping, fehlendem Drawable oder jedem System-Fehler (Aufrufer faellt auf das
     * System-Icon zurueck).
     */
    suspend fun loadIconBitmap(
        packPackage: String,
        packageName: String,
        componentFlat: String?,
        sizePx: Int,
    ): Bitmap? {
        val drawableName = IconPackAppFilterParser.resolveDrawableName(
            loadAppFilter(packPackage),
            packageName,
            componentFlat,
        ) ?: return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val resources = context.packageManager.getResourcesForApplication(packPackage)
                val drawableId = resources.getIdentifier(drawableName, RES_TYPE_DRAWABLE, packPackage)
                if (drawableId == 0) return@runCatching null
                val drawable = ResourcesCompat.getDrawable(resources, drawableId, null)
                    ?: return@runCatching null
                createBitmap(sizePx, sizePx).applyCanvas {
                    drawable.setBounds(0, 0, sizePx, sizePx)
                    drawable.draw(this)
                }
            }.getOrNull()
        }
    }

    /** Verwirft den Parse-Cache (Pack-Wechsel, -Update oder -Deinstallation). */
    suspend fun invalidate(packPackage: String? = null) = cacheMutex.withLock {
        if (packPackage == null) {
            appFilterCache.clear()
        } else {
            appFilterCache.remove(packPackage)
        }
    }

    private fun parseAppFilter(packPackage: String): Map<String, String> = runCatching {
        val resources = context.packageManager.getResourcesForApplication(packPackage)
        val xmlId = resources.getIdentifier(APPFILTER_NAME, RES_TYPE_XML, packPackage)
        if (xmlId != 0) {
            val parser = resources.getXml(xmlId)
            try {
                IconPackAppFilterParser.parse(parser)
            } finally {
                parser.close()
            }
        } else {
            // Manche Packs liefern den appfilter nur als Asset aus.
            resources.assets.open(APPFILTER_ASSET).use { stream ->
                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setInput(stream, null)
                IconPackAppFilterParser.parse(parser)
            }
        }
    }.getOrDefault(emptyMap())

    companion object {
        // Discovery-Intents der beiden verbreiteten Icon-Pack-Konventionen.
        private val THEME_INTENT_ACTIONS = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
        )

        private const val APPFILTER_NAME = "appfilter"
        private const val APPFILTER_ASSET = "appfilter.xml"
        private const val RES_TYPE_XML = "xml"
        private const val RES_TYPE_DRAWABLE = "drawable"
    }
}
