package com.example.androidlauncher

import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.AppUsageStats
import com.example.androidlauncher.data.FolderInfo
import com.example.androidlauncher.data.SearchHistoryEntry
import org.junit.Assert.*
import org.junit.Test

class LauncherLogicTest {

    private val apps = listOf(
        AppInfo("Browser", "com.android.browser", null),
        AppInfo("Camera", "com.android.camera", null),
        AppInfo("Contacts", "com.android.contacts", null),
        AppInfo("Email", "com.android.email", null)
    )

    private val folders = listOf(
        FolderInfo(id = "f1", name = "Work", appPackageNames = listOf("com.android.browser", "com.android.camera")),
        FolderInfo(id = "f2", name = "Social", appPackageNames = listOf("com.android.contacts"))
    )

    @Test
    fun `filterApps returns all apps when query is blank`() {
        val result = LauncherLogic.filterApps(apps, "")
        assertEquals(apps, result)
    }

    @Test
    fun `filterApps returns filtered apps when query matches`() {
        val result = LauncherLogic.filterApps(apps, "cam")
        assertEquals(1, result.size)
        assertEquals("com.android.camera", result[0].packageName)
    }

    @Test
    fun `filterApps is case insensitive`() {
        val result = LauncherLogic.filterApps(apps, "CAMERA")
        assertEquals(1, result.size)
        assertEquals("com.android.camera", result[0].packageName)
    }

    @Test
    fun `toggleFavorite adds package if not present`() {
        val favorites = listOf("pkg1", "pkg2")
        val result = LauncherLogic.toggleFavorite(favorites, "pkg3")
        assertEquals(listOf("pkg1", "pkg2", "pkg3"), result)
    }

    @Test
    fun `toggleFavorite removes package if present`() {
        val favorites = listOf("pkg1", "pkg2", "pkg3")
        val result = LauncherLogic.toggleFavorite(favorites, "pkg2")
        assertEquals(listOf("pkg1", "pkg3"), result)
    }

    @Test
    fun `toggleFavorite does not add more than MAX_FAVORITES`() {
        val favorites = (1..8).map { "pkg$it" }
        val result = LauncherLogic.toggleFavorite(favorites, "pkg9")
        assertEquals(8, result.size)
        assertFalse(result.contains("pkg9"))
    }

    @Test
    fun `moveFavoriteUp reorders correctly`() {
        val favorites = listOf("pkg1", "pkg2", "pkg3")
        val result = LauncherLogic.moveFavoriteUp(favorites, 1) // Move pkg2 up
        assertEquals(listOf("pkg2", "pkg1", "pkg3"), result)
    }

    @Test
    fun `moveFavoriteUp does nothing for first item`() {
        val favorites = listOf("pkg1", "pkg2", "pkg3")
        val result = LauncherLogic.moveFavoriteUp(favorites, 0)
        assertEquals(favorites, result)
    }

    @Test
    fun `moveFavoriteDown reorders correctly`() {
        val favorites = listOf("pkg1", "pkg2", "pkg3")
        val result = LauncherLogic.moveFavoriteDown(favorites, 1) // Move pkg2 down
        assertEquals(listOf("pkg1", "pkg3", "pkg2"), result)
    }

    @Test
    fun `moveFavoriteDown does nothing for last item`() {
        val favorites = listOf("pkg1", "pkg2", "pkg3")
        val result = LauncherLogic.moveFavoriteDown(favorites, 2)
        assertEquals(favorites, result)
    }

    @Test
    fun `getFavoriteApps returns correct AppInfo list`() {
        val favorites = listOf("com.android.camera", "com.android.browser")
        val result = LauncherLogic.getFavoriteApps(apps, favorites)
        assertEquals(2, result.size)
        assertEquals("Camera", result[0].label)
        assertEquals("Browser", result[1].label)
    }

    @Test
    fun `getFavoriteApps skips unknown packages and limits MAX_FAVORITES`() {
        val favorites = listOf("missing", "com.android.camera", "com.android.browser") + (1..10).map { "pkg$it" }
        val result = LauncherLogic.getFavoriteApps(apps, favorites)
        assertEquals(2, result.size)
        assertEquals("Camera", result[0].label)
        assertEquals("Browser", result[1].label)
    }

    @Test
    fun `getAppsInFolders returns all package names`() {
        val result = LauncherLogic.getAppsInFolders(folders)
        assertEquals(setOf("com.android.browser", "com.android.camera", "com.android.contacts"), result)
    }

