package com.example.androidlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeWidgetMetricsTest {

    @Test
    fun `clock scales with screen width and stays within bounds`() {
        // Typisches Phone (411dp): proportionaler Wert, keine Klemmung.
        assertEquals(411 * 0.165f, HomeWidgetMetrics.clockBaseSp(411), 0.001f)
        // Sehr schmal bzw. sehr breit: an die Grenzen geklemmt.
        assertEquals(58f, HomeWidgetMetrics.clockBaseSp(300), 0.001f)
        assertEquals(84f, HomeWidgetMetrics.clockBaseSp(900), 0.001f)
    }

    @Test
    fun `date scales with screen width and stays within bounds`() {
        assertEquals(411 * 0.048f, HomeWidgetMetrics.dateBaseSp(411), 0.001f)
        assertEquals(16f, HomeWidgetMetrics.dateBaseSp(300), 0.001f)
        assertEquals(22f, HomeWidgetMetrics.dateBaseSp(900), 0.001f)
    }

    @Test
    fun `top anchor scales with screen height and stays within bounds`() {
        assertEquals(891 * 0.035f, HomeWidgetMetrics.topAnchorDp(891), 0.001f)
        assertEquals(24f, HomeWidgetMetrics.topAnchorDp(600), 0.001f)
        assertEquals(44f, HomeWidgetMetrics.topAnchorDp(1400), 0.001f)
    }
}
