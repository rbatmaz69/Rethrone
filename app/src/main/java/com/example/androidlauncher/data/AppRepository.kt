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
class AppRepository(
    private val context: Context,
    // B4: optional, damit UI-Adhoc-Instanzen (UninstallAppsMenu) und Tests ohne
    // Icon-Pack-Unterstützung weiter funktionieren.
    private val iconPackRepository: IconPackRepository? = null,
) {

    /** Cache-Verzeichnis für App-Icons im permanenten Speicher. */
    private val iconCacheDir: File by lazy {
        File(context.filesDir, "app_icons").also { if (!it.exists()) it.mkdirs() }
    }

    /**
     * Leert den Icon-Cache, sobald die eigene App neu installiert/aktualisiert
     * wurde. So werden nach einem App-Update (z.B. geändertes Launcher-Icon)
     * keine veralteten Icon-Bitmaps mehr aus dem Cache geladen.
     *
     * Der Build-Token liegt als Marker-Datei direkt im Cache-Verzeichnis –
     * dieselbe Speicherstrategie wie der Cache selbst (statt SharedPreferences).
     */
    suspend fun invalidateCacheOnAppUpdate() = withContext(Dispatchers.IO) {
        try {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)

            @Suppress("DEPRECATION")
            val token = "${pkgInfo.versionCode}:${pkgInfo.lastUpdateTime}"
            val tokenFile = File(iconCacheDir, BUILD_TOKEN_FILE)
            val storedToken = if (tokenFile.exists()) tokenFile.readText() else migrateLegacyBuildToken()
            if (storedToken != token) {
                iconCacheDir.listFiles()?.forEach { it.delete() }
            }
            // Immer schreiben, damit auch nach der Legacy-Migration (Prefs bereits
            // geleert) der Token beim nächsten Start aus der Datei gelesen wird.
            tokenFile.writeText(token)
        } catch (_: Exception) {
            // Fehler hier sind unkritisch – im Zweifel bleibt der Cache bestehen.
        }
    }

    /**
     * Liest den Token einmalig aus den früher genutzten SharedPreferences und
     * räumt sie auf, damit bestehende Installationen nicht unnötig den Cache leeren.
     */
    private fun migrateLegacyBuildToken(): String? {
        val prefs = context.getSharedPreferences(LEGACY_META_PREFS, Context.MODE_PRIVATE)
        val legacy = prefs.getString(LEGACY_TOKEN_KEY, null) ?: return null
        prefs.edit().clear().apply()
        return legacy
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
     * bei Cache-Miss vom PackageManager (bzw. bei gesetztem [iconPack] aus dem Pack).
     */
    suspend fun loadIcon(packageName: String, iconPack: String? = null): ImageBitmap? {
        val bitmap = loadBitmap(packageName, iconPack) ?: return null
        return bitmap.toPreparedImageBitmap()
    }

    /**
     * Lädt ein einzelnes App-Icon und berechnet die automatische Fallback-Entscheidung.
     *
     * Hat das gewählte Icon-Pack ein Mapping für die App, wird die Qualitäts-Heuristik
     * bewusst übersprungen (keep-original) – Pack-Grafiken sollen nie durch den
     * Initialen-Fallback ersetzt werden.
     */
    suspend fun loadResolvedIcon(app: AppInfo, iconPack: String? = null): LoadedAppIcon? {
        val usesPackIcon = iconPack != null &&
            iconPackRepository?.hasPackIcon(iconPack, app.packageName, launchComponentFlat(app.packageName)) == true
        val bitmap = loadBitmap(app.packageName, iconPack) ?: return null
        return LoadedAppIcon(
            imageBitmap = bitmap.toPreparedImageBitmap(),
            autoFallback = if (usesPackIcon) {
                AutoIconFallback(type = AutoIconFallbackType.ORIGINAL, reason = "icon_pack")
            } else {
                IconQualityEvaluator.evaluate(
                    bitmap = bitmap,
                    packageName = app.packageName,
                    label = app.label,
                    explicitRule = app.autoIconRule
                )
            }
        )
    }

    /**
     * Lädt Icons für eine App-Liste mit Priorisierung der Favoriten.
     */
    suspend fun loadIconsWithPriority(
        apps: List<AppInfo>,
        favoritePackages: List<String>,
        iconPack: String? = null,
        onIconLoaded: suspend (Int, LoadedAppIcon) -> Unit
    ) {
        val favSet = favoritePackages.toSet()
        val sortedIndices = apps.indices.sortedByDescending { apps[it].packageName in favSet }

        sortedIndices.forEachIndexed { loopIdx, appIdx ->
            if (appIdx >= apps.size) return@forEachIndexed
            val app = apps[appIdx]
            val resolvedIcon = loadResolvedIcon(app, iconPack)
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

    private suspend fun loadBitmap(packageName: String, iconPack: String? = null): Bitmap? {
        val cached = loadBitmapFromCache(packageName)
        if (cached != null) return cached
        return loadBitmapFromSystem(packageName, iconPack)
    }

    /** Geflattete Launch-Activity-Component einer App (für das appfilter-Lookup). */
    private fun launchComponentFlat(packageName: String): String? = runCatching {
        context.packageManager.getLaunchIntentForPackage(packageName)?.component?.flattenToString()
    }.getOrNull()

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

    private suspend fun loadBitmapFromSystem(packageName: String, iconPack: String? = null): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                // B4: bei gewähltem Icon-Pack zuerst dessen Grafik versuchen; null → normaler
                // System-Icon-Pfad (Per-App-Fallback). Beide Quellen landen in derselben
                // "{pkg}.png"-Cache-Datei – sicher, weil ein Pack-Wechsel den Cache komplett leert.
                val packBitmap = if (iconPack != null) {
                    iconPackRepository?.loadIconBitmap(
                        packPackage = iconPack,
                        packageName = packageName,
                        componentFlat = launchComponentFlat(packageName),
                        sizePx = ICON_SIZE,
                    )
                } else {
                    null
                }

                val bitmap = packBitmap ?: run {
                    val pm = context.packageManager
                    val info = pm.getApplicationInfo(packageName, 0)
                    val icon = info.loadIcon(pm)

                    val foregroundDrawable = if (icon is AdaptiveIconDrawable) {
                        icon.foreground ?: icon
                    } else {
                        icon
                    }

                    createBitmap(ICON_SIZE, ICON_SIZE).applyCanvas {
                        foregroundDrawable.setBounds(0, 0, ICON_SIZE, ICON_SIZE)
                        foregroundDrawable.draw(this)
                    }
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

        // Marker-Datei im Icon-Cache; kollidiert nicht mit "$packageName.png"-Icons.
        private const val BUILD_TOKEN_FILE = ".build_token"

        // Frühere Ablage des Tokens – nur noch für die einmalige Migration gelesen.
        private const val LEGACY_META_PREFS = "icon_cache_meta"
        private const val LEGACY_TOKEN_KEY = "app_build_token"
    }
}
