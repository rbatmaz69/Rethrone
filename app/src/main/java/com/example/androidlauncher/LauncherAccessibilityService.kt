package com.example.androidlauncher

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

private const val TAG = "A11yReturn"

/**
 * Accessibility Service zur Unterstützung von System-Gesten.
 * Wird primär verwendet, um den Bildschirm zu sperren, da dies für Launcher
 * ab Android 9 (API 28) die empfohlene Methode ohne Device-Admin-Rechte ist.
 */
class LauncherAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val packageName = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
        Log.d(TAG, "event type=${event.eventType} package=$packageName")
        updateForegroundTracking(packageName)
    }

    override fun onInterrupt() {
        // Nicht benötigt
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
            if (packageName == launcherPackage && !currentPackage.isNullOrBlank() && currentPackage != launcherPackage) {
                putString(KEY_LAST_PACKAGE_BEFORE_LAUNCHER, currentPackage)
                putLong(KEY_LAST_PACKAGE_BEFORE_LAUNCHER_AT, System.currentTimeMillis())
                Log.d(TAG, "remember beforeLauncher=$currentPackage")
            }
        }.apply()
    }

    private fun isTransientSystemPackage(packageName: String): Boolean {
        return packageName == "com.android.systemui" || packageName.contains("systemui")
    }

    /**
     * Führt die globale Aktion zum Sperren des Bildschirms aus.
     */
    private fun lockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // GLOBAL_ACTION_LOCK_SCREEN wurde in API 28 eingeführt
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            Toast.makeText(this, "Sperren erst ab Android 9 (API 28) unterstützt", Toast.LENGTH_SHORT).show()
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
