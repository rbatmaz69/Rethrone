package com.example.androidlauncher

import com.example.androidlauncher.data.FolderInfo
import com.example.androidlauncher.data.FolderSerializer
import org.junit.Assert.*
import org.junit.Test

class FolderSerializerTest {

    @Test
    fun `serialize and parse roundtrip preserves data`() {
        val folders = listOf(
            FolderInfo(id = "f1", name = "Work", appPackageNames = listOf("com.app1", "com.app2")),
            FolderInfo(id = "f2", name = "Social", appPackageNames = listOf("com.app3"))
        )
        val json = FolderSerializer.serializeFolders(folders)
        val parsed = FolderSerializer.parseFolders(json)
        assertEquals(folders, parsed)
    }

    @Test
    fun `parse empty json array returns empty list`() {
        val result = FolderSerializer.parseFolders("[]")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse invalid json returns empty list`() {
        val result = FolderSerializer.parseFolders("not valid json")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse empty string returns empty list`() {
        val result = FolderSerializer.parseFolders("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `serialize empty list returns empty json array`() {
        val json = FolderSerializer.serializeFolders(emptyList())
        assertEquals("[]", json)
    }

    @Test
    fun `serialize folder with empty app list`() {
        val folders = listOf(FolderInfo(id = "f1", name = "Empty", appPackageNames = emptyList()))
        val json = FolderSerializer.serializeFolders(folders)
        val parsed = FolderSerializer.parseFolders(json)
        assertEquals(1, parsed.size)
        assertEquals("Empty", parsed[0].name)
        assertTrue(parsed[0].appPackageNames.isEmpty())
    }

    @Test
    fun `serialize preserves folder order`() {
        val folders = listOf(
            FolderInfo(id = "z", name = "Zebra", appPackageNames = emptyList()),
            FolderInfo(id = "a", name = "Alpha", appPackageNames = emptyList()),
            FolderInfo(id = "m", name = "Middle", appPackageNames = emptyList())
        )
        val json = FolderSerializer.serializeFolders(folders)
        val parsed = FolderSerializer.parseFolders(json)
        assertEquals("Zebra", parsed[0].name)
        assertEquals("Alpha", parsed[1].name)
        assertEquals("Middle", parsed[2].name)
    }

    @Test
    fun `serialize preserves app order within folder`() {
        val folders = listOf(
            FolderInfo(id = "f1", name = "Test", appPackageNames = listOf("c", "a", "b"))
        )
        val json = FolderSerializer.serializeFolders(folders)
        val parsed = FolderSerializer.parseFolders(json)
        assertEquals(listOf("c", "a", "b"), parsed[0].appPackageNames)
    }

    @Test
    fun `parse json with missing apps field returns empty list`() {
        val json = """[{"id":"f1","name":"Test"}]"""
        val result = FolderSerializer.parseFolders(json)
        // Should fail gracefully since "apps" key is missing
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse json with extra fields ignores them`() {
        val json = """[{"id":"f1","name":"Test","apps":["com.app"],"extra":"value"}]"""
        val result = FolderSerializer.parseFolders(json)
        assertEquals(1, result.size)
        assertEquals("f1", result[0].id)
        assertEquals("Test", result[0].name)
        assertEquals(listOf("com.app"), result[0].appPackageNames)
    }

    @Test
    fun `serialize and parse with special characters in name`() {
        val folders = listOf(
            FolderInfo(id = "f1", name = "Meine Ördner & <Stuff>", appPackageNames = listOf("com.app"))
        )
        val json = FolderSerializer.serializeFolders(folders)
        val parsed = FolderSerializer.parseFolders(json)
        assertEquals("Meine Ördner & <Stuff>", parsed[0].name)
    }

    @Test
    fun `serialize and parse with unicode emoji in name`() {
        val folders = listOf(
            FolderInfo(id = "f1", name = "📱 Apps", appPackageNames = emptyList())
        )
        val json = FolderSerializer.serializeFolders(folders)
        val parsed = FolderSerializer.parseFolders(json)
        assertEquals("📱 Apps", parsed[0].name)
    }

    @Test
    fun `serialize multiple folders and parse returns correct count`() {
        val folders = (1..10).map {
            FolderInfo(id = "f$it", name = "Folder$it", appPackageNames = listOf("com.app$it"))
        }
        val json = FolderSerializer.serializeFolders(folders)
        val parsed = FolderSerializer.parseFolders(json)
        assertEquals(10, parsed.size)
    }
}

