package com.example.androidlauncher

import com.example.androidlauncher.data.AutoIconFallback
import com.example.androidlauncher.data.AutoIconFallbackType
import com.example.androidlauncher.data.AutoIconRule
import com.example.androidlauncher.data.AutoIconRuleMode
import com.example.androidlauncher.data.IconAlphaMask
import com.example.androidlauncher.data.IconQualityEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.pow

class IconQualityEvaluatorTest {

    @Test
    fun `serialized fallback roundtrips with current version`() {
        val fallback = AutoIconFallback(
            type = AutoIconFallbackType.LUCIDE,
            lucideIconName = "Chrome",
            reason = "full_bleed_square"
        )

        val restored = AutoIconFallback.deserialize(fallback.serialize())

        assertEquals(fallback, restored)
    }

    @Test
    fun `serialized auto icon rule roundtrips`() {
        val rule = AutoIconRule(mode = AutoIconRuleMode.FORCE_FALLBACK, reason = "user_config")

        val restored = AutoIconRule.deserialize(rule.serialize())

        assertEquals(rule, restored)
    }

    @Test
    fun `invalid serialized version is ignored`() {
        val restored = AutoIconFallback.deserialize("LUCIDE|999|Chrome|full_bleed_square")
        assertNull(restored)
    }

    @Test
    fun `full bleed square chrome icon gets lucide fallback`() {
        val result = IconQualityEvaluator.evaluate(
            mask = filledMask(),
            packageName = "com.android.chrome",
            label = "Chrome"
        )

        assertEquals(AutoIconFallbackType.LUCIDE, result.type)
        assertEquals("Chrome", result.lucideIconName)
    }

    @Test
    fun `tiny unreadable icon gets neutral fallback when no mapping exists`() {
        val result = IconQualityEvaluator.evaluate(
            mask = singleDotMask(),
            packageName = "com.example.unknown",
            label = "Unknown Tool"
        )

        assertEquals(AutoIconFallbackType.NEUTRAL, result.type)
        assertNull(result.lucideIconName)
    }

    @Test
    fun `circular readable icon remains original`() {
        val result = IconQualityEvaluator.evaluate(
            mask = circularMask(),
            packageName = "com.example.reader",
            label = "Reader"
        )

        assertEquals(AutoIconFallbackType.ORIGINAL, result.type)
    }

    @Test
    fun `default firefox rule keeps original without hardcoded branch in caller`() {
        val result = IconQualityEvaluator.evaluate(
            mask = filledMask(),
            packageName = "org.mozilla.firefox",
            label = "Firefox"
        )

        assertEquals(AutoIconFallbackType.ORIGINAL, result.type)
        assertEquals("configured_keep_original", result.reason)
    }

    @Test
    fun `follow heuristic rule can override default firefox keep original`() {
        val result = IconQualityEvaluator.evaluate(
            mask = filledMask(),
            packageName = "org.mozilla.firefox",
            label = "Firefox",
            explicitRule = AutoIconRule(mode = AutoIconRuleMode.FOLLOW_HEURISTIC, reason = "user_config")
        )

        assertEquals(AutoIconFallbackType.NEUTRAL, result.type)
    }

    @Test
    fun `force fallback rule overrides readable icon`() {
        val result = IconQualityEvaluator.evaluate(
            mask = circularMask(),
            packageName = "com.example.notes",
            label = "Notes",
            explicitRule = AutoIconRule(mode = AutoIconRuleMode.FORCE_FALLBACK, reason = "user_config")
        )

        assertEquals(AutoIconFallbackType.LUCIDE, result.type)
        assertEquals("NotebookPen", result.lucideIconName)
    }

    @Test
    fun `keyword mapping can suggest lucide fallback for notes app`() {
        val iconName = IconQualityEvaluator.resolveLucideFallbackName(
            packageName = "com.example.notes",
            label = "My Notes"
        )

        assertNotNull(iconName)
        assertEquals("NotebookPen", iconName)
    }

    private fun filledMask(): IconAlphaMask {
        val width = 40
        val height = 40
        return IconAlphaMask(width = width, height = height, alpha = IntArray(width * height) { 255 })
    }

    private fun singleDotMask(): IconAlphaMask {
        val width = 40
        val height = 40
        val alpha = IntArray(width * height)
        alpha[(height / 2) * width + (width / 2)] = 255
        return IconAlphaMask(width = width, height = height, alpha = alpha)
    }

    private fun circularMask(): IconAlphaMask {
        val width = 48
        val height = 48
        val radius = 12
        val centerX = width / 2.0
        val centerY = height / 2.0
        val alpha = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val distanceSquared = (x - centerX).pow(2) + (y - centerY).pow(2)
                if (distanceSquared <= radius.toDouble().pow(2)) {
                    alpha[y * width + x] = 255
                }
            }
        }
        return IconAlphaMask(width = width, height = height, alpha = alpha)
    }
}
