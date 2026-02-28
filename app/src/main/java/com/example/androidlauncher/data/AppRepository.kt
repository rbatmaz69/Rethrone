package com.example.androidlauncher.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Repository für das Laden, Cachen und Bereitstellen der installierten Apps.
 *
 * Kapselt die gesamte Logik zum:
 * - Abfragen der installierten Apps vom PackageManager
 * - Laden und Cachen von App-Icons auf dem Dateisystem
 * - Priorisiertes Laden von Favoriten-Icons
 *
 * Durch die Auslagerung aus der Activity wird die Logik testbar
 * und die Activity schlanker.
 */
class AppRepository(private val context: Context) {

    /** Cache-Verzeichnis für App-Icons. */
    private val iconCacheDir: File by lazy {
        File(context.cacheDir, "app_icons").also { if (!it.exists()) it.mkdirs() }
    }

    /**
     * Lädt die Basis-Liste aller installierten Apps (ohne Icons).
     * Wird auf dem IO-Dispatcher ausgeführt.
     *
     * @return Alphabetisch sortierte Liste von [AppInfo]-Objekten.
     */
    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        pm.queryIntentActivities(intent, 0)
            .map { resolveInfo ->
                AppInfo(
                    label = resolveInfo.loadLabel(pm).toString(),
                    packageName = resolveInfo.activityInfo.packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * Lädt ein einzelnes App-Icon – zuerst aus dem Datei-Cache,
     * bei Cache-Miss vom PackageManager.
     *
     * @param packageName Der Paketname der App.
     * @return Das Icon als [ImageBitmap] oder null bei Fehler.
     */
    suspend fun loadIcon(packageName: String): ImageBitmap? {
        // 1. Cache prüfen
        val cached = loadIconFromCache(packageName)
        if (cached != null) return cached

        // 2. Vom System laden und cachen
        return loadIconFromSystem(packageName)
    }

    /**
     * Lädt Icons für eine App-Liste mit Priorisierung der Favoriten.
     * Favoriten-Icons werden zuerst geladen, damit sie sofort sichtbar sind.
     *
     * @param apps Die Ziel-Liste der Apps (wird in-place aktualisiert via Callback).
     * @param favoritePackages Paketnamen der Favoriten (werden priorisiert).
     * @param onIconLoaded Callback wenn ein Icon geladen wurde (Index + Bitmap).
     */
    suspend fun loadIconsWithPriority(
        apps: List<AppInfo>,
        favoritePackages: List<String>,
        onIconLoaded: suspend (Int, ImageBitmap) -> Unit
    ) {
        val favSet = favoritePackages.toSet()

        // Indizes nach Priorität sortieren: Favoriten zuerst
        val sortedIndices = apps.indices.sortedByDescending { apps[it].packageName in favSet }

        sortedIndices.forEachIndexed { loopIdx, appIdx ->
            if (appIdx >= apps.size) return@forEachIndexed
            val app = apps[appIdx]
            val bitmap = loadIcon(app.packageName)
            if (bitmap != null) {
                onIconLoaded(appIdx, bitmap)
            }
            // Periodisch yielden um UI-Thread nicht zu blockieren
            if (loopIdx % 5 == 0) delay(1)
        }
    }

    /**
     * Löscht den Icon-Cache komplett.
     * Nützlich wenn Icons nach App-Updates veraltet sind.
     */
    suspend fun clearIconCache() = withContext(Dispatchers.IO) {
        iconCacheDir.listFiles()?.forEach { it.delete() }
    }

    // ── Private Hilfsmethoden ────────────────────────────────────────

    private suspend fun loadIconFromCache(packageName: String): ImageBitmap? =
        withContext(Dispatchers.IO) {
            val iconFile = File(iconCacheDir, "$packageName.png")
            if (!iconFile.exists()) return@withContext null
            try {
                val b = BitmapFactory.decodeFile(iconFile.absolutePath) ?: return@withContext null
                val ib = b.asImageBitmap()
                ib.prepareToDraw()
                ib
            } catch (_: Exception) {
                null
            }
        }

    private suspend fun loadIconFromSystem(packageName: String): ImageBitmap? =
        withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val info = pm.getApplicationInfo(packageName, 0)
                val icon = info.loadIcon(pm)

                // Bei AdaptiveIcons nur den Vordergrund verwenden
                val foregroundDrawable = if (icon is AdaptiveIconDrawable) {
                    icon.foreground ?: icon
                } else {
                    icon
                }

                val b = createBitmap(ICON_SIZE, ICON_SIZE)
                b.applyCanvas {
                    foregroundDrawable.setBounds(0, 0, ICON_SIZE, ICON_SIZE)
                    foregroundDrawable.draw(this)
                }

                val ib = b.asImageBitmap()
                ib.prepareToDraw()

                // Im Cache speichern
                try {
                    FileOutputStream(File(iconCacheDir, "$packageName.png")).use { out ->
                        b.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                } catch (_: Exception) {
                    // Cache-Fehler sind nicht kritisch
                }

                ib
            } catch (_: Exception) {
                null
            }
        }

    companion object {
        /** Größe der gecachten Icons in Pixeln. */
        private const val ICON_SIZE = 144
    }
}


