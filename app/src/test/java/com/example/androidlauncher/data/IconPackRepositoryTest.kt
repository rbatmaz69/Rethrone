package com.example.androidlauncher.data

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IconPackRepositoryTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var repository: IconPackRepository

    @Before
    fun setup() {
        context = mockk()
        packageManager = mockk()
        every { context.packageManager } returns packageManager
        repository = IconPackRepository(context)
    }

    private fun resolveInfoFor(packageName: String, label: String?): ResolveInfo {
        val resolveInfo = mockk<ResolveInfo>()
        resolveInfo.activityInfo = ActivityInfo().apply { this.packageName = packageName }
        if (label != null) {
            every { resolveInfo.loadLabel(packageManager) } returns label
        } else {
            every { resolveInfo.loadLabel(packageManager) } throws RuntimeException("kein Label")
        }
        return resolveInfo
    }

    @Test
    fun `installedIconPacks queries both theme intents, dedupes and sorts by label`() = runTest {
        val packA = resolveInfoFor("com.pack.a", "Zebra Icons")
        val packB = resolveInfoFor("com.pack.b", "Arctic Icons")
        val packADup = resolveInfoFor("com.pack.a", "Zebra Icons")
        // Erste Query (ADW) liefert A+B, zweite (Nova) A erneut → Dedupe.
        every { packageManager.queryIntentActivities(any(), 0) } returnsMany listOf(
            listOf(packA, packB),
            listOf(packADup),
        )

        val packs = repository.installedIconPacks()

        assertEquals(
            listOf(
                IconPack("com.pack.b", "Arctic Icons"),
                IconPack("com.pack.a", "Zebra Icons"),
            ),
            packs
        )
        verify(exactly = 2) { packageManager.queryIntentActivities(any(), 0) }
    }

    @Test
    fun `installedIconPacks survives PM exceptions and null labels`() = runTest {
        val unlabeled = resolveInfoFor("com.pack.c", label = null)
        every { packageManager.queryIntentActivities(any(), 0) } returnsMany listOf(
            listOf(unlabeled),
        ) andThenThrows SecurityException("OEM")

        val packs = repository.installedIconPacks()

        // Label-Fallback auf den Package-Namen; zweite Query schlug fehl → nur ein Eintrag.
        assertEquals(listOf(IconPack("com.pack.c", "com.pack.c")), packs)
    }

    @Test
    fun `loadAppFilter memoizes per pack`() = runTest {
        val resources = mockk<Resources>()
        every { packageManager.getResourcesForApplication("com.pack.a") } returns resources
        every { resources.getIdentifier("appfilter", "xml", "com.pack.a") } returns 0
        every { resources.assets } throws IllegalStateException("kein Asset")

        assertTrue(repository.loadAppFilter("com.pack.a").isEmpty())
        assertTrue(repository.loadAppFilter("com.pack.a").isEmpty())

        // Zweiter Aufruf kommt aus dem Cache – Resources nur einmal angefasst.
        verify(exactly = 1) { packageManager.getResourcesForApplication("com.pack.a") }
    }

    @Test
    fun `invalidate drops the cached app filter`() = runTest {
        val resources = mockk<Resources>()
        every { packageManager.getResourcesForApplication("com.pack.a") } returns resources
        every { resources.getIdentifier("appfilter", "xml", "com.pack.a") } returns 0
        every { resources.assets } throws IllegalStateException("kein Asset")

        repository.loadAppFilter("com.pack.a")
        repository.invalidate("com.pack.a")
        repository.loadAppFilter("com.pack.a")

        verify(exactly = 2) { packageManager.getResourcesForApplication("com.pack.a") }
    }

    @Test
    fun `loadIconBitmap returns null without a mapping and hasPackIcon reflects it`() = runTest {
        every { packageManager.getResourcesForApplication("com.pack.a") } throws
            PackageManager.NameNotFoundException()

        assertNull(repository.loadIconBitmap("com.pack.a", "com.app", null, sizePx = 144))
        assertEquals(false, repository.hasPackIcon("com.pack.a", "com.app", null))
    }
}
