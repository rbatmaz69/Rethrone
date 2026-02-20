package com.example.androidlauncher

import com.example.androidlauncher.data.AppInfo
import org.junit.Assert.*
import org.junit.Test

class LauncherLogicTest {

    private val apps = listOf(
        AppInfo("Browser", "com.android.browser", null),
        AppInfo("Camera", "com.android.camera", null),
        AppInfo("Contacts", "com.android.contacts", null),
        AppInfo("Email", "com.android.email", null)
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
}
