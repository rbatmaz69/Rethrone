package com.example.androidlauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
}

