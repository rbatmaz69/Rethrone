package com.example.androidlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests für die Punch-Hole-Heuristik [estimateCameraBounds]: schätzt die echte Kamera-Mitte
 * aus dem gemeldeten Cutout-Bounding-Rect (Fallback, wenn kein exakter Cutout-Pfad verfügbar ist).
 */
class CameraBoundsTest {

    @Test
    fun `rect vom bildschirmrand bis unter die kamera - mitte nahe der unterkante angenommen`() {
        // Typische Punch-Hole-Meldung: top = 0, Rect deutlich höher als breit.
        val bounds = estimateCameraBounds(left = 490, top = 0, right = 590, bottom = 132)

        assertEquals(540f, bounds.centerX, 0f)
        // Kamera-Mitte 0.43 × Breite über der Unterkante: 132 - 43 = 89, NICHT die Rect-Mitte 66.
        assertEquals(89f, bounds.centerY, 0.01f)
        assertEquals(100, bounds.widthPx)
        assertEquals(100, bounds.heightPx)
    }

    @Test
    fun `motorola edge 70 - rechteckiger cutout-pfad wird korrigiert`() {
        // Reale Werte des Geräts: Pfad/Rect = (554, 0, 666, 125), 112 breit, 125 hoch.
        // Am Gerät kalibriert: echte Kamera-Mitte liegt bei ~77px.
        val bounds = estimateCameraBounds(left = 554, top = 0, right = 666, bottom = 125)

        assertEquals(610f, bounds.centerX, 0f)
        assertEquals(76.84f, bounds.centerY, 0.01f)
        assertEquals(112, bounds.widthPx)
        assertEquals(112, bounds.heightPx)
    }

    @Test
    fun `enges rect mit abstand zum rand - rect-mitte wird uebernommen`() {
        val bounds = estimateCameraBounds(left = 490, top = 20, right = 590, bottom = 120)

        assertEquals(540f, bounds.centerX, 0f)
        assertEquals(70f, bounds.centerY, 0f)
        assertEquals(100, bounds.widthPx)
        assertEquals(100, bounds.heightPx)
    }

    @Test
    fun `breite notch - rect-mitte wird uebernommen`() {
        // Notch: breiter als hoch, beruehrt den Rand → keine Kreis-Annahme.
        val bounds = estimateCameraBounds(left = 300, top = 0, right = 780, bottom = 90)

        assertEquals(540f, bounds.centerX, 0f)
        assertEquals(45f, bounds.centerY, 0f)
        assertEquals(480, bounds.widthPx)
        assertEquals(90, bounds.heightPx)
    }

    @Test
    fun `quadratisches rect am rand - rect-mitte wird uebernommen`() {
        // Hoehe == Breite: keine Luft ueber der Kamera im Rect, Mitte stimmt bereits.
        val bounds = estimateCameraBounds(left = 490, top = 0, right = 590, bottom = 100)

        assertEquals(50f, bounds.centerY, 0f)
        assertEquals(100, bounds.heightPx)
    }
}
