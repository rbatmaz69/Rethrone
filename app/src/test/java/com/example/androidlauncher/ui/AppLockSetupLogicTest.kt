package com.example.androidlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests der Einrichtungs-Logik [handleEntry] (Erst-/Bestätigungseingabe von PIN und Muster):
 * Mindestlänge, Speichern der ersten Eingabe, Übereinstimmung bzw. Abweichung.
 */
class AppLockSetupLogicTest {

    private class Result {
        var tooShort: String? = null
        var first: String? = null
        var mismatch = false
        var match: String? = null
    }

    private fun run(entered: String, firstEntry: String?, minLength: Int = 4): Result {
        val r = Result()
        handleEntry(
            entered = entered,
            firstEntry = firstEntry,
            minLength = minLength,
            tooShortMessage = "zu kurz",
            onTooShort = { r.tooShort = it },
            onFirst = { r.first = it },
            onMismatch = { r.mismatch = true },
            onMatch = { r.match = it }
        )
        return r
    }

    @Test
    fun pinTooShort_triggersOnTooShort() {
        val r = run(entered = "123", firstEntry = null)
        assertEquals("zu kurz", r.tooShort)
        assertNull(r.first)
    }

    @Test
    fun firstValidEntry_storedViaOnFirst() {
        val r = run(entered = "1234", firstEntry = null)
        assertEquals("1234", r.first)
        assertNull(r.match)
    }

    @Test
    fun matchingConfirmation_triggersOnMatch() {
        val r = run(entered = "1234", firstEntry = "1234")
        assertEquals("1234", r.match)
    }

    @Test
    fun mismatchingConfirmation_triggersOnMismatch() {
        val r = run(entered = "1234", firstEntry = "0000")
        assertTrue(r.mismatch)
        assertNull(r.match)
    }

    @Test
    fun patternEntry_countsNodesAsLength() {
        // "0,4,8,5" sind 4 Knoten -> erfüllt Mindestlänge 4, wird als erste Eingabe akzeptiert.
        val r = run(entered = "0,4,8,5", firstEntry = null)
        assertEquals("0,4,8,5", r.first)
        assertNull(r.tooShort)
    }
}
