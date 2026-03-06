package com.example.androidlauncher

import android.content.Context
import android.util.Log
import androidx.compose.ui.geometry.Rect

private const val PREFS_NAME = "return_origin_store"
private const val ENTRY_PREFIX = "origin_"
private const val LAST_LAUNCHED_PACKAGE_KEY = "last_launched_package"
private const val TAG = "ReturnStore"

object ReturnOriginStore {
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getStoredPackageNames(context: Context): Set<String> {
        val packages = prefs(context).all.keys
            .asSequence()
            .filter { it.startsWith(ENTRY_PREFIX) }
            .map { it.removePrefix(ENTRY_PREFIX) }
            .toSet()
        Log.d(TAG, "storedPackages=$packages")
        return packages
    }

    fun getStoredOriginCount(context: Context): Int {
        val count = prefs(context).all.keys.count { it.startsWith(ENTRY_PREFIX) }
        Log.d(TAG, "storedOriginCount=$count")
        return count
    }

    fun save(context: Context, launchedPackageName: String, animation: ReturnAnimation) {
        Log.d(TAG, "save launched=$launchedPackageName source=${animation.source} target=${animation.packageName} bounds=${animation.bounds != null}")
        prefs(context).edit()
            .putString(ENTRY_PREFIX + launchedPackageName, encode(animation))
            .putString(LAST_LAUNCHED_PACKAGE_KEY, launchedPackageName)
            .apply()
    }

    fun get(context: Context, launchedPackageName: String): ReturnAnimation? {
        val encoded = prefs(context).getString(ENTRY_PREFIX + launchedPackageName, null) ?: return null
        val decoded = decode(encoded)
        Log.d(TAG, "get launched=$launchedPackageName hit=${decoded != null} target=${decoded?.packageName} source=${decoded?.source}")
        return decoded
    }

    fun getLastLaunchedPackageName(context: Context): String? {
        val pkg = prefs(context).getString(LAST_LAUNCHED_PACKAGE_KEY, null)
        Log.d(TAG, "lastLaunched=$pkg")
        return pkg
    }

    fun clear(context: Context, launchedPackageName: String) {
        Log.d(TAG, "clear launched=$launchedPackageName")
        prefs(context).edit()
            .remove(ENTRY_PREFIX + launchedPackageName)
            .apply()
    }

    private fun encode(animation: ReturnAnimation): String {
        val bounds = animation.bounds
        val boundsPart = if (bounds == null) {
            "null"
        } else {
            listOf(bounds.left, bounds.top, bounds.right, bounds.bottom).joinToString(",")
        }
        return listOf(
            animation.source.name,
            escape(animation.packageName),
            escape(animation.launchedPackageName),
            boundsPart
        ).joinToString("|")
    }

    private fun decode(encoded: String): ReturnAnimation? {
        val parts = encoded.split("|", limit = 4)
        if (parts.size != 4) {
            Log.d(TAG, "decode failed invalid parts=$parts")
            return null
        }
        return try {
            val source = LaunchSource.valueOf(parts[0])
            val packageName = unescape(parts[1])
            val launchedPackageName = unescape(parts[2])
            val bounds = decodeBounds(parts[3])
            ReturnAnimation(
                bounds = bounds,
                source = source,
                packageName = packageName,
                launchedPackageName = launchedPackageName
            )
        } catch (_: Exception) {
            Log.d(TAG, "decode failed exception for=$encoded")
            null
        }
    }

    private fun decodeBounds(raw: String): Rect? {
        if (raw == "null") return null
        val values = raw.split(",")
        if (values.size != 4) {
            Log.d(TAG, "decodeBounds failed raw=$raw")
            return null
        }
        return Rect(
            left = values[0].toFloat(),
            top = values[1].toFloat(),
            right = values[2].toFloat(),
            bottom = values[3].toFloat()
        )
    }

    private fun escape(value: String) = value.replace("%", "%25").replace("|", "%7C")
    private fun unescape(value: String) = value.replace("%7C", "|").replace("%25", "%")
}
