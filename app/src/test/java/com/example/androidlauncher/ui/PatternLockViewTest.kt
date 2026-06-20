package com.example.androidlauncher.ui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests der Trefferlogik [hitNode] des 3×3-Muster-Gitters: Berührungen nahe einem Knoten
 * werden korrekt zugeordnet, Berührungen dazwischen ergeben keinen Treffer.
 */
class PatternLockViewTest {

    private val size = 300f // 3×3 → Schrittweite 100, Zentren bei 50/150/250

    @Test
    fun cellCenters_mapToCorrectIndex() {
        // Zentrum jeder Zelle (row*3 + col)
        assertEquals(0, hitNode(Offset(50f, 50f), size))    // oben links
        assertEquals(2, hitNode(Offset(250f, 50f), size))   // oben rechts
        assertEquals(4, hitNode(Offset(150f, 150f), size))  // Mitte
        assertEquals(6, hitNode(Offset(50f, 250f), size))   // unten links
        assertEquals(8, hitNode(Offset(250f, 250f), size))  // unten rechts
    }

    @Test
    fun betweenNodes_returnsNull() {
        // Punkt mittig zwischen Knoten 0 und 1 – außerhalb des Trefferradius.
        assertNull(hitNode(Offset(100f, 50f), size))
    }

    @Test
    fun nonPositiveSize_returnsNull() {
        assertNull(hitNode(Offset(50f, 50f), 0f))
        assertNull(hitNode(Offset(50f, 50f), -10f))
    }
}