    @Test
    fun `getVisibleApps excludes apps that are inside folders`() {
        val result = LauncherLogic.getVisibleApps(apps, folders)
        assertEquals(listOf("com.android.email"), result.map { it.packageName })
    }

    @Test
    fun `addAppToFolder moves app into target folder and removes from others`() {
        val result = LauncherLogic.addAppToFolder(folders, "f2", "com.android.camera")
        val work = result.first { it.id == "f1" }
        val social = result.first { it.id == "f2" }
        assertFalse(work.appPackageNames.contains("com.android.camera"))
        assertTrue(social.appPackageNames.contains("com.android.camera"))
    }

    @Test
    fun `addAppToFolder does not duplicate app in target folder`() {
        val result = LauncherLogic.addAppToFolder(folders, "f1", "com.android.browser")
        val work = result.first { it.id == "f1" }
        assertEquals(2, work.appPackageNames.size)
    }

    @Test
    fun `removeAppFromFolder only removes from target folder`() {
        val result = LauncherLogic.removeAppFromFolder(folders, "f1", "com.android.camera")
        val work = result.first { it.id == "f1" }
        val social = result.first { it.id == "f2" }
        assertEquals(listOf("com.android.browser"), work.appPackageNames)
        assertEquals(listOf("com.android.contacts"), social.appPackageNames)
    }

    @Test
    fun `createFolder adds new folder with name and empty apps`() {
        val result = LauncherLogic.createFolder(folders, "New")
        val created = result.last()
        assertEquals("New", created.name)
        assertTrue(created.appPackageNames.isEmpty())
        assertTrue(created.id.isNotBlank())
    }

    @Test
    fun `deleteFolder removes folder by id`() {
        val result = LauncherLogic.deleteFolder(folders, "f1")
        assertEquals(1, result.size)
        assertEquals("f2", result.first().id)
    }

    @Test
    fun `renameFolder updates only target folder`() {
        val result = LauncherLogic.renameFolder(folders, "f2", "Friends")
        val work = result.first { it.id == "f1" }
        val social = result.first { it.id == "f2" }
        assertEquals("Work", work.name)
        assertEquals("Friends", social.name)
    }

    // ---- Edge-Case Tests ----

