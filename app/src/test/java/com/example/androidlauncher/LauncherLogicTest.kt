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
}
