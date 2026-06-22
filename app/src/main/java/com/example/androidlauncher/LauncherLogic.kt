package com.example.androidlauncher

import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.AppUsageStats
import com.example.androidlauncher.data.AutoIconFallback
import com.example.androidlauncher.data.AutoIconRule
import com.example.androidlauncher.data.FolderInfo
import com.example.androidlauncher.data.IconQualityEvaluator
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

    /**
     * Verschmilzt die frisch vom System geladene App-Liste [basicApps] mit den bereits
     * angezeigten [existingApps]:
     * - bereits geladene Icons und automatischen Icon-Fallbacks werden übernommen, damit
     *   beim Aktualisieren nicht kurz leere Icons aufblitzen,
     * - gespeicherte Fallbacks ([storedFallbacks]) haben Vorrang vor den übernommenen,
     * - die Icon-Regel kommt aus [autoIconRules], sonst aus der Default-Regel.
     *
     * Reine Transformation ohne Seiteneffekte (unit-testbar). Reihenfolge und Umfang folgen
     * [basicApps]; nicht mehr installierte Apps fallen damit automatisch heraus.
     */
    fun mergeInstalledApps(
        basicApps: List<AppInfo>,
        existingApps: List<AppInfo>,
        storedFallbacks: Map<String, AutoIconFallback>,
        autoIconRules: Map<String, AutoIconRule>,
    ): List<AppInfo> {
        val currentIcons = existingApps.associate { it.packageName to it.iconBitmap }
        val currentFallbacks = existingApps.associate { it.packageName to it.autoIconFallback }
        return basicApps.map { app ->
            val resolvedRule = autoIconRules[app.packageName]
                ?: IconQualityEvaluator.resolveDefaultRule(app.packageName)
            app.copy(
                iconBitmap = currentIcons[app.packageName],
                autoIconFallback = storedFallbacks[app.packageName] ?: currentFallbacks[app.packageName],
                autoIconRule = resolvedRule,
            )
        }
    }

    /**
     * Dedupliziert, filtert und sortiert die rohen Launcher-Eintraege zur finalen App-Liste.
     *
     * Reine Datentransformation ohne Framework-Bezug – damit ohne PackageManager-Mocking
     * unit-testbar. [com.example.androidlauncher.data.AppRepository.getInstalledApps] uebernimmt
     * nur noch die plattformnahe Abfrage und delegiert die Aufbereitung hierher.
     *
     * @param ownPackage Paketname von Rethrone selbst.
     * @param isOwnDefaultLauncher true, wenn Rethrone aktueller Standard-Launcher ist – dann wird
     *   der eigene Eintrag ausgeblendet (sonst bleibt Rethrone startbar in der Liste).
     */
    fun normalizeInstalledApps(
        rawApps: List<AppInfo>,
        ownPackage: String,
        isOwnDefaultLauncher: Boolean,
    ): List<AppInfo> =
        rawApps
            .distinctBy { it.packageName }
            .filterNot { isOwnDefaultLauncher && it.packageName == ownPackage }
            .sortedBy { it.label.lowercase() }

    /** Ergebnis von [evaluateDefaultLauncherWarning]: ob gewarnt wird + der neue Zustand. */
    data class DefaultLauncherWarningDecision(
        val showWarning: Boolean,
        val warningShown: Boolean,
        val lastPackage: String?,
    )

    /**
     * Entscheidet, ob die "Rethrone ist nicht Standard-Launcher"-Warnung gezeigt werden soll, und
     * berechnet den Folgezustand. Reine Logik – der eigentliche Toast bleibt in der Activity.
     *
     * Regeln: Ist Rethrone selbst der aufgeloeste Home-Launcher, wird nicht gewarnt und der
     * Warn-Merker zurueckgesetzt. Andernfalls wird gewarnt, solange nicht bereits fuer genau
     * dieses Paket gewarnt wurde (verhindert wiederholte Toasts beim selben Fremd-Launcher).
     */
    fun evaluateDefaultLauncherWarning(
        resolvedPackage: String,
        ownPackage: String,
        warningAlreadyShown: Boolean,
        lastPackage: String?,
    ): DefaultLauncherWarningDecision {
        if (resolvedPackage == ownPackage) {
            return DefaultLauncherWarningDecision(
                showWarning = false,
                warningShown = false,
                lastPackage = resolvedPackage,
            )
        }
        val show = !warningAlreadyShown || lastPackage != resolvedPackage
        return DefaultLauncherWarningDecision(
            showWarning = show,
            warningShown = if (show) true else warningAlreadyShown,
            lastPackage = resolvedPackage,
        )
    }

    /**
     * Bekannte Uhr-App-Pakete in Prioritaetsreihenfolge (vendor-spezifische Varianten zuerst,
     * dann generische AOSP-/Google-Pakete). Wird beim Tap auf die Uhr durchprobiert.
     */
    val KNOWN_CLOCK_PACKAGES: List<String> = listOf(
        "cn.nubia.deskclock.preset",
        "cn.nubia.deskclock",
        "cn.nubia.clock",
        "com.android.deskclock",
        "com.google.android.deskclock",
        "com.sec.android.app.clockpackage",
        "com.huawei.android.clock",
        "com.miui.clock",
        "com.zte.deskclock",
        "com.android.clock",
    )

    /**
     * Liefert das erste Paket aus [candidates], fuer das [isLaunchable] true ergibt, sonst null.
     * Exceptions je Kandidat werden als "nicht startbar" gewertet (defensiv gegenueber
     * PackageManager-Fehlern). Framework-frei und damit unit-testbar.
     */
    fun firstLaunchablePackage(
        candidates: List<String>,
        isLaunchable: (String) -> Boolean,
    ): String? = candidates.firstOrNull { runCatching { isLaunchable(it) }.getOrDefault(false) }

    /**
     * Loest eine Sucheingabe in eine Ziel-URL auf: Sieht die Eingabe wie eine Adresse aus
     * (beginnt mit "http" oder enthaelt einen Punkt), wird sie direkt geoeffnet (ggf. mit
     * vorangestelltem "https://"); andernfalls wird eine Google-Suche gebaut. Liefert null bei
     * leerer Eingabe.
     *
     * Der URL-Encoder wird injiziert ([encodeQuery]), damit die Logik framework-frei (ohne
     * `android.net.Uri`) unit-testbar bleibt.
     */
    fun resolveSearchUrl(query: String, encodeQuery: (String) -> String): String? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return null
        return if (trimmed.startsWith("http") || trimmed.contains(".")) {
            if (!trimmed.startsWith("http")) "https://$trimmed" else trimmed
        } else {
            "https://www.google.com/search?q=${encodeQuery(trimmed)}"
        }
    }

    /**
     * Position des ersten case-insensitiven Treffers von [query] in [text] als Paar
     * (Start, End-exklusiv) zur Hervorhebung in Suchvorschlaegen; null bei leerer Eingabe oder
     * wenn kein Treffer existiert. Das End ist auf die Textlaenge begrenzt.
     */
    fun highlightRange(text: String, query: String): Pair<Int, Int>? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return null
        val start = text.indexOf(trimmed, ignoreCase = true)
        if (start < 0) return null
        return start to (start + trimmed.length).coerceAtMost(text.length)
    }
}
