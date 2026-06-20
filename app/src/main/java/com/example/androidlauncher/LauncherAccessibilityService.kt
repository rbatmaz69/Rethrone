package com.example.androidlauncher

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.example.androidlauncher.data.AppLockManager
import com.example.androidlauncher.data.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "A11yReturn"

/**
 * Accessibility Service zur Unterstützung von System-Gesten.
 * Wird primär verwendet, um den Bildschirm zu sperren, da dies für Launcher
 * ab Android 9 (API 28) die empfohlene Methode ohne Device-Admin-Rechte ist.
 */
class LauncherAccessibilityService : AccessibilityService() {

    // In-Memory-Cache der gesperrten Pakete, gefüllt aus dem DataStore-Flow.
    @Volatile
    private var lockedAppsCache: Set<String> = emptySet()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Beim Sperren des Geräts alle Entsperr-Sitzungen verwerfen.
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                AppLockManager.lockAll()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val themeManager = ThemeManager(applicationContext)
        serviceScope.launch {
            themeManager.lockedApps.collectLatest { lockedAppsCache = it }
        }
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val packageName = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
        Log.d(TAG, "event type=${event.eventType} package=$packageName")
        updateForegroundTracking(packageName)
        enforceAppLock(packageName)
    }

    /**
     * Prüft beim Vordergrundwechsel, ob das Paket gesperrt und noch nicht in der aktuellen
     * Sitzung entsperrt ist – falls ja, wird die [AppLockActivity] über die App gelegt.
     */
    private fun enforceAppLock(packageName: String) {
        val isTransient = isTransientSystemPackage(packageName)
        if (isTransient) return
        val alreadyUnlocked = AppLockManager.isUnlocked(packageName)
        // Nur das aktuelle Vordergrund-Paket bleibt entsperrt; verlassene Apps werden neu gesperrt.
        AppLockManager.retainOnly(packageName)
        val shouldLock = shouldShowLockScreen(
            packageName = packageName,
            ownPackage = applicationContext.packageName,
            lockedApps = lockedAppsCache,
            alreadyUnlocked = alreadyUnlocked,
            isTransient = isTransient
        )
        if (shouldLock) {
            val intent = Intent(this, AppLockActivity::class.java).apply {
                putExtra(AppLockActivity.EXTRA_PACKAGE, packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            runCatching { startActivity(intent) }
                .onFailure { Log.w(TAG, "AppLockActivity konnte nicht gestartet werden", it) }
        }
    }

    override fun onInterrupt() {
        // Nicht benötigt
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(screenOffReceiver) }
        serviceScope.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Verarbeitet Befehle, die über einen Intent an den Service gesendet werden
        if (intent?.action == ACTION_LOCK_SCREEN) {
            lockScreen()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateForegroundTracking(packageName: String) {
        val prefs = prefs(this)
        if (isTransientSystemPackage(packageName)) {
            Log.d(TAG, "ignore transient package=$packageName")
            return
        }

        val launcherPackage = applicationContext.packageName
        val currentPackage = prefs.getString(KEY_CURRENT_FOREGROUND_PACKAGE, null)
        if (currentPackage == packageName) {
            Log.d(TAG, "skip unchanged current=$currentPackage")
            return
        }

        Log.d(TAG, "foreground current=$currentPackage next=$packageName")

        prefs.edit().apply {
            putString(KEY_PREVIOUS_FOREGROUND_PACKAGE, currentPackage)
            putString(KEY_CURRENT_FOREGROUND_PACKAGE, packageName)
            putLong(KEY_LAST_FOREGROUND_UPDATE_AT, System.currentTimeMillis())
            if (packageName != launcherPackage) {
                putString(KEY_LAST_NON_LAUNCHER_PACKAGE, packageName)
                Log.d(TAG, "remember lastNonLauncher=$packageName")
            }
            if (packageName == launcherPackage && !currentPackage.isNullOrBlank()) {
                putString(KEY_LAST_PACKAGE_BEFORE_LAUNCHER, currentPackage)
                putLong(KEY_LAST_PACKAGE_BEFORE_LAUNCHER_AT, System.currentTimeMillis())
                Log.d(TAG, "remember beforeLauncher=$currentPackage")
            }
        }.apply()
    }

    /**
     * Führt die globale Aktion zum Sperren des Bildschirms aus.
     */
    private fun lockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // GLOBAL_ACTION_LOCK_SCREEN wurde in API 28 eingeführt
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            Toast.makeText(this, getString(R.string.lock_requires_android_9), Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val ACTION_LOCK_SCREEN = "com.example.androidlauncher.ACTION_LOCK_SCREEN"
        private const val PREFS_NAME = "launcher_accessibility_tracking"
        private const val KEY_CURRENT_FOREGROUND_PACKAGE = "current_foreground_package"
        private const val KEY_PREVIOUS_FOREGROUND_PACKAGE = "previous_foreground_package"
        private const val KEY_LAST_PACKAGE_BEFORE_LAUNCHER = "last_package_before_launcher"
        private const val KEY_LAST_PACKAGE_BEFORE_LAUNCHER_AT = "last_package_before_launcher_at"
        private const val KEY_LAST_NON_LAUNCHER_PACKAGE = "last_non_launcher_package"
        private const val KEY_LAST_FOREGROUND_UPDATE_AT = "last_foreground_update_at"

        private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        internal fun isTransientSystemPackage(packageName: String): Boolean {
            return packageName == "com.android.systemui" || packageName.contains("systemui")
        }

        /**
         * Reine Entscheidungslogik (ohne Android-Seiteneffekte, daher unit-testbar): Soll für das
         * gerade in den Vordergrund gekommene Paket der Sperrbildschirm gezeigt werden?
         */
        internal fun shouldShowLockScreen(
            packageName: String,
            ownPackage: String,
            lockedApps: Set<String>,
            alreadyUnlocked: Boolean,
            isTransient: Boolean
        ): Boolean = !isTransient &&
            packageName != ownPackage &&
            packageName in lockedApps &&
            !alreadyUnlocked

        /**
         * Prüft, ob der Accessibility Service in den Systemeinstellungen aktiviert wurde.
         */
        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val expectedComponentName = "${context.packageName}/${LauncherAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(expectedComponentName)
        }

        fun getLastForegroundPackageBeforeLauncher(context: Context): String? {
            val pkg = prefs(context).getString(KEY_LAST_PACKAGE_BEFORE_LAUNCHER, null)
            Log.d(TAG, "getLastBeforeLauncher=$pkg")
            return pkg
        }

        fun getLastForegroundPackageBeforeLauncherAt(context: Context): Long {
            val observedAt = prefs(context).getLong(KEY_LAST_PACKAGE_BEFORE_LAUNCHER_AT, 0L)
            Log.d(TAG, "getLastBeforeLauncherAt=$observedAt")
            return observedAt
        }

        fun getLastForegroundObservationBeforeLauncher(context: Context): ForegroundAppObservation? {
            val packageName = getLastForegroundPackageBeforeLauncher(context) ?: return null
            val observedAt = getLastForegroundPackageBeforeLauncherAt(context)
            if (observedAt <= 0L) return null
            return ForegroundAppObservation(
                packageName = packageName,
                observedAtMs = observedAt,
                source = "accessibility-before-launcher"
            )
        }

        fun getBestReturnCandidatePackage(context: Context): String? {
            val prefs = prefs(context)
            val pkg = getLastForegroundPackageBeforeLauncher(context)
                ?: prefs.getString(KEY_LAST_NON_LAUNCHER_PACKAGE, null)
            Log.d(TAG, "getBestCandidate=$pkg")
            return pkg
        }

        /**
         * Sendet einen Befehl an den Service, um den Bildschirm zu sperren.
         */
        fun requestLockScreen(context: Context) {
            val intent = Intent(context, LauncherAccessibilityService::class.java).apply {
                action = ACTION_LOCK_SCREEN
            }
            context.startService(intent)
        }
    }
}
