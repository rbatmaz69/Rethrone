package com.example.androidlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSerializerTest {

    private val widgets = listOf(
        HostedWidget(
            appWidgetId = 42,
            provider = "com.example.app/com.example.app.ClockWidgetProvider",
            widthDp = 280,
            heightDp = 140,
            offsetX = 12.5f,
            offsetY = -30f,
        ),
        HostedWidget(
            appWidgetId = 7,
            provider = "com.other.app/com.other.app.Widget",
            widthDp = 110,
            heightDp = 40,
        ),
    )

    @Test
    fun `roundtrip preserves all fields`() {
        val json = WidgetSerializer.serializeWidgets(widgets)
        val parsed = WidgetSerializer.parseWidgets(json)

        assertEquals(widgets, parsed)
    }

    @Test
    fun `empty list roundtrips to empty list`() {
        val json = WidgetSerializer.serializeWidgets(emptyList())
        assertTrue(WidgetSerializer.parseWidgets(json).isEmpty())
    }

    @Test
    fun `corrupt json returns empty list`() {
        assertTrue(WidgetSerializer.parseWidgets("not json at all").isEmpty())
        assertTrue(WidgetSerializer.parseWidgets("{\"kein\":\"array\"}").isEmpty())
        assertTrue(WidgetSerializer.parseWidgets("").isEmpty())
    }

    @Test
    fun `entry with missing required field returns empty list`() {
        // widthDp fehlt – ein halb geparster Bestand waere inkonsistent, also alles verwerfen.
        val json = """[{"appWidgetId":1,"provider":"a/b","heightDp":100}]"""
        assertTrue(WidgetSerializer.parseWidgets(json).isEmpty())
    }

    @Test
    fun `missing offsets default to zero`() {
        val json = """[{"appWidgetId":1,"provider":"a/b","widthDp":100,"heightDp":50}]"""
        val parsed = WidgetSerializer.parseWidgets(json)

        assertEquals(1, parsed.size)
        assertEquals(0f, parsed.first().offsetX, 0f)
        assertEquals(0f, parsed.first().offsetY, 0f)
    }
}
