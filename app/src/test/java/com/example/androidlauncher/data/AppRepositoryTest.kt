package com.example.androidlauncher.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AppRepositoryTest {

    @Test
    fun testGetInstalledApps() = runTest {
        val context = mockk<Context>()
        val packageManager = mockk<PackageManager>()

        val dummyFile = File.createTempFile("dummy", "dir")
        dummyFile.delete()
        dummyFile.mkdir()
        every { context.filesDir } returns dummyFile

        every { context.packageManager } returns packageManager
        // Eigener Paketname wird nun (eager) an LauncherLogic.normalizeInstalledApps gereicht.
        every { context.packageName } returns "com.rethrone.launcher"

        val intentB = mockk<Intent>()
        every { packageManager.getLaunchIntentForPackage("com.app.b") } returns intentB

        val intentA = mockk<Intent>()
        every { packageManager.getLaunchIntentForPackage("com.app.a") } returns intentA

        val resolveInfo1 = mockk<ResolveInfo>()
        every { resolveInfo1.loadLabel(packageManager) } returns "App B"
        resolveInfo1.activityInfo = ActivityInfo().apply { packageName = "com.app.b" }

        val resolveInfo2 = mockk<ResolveInfo>()
        every { resolveInfo2.loadLabel(packageManager) } returns "App A"
        resolveInfo2.activityInfo = ActivityInfo().apply { packageName = "com.app.a" }

        val resolveInfo3 = mockk<ResolveInfo>()
        every { resolveInfo3.loadLabel(packageManager) } returns "App A Dup"
        resolveInfo3.activityInfo = ActivityInfo().apply { packageName = "com.app.a" }

        every {
            packageManager.queryIntentActivities(any(), 0)
        } returns listOf(resolveInfo1, resolveInfo2, resolveInfo3)

        val repository = AppRepository(context)
        val apps = repository.getInstalledApps()

        assertEquals(2, apps.size)
        assertEquals("App A", apps[0].label)
        assertEquals("com.app.a", apps[0].packageName)

        assertEquals("App B", apps[1].label)
        assertEquals("com.app.b", apps[1].packageName)

        dummyFile.deleteRecursively()
    }

    @Test
    fun testCleanupLegacyCache() {
        val context = mockk<Context>()
        val dummyFilesDir = File.createTempFile("dummy", "filesdir")
        dummyFilesDir.delete()
        dummyFilesDir.mkdir()
        val dummyCacheDir = File.createTempFile("dummy", "cachedir")
        dummyCacheDir.delete()
        dummyCacheDir.mkdir()

        val legacyDir = File(dummyCacheDir, "app_icons")
        legacyDir.mkdir()
        val testFile = File(legacyDir, "test.png")
        testFile.createNewFile()

        every { context.filesDir } returns dummyFilesDir
        every { context.cacheDir } returns dummyCacheDir

        val repository = AppRepository(context)
        assert(legacyDir.exists())

        repository.cleanupLegacyCache()

        assert(!legacyDir.exists())

        dummyFilesDir.deleteRecursively()
        dummyCacheDir.deleteRecursively()
    }

    // --- invalidateCacheOnAppUpdate: Build-Token als Marker-Datei im Cache-Verzeichnis ---

    /** Baut Context+PackageManager-Mocks für die Token-Tests; Prefs liefern [legacyToken]. */
    private fun mockContextForInvalidate(
        filesDir: File,
        versionCode: Int,
        lastUpdateTime: Long,
        legacyToken: String? = null,
    ): Pair<Context, SharedPreferences.Editor> {
        val context = mockk<Context>()
        val packageManager = mockk<PackageManager>()
        val pkgInfo = PackageInfo().apply {
            @Suppress("DEPRECATION")
            this.versionCode = versionCode
            this.lastUpdateTime = lastUpdateTime
        }
        every { context.filesDir } returns filesDir
        every { context.packageName } returns "com.rethrone.launcher"
        every { context.packageManager } returns packageManager
        every { packageManager.getPackageInfo("com.rethrone.launcher", 0) } returns pkgInfo

        val prefs = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>()
        every { context.getSharedPreferences("icon_cache_meta", Context.MODE_PRIVATE) } returns prefs
        every { prefs.getString("app_build_token", null) } returns legacyToken
        every { prefs.edit() } returns editor
        every { editor.clear() } returns editor
        every { editor.apply() } just Runs
        return context to editor
    }

    @Test
    fun invalidateCache_firstRun_clearsCacheAndWritesTokenFile() = runTest {
        val filesDir = createTempDir()
        val iconDir = File(filesDir, "app_icons").apply { mkdirs() }
        val staleIcon = File(iconDir, "com.app.a.png").apply { createNewFile() }
        val (context, _) = mockContextForInvalidate(filesDir, versionCode = 2, lastUpdateTime = 1000L)

        AppRepository(context).invalidateCacheOnAppUpdate()

        assertFalse(staleIcon.exists())
        assertEquals("2:1000", File(iconDir, ".build_token").readText())
        filesDir.deleteRecursively()
    }

    @Test
    fun invalidateCache_unchangedToken_keepsCache() = runTest {
        val filesDir = createTempDir()
        val iconDir = File(filesDir, "app_icons").apply { mkdirs() }
        File(iconDir, ".build_token").writeText("2:1000")
        val cachedIcon = File(iconDir, "com.app.a.png").apply { createNewFile() }
        val (context, _) = mockContextForInvalidate(filesDir, versionCode = 2, lastUpdateTime = 1000L)

        AppRepository(context).invalidateCacheOnAppUpdate()

        assertTrue(cachedIcon.exists())
        filesDir.deleteRecursively()
    }

    @Test
    fun invalidateCache_changedToken_clearsCacheAndUpdatesTokenFile() = runTest {
        val filesDir = createTempDir()
        val iconDir = File(filesDir, "app_icons").apply { mkdirs() }
        File(iconDir, ".build_token").writeText("1:500")
        val staleIcon = File(iconDir, "com.app.a.png").apply { createNewFile() }
        val (context, _) = mockContextForInvalidate(filesDir, versionCode = 2, lastUpdateTime = 1000L)

        AppRepository(context).invalidateCacheOnAppUpdate()

        assertFalse(staleIcon.exists())
        assertEquals("2:1000", File(iconDir, ".build_token").readText())
        filesDir.deleteRecursively()
    }

    @Test
    fun invalidateCache_migratesMatchingLegacyToken_withoutClearingCache() = runTest {
        val filesDir = createTempDir()
        val iconDir = File(filesDir, "app_icons").apply { mkdirs() }
        val cachedIcon = File(iconDir, "com.app.a.png").apply { createNewFile() }
        val (context, editor) = mockContextForInvalidate(
            filesDir,
            versionCode = 2,
            lastUpdateTime = 1000L,
            legacyToken = "2:1000",
        )

        AppRepository(context).invalidateCacheOnAppUpdate()

        // Cache bleibt erhalten, Token wandert in die Marker-Datei, Legacy-Prefs werden geleert.
        assertTrue(cachedIcon.exists())
        assertEquals("2:1000", File(iconDir, ".build_token").readText())
        verify { editor.clear() }
        filesDir.deleteRecursively()
    }

    // --- B4: Icon-Pack-Integration ---

    /** Context+PM-Mock mit leerem Icon-Cache-Verzeichnis für die Pack-Tests. */
    private fun mockContextForIconPack(filesDir: File): Pair<Context, PackageManager> {
        val context = mockk<Context>()
        val packageManager = mockk<PackageManager>()
        every { context.filesDir } returns filesDir
        every { context.packageManager } returns packageManager
        every { packageManager.getLaunchIntentForPackage(any()) } returns null
        return context to packageManager
    }

    @Test
    fun loadResolvedIcon_prefersPackIconAndSkipsQualityEvaluation() = runTest {
        val filesDir = createTempDir()
        val (context, packageManager) = mockContextForIconPack(filesDir)
        val iconPacks = mockk<IconPackRepository>()
        coEvery { iconPacks.hasPackIcon("com.pack.a", "com.app.a", null) } returns true
        coEvery {
            iconPacks.loadIconBitmap("com.pack.a", "com.app.a", null, any())
        } returns mockk(relaxed = true)

        val repository = AppRepository(context, iconPacks)
        val resolved = repository.loadResolvedIcon(
            AppInfo(label = "App A", packageName = "com.app.a", launchIntent = null),
            iconPack = "com.pack.a",
        )

        // Pack-Grafik wird genutzt und die Qualitäts-Heuristik übersprungen (keep-original).
        assertEquals(AutoIconFallbackType.ORIGINAL, resolved?.autoFallback?.type)
        assertEquals("icon_pack", resolved?.autoFallback?.reason)
        // System-Icon-Pfad wurde nie betreten.
        verify(exactly = 0) { packageManager.getApplicationInfo(any<String>(), any<Int>()) }
        filesDir.deleteRecursively()
    }

    @Test
    fun loadResolvedIcon_fallsBackToSystemWhenPackHasNoIcon() = runTest {
        val filesDir = createTempDir()
        val (context, packageManager) = mockContextForIconPack(filesDir)
        every { packageManager.getApplicationInfo("com.app.a", 0) } throws
            PackageManager.NameNotFoundException()
        val iconPacks = mockk<IconPackRepository>()
        coEvery { iconPacks.hasPackIcon(any(), any(), any()) } returns false
        coEvery { iconPacks.loadIconBitmap(any(), any(), any(), any()) } returns null

        val repository = AppRepository(context, iconPacks)
        val resolved = repository.loadResolvedIcon(
            AppInfo(label = "App A", packageName = "com.app.a", launchIntent = null),
            iconPack = "com.pack.a",
        )

        // Pack liefert nichts → System-Pfad wird versucht (schlägt im JVM-Test kontrolliert fehl).
        assertEquals(null, resolved)
        verify { packageManager.getApplicationInfo("com.app.a", 0) }
        filesDir.deleteRecursively()
    }

    @Test
    fun loadIcon_withoutIconPackNeverTouchesThePackRepository() = runTest {
        val filesDir = createTempDir()
        val (context, packageManager) = mockContextForIconPack(filesDir)
        every { packageManager.getApplicationInfo("com.app.a", 0) } throws
            PackageManager.NameNotFoundException()
        val iconPacks = mockk<IconPackRepository>()

        val repository = AppRepository(context, iconPacks)
        repository.loadIcon("com.app.a")

        coVerify(exactly = 0) { iconPacks.loadIconBitmap(any(), any(), any(), any()) }
        coVerify(exactly = 0) { iconPacks.hasPackIcon(any(), any(), any()) }
        filesDir.deleteRecursively()
    }

    private fun createTempDir(): File = File.createTempFile("dummy", "filesdir").apply {
        delete()
        mkdir()
    }
}
