package com.example.androidlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetBindFlowTest {

    private val plain = PendingWidgetBind(appWidgetId = 5, provider = "a/b", needsConfigure = false)
    private val withConfigure = plain.copy(needsConfigure = true)

    // --- afterAllocate ---

    @Test
    fun `bind succeeded without configure commits directly`() {
        assertEquals(WidgetBindStep.Commit(plain), WidgetBindFlow.afterAllocate(plain, bindSucceeded = true))
    }

    @Test
    fun `bind succeeded with configure launches configure`() {
        assertEquals(
            WidgetBindStep.LaunchConfigure(withConfigure),
            WidgetBindFlow.afterAllocate(withConfigure, bindSucceeded = true)
        )
    }

    @Test
    fun `bind denied requests permission regardless of configure`() {
        assertEquals(
            WidgetBindStep.RequestBindPermission(plain),
            WidgetBindFlow.afterAllocate(plain, bindSucceeded = false)
        )
        assertEquals(
            WidgetBindStep.RequestBindPermission(withConfigure),
            WidgetBindFlow.afterAllocate(withConfigure, bindSucceeded = false)
        )
    }

    // --- afterBindPermission ---

    @Test
    fun `granted permission without configure commits`() {
        assertEquals(WidgetBindStep.Commit(plain), WidgetBindFlow.afterBindPermission(plain, granted = true))
    }

    @Test
    fun `granted permission with configure launches configure`() {
        assertEquals(
            WidgetBindStep.LaunchConfigure(withConfigure),
            WidgetBindFlow.afterBindPermission(withConfigure, granted = true)
        )
    }

    @Test
    fun `denied permission aborts with the allocated id`() {
        assertEquals(WidgetBindStep.Abort(5), WidgetBindFlow.afterBindPermission(plain, granted = false))
    }

    // --- afterConfigure ---

    @Test
    fun `configure ok commits, cancel aborts`() {
        assertEquals(
            WidgetBindStep.Commit(withConfigure),
            WidgetBindFlow.afterConfigure(withConfigure, resultOk = true)
        )
        assertEquals(
            WidgetBindStep.Abort(5),
            WidgetBindFlow.afterConfigure(withConfigure, resultOk = false)
        )
    }

    // --- resolveWidgetCleanup ---

    @Test
    fun `cleanup with consistent state is empty`() {
        val cleanup = resolveWidgetCleanup(
            persistedIds = listOf(1, 2),
            allocatedIds = listOf(1, 2),
            infoExists = { true },
        )
        assertEquals(emptyList<Int>(), cleanup.idsToDelete)
        assertEquals(emptyList<Int>(), cleanup.idsToRemoveFromPersistence)
    }

    @Test
    fun `allocated but unpersisted ids are deleted only`() {
        val cleanup = resolveWidgetCleanup(
            persistedIds = listOf(1),
            allocatedIds = listOf(1, 9),
            infoExists = { true },
        )
        assertEquals(listOf(9), cleanup.idsToDelete)
        assertEquals(emptyList<Int>(), cleanup.idsToRemoveFromPersistence)
    }

    @Test
    fun `persisted ids without provider info are deleted and unpersisted`() {
        val cleanup = resolveWidgetCleanup(
            persistedIds = listOf(1, 2),
            allocatedIds = listOf(1, 2),
            infoExists = { it == 1 },
        )
        assertEquals(listOf(2), cleanup.idsToDelete)
        assertEquals(listOf(2), cleanup.idsToRemoveFromPersistence)
    }

    @Test
    fun `leaked and stale ids combine`() {
        val cleanup = resolveWidgetCleanup(
            persistedIds = listOf(1, 2),
            allocatedIds = listOf(1, 2, 3),
            infoExists = { it == 1 },
        )
        assertEquals(listOf(3, 2), cleanup.idsToDelete)
        assertEquals(listOf(2), cleanup.idsToRemoveFromPersistence)
    }

    // --- defaultWidgetSizeDp ---

    @Test
    fun `target cells take precedence over min sizes`() {
        val size = defaultWidgetSizeDp(
            targetCellWidth = 4,
            targetCellHeight = 2,
            minWidthDp = 50,
            minHeightDp = 50,
            screenWidthDp = 400,
            screenHeightDp = 800,
        )
        assertEquals(WidgetSizeDp(280, 140), size)
    }

    @Test
    fun `without target cells min sizes are used and floored`() {
        val size = defaultWidgetSizeDp(
            targetCellWidth = 0,
            targetCellHeight = 0,
            minWidthDp = 60,
            minHeightDp = 20,
            screenWidthDp = 400,
            screenHeightDp = 800,
        )
        // Untergrenzen: 110dp Breite, 40dp Hoehe.
        assertEquals(WidgetSizeDp(110, 40), size)
    }

    @Test
    fun `width is capped by screen minus margin and height by screen fraction`() {
        val size = defaultWidgetSizeDp(
            targetCellWidth = 0,
            targetCellHeight = 0,
            minWidthDp = 999,
            minHeightDp = 999,
            screenWidthDp = 400,
            screenHeightDp = 800,
        )
        assertEquals(WidgetSizeDp(400 - 48, (800 * 0.6f).toInt()), size)
    }
}
