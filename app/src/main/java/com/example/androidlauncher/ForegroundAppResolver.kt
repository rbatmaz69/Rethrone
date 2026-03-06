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
    private const val EVENT_LOOKBACK_MS = 15_000L
    private const val STATS_LOOKBACK_MS = 6 * 60 * 60 * 1000L

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

    fun getRecentForegroundPackage(context: Context, allowedPackages: Set<String> = emptySet()): String? {
        val observation = getRecentForegroundObservation(context, allowedPackages)
        Log.d(TAG, "recentForegroundCandidate=${observation?.packageName} source=${observation?.source} observedAt=${observation?.observedAtMs}")
        return observation?.packageName
    }

    fun getRecentForegroundObservation(context: Context, allowedPackages: Set<String> = emptySet()): ForegroundAppObservation? {
        if (!hasUsageAccess(context)) return null
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val eventCandidate = getEventCandidate(context, usageStatsManager, allowedPackages)
        val statsCandidate = getUsageStatsCandidate(context, usageStatsManager, allowedPackages)
        val resolved = eventCandidate ?: statsCandidate
        Log.d(TAG, "recentForegroundObservation=$resolved eventCandidate=$eventCandidate statsCandidate=$statsCandidate")
        return resolved
    }

    private fun getEventCandidate(context: Context, usageStatsManager: UsageStatsManager, allowedPackages: Set<String>): ForegroundAppObservation? {
        val now = System.currentTimeMillis()
        val begin = now - EVENT_LOOKBACK_MS
        val events = usageStatsManager.queryEvents(begin, now)
        val event = UsageEvents.Event()
        var candidate: ForegroundAppObservation? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val packageName = event.packageName?.takeIf { it.isNotBlank() } ?: continue
            if (shouldIgnorePackage(context, packageName, allowedPackages)) continue
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                candidate = ForegroundAppObservation(
                    packageName = packageName,
                    observedAtMs = event.timeStamp,
                    source = "usage-events"
                )
            }
        }
        Log.d(TAG, "eventCandidate=$candidate")
        return candidate
    }

    private fun getUsageStatsCandidate(context: Context, usageStatsManager: UsageStatsManager, allowedPackages: Set<String>): ForegroundAppObservation? {
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - STATS_LOOKBACK_MS,
            now
        ) ?: return null

        val candidate = stats
            .asSequence()
            .filter { !shouldIgnorePackage(context, it.packageName, allowedPackages) }
            .maxByOrNull { it.lastTimeUsed }
            ?.takeIf { it.lastTimeUsed > 0L }
            ?.let {
                ForegroundAppObservation(
                    packageName = it.packageName,
                    observedAtMs = it.lastTimeUsed,
                    source = "usage-stats"
                )
            }

        Log.d(TAG, "statsCandidate=$candidate")
        return candidate
    }

    private fun shouldIgnorePackage(context: Context, packageName: String, allowedPackages: Set<String>): Boolean {
        if (packageName == context.packageName || packageName == "com.android.systemui" || packageName.contains("systemui")) return true
        if (allowedPackages.isNotEmpty() && packageName !in allowedPackages) return true
        return false
    }
}
