package com.example.androidlauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
     * Leert den Icon-Cache, sobald die eigene App neu installiert/aktualisiert
     * wurde. So werden nach einem App-Update (z.B. geändertes Launcher-Icon)
     * keine veralteten Icon-Bitmaps mehr aus dem Cache geladen.
     */
    suspend fun invalidateCacheOnAppUpdate() = withContext(Dispatchers.IO) {
        try {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val token = "${pkgInfo.versionCode}:${pkgInfo.lastUpdateTime}"
            val prefs = context.getSharedPreferences("icon_cache_meta", Context.MODE_PRIVATE)
            if (prefs.getString("app_build_token", null) != token) {
                iconCacheDir.listFiles()?.forEach { it.delete() }
                prefs.edit().putString("app_build_token", token).apply()
            }
        } catch (_: Exception) {
            // Fehler hier sind unkritisch – im Zweifel bleibt der Cache bestehen.
        }
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
        // Rethrone selbst nur ausblenden, wenn es der aktive Standard-Launcher ist.
        // (Ist Rethrone nicht Standard, bleibt es startbar in der Liste.)
        // Zum Deinstallieren wird die eigene App separat im UninstallAppsMenu eingeblendet.
        val rawApps = pm.queryIntentActivities(intent, 0)
            .map { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                AppInfo(
                    label = resolveInfo.loadLabel(pm).toString(),
                    packageName = packageName,
                    launchIntent = pm.getLaunchIntentForPackage(packageName)
                )
            }
        // Aufbereitung (dedup/filter/sort) liegt framework-frei in LauncherLogic und ist dort
        // ohne PackageManager-Mocking getestet.
        com.example.androidlauncher.LauncherLogic.normalizeInstalledApps(
            rawApps = rawApps,
            ownPackage = context.packageName,
            isOwnDefaultLauncher = isDefaultLauncher(),
        )
    }

    /**
     * Prüft, ob Rethrone aktuell der vom System gesetzte Standard-Launcher ist.
     */
    fun isDefaultLauncher(): Boolean {
        return try {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val defaultLauncher = context.packageManager
                .resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
            defaultLauncher == context.packageName
        } catch (_: Exception) {
            false
        }
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
            autoFallback = IconQualityEvaluator.evaluate(
                bitmap = bitmap,
                packageName = app.packageName,
                label = app.label,
                explicitRule = app.autoIconRule
            )
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
