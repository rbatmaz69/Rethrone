package com.example.androidlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

class IconPackAppFilterParserTest {

    private fun parserFor(xml: String): XmlPullParser =
        XmlPullParserFactory.newInstance().newPullParser().apply {
            setInput(StringReader(xml))
        }

    // --- parse ---

    @Test
    fun `parses component to drawable entries`() {
        val xml = """
            <resources>
              <item component="ComponentInfo{com.android.chrome/com.google.android.apps.chrome.Main}" drawable="icon_chrome"/>
              <item component="ComponentInfo{com.foo/com.foo.Main}" drawable="icon_foo"/>
            </resources>
        """.trimIndent()

        val result = IconPackAppFilterParser.parse(parserFor(xml))

        assertEquals(
            mapOf(
                "com.android.chrome/com.google.android.apps.chrome.Main" to "icon_chrome",
                "com.foo/com.foo.Main" to "icon_foo",
            ),
            result
        )
    }

    @Test
    fun `first entry wins on duplicate components`() {
        val xml = """
            <resources>
              <item component="ComponentInfo{com.foo/com.foo.Main}" drawable="first"/>
              <item component="ComponentInfo{com.foo/com.foo.Main}" drawable="second"/>
            </resources>
        """.trimIndent()

        assertEquals(mapOf("com.foo/com.foo.Main" to "first"), IconPackAppFilterParser.parse(parserFor(xml)))
    }

    @Test
    fun `skips malformed items and foreign tags`() {
        val xml = """
            <resources>
              <calendar prefix="cal_"/>
              <scale factor="0.75"/>
              <item drawable="orphan_without_component"/>
              <item component="ComponentInfo{com.foo/com.foo.Main}"/>
              <item component="not a component" drawable="broken"/>
              <item component="ComponentInfo{com.ok/com.ok.Main}" drawable="icon_ok"/>
            </resources>
        """.trimIndent()

        assertEquals(mapOf("com.ok/com.ok.Main" to "icon_ok"), IconPackAppFilterParser.parse(parserFor(xml)))
    }

    @Test
    fun `keeps entries collected before a mid-document parse error`() {
        val xml = """
            <resources>
              <item component="ComponentInfo{com.ok/com.ok.Main}" drawable="icon_ok"/>
              <item component="ComponentInfo{com.broken/
        """.trimIndent()

        assertEquals(mapOf("com.ok/com.ok.Main" to "icon_ok"), IconPackAppFilterParser.parse(parserFor(xml)))
    }

    @Test
    fun `empty or itemless document yields empty map`() {
        assertTrue(IconPackAppFilterParser.parse(parserFor("<resources/>")).isEmpty())
        assertTrue(IconPackAppFilterParser.parse(parserFor("")).isEmpty())
    }

    // --- parseComponentInfo ---

    @Test
    fun `parseComponentInfo handles wrapper and relative class names`() {
        assertEquals(
            "com.foo/com.foo.Main",
            IconPackAppFilterParser.parseComponentInfo("ComponentInfo{com.foo/com.foo.Main}")
        )
        assertEquals(
            "com.foo/com.foo.Main",
            IconPackAppFilterParser.parseComponentInfo("ComponentInfo{com.foo/.Main}")
        )
        // Ohne Wrapper (manche Packs schreiben die Component roh).
        assertEquals(
            "com.foo/com.foo.Main",
            IconPackAppFilterParser.parseComponentInfo("com.foo/com.foo.Main")
        )
    }

    @Test
    fun `parseComponentInfo rejects garbage`() {
        assertNull(IconPackAppFilterParser.parseComponentInfo(""))
        assertNull(IconPackAppFilterParser.parseComponentInfo("no-slash-here"))
        assertNull(IconPackAppFilterParser.parseComponentInfo("ComponentInfo{com.foo/}"))
        assertNull(IconPackAppFilterParser.parseComponentInfo("ComponentInfo{/com.foo.Main}"))
    }

    // --- resolveDrawableName ---

    @Test
    fun `resolve prefers exact component match`() {
        val filter = mapOf(
            "com.foo/com.foo.Main" to "icon_main",
            "com.foo/com.foo.Other" to "icon_other",
        )

        assertEquals(
            "icon_other",
            IconPackAppFilterParser.resolveDrawableName(filter, "com.foo", "com.foo/com.foo.Other")
        )
    }

    @Test
    fun `resolve falls back to any entry of the same package`() {
        val filter = mapOf("com.foo/com.foo.Main" to "icon_main")

        // Launch-Activity weicht von der gemappten Activity ab → Package-Fallback.
        assertEquals(
            "icon_main",
            IconPackAppFilterParser.resolveDrawableName(filter, "com.foo", "com.foo/com.foo.Alias")
        )
        assertEquals(
            "icon_main",
            IconPackAppFilterParser.resolveDrawableName(filter, "com.foo", null)
        )
    }

    @Test
    fun `resolve returns null when the package is unmapped`() {
        val filter = mapOf("com.foo/com.foo.Main" to "icon_main")

        assertNull(IconPackAppFilterParser.resolveDrawableName(filter, "com.bar", "com.bar/com.bar.Main"))
        // Kein Substring-Match: "com.foo.bar" darf nicht auf "com.foo" matchen.
        assertNull(IconPackAppFilterParser.resolveDrawableName(filter, "com.fo", null))
    }
}
