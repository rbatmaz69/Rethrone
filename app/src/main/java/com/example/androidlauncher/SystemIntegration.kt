package com.example.androidlauncher

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.widget.Toast

/**
 * Nicht-Compose-Systemintegration: Abfragen und Öffnen von Systemeinstellungen,
 * Standard-Launcher-Status, App-Shortcuts sowie das Senden von PendingIntents.
 * Bewusst framework-nah und UI-frei – Compose-Helfer liegen unter `ui/`.
 */

/**
 * Checks if the notification listener service is enabled for this app.
 */
fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(pkgName)
}

fun openNotificationSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
        val componentName = ComponentName(context, NotificationService::class.java)
        intent.putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, componentName.flattenToString())
        context.startActivity(intent)
    } else {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        context.startActivity(intent)
    }
}

fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    context.startActivity(intent)
}

/**
 * Prüft, ob diese App aktuell als Standard-Launcher (Start-App) gesetzt ist.
 */
fun isDefaultLauncher(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true
    } else {
        val resolved = context.packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) },
            PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.packageName
        resolved == context.packageName
    }
}

/**
 * Öffnet einen passenden Systembildschirm, um diese App als Standard-Launcher festzulegen.
 *
 * Universelle Fallback-Kette: probiert mehrere System-Intents in Reihenfolge und startet den
 * ersten, der sich tatsächlich auflösen lässt. Das deckt OEM-Eigenheiten (Stock, Motorola,
 * Samsung, Xiaomi/MIUI) ab, wo einzelne Intents fehlen können. Der direkte Rollen-Dialog
 * (ROLE_HOME) wird in der MainActivity über die ActivityResult-API ausgelöst.
 */
fun openDefaultLauncherSettings(context: Context) {
    val pm = context.packageManager
    val candidates = buildList {
        add(Intent(Settings.ACTION_HOME_SETTINGS))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            add(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        }
        add(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )
        )
        add(Intent(Settings.ACTION_SETTINGS))
    }

    for (intent in candidates) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (pm.resolveActivity(intent, 0) == null) continue
        if (runCatching { context.startActivity(intent) }.isSuccess) return
    }

    Toast.makeText(
        context,
        "Bitte in den Systemeinstellungen als Standard-Launcher festlegen",
        Toast.LENGTH_LONG
    ).show()
}

/**
 * Optionen, die dem feuernden App-Prozess Background-Activity-Launch erlauben. Ab Android 14
 * (API 34) blockiert das System sonst still jede Activity, die ein fremder PendingIntent startet
 * (z. B. „Öffnen", „Zurückrufen", contentIntent) – die Buttons erscheinen, reagieren aber nicht.
 */
private fun backgroundActivityStartOptions(): android.os.Bundle? =
    if (Build.VERSION.SDK_INT >= 34) {
        android.app.ActivityOptions.makeBasic()
            .setPendingIntentBackgroundActivityStartMode(
                android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            )
            .toBundle()
    } else {
        null
    }

/**
 * Sendet einen [android.app.PendingIntent] (z. B. eine Benachrichtigungs-Aktion oder den
 * contentIntent zum Öffnen der App). Abgebrochene Intents werden still ignoriert.
 *
 * @return true, wenn das Senden ausgelöst wurde.
 */
fun sendPendingIntent(context: Context, pendingIntent: android.app.PendingIntent?): Boolean {
    if (pendingIntent == null) return false
    return try {
        pendingIntent.send(context, 0, null, null, null, null, backgroundActivityStartOptions())
        true
    } catch (_: android.app.PendingIntent.CanceledException) {
        false
    }
}

/**
 * Beantwortet eine Reply-Aktion (RemoteInput, z. B. WhatsApp „Antworten"). Füllt die
 * RemoteInput-Ergebnisse mit [replyText] in einen Intent und feuert den zugehörigen
 * PendingIntent **mit Kontext** – ein bares `send()` würde nur einen leeren Reply liefern.
 *
 * @return true, wenn das Senden ausgelöst wurde.
 */
fun sendNotificationReply(
    context: Context,
    action: com.example.androidlauncher.data.NotificationAction,
    replyText: CharSequence
): Boolean {
    if (action.remoteInputs.isEmpty()) return sendPendingIntent(context, action.intent)
    val fillIn = Intent()
    val results = android.os.Bundle().apply {
        action.remoteInputs.forEach { putCharSequence(it.resultKey, replyText) }
    }
    android.app.RemoteInput.addResultsToIntent(action.remoteInputs.toTypedArray(), fillIn, results)
    return try {
        action.intent.send(context, 0, fillIn, null, null, null, backgroundActivityStartOptions())
        true
    } catch (_: android.app.PendingIntent.CanceledException) {
        false
    }
}

fun getAppShortcuts(context: Context, packageName: String): List<ShortcutInfo> {
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val query = LauncherApps.ShortcutQuery().apply {
        setPackage(packageName)
        setQueryFlags(
            LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
        )
    }
    return try {
        launcherApps.getShortcuts(query, Process.myUserHandle()) ?: emptyList()
    } catch (_: SecurityException) {
        emptyList()
    }
}

fun launchShortcut(context: Context, packageName: String, shortcutId: String) {
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    try {
        launcherApps.startShortcut(packageName, shortcutId, null, null, Process.myUserHandle())
    } catch (_: Exception) {
        Toast.makeText(context, context.getString(R.string.shortcut_open_failed), Toast.LENGTH_SHORT).show()
    }
}
