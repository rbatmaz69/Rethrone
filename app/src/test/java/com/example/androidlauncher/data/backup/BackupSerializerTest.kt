package com.example.androidlauncher.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializerTest {

    private val allTypesSnapshot = BackupSnapshot(
        backupVersion = BackupSpec.CURRENT_VERSION,
        appVersionCode = 7L,
        exportedAtMs = 1_720_000_000_000L,
        stores = mapOf(
            BackupSpec.STORE_SETTINGS to listOf(
                BackupEntry("a_string", BackupValueType.STRING, "SOFT_SAND"),
                BackupEntry("a_boolean", BackupValueType.BOOLEAN, true),
                BackupEntry("an_int", BackupValueType.INT, 42),
                BackupEntry("a_long", BackupValueType.LONG, 9_876_543_210L),
                BackupEntry("a_float", BackupValueType.FLOAT, 0.75f),
                BackupEntry("a_double", BackupValueType.DOUBLE, 3.14159),
                BackupEntry("a_set", BackupValueType.STRING_SET, setOf("x", "y")),
            ),
            BackupSpec.STORE_FAVORITES to listOf(
                BackupEntry("favorites_list", BackupValueType.STRING, "com.a,com.b"),
            ),
        ),
        widgets = listOf(
            WidgetBackup("com.x/.ClockWidget", 180, 110, 12.5f, -4f),
        ),
    )

    @Test
    fun `round trip preserves every value type exactly`() {
        val parsed = BackupSerializer.parse(BackupSerializer.serialize(allTypesSnapshot))

        assertNotNull(parsed)
        assertEquals(allTypesSnapshot, parsed)
    }

    @Test
    fun `round trip preserves widget section without ids`() {
        val parsed = BackupSerializer.parse(BackupSerializer.serialize(allTypesSnapshot))!!

        assertEquals(allTypesSnapshot.widgets, parsed.widgets)
    }

    @Test
    fun `corrupt json returns null`() {
        assertNull(BackupSerializer.parse("not json at all"))
        assertNull(BackupSerializer.parse("{\"stores\": 12}"))
        assertNull(BackupSerializer.parse(""))
    }

    @Test
    fun `missing version returns null`() {
        assertNull(BackupSerializer.parse("{\"stores\":{}}"))
    }

    @Test
    fun `unknown type tags and malformed entries are skipped`() {
        val json = """
            {
              "backupVersion": 1,
              "stores": {
                "settings": [
                  {"k": "kept", "t": "string", "v": "ok"},
                  {"k": "future_type", "t": "byteArray", "v": "AAAA"},
                  {"k": "", "t": "string", "v": "empty key"},
                  {"k": "no_value", "t": "string"},
                  {"k": "wrong_value", "t": "int", "v": "keine Zahl"},
                  "kein Objekt"
                ],
                "unknown_future_store": [
                  {"k": "whatever", "t": "string", "v": "tolerated"}
                ]
              }
            }
        """.trimIndent()

        val parsed = BackupSerializer.parse(json)!!

        assertEquals(
            listOf(BackupEntry("kept", BackupValueType.STRING, "ok")),
            parsed.stores[BackupSpec.STORE_SETTINGS],
        )
        // Unbekannte Sektionen werden geparst, aber vom Restore ignoriert.
        assertTrue(parsed.stores.containsKey("unknown_future_store"))
    }

    @Test
    fun `newer version still parses so the manager can refuse it explicitly`() {
        val parsed = BackupSerializer.parse("""{"backupVersion": 99, "stores": {}}""")

        assertNotNull(parsed)
        assertEquals(99, parsed!!.backupVersion)
    }

    @Test
    fun `type tag derives from runtime class and rejects unsupported values`() {
        assertEquals(BackupValueType.INT, BackupValueType.fromValue(1))
        assertEquals(BackupValueType.LONG, BackupValueType.fromValue(1L))
        assertEquals(BackupValueType.FLOAT, BackupValueType.fromValue(1f))
        assertEquals(BackupValueType.DOUBLE, BackupValueType.fromValue(1.0))
        assertEquals(BackupValueType.STRING_SET, BackupValueType.fromValue(setOf("a")))
        assertNull(BackupValueType.fromValue(byteArrayOf(1)))
    }
}
