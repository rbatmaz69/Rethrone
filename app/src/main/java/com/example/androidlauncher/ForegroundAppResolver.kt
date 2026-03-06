package com.example.androidlauncher

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log

private const val TAG = "UsageReturn"

object ForegroundAppResolver {
    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        val granted = mode == AppOpsManager.MODE_ALLOWED
        Log.d(TAG, "hasUsageAccess=$granted mode=$mode")
        return granted
    }

    fun getRecentForegroundPackage(context: Context, lookbackMs: Long = 15_000L): String? {
        if (!hasUsageAccess(context)) return null
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val now = System.currentTimeMillis()
        val begin = now - lookbackMs
        val events = usageStatsManager.queryEvents(begin, now)
        val event = UsageEvents.Event()
        var candidate: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val packageName = event.packageName?.takeIf { it.isNotBlank() } ?: continue
            if (packageName == context.packageName) continue
            if (packageName == "com.android.systemui" || packageName.contains("systemui")) continue
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                candidate = packageName
            }
        }
        Log.d(TAG, "recentForegroundCandidate=$candidate")
        return candidate
    }
}
