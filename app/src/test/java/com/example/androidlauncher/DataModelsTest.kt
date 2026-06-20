package com.example.androidlauncher

import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.FolderInfo
import com.example.androidlauncher.data.FavoriteSpacing
import com.example.androidlauncher.data.FontSize
import com.example.androidlauncher.data.FontWeightLevel
import com.example.androidlauncher.data.IconSize
import org.junit.Assert.*
import org.junit.Test
import androidx.compose.ui.unit.dp

class DataModelsTest {

    @Test
    fun `FontSize presets expose expected scales`() {
        assertEquals(0.85f, FontSize.SMALL.scale, 0.0001f)
        assertEquals(1.0f, FontSize.STANDARD.scale, 0.0001f)
        assertEquals(1.2f, FontSize.LARGE.scale, 0.0001f)
    }

    @Test
    fun `FontSize of clamps to range`() {
        assertEquals(FontSize.MIN, FontSize.of(0.1f).scale, 0.0001f)
        assertEquals(FontSize.MAX, FontSize.of(5f).scale, 0.0001f)
        assertEquals(1.15f, FontSize.of(1.15f).scale, 0.0001f)
    }

    @Test
    fun `IconSize presets expose expected sizes`() {
        assertEquals(40.dp, IconSize.SMALL.size)
        assertEquals(48.dp, IconSize.STANDARD.size)
        assertEquals(56.dp, IconSize.LARGE.size)
    }

    @Test
    fun `IconSize scale is derived relative to 48dp`() {
        assertEquals(1.0f, IconSize.STANDARD.scale, 0.0001f)
        assertEquals(0.5f, IconSize(24.dp).scale, 0.0001f)
    }

    @Test
    fun `IconSize of clamps to range`() {
        assertEquals(IconSize.MIN, IconSize.of(1.dp).size)
        assertEquals(IconSize.MAX, IconSize.of(200.dp).size)
        assertEquals(50.dp, IconSize.of(50.dp).size)
    }

    @Test
    fun `FavoriteSpacing of clamps to range`() {
        assertEquals(FavoriteSpacing.MIN, FavoriteSpacing.of((-5).dp).spacing)
        assertEquals(FavoriteSpacing.MAX, FavoriteSpacing.of(100.dp).spacing)
        assertEquals(16.dp, FavoriteSpacing.of(16.dp).spacing)
    }

    @Test
    fun `FontWeightLevel of clamps and labels by range`() {
        assertEquals(FontWeightLevel.MIN, FontWeightLevel.of(0).weightValue)
        assertEquals(FontWeightLevel.MAX, FontWeightLevel.of(2000).weightValue)
        assertEquals(R.string.font_weight_normal, FontWeightLevel.of(400).labelRes)
        assertEquals(R.string.font_weight_bold, FontWeightLevel.of(700).labelRes)
        assertEquals(R.string.font_weight_black, FontWeightLevel.of(900).labelRes)
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
