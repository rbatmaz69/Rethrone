package com.example.androidlauncher.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SearchSuggestionsManagerTest {

    private lateinit var testFile: File
    private lateinit var manager: SearchSuggestionsManager

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        testFile = File.createTempFile("search_suggestions_test", ".preferences_pb")

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { testFile }
        )

        manager = SearchSuggestionsManager(testDataStore)
    }

    @After
    fun tearDown() {
        testFile.delete()
    }

    @Test
    fun `webHistory empty initially`() = testScope.runTest {
        val history = manager.webHistory.first()
        assertTrue(history.isEmpty())
    }

    @Test
    fun `recordWebSearch saves entry`() = testScope.runTest {
        manager.recordWebSearch("hello", 1000L)
        val history = manager.webHistory.first()
        assertEquals(1, history.size)
        assertEquals("hello", history[0].query)
        assertEquals(1, history[0].usageCount)
        assertEquals(1000L, history[0].lastSearchedAt)
    }

    @Test
    fun `recordWebSearch increments usage count and updates timestamp`() = testScope.runTest {
        manager.recordWebSearch("hello", 1000L)
        manager.recordWebSearch("Hello", 2000L) // Case insensitive update

        val history = manager.webHistory.first()
        assertEquals(1, history.size)
        assertEquals("Hello", history[0].query) // uses latest query casing
        assertEquals(2, history[0].usageCount)
        assertEquals(2000L, history[0].lastSearchedAt)
    }

    @Test
    fun `recordWebSearch ignores empty queries`() = testScope.runTest {
        manager.recordWebSearch("   ", 1000L)
        val history = manager.webHistory.first()
        assertTrue(history.isEmpty())
    }

    @Test
    fun `removeWebSearch removes correct entry`() = testScope.runTest {
        manager.recordWebSearch("hello", 1000L)
        manager.recordWebSearch("world", 2000L)

        manager.removeWebSearch("HeLlo")

        val history = manager.webHistory.first()
        assertEquals(1, history.size)
        assertEquals("world", history[0].query)
    }

    @Test
    fun `clearWebHistory removes all entries`() = testScope.runTest {
        manager.recordWebSearch("hello", 1000L)
        manager.recordWebSearch("world", 2000L)

        manager.clearWebHistory()

        val history = manager.webHistory.first()
        assertTrue(history.isEmpty())
    }

    @Test
    fun `recordAppLaunch saves entry`() = testScope.runTest {
        manager.recordAppLaunch("com.example.app", 1000L)
        val stats = manager.appUsageStats.first()
        assertEquals(1, stats.size)

        val entry = stats["com.example.app"]!!
        assertEquals("com.example.app", entry.packageName)
        assertEquals(1, entry.launchCount)
        assertEquals(1000L, entry.lastLaunchedAt)
    }

    @Test
    fun `recordAppLaunch increments usage count`() = testScope.runTest {
        manager.recordAppLaunch("com.example.app", 1000L)
        manager.recordAppLaunch("com.example.app", 2000L)

        val stats = manager.appUsageStats.first()
        assertEquals(1, stats.size)

        val entry = stats["com.example.app"]!!
        assertEquals(2, entry.launchCount)
        assertEquals(2000L, entry.lastLaunchedAt)
    }

    @Test
    fun `recordAppLaunch ignores blank package name`() = testScope.runTest {
        manager.recordAppLaunch("   ", 1000L)
        val stats = manager.appUsageStats.first()
        assertTrue(stats.isEmpty())
    }
}
