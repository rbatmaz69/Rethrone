package com.example.androidlauncher.ui

import androidx.compose.ui.geometry.Offset
import com.example.androidlauncher.data.HomeLayout
import com.example.androidlauncher.data.HostedWidget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Absicherung des A6-Splits: der Holder seedet aus dem persistierten Layout und
 * liefert die Live-Offsets als speicherbares HomeLayout zurück (Roundtrip).
 * Seit B1 zusätzlich: gehostete System-Widgets als dynamische Drag-Targets.
 */
class HomeDragStateHolderTest {

    private fun widget(id: Int, x: Float = 0f, y: Float = 0f) = HostedWidget(
        appWidgetId = id,
        provider = "com.example/$id",
        widthDp = 200,
        heightDp = 100,
        offsetX = x,
        offsetY = y,
    )

    @Test
    fun `initial state mirrors the given layout and has no selection`() {
        val layout = HomeLayout(clock = Offset(10f, 20f), favorites = Offset(-4f, 8f))
        val holder = HomeDragStateHolder(layout)

        assertEquals(layout, holder.toHomeLayout())
        assertNull(holder.selectedEditTarget)
        assertFalse(holder.isEditTargetUserPinned)
        assertFalse(holder.collisionHapticWasTriggered)
    }

    @Test
    fun `live edits roundtrip into a persistable layout`() {
        val holder = HomeDragStateHolder(HomeLayout())

        holder.offsets[HomeEditTarget.Clock] = Offset(42f, -12f)
        holder.offsets[HomeEditTarget.Weather] = Offset(0f, 30f)

        assertEquals(
            HomeLayout(clock = Offset(42f, -12f), weather = Offset(0f, 30f)),
            holder.toHomeLayout()
        )
    }

    @Test
    fun `seedFrom reverts live offsets to the stored layout`() {
        val stored = HomeLayout(date = Offset(5f, 5f))
        val holder = HomeDragStateHolder(stored)

        holder.offsets[HomeEditTarget.Date] = Offset(99f, 99f)
        holder.seedFrom(stored)

        assertEquals(stored, holder.toHomeLayout())
    }

    // --- B1: gehostete Widgets ---

    @Test
    fun `widget offsets seed from persisted entries and roundtrip`() {
        val holder = HomeDragStateHolder(HomeLayout(), listOf(widget(7, x = 12f, y = -8f)))

        assertEquals(mapOf(7 to Offset(12f, -8f)), holder.toWidgetOffsets())

        holder.offsets[HomeEditTarget.Widget(7)] = Offset(30f, 40f)
        assertEquals(mapOf(7 to Offset(30f, 40f)), holder.toWidgetOffsets())

        // Abbrechen: seedFrom revertiert auch die Widget-Offsets.
        holder.seedFrom(HomeLayout(), listOf(widget(7, x = 12f, y = -8f)))
        assertEquals(mapOf(7 to Offset(12f, -8f)), holder.toWidgetOffsets())
    }

    @Test
    fun `removed widgets drop their map entries and selection`() {
        val holder = HomeDragStateHolder(HomeLayout(), listOf(widget(1), widget(2)))
        holder.selectedEditTarget = HomeEditTarget.Widget(2)
        holder.isEditTargetUserPinned = true
        holder.lastBlockedHapticMs[HomeEditTarget.Widget(2)] = 123L

        holder.seedFrom(HomeLayout(), listOf(widget(1)))

        assertEquals(mapOf(1 to Offset.Zero), holder.toWidgetOffsets())
        assertFalse(holder.offsets.containsKey(HomeEditTarget.Widget(2)))
        assertFalse(holder.lastBlockedHapticMs.containsKey(HomeEditTarget.Widget(2)))
        assertNull(holder.selectedEditTarget)
        assertFalse(holder.isEditTargetUserPinned)
    }

    @Test
    fun `new widgets join the drag state via seedFrom`() {
        val holder = HomeDragStateHolder(HomeLayout())

        holder.seedFrom(HomeLayout(), listOf(widget(5, x = 3f, y = 4f)))

        assertTrue(holder.offsets.containsKey(HomeEditTarget.Widget(5)))
        assertEquals(mapOf(5 to Offset(3f, 4f)), holder.toWidgetOffsets())
    }
}
