package com.example.androidlauncher

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * Accessibility Service zur Unterstützung von System-Gesten.
 * Wird primär verwendet, um den Bildschirm zu sperren, da dies für Launcher
 * ab Android 9 (API 28) die empfohlene Methode ohne Device-Admin-Rechte ist.
 */
class LauncherAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Nicht benötigt für diese Basisfunktionalität
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