    @Test
    fun `filterApps on empty list returns empty list`() {
        val result = LauncherLogic.filterApps(emptyList(), "cam")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterApps returns empty list when no apps match`() {
        val result = LauncherLogic.filterApps(apps, "zzzzz")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterApps with whitespace-only query returns all apps`() {
        val result = LauncherLogic.filterApps(apps, "   ")
        assertEquals(apps, result)
    }

    @Test
    fun `toggleFavorite on empty list adds package`() {
        val result = LauncherLogic.toggleFavorite(emptyList(), "pkg1")
        assertEquals(listOf("pkg1"), result)
    }

    @Test
    fun `moveFavoriteUp with negative index returns unchanged list`() {
        val favorites = listOf("pkg1", "pkg2", "pkg3")
        val result = LauncherLogic.moveFavoriteUp(favorites, -1)
        assertEquals(favorites, result)
    }

    @Test
    fun `moveFavoriteUp with out of bounds index returns unchanged list`() {
        val favorites = listOf("pkg1", "pkg2")
        val result = LauncherLogic.moveFavoriteUp(favorites, 5)
        assertEquals(favorites, result)
    }

    @Test
    fun `moveFavoriteDown with negative index returns unchanged list`() {
        val favorites = listOf("pkg1", "pkg2", "pkg3")
        val result = LauncherLogic.moveFavoriteDown(favorites, -1)
        assertEquals(favorites, result)
    }

    @Test
    fun `moveFavoriteDown with out of bounds index returns unchanged list`() {
        val favorites = listOf("pkg1", "pkg2")
        val result = LauncherLogic.moveFavoriteDown(favorites, 5)
        assertEquals(favorites, result)
    }

    @Test
    fun `moveFavoriteUp on single element list returns unchanged list`() {
        val favorites = listOf("pkg1")
        val result = LauncherLogic.moveFavoriteUp(favorites, 0)
        assertEquals(favorites, result)
    }

    @Test
    fun `moveFavoriteDown on single element list returns unchanged list`() {
        val favorites = listOf("pkg1")
        val result = LauncherLogic.moveFavoriteDown(favorites, 0)
        assertEquals(favorites, result)
    }

    @Test
    fun `moveFavoriteUp on empty list returns empty list`() {
        val result = LauncherLogic.moveFavoriteUp(emptyList(), 0)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `moveFavoriteDown on empty list returns empty list`() {
        val result = LauncherLogic.moveFavoriteDown(emptyList(), 0)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getFavoriteApps with empty allApps returns empty list`() {
        val result = LauncherLogic.getFavoriteApps(emptyList(), listOf("pkg1"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getFavoriteApps with empty favoritePackages returns empty list`() {
        val result = LauncherLogic.getFavoriteApps(apps, emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getFavoriteApps respects MAX_FAVORITES limit`() {
        val manyApps = (1..12).map { AppInfo("App$it", "pkg$it", null) }
        val manyFavorites = (1..12).map { "pkg$it" }
        val result = LauncherLogic.getFavoriteApps(manyApps, manyFavorites)
        assertEquals(LauncherLogic.MAX_FAVORITES, result.size)
    }

    @Test
    fun `getAppsInFolders with empty folders returns empty set`() {
        val result = LauncherLogic.getAppsInFolders(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getVisibleApps with no folders returns all apps`() {
        val result = LauncherLogic.getVisibleApps(apps, emptyList())
        assertEquals(apps, result)
    }

    @Test
    fun `getVisibleApps with all apps in folders returns empty list`() {
        val allInFolders = listOf(
            FolderInfo("f1", "All", apps.map { it.packageName })
        )
        val result = LauncherLogic.getVisibleApps(apps, allInFolders)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `addAppToFolder with non-existent folder id returns folders unchanged`() {
        val result = LauncherLogic.addAppToFolder(folders, "nonexistent", "com.android.email")
        // App should be removed from other folders but no folder gains it
        assertEquals(folders.size, result.size)
    }

    @Test
    fun `removeAppFromFolder with non-existent package leaves folders unchanged`() {
        val result = LauncherLogic.removeAppFromFolder(folders, "f1", "com.nonexistent")
        assertEquals(folders, result)
    }

    @Test
    fun `removeAppFromFolder with non-existent folder id leaves folders unchanged`() {
        val result = LauncherLogic.removeAppFromFolder(folders, "nonexistent", "com.android.browser")
        assertEquals(folders, result)
    }

    @Test
    fun `deleteFolder with non-existent id returns same list`() {
        val result = LauncherLogic.deleteFolder(folders, "nonexistent")
        assertEquals(folders, result)
    }

    @Test
    fun `renameFolder with non-existent id returns same list`() {
        val result = LauncherLogic.renameFolder(folders, "nonexistent", "NewName")
        assertEquals(folders, result)
    }

    @Test
    fun `createFolder on empty list creates first folder`() {
        val result = LauncherLogic.createFolder(emptyList(), "First")
        assertEquals(1, result.size)
        assertEquals("First", result[0].name)
        assertTrue(result[0].appPackageNames.isEmpty())
    }

    @Test
    fun `toggleFavorite removes and adds correctly in sequence`() {
        var favorites = emptyList<String>()
        favorites = LauncherLogic.toggleFavorite(favorites, "pkg1")
        assertEquals(listOf("pkg1"), favorites)
        favorites = LauncherLogic.toggleFavorite(favorites, "pkg2")
        assertEquals(listOf("pkg1", "pkg2"), favorites)
        favorites = LauncherLogic.toggleFavorite(favorites, "pkg1")
        assertEquals(listOf("pkg2"), favorites)
    }

    @Test
    fun `moveFavoriteUp last element moves to second to last`() {
        val favorites = listOf("a", "b", "c", "d")
        val result = LauncherLogic.moveFavoriteUp(favorites, 3)
        assertEquals(listOf("a", "b", "d", "c"), result)
    }

    @Test
    fun `moveFavoriteDown first element moves to second`() {
        val favorites = listOf("a", "b", "c", "d")
        val result = LauncherLogic.moveFavoriteDown(favorites, 0)
        assertEquals(listOf("b", "a", "c", "d"), result)
    }

    @Test
    fun `filterApps matches partial label at beginning`() {
        val result = LauncherLogic.filterApps(apps, "Bro")
        assertEquals(1, result.size)
        assertEquals("Browser", result[0].label)
    }

    @Test
    fun `filterApps matches partial label in middle`() {
        val result = LauncherLogic.filterApps(apps, "onta")
        assertEquals(1, result.size)
        assertEquals("Contacts", result[0].label)
    }

    @Test
    fun `rankAppSuggestions bevorzugt Prefix Treffer mit Nutzung vor schwächerem Match`() {
        val now = 1_000_000L
        val rankedApps = LauncherLogic.rankAppSuggestions(
            apps = listOf(
                AppInfo("YouTube", "com.google.android.youtube"),
                AppInfo("Your Files", "com.example.files"),
                AppInfo("Daily You", "com.example.daily")
            ),
            query = "you",
            appUsageStats = mapOf(
                "com.example.files" to AppUsageStats(
                    packageName = "com.example.files",
                    launchCount = 5,
                    lastLaunchedAt = now - 2_000L
                )
            ),
            now = now,
            limit = 3
        )

        assertEquals(
            listOf("Your Files", "YouTube", "Daily You"),
            rankedApps.map { it.label }
        )
    }

    @Test
    fun `rankAppSuggestions bevorzugt Wortanfang vor reinem Enthalten`() {
        val rankedApps = LauncherLogic.rankAppSuggestions(
            apps = listOf(
                AppInfo("GitHub Copilot", "copilot"),
                AppInfo("My github notes", "notes")
            ),
            query = "cop",
            appUsageStats = emptyMap(),
            now = 10_000L,
            limit = 2
        )

        assertEquals(listOf("GitHub Copilot"), rankedApps.map { it.label })
    }

    @Test
    fun `rankWebSuggestions bevorzugt haeufige und aktuelle Verlaufseintraege bei gleichem Match`() {
        val now = 5_000_000L
        val rankedHistory = LauncherLogic.rankWebSuggestions(
            history = listOf(
                SearchHistoryEntry("youtube video downloader", usageCount = 6, lastSearchedAt = now - 1_000L),
                SearchHistoryEntry("youtube music", usageCount = 1, lastSearchedAt = now - 500L),
                SearchHistoryEntry("learn kotlin", usageCount = 9, lastSearchedAt = now - 100L)
            ),
            query = "you",
            now = now,
            limit = 3
        )

        assertEquals(
            listOf("youtube video downloader", "youtube music"),
            rankedHistory.map { it.query }
        )
    }

    @Test
    fun `rankWebSuggestions gibt leer bei leerer Query zurueck`() {
        val rankedHistory = LauncherLogic.rankWebSuggestions(
            history = listOf(SearchHistoryEntry("weather berlin", usageCount = 2, lastSearchedAt = 100L)),
            query = "   ",
            now = 1_000L,
            limit = 3
        )

        assertTrue(rankedHistory.isEmpty())
    }

    @Test
    fun `resolvePrimarySuggestion bevorzugt beste App vor Verlauf bei hoeherem Score`() {
        val now = 2_000_000L
        val result = LauncherLogic.resolvePrimarySuggestion(
            apps = listOf(
                AppInfo("YouTube", "youtube"),
                AppInfo("Browser", "browser")
            ),
            history = listOf(
                SearchHistoryEntry("youtube video downloader", usageCount = 1, lastSearchedAt = now - 20_000L)
            ),
            query = "you",
            appUsageStats = mapOf(
                "youtube" to AppUsageStats("youtube", launchCount = 5, lastLaunchedAt = now - 1_000L)
            ),
            smartSuggestionsEnabled = true,
            now = now
        )

        assertTrue(result is LauncherLogic.PrimarySearchSuggestion.AppSuggestion)
        assertEquals("YouTube", (result as LauncherLogic.PrimarySearchSuggestion.AppSuggestion).app.label)
    }

    @Test
    fun `resolvePrimarySuggestion nimmt Verlauf wenn kein besserer App Treffer existiert`() {
        val now = 3_000_000L
        val result = LauncherLogic.resolvePrimarySuggestion(
            apps = listOf(AppInfo("Browser", "browser")),
            history = listOf(
                SearchHistoryEntry("weather berlin", usageCount = 4, lastSearchedAt = now - 1_000L)
            ),
            query = "wea",
            appUsageStats = emptyMap(),
            smartSuggestionsEnabled = true,
            now = now
        )

        assertTrue(result is LauncherLogic.PrimarySearchSuggestion.HistorySuggestion)
        assertEquals("weather berlin", (result as LauncherLogic.PrimarySearchSuggestion.HistorySuggestion).entry.query)
    }

    @Test
    fun `resolvePrimarySuggestion faellt auf Websuche zurueck wenn kein Treffer existiert`() {
        val result = LauncherLogic.resolvePrimarySuggestion(
            apps = apps,
            history = emptyList(),
            query = "unbekanntes suchwort",
            appUsageStats = emptyMap(),
            smartSuggestionsEnabled = true,
            now = 10_000L
        )

        assertTrue(result is LauncherLogic.PrimarySearchSuggestion.WebSuggestion)
        assertEquals(
            "unbekanntes suchwort",
            (result as LauncherLogic.PrimarySearchSuggestion.WebSuggestion).query
        )
    }

    @Test
    fun `resolvePrimarySuggestion nutzt bei deaktivierten smart suggestions nur App oder Web Fallback`() {
        val result = LauncherLogic.resolvePrimarySuggestion(
            apps = listOf(AppInfo("YouTube", "youtube")),
            history = listOf(SearchHistoryEntry("youtube video downloader", usageCount = 99, lastSearchedAt = 1_000L)),
            query = "you",
            appUsageStats = emptyMap(),
            smartSuggestionsEnabled = false,
            now = 2_000L
        )

        assertTrue(result is LauncherLogic.PrimarySearchSuggestion.AppSuggestion)
        assertEquals("YouTube", (result as LauncherLogic.PrimarySearchSuggestion.AppSuggestion).app.label)
    }

    @Test
    fun `rankAppSuggestions respects max of three suggestions`() {
        val now = 7_000_000L
        val rankedApps = LauncherLogic.rankAppSuggestions(
            apps = listOf(
                AppInfo("YouTube", "youtube"),
                AppInfo("YouTube Music", "ytmusic"),
                AppInfo("Your Files", "files"),
                AppInfo("Young Calendar", "calendar")
            ),
            query = "you",
            appUsageStats = mapOf(
                "youtube" to AppUsageStats("youtube", 4, now - 1_000L),
                "ytmusic" to AppUsageStats("ytmusic", 3, now - 2_000L),
                "files" to AppUsageStats("files", 2, now - 3_000L),
                "calendar" to AppUsageStats("calendar", 1, now - 4_000L)
            ),
            now = now,
            limit = 3
        )

        assertEquals(3, rankedApps.size)
    }

    @Test
    fun `rankWebSuggestions respects max of one suggestion`() {
        val now = 8_000_000L
        val rankedHistory = LauncherLogic.rankWebSuggestions(
            history = listOf(
                SearchHistoryEntry("weather berlin", usageCount = 4, lastSearchedAt = now - 1_000L),
                SearchHistoryEntry("weather hamburg", usageCount = 3, lastSearchedAt = now - 2_000L)
            ),
            query = "wea",
            now = now,
            limit = 1
        )

        assertEquals(1, rankedHistory.size)
        assertEquals("weather berlin", rankedHistory.first().query)
    }

    // --- mergeInstalledApps ---

    @Test
    fun `mergeInstalledApps follows the basic list order and contents`() {
        val basic = listOf(
            AppInfo("Camera", "com.android.camera", null),
            AppInfo("Browser", "com.android.browser", null),
        )
        val result = LauncherLogic.mergeInstalledApps(
            basicApps = basic,
            existingApps = emptyList(),
        )
        assertEquals(listOf("com.android.camera", "com.android.browser"), result.map { it.packageName })
    }

    @Test
    fun `mergeInstalledApps drops apps that are no longer installed`() {
        val basic = listOf(AppInfo("Camera", "com.android.camera", null))
        val existing = listOf(
            AppInfo("Camera", "com.android.camera", null),
            AppInfo("Removed", "com.android.removed", null),
        )
        val result = LauncherLogic.mergeInstalledApps(basic, existing)
        assertEquals(listOf("com.android.camera"), result.map { it.packageName })
    }

    @Test
    fun `normalizeInstalledApps sorts case-insensitively by label`() {
        val raw = listOf(
            AppInfo("zebra", "com.z"),
            AppInfo("Apple", "com.a"),
            AppInfo("banana", "com.b"),
        )
        val result = LauncherLogic.normalizeInstalledApps(raw, ownPackage = "com.me", isOwnDefaultLauncher = false)
        assertEquals(listOf("com.a", "com.b", "com.z"), result.map { it.packageName })
    }

    @Test
    fun `normalizeInstalledApps deduplicates by package keeping first occurrence`() {
        val raw = listOf(
            AppInfo("App A", "com.app.a"),
            AppInfo("App A Alias", "com.app.a"),
            AppInfo("App B", "com.app.b"),
        )
        val result = LauncherLogic.normalizeInstalledApps(raw, ownPackage = "com.me", isOwnDefaultLauncher = false)
        assertEquals(2, result.size)
        assertEquals("App A", result.first { it.packageName == "com.app.a" }.label)
    }

    @Test
    fun `normalizeInstalledApps hides own package only when it is the default launcher`() {
        val raw = listOf(
            AppInfo("Rethrone", "com.me"),
            AppInfo("Other", "com.other"),
        )
        val whenDefault = LauncherLogic.normalizeInstalledApps(
            raw,
            ownPackage = "com.me",
            isOwnDefaultLauncher = true,
        )
        assertEquals(listOf("com.other"), whenDefault.map { it.packageName })

        val whenNotDefault = LauncherLogic.normalizeInstalledApps(
            raw,
            ownPackage = "com.me",
            isOwnDefaultLauncher = false,
        )
        assertEquals(setOf("com.me", "com.other"), whenNotDefault.map { it.packageName }.toSet())
    }

    @Test
    fun `evaluateDefaultLauncherWarning does not warn when Rethrone is the launcher`() {
        val decision = LauncherLogic.evaluateDefaultLauncherWarning(
            resolvedPackage = "com.me",
            ownPackage = "com.me",
            warningAlreadyShown = true,
            lastPackage = "com.other",
        )
        assertFalse(decision.showWarning)
        assertFalse(decision.warningShown)
        assertEquals("com.me", decision.lastPackage)
    }

    @Test
    fun `evaluateDefaultLauncherWarning warns first time a foreign launcher is default`() {
        val decision = LauncherLogic.evaluateDefaultLauncherWarning(
            resolvedPackage = "com.other",
            ownPackage = "com.me",
            warningAlreadyShown = false,
            lastPackage = null,
        )
        assertTrue(decision.showWarning)
        assertTrue(decision.warningShown)
        assertEquals("com.other", decision.lastPackage)
    }

    @Test
    fun `evaluateDefaultLauncherWarning does not repeat warning for the same foreign launcher`() {
        val decision = LauncherLogic.evaluateDefaultLauncherWarning(
            resolvedPackage = "com.other",
            ownPackage = "com.me",
            warningAlreadyShown = true,
            lastPackage = "com.other",
        )
        assertFalse(decision.showWarning)
        assertTrue(decision.warningShown)
    }

    @Test
    fun `evaluateDefaultLauncherWarning warns again when the foreign launcher changes`() {
        val decision = LauncherLogic.evaluateDefaultLauncherWarning(
            resolvedPackage = "com.new",
            ownPackage = "com.me",
            warningAlreadyShown = true,
            lastPackage = "com.other",
        )
        assertTrue(decision.showWarning)
        assertEquals("com.new", decision.lastPackage)
    }

    @Test
    fun `firstLaunchablePackage returns the first candidate in priority order`() {
        val installed = setOf("com.google.android.deskclock", "com.android.clock")
        val result = LauncherLogic.firstLaunchablePackage(LauncherLogic.KNOWN_CLOCK_PACKAGES) {
            it in installed
        }
        assertEquals("com.google.android.deskclock", result)
    }

    @Test
    fun `firstLaunchablePackage returns null when no candidate is launchable`() {
        val result = LauncherLogic.firstLaunchablePackage(LauncherLogic.KNOWN_CLOCK_PACKAGES) { false }
        assertNull(result)
    }

    @Test
    fun `firstLaunchablePackage treats predicate exceptions as not launchable`() {
        val result = LauncherLogic.firstLaunchablePackage(listOf("a", "b")) { pkg ->
            if (pkg == "a") error("boom") else true
        }
        assertEquals("b", result)
    }

    // Test-Encoder: macht Encoding sichtbar, ohne android.net.Uri zu brauchen.
    private val testEncoder: (String) -> String = { it.replace(" ", "+") }

    @Test
    fun `resolveSearchUrl returns null for blank input`() {
        assertNull(LauncherLogic.resolveSearchUrl("   ", testEncoder))
    }

    @Test
    fun `resolveSearchUrl keeps an explicit http url as-is`() {
        assertEquals(
            "http://example.com/path",
            LauncherLogic.resolveSearchUrl("http://example.com/path", testEncoder),
        )
    }

    @Test
    fun `resolveSearchUrl prepends https for a bare domain`() {
        assertEquals(
            "https://example.com",
            LauncherLogic.resolveSearchUrl("example.com", testEncoder),
        )
    }

    @Test
    fun `resolveSearchUrl builds a google search for a plain term`() {
        assertEquals(
            "https://www.google.com/search?q=hello+world",
            LauncherLogic.resolveSearchUrl("hello world", testEncoder),
        )
    }

    @Test
    fun `highlightRange finds a case-insensitive match`() {
        assertEquals(6 to 11, LauncherLogic.highlightRange("Hello World", "world"))
    }

    @Test
    fun `highlightRange returns null when there is no match`() {
        assertNull(LauncherLogic.highlightRange("Hello", "xyz"))
    }

    @Test
    fun `highlightRange returns null for a blank query`() {
        assertNull(LauncherLogic.highlightRange("Hello", "   "))
    }

    @Test
    fun `highlightRange matches at the start`() {
        assertEquals(0 to 3, LauncherLogic.highlightRange("Settings", "set"))
    }

    // --- shouldShowNotificationDot ---

    @Test
    fun `notification dot shows for package with active notification`() {
        assertTrue(
            LauncherLogic.shouldShowNotificationDot(
                packageName = "com.whatsapp",
                activeNotificationPackages = setOf("com.whatsapp", "com.spotify.music"),
                dotsEnabled = true,
            )
        )
    }

    @Test
    fun `notification dot hidden without active notification`() {
        assertFalse(
            LauncherLogic.shouldShowNotificationDot(
                packageName = "com.whatsapp",
                activeNotificationPackages = setOf("com.spotify.music"),
                dotsEnabled = true,
            )
        )
    }

    @Test
    fun `notification dot hidden when setting is disabled`() {
        assertFalse(
            LauncherLogic.shouldShowNotificationDot(
                packageName = "com.whatsapp",
                activeNotificationPackages = setOf("com.whatsapp"),
                dotsEnabled = false,
            )
        )
    }

    // --- Ordner-Grid (3×3-Pager) ---

    @Test
    fun `folderPageCount rounds up and never drops below one page`() {
        assertEquals(1, LauncherLogic.folderPageCount(0))
        assertEquals(1, LauncherLogic.folderPageCount(9))
        assertEquals(2, LauncherLogic.folderPageCount(10))
        assertEquals(3, LauncherLogic.folderPageCount(19))
    }

    @Test
    fun `folderGridSlotAt maps touch to cell and page offset`() {
        // 300×300-Grid → Zellen à 100×100; Mitte der mittleren Zelle = Slot 4.
        assertEquals(4, LauncherLogic.folderGridSlotAt(150f, 150f, 300, 300, currentPage = 0))
        // Gleiche Zelle auf Seite 2 → +9 Slots.
        assertEquals(13, LauncherLogic.folderGridSlotAt(150f, 150f, 300, 300, currentPage = 1))
        // Berührung außerhalb wird auf die Randzelle begrenzt (unten rechts = Slot 8).
        assertEquals(8, LauncherLogic.folderGridSlotAt(999f, 999f, 300, 300, currentPage = 0))
    }

    @Test
    fun `folderGridSlotAt returns null for an unmeasured grid`() {
        assertNull(LauncherLogic.folderGridSlotAt(10f, 10f, 0, 300, currentPage = 0))
        assertNull(LauncherLogic.folderGridSlotAt(10f, 10f, 300, 0, currentPage = 0))
    }

    @Test
    fun `moveFolderApp reorders and clamps the target index`() {
        val packages = listOf("a", "b", "c", "d")

        assertEquals(listOf("b", "c", "a", "d"), LauncherLogic.moveFolderApp(packages, "a", 2))
        // Ziel hinter dem Listenende wird auf den letzten Platz begrenzt.
        assertEquals(listOf("b", "c", "d", "a"), LauncherLogic.moveFolderApp(packages, "a", 99))
    }

    @Test
    fun `moveFolderApp returns null when nothing changes or the app is missing`() {
        val packages = listOf("a", "b", "c")

        assertNull(LauncherLogic.moveFolderApp(packages, "b", 1))
        assertNull(LauncherLogic.moveFolderApp(packages, "zz", 0))
        assertNull(LauncherLogic.moveFolderApp(emptyList(), "a", 0))
    }
}
