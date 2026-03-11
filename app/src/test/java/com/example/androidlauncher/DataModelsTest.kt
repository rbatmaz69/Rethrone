package com.example.androidlauncher

import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.FolderInfo
import com.example.androidlauncher.data.FontSize
import com.example.androidlauncher.data.IconSize
import org.junit.Assert.*
import org.junit.Test
import androidx.compose.ui.unit.dp

class DataModelsTest {

    @Test
    fun `font sizes expose expected labels and scales`() {
        assertEquals("Klein", FontSize.SMALL.label)
        assertEquals(0.85f, FontSize.SMALL.scale, 0.0001f)

        assertEquals("Standard", FontSize.STANDARD.label)
        assertEquals(1.0f, FontSize.STANDARD.scale, 0.0001f)

        assertEquals("Groß", FontSize.LARGE.label)
        assertEquals(1.2f, FontSize.LARGE.scale, 0.0001f)
    }

    @Test
    fun `icon sizes expose expected labels and values`() {
        assertEquals("Klein", IconSize.SMALL.label)
        assertEquals(40.dp, IconSize.SMALL.size)

        assertEquals("Standard", IconSize.STANDARD.label)
        assertEquals(48.dp, IconSize.STANDARD.size)

        assertEquals("Groß", IconSize.LARGE.label)
        assertEquals(56.dp, IconSize.LARGE.size)
    }

    @Test
    fun `FontSize values count is 3`() {
        assertEquals(3, FontSize.entries.size)
    }

    @Test
    fun `IconSize values count is 3`() {
        assertEquals(3, IconSize.entries.size)
    }

    @Test
    fun `FontSize valueOf works for all entries`() {
        FontSize.entries.forEach { fs ->
            assertEquals(fs, FontSize.valueOf(fs.name))
        }
    }

    @Test
    fun `IconSize valueOf works for all entries`() {
        IconSize.entries.forEach { ic ->
            assertEquals(ic, IconSize.valueOf(ic.name))
        }
    }

    @Test
    fun `FontSize scales are ordered ascending`() {
        assertTrue(FontSize.SMALL.scale < FontSize.STANDARD.scale)
        assertTrue(FontSize.STANDARD.scale < FontSize.LARGE.scale)
    }

    // ---- AppInfo Tests ----

    @Test
    fun `AppInfo default optional fields are null`() {
        val app = AppInfo(label = "Test", packageName = "com.test")
        assertNull(app.iconBitmap)
        assertNull(app.lucideIcon)
        assertNull(app.autoIconFallback)
        assertNull(app.autoIconRule)
    }

    @Test
    fun `AppInfo equality works for same data`() {
        val a = AppInfo(label = "App", packageName = "com.app")
        val b = AppInfo(label = "App", packageName = "com.app")
        assertEquals(a, b)
    }

    @Test
    fun `AppInfo inequality for different packageName`() {
        val a = AppInfo(label = "App", packageName = "com.app1")
        val b = AppInfo(label = "App", packageName = "com.app2")
        assertNotEquals(a, b)
    }

    @Test
    fun `AppInfo inequality for different label`() {
        val a = AppInfo(label = "App1", packageName = "com.app")
        val b = AppInfo(label = "App2", packageName = "com.app")
        assertNotEquals(a, b)
    }

    @Test
    fun `AppInfo copy changes only specified fields`() {
        val original = AppInfo(label = "App", packageName = "com.app")
        val copied = original.copy(label = "NewApp")
        assertEquals("NewApp", copied.label)
        assertEquals("com.app", copied.packageName)
        assertNull(copied.iconBitmap)
        assertNull(copied.autoIconFallback)
        assertNull(copied.autoIconRule)
    }

    @Test
    fun `AppInfo hashCode consistent for equal instances`() {
        val a = AppInfo(label = "App", packageName = "com.app")
        val b = AppInfo(label = "App", packageName = "com.app")
        assertEquals(a.hashCode(), b.hashCode())
    }

    // ---- FolderInfo Tests ----

    @Test
    fun `FolderInfo equality works for same data`() {
        val a = FolderInfo(id = "1", name = "Work", appPackageNames = listOf("com.app"))
        val b = FolderInfo(id = "1", name = "Work", appPackageNames = listOf("com.app"))
        assertEquals(a, b)
    }

    @Test
    fun `FolderInfo inequality for different id`() {
        val a = FolderInfo(id = "1", name = "Work", appPackageNames = emptyList())
        val b = FolderInfo(id = "2", name = "Work", appPackageNames = emptyList())
        assertNotEquals(a, b)
    }

    @Test
    fun `FolderInfo inequality for different name`() {
        val a = FolderInfo(id = "1", name = "Work", appPackageNames = emptyList())
        val b = FolderInfo(id = "1", name = "Play", appPackageNames = emptyList())
        assertNotEquals(a, b)
    }

    @Test
    fun `FolderInfo inequality for different apps`() {
        val a = FolderInfo(id = "1", name = "Work", appPackageNames = listOf("com.a"))
        val b = FolderInfo(id = "1", name = "Work", appPackageNames = listOf("com.b"))
        assertNotEquals(a, b)
    }

    @Test
    fun `FolderInfo copy changes only specified fields`() {
        val original = FolderInfo(id = "1", name = "Work", appPackageNames = listOf("com.app"))
        val copied = original.copy(name = "Play")
        assertEquals("1", copied.id)
        assertEquals("Play", copied.name)
        assertEquals(listOf("com.app"), copied.appPackageNames)
    }

    @Test
    fun `FolderInfo hashCode consistent for equal instances`() {
        val a = FolderInfo(id = "1", name = "Work", appPackageNames = listOf("com.app"))
        val b = FolderInfo(id = "1", name = "Work", appPackageNames = listOf("com.app"))
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `FolderInfo with empty app list`() {
        val folder = FolderInfo(id = "1", name = "Empty", appPackageNames = emptyList())
        assertTrue(folder.appPackageNames.isEmpty())
    }
}
