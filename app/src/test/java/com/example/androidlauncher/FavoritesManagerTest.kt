package com.example.androidlauncher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
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

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesManagerTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var testFile: File
    private lateinit var manager: FavoritesManager

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)

        every { context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE) } returns sharedPreferences

        testFile = File.createTempFile("favorites_test", ".preferences_pb")

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { testFile }
        )

        manager = FavoritesManager(testDataStore, context)
    }

    @After
    fun tearDown() {
        testFile.delete()
    }

    @Test
    fun `favorites flow emits empty list initially`() = testScope.runTest {
        val favorites = manager.favorites.first()
        assertEquals(emptyList<String>(), favorites)
    }

    @Test
    fun `saveFavorites updates datastore and flow emits new list`() = testScope.runTest {
        val testFavorites = listOf("com.example.app1", "com.example.app2")

        manager.saveFavorites(testFavorites)

        val newFavorites = manager.favorites.first()
        assertEquals(testFavorites, newFavorites)
    }

    @Test
    fun `migrateFromSharedPreferences copies existing data to datastore`() = testScope.runTest {
        val oldFavorites = "com.old.app1,com.old.app2"
        every { sharedPreferences.getString("favorites_list", null) } returns oldFavorites
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { sharedPreferences.edit() } returns editor
        every { editor.remove("favorites_list") } returns editor

        manager.migrateFromSharedPreferences(context)

        val newFavorites = manager.favorites.first()
        assertEquals(listOf("com.old.app1", "com.old.app2"), newFavorites)
        verify { editor.remove("favorites_list") }
    }
}
