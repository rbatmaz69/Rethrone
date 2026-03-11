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

    /** Cache-Verzeichnis für App-Icons im permanenten Speicher. */
    private val iconCacheDir: File by lazy {
        File(context.filesDir, "app_icons").also { if (!it.exists()) it.mkdirs() }
    }

    /**
     * Bereinigt veraltete Cache-Verzeichnisse (z.B. aus dem temporary cacheDir).
     */
    fun cleanupLegacyCache() {
        try {
            val legacyDir = File(context.cacheDir, "app_icons")
            if (legacyDir.exists()) {
                legacyDir.deleteRecursively()
            }
        } catch (_: Exception) {}
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
     */
    suspend fun loadIcon(packageName: String): ImageBitmap? {
        val bitmap = loadBitmap(packageName) ?: return null
        return bitmap.toPreparedImageBitmap()
    }

    /**
     * Lädt ein einzelnes App-Icon und berechnet die automatische Fallback-Entscheidung.
     */
    suspend fun loadResolvedIcon(app: AppInfo): LoadedAppIcon? {
        val bitmap = loadBitmap(app.packageName) ?: return null
        return LoadedAppIcon(
            imageBitmap = bitmap.toPreparedImageBitmap(),
            autoFallback = IconQualityEvaluator.evaluate(bitmap, app.packageName, app.label)
        )
    }

    /**
     * Lädt Icons für eine App-Liste mit Priorisierung der Favoriten.
     */
    suspend fun loadIconsWithPriority(
        apps: List<AppInfo>,
        favoritePackages: List<String>,
        onIconLoaded: suspend (Int, LoadedAppIcon) -> Unit
    ) {
        val favSet = favoritePackages.toSet()
        val sortedIndices = apps.indices.sortedByDescending { apps[it].packageName in favSet }

        sortedIndices.forEachIndexed { loopIdx, appIdx ->
            if (appIdx >= apps.size) return@forEachIndexed
            val app = apps[appIdx]
            val resolvedIcon = loadResolvedIcon(app)
            if (resolvedIcon != null) {
                onIconLoaded(appIdx, resolvedIcon)
            }
            if (loopIdx % 5 == 0) delay(1)
        }
    }

    /**
     * Löscht den Icon-Cache komplett.
     */
    suspend fun clearIconCache() = withContext(Dispatchers.IO) {
        iconCacheDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Löscht den Icon-Cache für genau ein Paket.
     */
    suspend fun clearIconCache(packageName: String) = withContext(Dispatchers.IO) {
        File(iconCacheDir, "$packageName.png").takeIf { it.exists() }?.delete()
    }

    // ── Private Hilfsmethoden ────────────────────────────────────────

    private suspend fun loadBitmap(packageName: String): Bitmap? {
        val cached = loadBitmapFromCache(packageName)
        if (cached != null) return cached
        return loadBitmapFromSystem(packageName)
    }

    private suspend fun loadBitmapFromCache(packageName: String): Bitmap? =
        withContext(Dispatchers.IO) {
            val iconFile = File(iconCacheDir, "$packageName.png")
            if (!iconFile.exists()) return@withContext null
            try {
                BitmapFactory.decodeFile(iconFile.absolutePath)
            } catch (_: Exception) {
                null
            }
        }

    private suspend fun loadBitmapFromSystem(packageName: String): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val info = pm.getApplicationInfo(packageName, 0)
                val icon = info.loadIcon(pm)

                val foregroundDrawable = if (icon is AdaptiveIconDrawable) {
                    icon.foreground ?: icon
                } else {
                    icon
                }

                val bitmap = createBitmap(ICON_SIZE, ICON_SIZE)
                bitmap.applyCanvas {
                    foregroundDrawable.setBounds(0, 0, ICON_SIZE, ICON_SIZE)
                    foregroundDrawable.draw(this)
                }

                try {
                    FileOutputStream(File(iconCacheDir, "$packageName.png")).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                } catch (_: Exception) {
                    // Cache-Fehler sind nicht kritisch
                }

                bitmap
            } catch (_: Exception) {
                null
            }
        }

    private fun Bitmap.toPreparedImageBitmap(): ImageBitmap {
        val imageBitmap = asImageBitmap()
        imageBitmap.prepareToDraw()
        return imageBitmap
    }

    data class LoadedAppIcon(
        val imageBitmap: ImageBitmap,
        val autoFallback: AutoIconFallback
    )

    companion object {
        private const val ICON_SIZE = 144
    }
}
