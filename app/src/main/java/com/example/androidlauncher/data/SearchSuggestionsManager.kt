package com.example.androidlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.searchSuggestionsDataStore by preferencesDataStore(name = "search_suggestions")

data class SearchHistoryEntry(
    val query: String,
    val usageCount: Int,
    val lastSearchedAt: Long
)

data class AppUsageStats(
    val packageName: String,
    val launchCount: Int,
    val lastLaunchedAt: Long
)

class SearchSuggestionsManager(
    private val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
) {
    constructor(context: Context) : this(context.searchSuggestionsDataStore)

    companion object {
        private val WEB_HISTORY_KEY = stringPreferencesKey("web_history_entries")
        private val APP_USAGE_KEY = stringPreferencesKey("app_usage_entries")

        const val MAX_HISTORY_ITEMS = 75
        private const val MAX_APP_USAGE_ITEMS = 128
        private const val EMPTY_JSON_ARRAY = "[]"
    }

    val webHistory: Flow<List<SearchHistoryEntry>> = dataStore.data
        .map { preferences ->
            parseHistoryEntries(preferences[WEB_HISTORY_KEY] ?: EMPTY_JSON_ARRAY)
        }

    val appUsageStats: Flow<Map<String, AppUsageStats>> = dataStore.data
        .map { preferences ->
            parseAppUsageEntries(preferences[APP_USAGE_KEY] ?: EMPTY_JSON_ARRAY)
                .associateBy { it.packageName }
        }

    suspend fun recordWebSearch(query: String, timestamp: Long = System.currentTimeMillis()) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return

        dataStore.edit { preferences ->
            val currentEntries = parseHistoryEntries(preferences[WEB_HISTORY_KEY] ?: EMPTY_JSON_ARRAY)
            val updatedEntries = upsertHistoryEntry(currentEntries, trimmedQuery, timestamp)
            preferences[WEB_HISTORY_KEY] = serializeHistoryEntries(updatedEntries)
        }
    }

    suspend fun recordAppLaunch(packageName: String, timestamp: Long = System.currentTimeMillis()) {
        if (packageName.isBlank()) return

        dataStore.edit { preferences ->
            val currentEntries = parseAppUsageEntries(preferences[APP_USAGE_KEY] ?: EMPTY_JSON_ARRAY)
            val updatedEntries = upsertAppUsageEntry(currentEntries, packageName, timestamp)
            preferences[APP_USAGE_KEY] = serializeAppUsageEntries(updatedEntries)
        }
    }

    suspend fun removeWebSearch(query: String) {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) return

        dataStore.edit { preferences ->
            val currentEntries = parseHistoryEntries(preferences[WEB_HISTORY_KEY] ?: EMPTY_JSON_ARRAY)
            val updatedEntries = currentEntries.filterNot { normalize(it.query) == normalizedQuery }
            preferences[WEB_HISTORY_KEY] = serializeHistoryEntries(updatedEntries)
        }
    }

    suspend fun clearWebHistory() {
        dataStore.edit { preferences ->
            preferences.remove(WEB_HISTORY_KEY)
        }
    }

    private fun upsertHistoryEntry(
        currentEntries: List<SearchHistoryEntry>,
        query: String,
        timestamp: Long
    ): List<SearchHistoryEntry> {
        val normalizedQuery = normalize(query)
        val existingEntry = currentEntries.firstOrNull { normalize(it.query) == normalizedQuery }
        val updatedEntry = SearchHistoryEntry(
            query = query,
            usageCount = (existingEntry?.usageCount ?: 0) + 1,
            lastSearchedAt = timestamp
        )

        return (currentEntries.filterNot { normalize(it.query) == normalizedQuery } + updatedEntry)
            .sortedWith(
                compareByDescending<SearchHistoryEntry> { it.lastSearchedAt }
                    .thenByDescending { it.usageCount }
                    .thenBy { it.query.lowercase() }
            )
            .take(MAX_HISTORY_ITEMS)
    }

    private fun upsertAppUsageEntry(
        currentEntries: List<AppUsageStats>,
        packageName: String,
        timestamp: Long
    ): List<AppUsageStats> {
        val existingEntry = currentEntries.firstOrNull { it.packageName == packageName }
        val updatedEntry = AppUsageStats(
            packageName = packageName,
            launchCount = (existingEntry?.launchCount ?: 0) + 1,
            lastLaunchedAt = timestamp
        )

        return (currentEntries.filterNot { it.packageName == packageName } + updatedEntry)
            .sortedWith(
                compareByDescending<AppUsageStats> { it.launchCount }
                    .thenByDescending { it.lastLaunchedAt }
                    .thenBy { it.packageName }
            )
            .take(MAX_APP_USAGE_ITEMS)
    }

    private fun serializeHistoryEntries(entries: List<SearchHistoryEntry>): String {
        return JSONArray().apply {
            entries.forEach { entry ->
                put(
                    JSONObject()
                        .put("query", entry.query)
                        .put("usageCount", entry.usageCount)
                        .put("lastSearchedAt", entry.lastSearchedAt)
                )
            }
        }.toString()
    }

    private fun parseHistoryEntries(json: String): List<SearchHistoryEntry> {
        return runCatching {
            val jsonArray = JSONArray(json)
            buildList(jsonArray.length()) {
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(index) ?: continue
                    val query = item.optString("query").trim()
                    if (query.isEmpty()) continue
                    add(
                        SearchHistoryEntry(
                            query = query,
                            usageCount = item.optInt("usageCount", 0).coerceAtLeast(0),
                            lastSearchedAt = item.optLong("lastSearchedAt", 0L).coerceAtLeast(0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeAppUsageEntries(entries: List<AppUsageStats>): String {
        return JSONArray().apply {
            entries.forEach { entry ->
                put(
                    JSONObject()
                        .put("packageName", entry.packageName)
                        .put("launchCount", entry.launchCount)
                        .put("lastLaunchedAt", entry.lastLaunchedAt)
                )
            }
        }.toString()
    }

    private fun parseAppUsageEntries(json: String): List<AppUsageStats> {
        return runCatching {
            val jsonArray = JSONArray(json)
            buildList(jsonArray.length()) {
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(index) ?: continue
                    val packageName = item.optString("packageName").trim()
                    if (packageName.isEmpty()) continue
                    add(
                        AppUsageStats(
                            packageName = packageName,
                            launchCount = item.optInt("launchCount", 0).coerceAtLeast(0),
                            lastLaunchedAt = item.optLong("lastLaunchedAt", 0L).coerceAtLeast(0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun normalize(value: String): String = value.trim().lowercase()
}
