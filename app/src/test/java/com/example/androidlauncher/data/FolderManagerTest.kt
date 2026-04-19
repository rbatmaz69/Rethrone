package com.example.androidlauncher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

@OptIn(ExperimentalCoroutinesApi::class)
class FolderManagerTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var testFile: File
    private lateinit var manager: FolderManager

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)

        every { context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE) } returns sharedPreferences

        testFile = File.createTempFile("folders_test", ".preferences_pb")

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { testFile }
        )

        manager = FolderManager(testDataStore, context)
    }

    @After
    fun tearDown() {
        testFile.delete()
    }

    @Test
    fun `folders flow emits empty list initially`() = testScope.runTest {
        val folders = manager.folders.first()
        assertEquals(emptyList<FolderInfo>(), folders)
    }

    @Test
    fun `saveFolders updates datastore and flow emits new list`() = testScope.runTest {
        val folderInfo = FolderInfo(id = "folder1", name = "My Folder", appPackageNames = listOf("app1", "app2"))
        val testFolders = listOf(folderInfo)

        manager.saveFolders(testFolders)

        val newFolders = manager.folders.first()
        assertEquals(1, newFolders.size)
        assertEquals("folder1", newFolders[0].id)
        assertEquals("My Folder", newFolders[0].name)
    }

    @Test
    fun `createOrUpdateFolder adds new folder if not exists`() = testScope.runTest {
        val folderInfo = FolderInfo(id = "folder1", name = "My Folder", appPackageNames = listOf("app1"))
        manager.createOrUpdateFolder(folderInfo)

        val newFolders = manager.folders.first()
        assertEquals(1, newFolders.size)
        assertEquals("folder1", newFolders[0].id)
    }

    @Test
    fun `createOrUpdateFolder updates existing folder`() = testScope.runTest {
        val folderInfo = FolderInfo(id = "folder1", name = "My Folder", appPackageNames = listOf("app1"))
        manager.saveFolders(listOf(folderInfo))

        val updatedFolder = folderInfo.copy(name = "New Name")
        manager.createOrUpdateFolder(updatedFolder)

        val newFolders = manager.folders.first()
        assertEquals(1, newFolders.size)
        assertEquals("New Name", newFolders[0].name)
    }

    @Test
    fun `deleteFolder removes folder with given id`() = testScope.runTest {
        val folderInfo1 = FolderInfo(id = "folder1", name = "My Folder", appPackageNames = listOf("app1"))
        val folderInfo2 = FolderInfo(id = "folder2", name = "Folder 2", appPackageNames = listOf("app2"))
        manager.saveFolders(listOf(folderInfo1, folderInfo2))

        manager.deleteFolder("folder1")

        val newFolders = manager.folders.first()
        assertEquals(1, newFolders.size)
        assertEquals("folder2", newFolders[0].id)
    }
}
