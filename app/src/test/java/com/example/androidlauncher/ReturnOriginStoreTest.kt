package com.example.androidlauncher

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.geometry.Rect
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReturnOriginStoreTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        prefs = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences("return_origin_store", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
    }

    @Test
    fun testGetStoredPackageNames() {
        val map = mapOf(
            "origin_com.test.app" to "data",
            "origin_com.other.app" to "data2",
            "last_launched_package" to "ignore"
        )
        every { prefs.all } returns map

        val packages = ReturnOriginStore.getStoredPackageNames(context)
        assertEquals(2, packages.size)
        assertTrue(packages.contains("com.test.app"))
        assertTrue(packages.contains("com.other.app"))
    }

    @Test
    fun testGetStoredOriginCount() {
        val map = mapOf(
            "origin_com.test.app" to "data",
            "origin_com.other.app" to "data2",
            "last_launched_package" to "ignore"
        )
        every { prefs.all } returns map

        val count = ReturnOriginStore.getStoredOriginCount(context)
        assertEquals(2, count)
    }

    @Test
    fun testSaveAndGet_WithBounds() {
        val animation = ReturnAnimation(
            bounds = Rect(10f, 20f, 30f, 40f),
            source = LaunchSource.HOME,
            packageName = "com.source.app",
            launchedPackageName = "com.target.app"
        )

        ReturnOriginStore.save(context, "com.target.app", animation)

        verify {
            editor.putString("origin_com.target.app", "HOME|com.source.app|com.target.app|10.0,20.0,30.0,40.0")
        }
        verify {
            editor.putString("last_launched_package", "com.target.app")
        }
        verify { editor.apply() }

        // test get
        every { prefs.getString("origin_com.target.app", null) } returns "HOME|com.source.app|com.target.app|10.0,20.0,30.0,40.0"

        val decoded = ReturnOriginStore.get(context, "com.target.app")
        assertNotNull(decoded)
        assertEquals(LaunchSource.HOME, decoded?.source)
        assertEquals("com.source.app", decoded?.packageName)
        assertEquals("com.target.app", decoded?.launchedPackageName)
        assertEquals(Rect(10f, 20f, 30f, 40f), decoded?.bounds)
    }

    @Test
    fun testSaveAndGet_WithoutBounds() {
        val animation = ReturnAnimation(
            bounds = null,
            source = LaunchSource.DRAWER,
            packageName = "com.source.app",
            launchedPackageName = "com.target.app"
        )

        ReturnOriginStore.save(context, "com.target.app", animation)

        verify {
            editor.putString("origin_com.target.app", "DRAWER|com.source.app|com.target.app|null")
        }

        every { prefs.getString("origin_com.target.app", null) } returns "DRAWER|com.source.app|com.target.app|null"

        val decoded = ReturnOriginStore.get(context, "com.target.app")
        assertNotNull(decoded)
        assertNull(decoded?.bounds)
        assertEquals(LaunchSource.DRAWER, decoded?.source)
    }

    @Test
    fun testGetLastLaunchedPackageName() {
        every { prefs.getString("last_launched_package", null) } returns "com.some.app"
        val pkg = ReturnOriginStore.getLastLaunchedPackageName(context)
        assertEquals("com.some.app", pkg)
    }

    @Test
    fun testClear() {
        ReturnOriginStore.clear(context, "com.target.app")
        verify { editor.remove("origin_com.target.app") }
        verify { editor.apply() }
    }
}









