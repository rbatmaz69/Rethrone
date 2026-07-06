package com.example.androidlauncher.data

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.androidlauncher.data.settings.HomeLayoutSettings
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetHostManagerTest {

    private lateinit var testFile: File
    private lateinit var settings: HomeLayoutSettings
    private lateinit var host: AppWidgetHost
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var manager: WidgetHostManager

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        testFile = File.createTempFile("widget_host_manager_test", ".preferences_pb")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { testFile }
        )
        settings = HomeLayoutSettings(testDataStore)
        host = mockk(relaxed = true)
        appWidgetManager = mockk(relaxed = true)
        manager = WidgetHostManager(mockk<Context>(relaxed = true), host, appWidgetManager, settings)
    }

    @After
    fun tearDown() {
        testFile.delete()
    }

    private fun widget(id: Int) = HostedWidget(
        appWidgetId = id,
        provider = "com.example/$id",
        widthDp = 200,
        heightDp = 100,
    )

    @Test
    fun `allocateWidgetId delegates to host`() {
        every { host.allocateAppWidgetId() } returns 42

        assertEquals(42, manager.allocateWidgetId())
    }

    @Test
    fun `bindIfAllowed returns manager result and treats exceptions as not bound`() {
        val provider = mockk<ComponentName>()
        every { appWidgetManager.bindAppWidgetIdIfAllowed(1, provider) } returns true
        every { appWidgetManager.bindAppWidgetIdIfAllowed(2, provider) } throws SecurityException("OEM")

        assertTrue(manager.bindIfAllowed(1, provider))
        assertFalse(manager.bindIfAllowed(2, provider))
    }

    @Test
    fun `addWidget appends to persisted list`() = testScope.runTest {
        manager.addWidget(widget(1))
        manager.addWidget(widget(2))

        assertEquals(listOf(widget(1), widget(2)), manager.widgets.first())
    }

    @Test
    fun `removeWidget deletes host id and removes from persistence`() = testScope.runTest {
        every { host.deleteAppWidgetId(any()) } just Runs
        manager.addWidget(widget(1))
        manager.addWidget(widget(2))

        manager.removeWidget(1)

        assertEquals(listOf(widget(2)), manager.widgets.first())
        verify { host.deleteAppWidgetId(1) }
    }

    @Test
    fun `updateOffsets rewrites only matching entries`() = testScope.runTest {
        manager.addWidget(widget(1))
        manager.addWidget(widget(2))

        manager.updateOffsets(mapOf(1 to Offset(15f, -25f)))

        val result = manager.widgets.first()
        assertEquals(widget(1).copy(offsetX = 15f, offsetY = -25f), result.first { it.appWidgetId == 1 })
        assertEquals(widget(2), result.first { it.appWidgetId == 2 })
    }

    @Test
    fun `cleanupOrphans deletes leaked ids and prunes uninstalled providers`() = testScope.runTest {
        every { host.deleteAppWidgetId(any()) } just Runs
        // Persistiert: 1 (Provider vorhanden) und 2 (Provider deinstalliert); im Host geleakt: 3.
        manager.addWidget(widget(1))
        manager.addWidget(widget(2))
        every { host.appWidgetIds } returns intArrayOf(1, 2, 3)
        every { appWidgetManager.getAppWidgetInfo(1) } returns mockk()
        every { appWidgetManager.getAppWidgetInfo(2) } returns null

        manager.cleanupOrphans()

        assertEquals(listOf(widget(1)), manager.widgets.first())
        verify { host.deleteAppWidgetId(3) }
        verify { host.deleteAppWidgetId(2) }
        verify(exactly = 0) { host.deleteAppWidgetId(1) }
    }

    @Test
    fun `createView returns null when provider info is missing`() {
        every { appWidgetManager.getAppWidgetInfo(9) } returns null

        assertNull(manager.createView(9))
    }
}
