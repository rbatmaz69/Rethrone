package com.example.androidlauncher.ui

import androidx.compose.ui.geometry.Offset
import com.example.androidlauncher.data.HomeLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Absicherung des A6-Splits: der Holder seedet aus dem persistierten Layout und
 * liefert die Live-Offsets als speicherbares HomeLayout zurück (Roundtrip).
 */
class HomeDragStateHolderTest {

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

        holder.offsets[HomeEditTarget.CLOCK] = Offset(42f, -12f)
        holder.offsets[HomeEditTarget.WEATHER] = Offset(0f, 30f)

        assertEquals(
            HomeLayout(clock = Offset(42f, -12f), weather = Offset(0f, 30f)),
            holder.toHomeLayout()
        )
    }

    @Test
    fun `seedFrom reverts live offsets to the stored layout`() {
        val stored = HomeLayout(date = Offset(5f, 5f))
        val holder = HomeDragStateHolder(stored)

        holder.offsets[HomeEditTarget.DATE] = Offset(99f, 99f)
        holder.seedFrom(stored)

        assertEquals(stored, holder.toHomeLayout())
    }
}
