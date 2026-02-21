package com.example.androidlauncher

import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.FolderInfo
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
}
