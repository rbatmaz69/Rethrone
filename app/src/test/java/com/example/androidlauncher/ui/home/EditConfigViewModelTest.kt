package com.example.androidlauncher.ui.home

import androidx.compose.ui.geometry.Offset
import com.example.androidlauncher.data.HomeLayout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditConfigViewModelTest {

    @Test
    fun `default layout has no custom offsets`() {
        assertFalse(EditConfigViewModel.hasCustomOffsets(HomeLayout()))
    }

    @Test
    fun `sub-pixel noise below the epsilon does not count as custom`() {
        val layout = HomeLayout(clock = Offset(0.3f, -0.4f), weather = Offset(0.5f, 0f))
        assertFalse(EditConfigViewModel.hasCustomOffsets(layout))
    }

    @Test
    fun `any single moved element counts as custom`() {
        assertTrue(EditConfigViewModel.hasCustomOffsets(HomeLayout(date = Offset(0f, 12f))))
        assertTrue(EditConfigViewModel.hasCustomOffsets(HomeLayout(favorites = Offset(-8f, 0f))))
    }
}
