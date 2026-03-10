package com.example.androidlauncher

import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.AppUsageStats
import com.example.androidlauncher.data.FolderInfo
import com.example.androidlauncher.data.SearchHistoryEntry
import java.util.UUID

/**
 * Logic controller for the launcher operations.
 * Handles filtering apps, managing favorites (add/remove/reorder),
 * and folder management (create/delete/rename/add app/remove app).
 * This object is stateless and operates on immutable lists/objects passed to it.
 */
object LauncherLogic {
    const val MAX_FAVORITES = 6
    private const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L
    private val WORD_SEPARATOR_REGEX = Regex("[\\s._-]+")

    sealed interface PrimarySearchSuggestion {
        data class AppSuggestion(val app: AppInfo) : PrimarySearchSuggestion
        data class HistorySuggestion(val entry: SearchHistoryEntry) : PrimarySearchSuggestion
        data class WebSuggestion(val query: String) : PrimarySearchSuggestion
    }

    private data class ScoredPrimarySuggestion(
        val suggestion: PrimarySearchSuggestion,
        val score: Int,
        val typePriority: Int,
        val sortLabel: String
    )

    /**
     * Filters the list of apps based on a search query.
     * Case-insensitive.
     */
    fun filterApps(apps: List<AppInfo>, query: String): List<AppInfo> {
        if (query.isBlank()) return apps
        return apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    /**
     * Filters apps by relevance:
     * 1. Exact match (case insensitive)
     * 2. Starts with query (case insensitive)
     * 3. Contains query (case insensitive)
     */
    fun filterAppsByRelevance(apps: List<AppInfo>, query: String): List<AppInfo> {
        if (query.isBlank()) return emptyList()
        val lowerQuery = query.lowercase()

        return apps.filter { it.label.contains(lowerQuery, ignoreCase = true) }
            .sortedWith(compareBy<AppInfo> {
                val lowerLabel = it.label.lowercase()
                when {
                    lowerLabel == lowerQuery -> 0
                    lowerLabel.startsWith(lowerQuery) -> 1
                    else -> 2
                }
            }.thenBy { it.label })
    }

    fun rankAppSuggestions(
        apps: List<AppInfo>,
        query: String,
        appUsageStats: Map<String, AppUsageStats>,
        now: Long = System.currentTimeMillis(),
        limit: Int = 4
    ): List<AppInfo> {
        val normalizedQuery = normalizeSearchText(query)
        if (normalizedQuery.isEmpty()) return emptyList()

        return buildAppCandidates(apps, normalizedQuery, appUsageStats, now)
            .take(limit)
            .map { (it.suggestion as PrimarySearchSuggestion.AppSuggestion).app }
    }

    fun rankWebSuggestions(
        history: List<SearchHistoryEntry>,
        query: String,
        now: Long = System.currentTimeMillis(),
        limit: Int = 4
    ): List<SearchHistoryEntry> {
        val normalizedQuery = normalizeSearchText(query)
        if (normalizedQuery.isEmpty()) return emptyList()

        return buildHistoryCandidates(history, normalizedQuery, now)
            .take(limit)
            .map { (it.suggestion as PrimarySearchSuggestion.HistorySuggestion).entry }
    }

    fun resolvePrimarySuggestion(
        apps: List<AppInfo>,
        history: List<SearchHistoryEntry>,
        query: String,
        appUsageStats: Map<String, AppUsageStats>,
        smartSuggestionsEnabled: Boolean,
        now: Long = System.currentTimeMillis()
    ): PrimarySearchSuggestion? {
        val normalizedQuery = normalizeSearchText(query)
        if (normalizedQuery.isEmpty()) return null
        val trimmedQuery = query.trim()

        if (!smartSuggestionsEnabled) {
            return filterAppsByRelevance(apps, trimmedQuery).firstOrNull()?.let {
                PrimarySearchSuggestion.AppSuggestion(it)
            } ?: PrimarySearchSuggestion.WebSuggestion(trimmedQuery)
        }

        val bestSmartSuggestion = (
            buildAppCandidates(apps, normalizedQuery, appUsageStats, now) +
                buildHistoryCandidates(history, normalizedQuery, now)
            )
            .sortedWith(primarySuggestionComparator())
            .firstOrNull()
            ?.suggestion

        return bestSmartSuggestion ?: PrimarySearchSuggestion.WebSuggestion(trimmedQuery)
    }

    /**
     * Toggles the favorite status of an app.
     * Adds to favorites if not already a favorite and max favorites limit is not reached.
     * Removes from favorites if it is already a favorite.
     */
    fun toggleFavorite(currentFavorites: List<String>, packageName: String): List<String> {
        return if (packageName in currentFavorites) {
            // Remove if already favorite
            currentFavorites - packageName
        } else {
            // Add if not maxed out
            if (currentFavorites.size < MAX_FAVORITES) {
                currentFavorites + packageName
            } else {
                currentFavorites
            }
        }
    }

    /**
     * Moves a favorite item up in the list (decreases index).
     */
    fun moveFavoriteUp(currentFavorites: List<String>, index: Int): List<String> {
        if (index <= 0 || index >= currentFavorites.size) return currentFavorites
        val newList = currentFavorites.toMutableList()
        val item = newList.removeAt(index)
        newList.add(index - 1, item)
        return newList
    }

    /**
     * Moves a favorite item down in the list (increases index).
     */
    fun moveFavoriteDown(currentFavorites: List<String>, index: Int): List<String> {
        if (index < 0 || index >= currentFavorites.size - 1) return currentFavorites
        val newList = currentFavorites.toMutableList()
        val item = newList.removeAt(index)
        newList.add(index + 1, item)
        return newList
    }

    /**
     * Resolves the list of favorite packages to actual AppInfo objects.
     * Maintains the order of the favorite packages list.
     */
    fun getFavoriteApps(allApps: List<AppInfo>, favoritePackages: List<String>): List<AppInfo> {
        return favoritePackages.mapNotNull { pkg ->
            allApps.find { it.packageName == pkg }
        }.take(MAX_FAVORITES)
    }

    // Folder Logic

    /**
     * returns a set of all package names that are currently inside any folder.
     */
    fun getAppsInFolders(folders: List<FolderInfo>): Set<String> {
        return folders.flatMap { it.appPackageNames }.toSet()
    }

    /**
     * Returns apps that are NOT inside any folder.
     * Used for the main app drawer list to hide folder contents.
     */
    fun getVisibleApps(allApps: List<AppInfo>, folders: List<FolderInfo>): List<AppInfo> {
        val appsInFolders = getAppsInFolders(folders)
        return allApps.filter { it.packageName !in appsInFolders }
    }

    /**
     * Adds an app to a specific folder.
     * Ensures an app exists only in one folder at a time by removing it from others.
     */
    fun addAppToFolder(folders: List<FolderInfo>, folderId: String, packageName: String): List<FolderInfo> {
        return folders.map { folder ->
            if (folder.id == folderId) {
                if (packageName !in folder.appPackageNames) {
                    folder.copy(appPackageNames = folder.appPackageNames + packageName)
                } else folder
            } else {
                // Ensure app is not in other folders
                folder.copy(appPackageNames = folder.appPackageNames - packageName)
            }
        }
    }

    /**
     * Removes an app from a specific folder.
     * CUSTOM: Deletes the folder if it becomes empty.
     */
    fun removeAppFromFolder(folders: List<FolderInfo>, folderId: String, packageName: String): List<FolderInfo> {
        return folders.mapNotNull { folder ->
            if (folder.id == folderId) {
                val newList = folder.appPackageNames - packageName
                if (newList.isEmpty()) null else folder.copy(appPackageNames = newList)
            } else folder
        }
    }

    /**
     * Creates a new folder info object with a generated ID.
     */
    fun createNewFolder(name: String): FolderInfo {
        return FolderInfo(
            id = UUID.randomUUID().toString(),
            name = name,
            appPackageNames = emptyList()
        )
    }

    /**
     * Creates a new folder with a generated ID and adds it to the list.
     */
    fun createFolder(folders: List<FolderInfo>, name: String): List<FolderInfo> {
        val newFolder = createNewFolder(name)
        return folders + newFolder
    }

    /**
     * Deletes a folder by ID. Apps inside will become visible in the main list again
     * (because they are no longer in `getAppsInFolders`).
     */
    fun deleteFolder(folders: List<FolderInfo>, folderId: String): List<FolderInfo> {
        return folders.filter { it.id != folderId }
    }

    /**
     * Renames a folder.
     */
    fun renameFolder(folders: List<FolderInfo>, folderId: String, newName: String): List<FolderInfo> {
        return folders.map { if (it.id == folderId) it.copy(name = newName) else it }
    }

    private fun computeTextMatchScore(text: String, normalizedQuery: String): Int {
        val normalizedText = normalizeSearchText(text)
        if (normalizedText.isEmpty() || normalizedQuery.isEmpty()) return 0

        return when {
            normalizedText == normalizedQuery -> 4_000
            normalizedText.startsWith(normalizedQuery) -> 3_000
            normalizedText.split(WORD_SEPARATOR_REGEX).any { it.startsWith(normalizedQuery) } -> 2_200
            normalizedText.contains(normalizedQuery) -> 1_400
            else -> 0
        }
    }

    private fun usageFrequencyScore(usageCount: Int): Int {
        return (usageCount.coerceAtLeast(0) * 90).coerceAtMost(900)
    }

    private fun recencyScore(lastUsedAt: Long, now: Long): Int {
        if (lastUsedAt <= 0L) return 0
        val age = (now - lastUsedAt).coerceAtLeast(0L)
        return when {
            age <= DAY_IN_MILLIS -> 260
            age <= 7 * DAY_IN_MILLIS -> 180
            age <= 30 * DAY_IN_MILLIS -> 100
            age <= 90 * DAY_IN_MILLIS -> 40
            else -> 0
        }
    }

    private fun buildAppCandidates(
        apps: List<AppInfo>,
        normalizedQuery: String,
        appUsageStats: Map<String, AppUsageStats>,
        now: Long
    ): List<ScoredPrimarySuggestion> {
        return apps.mapNotNull { app ->
            val matchScore = computeTextMatchScore(app.label, normalizedQuery)
            if (matchScore == 0) return@mapNotNull null

            val usage = appUsageStats[app.packageName]
            val totalScore = matchScore +
                usageFrequencyScore(usage?.launchCount ?: 0) +
                recencyScore(usage?.lastLaunchedAt ?: 0L, now)

            ScoredPrimarySuggestion(
                suggestion = PrimarySearchSuggestion.AppSuggestion(app),
                score = totalScore,
                typePriority = 2,
                sortLabel = app.label.lowercase()
            )
        }.sortedWith(primarySuggestionComparator())
    }

    private fun buildHistoryCandidates(
        history: List<SearchHistoryEntry>,
        normalizedQuery: String,
        now: Long
    ): List<ScoredPrimarySuggestion> {
        return history.mapNotNull { entry ->
            val matchScore = computeTextMatchScore(entry.query, normalizedQuery)
            if (matchScore == 0) return@mapNotNull null

            val totalScore = matchScore +
                usageFrequencyScore(entry.usageCount) +
                recencyScore(entry.lastSearchedAt, now)

            ScoredPrimarySuggestion(
                suggestion = PrimarySearchSuggestion.HistorySuggestion(entry),
                score = totalScore,
                typePriority = 1,
                sortLabel = entry.query.lowercase()
            )
        }.sortedWith(primarySuggestionComparator())
    }

    private fun primarySuggestionComparator() = compareByDescending<ScoredPrimarySuggestion> { it.score }
        .thenByDescending { it.typePriority }
        .thenBy { it.sortLabel }

    private fun normalizeSearchText(value: String): String {
        return value.trim().lowercase()
    }
}
