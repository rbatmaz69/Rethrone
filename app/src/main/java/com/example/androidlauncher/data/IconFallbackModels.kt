package com.example.androidlauncher.data

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.max

private const val AUTO_ICON_SERIALIZATION_DELIMITER = "|"

private val DEFAULT_AUTO_ICON_RULES: Map<String, AutoIconRule> = mapOf(
    "org.mozilla.firefox" to AutoIconRule(AutoIconRuleMode.KEEP_ORIGINAL, reason = "default_firefox_brand"),
    "org.mozilla.fenix" to AutoIconRule(AutoIconRuleMode.KEEP_ORIGINAL, reason = "default_firefox_brand"),
    "org.mozilla.focus" to AutoIconRule(AutoIconRuleMode.KEEP_ORIGINAL, reason = "default_firefox_brand"),
    "org.mozilla.klar" to AutoIconRule(AutoIconRuleMode.KEEP_ORIGINAL, reason = "default_firefox_brand")
)

private val FALLBACK_LUCIDE_PACKAGE_MAP: Map<String, String> = mapOf(
    "com.android.chrome" to "Chrome",
    "com.android.vending" to "Play",
    "com.google.android.youtube" to "Youtube",
    "com.google.android.apps.youtube.music" to "Music",
    "com.google.android.calendar" to "Calendar",
    "com.android.calendar" to "Calendar",
    "com.android.camera" to "Camera",
    "com.google.android.GoogleCamera" to "Camera",
    "com.google.android.calculator" to "Calculator",
    "com.google.android.googlequicksearchbox" to "Mic",
    "com.google.android.apps.nbu.files" to "FolderOpen",
    "com.example.androidlauncher" to "Crown"
)

private val FALLBACK_LUCIDE_KEYWORDS: List<Pair<Regex, String>> = listOf(
    Regex("calendar|kalender", RegexOption.IGNORE_CASE) to "Calendar",
    Regex("camera|kamera", RegexOption.IGNORE_CASE) to "Camera",
    Regex("music|musik|spotify|audio", RegexOption.IGNORE_CASE) to "Music",
    Regex("youtube|video", RegexOption.IGNORE_CASE) to "Youtube",
    Regex("chrome|browser", RegexOption.IGNORE_CASE) to "Chrome",
    Regex("calculator|rechner", RegexOption.IGNORE_CASE) to "Calculator",
    Regex("files|dateien|file manager", RegexOption.IGNORE_CASE) to "FolderOpen",
    Regex("mail|gmail|email", RegexOption.IGNORE_CASE) to "Mail",
    Regex("maps|karten|navigation", RegexOption.IGNORE_CASE) to "Map",
    Regex("phone|telefon|dialer", RegexOption.IGNORE_CASE) to "Phone",
    Regex("message|nachrichten|sms|chat", RegexOption.IGNORE_CASE) to "MessageCircle",
    Regex("settings|einstellungen", RegexOption.IGNORE_CASE) to "Settings",
    Regex("clock|uhr|alarm", RegexOption.IGNORE_CASE) to "Clock",
    Regex("gallery|photos|fotos|bilder", RegexOption.IGNORE_CASE) to "Image",
    Regex("notes|notizen", RegexOption.IGNORE_CASE) to "NotebookPen"
)

enum class AutoIconFallbackType {
    ORIGINAL,
    LUCIDE,
    NEUTRAL
}

enum class AutoIconRuleMode {
    FOLLOW_HEURISTIC,
    KEEP_ORIGINAL,
    FORCE_FALLBACK
}

data class AutoIconRule(
    val mode: AutoIconRuleMode,
    val reason: String = "user_rule"
) {
    fun serialize(): String = listOf(mode.name, reason).joinToString(AUTO_ICON_SERIALIZATION_DELIMITER)

    companion object {
        fun deserialize(raw: String): AutoIconRule? {
            val parts = raw.split(AUTO_ICON_SERIALIZATION_DELIMITER, limit = 2)
            if (parts.size != 2) return null
            val mode = runCatching { AutoIconRuleMode.valueOf(parts[0]) }.getOrNull() ?: return null
            return AutoIconRule(mode = mode, reason = parts[1].ifBlank { "user_rule" })
        }
    }
}

data class AutoIconFallback(
    val type: AutoIconFallbackType,
    val lucideIconName: String? = null,
    val reason: String = "kept_original",
    val analysisVersion: Int = CURRENT_ANALYSIS_VERSION
) {
    val isFallback: Boolean get() = type != AutoIconFallbackType.ORIGINAL

    fun serialize(): String = listOf(
        type.name,
        analysisVersion.toString(),
        lucideIconName.orEmpty(),
        reason
    ).joinToString(AUTO_ICON_SERIALIZATION_DELIMITER)

    companion object {
        const val CURRENT_ANALYSIS_VERSION = 1

        fun deserialize(raw: String): AutoIconFallback? {
            val parts = raw.split(AUTO_ICON_SERIALIZATION_DELIMITER, limit = 4)
            if (parts.size != 4) return null
            val type = parts[0].let { runCatching { AutoIconFallbackType.valueOf(it) }.getOrNull() } ?: return null
            val version = parts[1].toIntOrNull() ?: return null
            if (version != CURRENT_ANALYSIS_VERSION) return null
            return AutoIconFallback(
                type = type,
                analysisVersion = version,
                lucideIconName = parts[2].ifBlank { null },
                reason = parts[3].ifBlank { "unknown" }
            )
        }
    }
}

class IconAlphaMask(
    val width: Int,
    val height: Int,
    val alpha: IntArray
) {
    init {
        require(width > 0) { "width must be > 0" }
        require(height > 0) { "height must be > 0" }
        require(alpha.size == width * height) { "alpha size must match width * height" }
    }
}

object IconQualityEvaluator {
    fun evaluate(
        mask: IconAlphaMask,
        packageName: String,
        label: String,
        explicitRule: AutoIconRule? = null
    ): AutoIconFallback {
        when (resolveConfiguredRule(packageName, explicitRule)) {
            AutoIconRuleMode.KEEP_ORIGINAL -> {
                return AutoIconFallback(type = AutoIconFallbackType.ORIGINAL, reason = "configured_keep_original")
            }
            AutoIconRuleMode.FORCE_FALLBACK -> {
                return fallbackFor(packageName, label, reason = "configured_force_fallback")
            }
            AutoIconRuleMode.FOLLOW_HEURISTIC,
            null -> Unit
        }

        val width = mask.width
        val height = mask.height
        val totalPixels = width * height
        val visibleThreshold = 24
        val strongThreshold = 180
        val marginX = max(1, width / 10)
        val marginY = max(1, height / 10)
        val cornerWidth = max(1, width / 5)
        val cornerHeight = max(1, height / 5)
        val centerStartX = width / 4
        val centerEndX = width - centerStartX
        val centerStartY = height / 4
        val centerEndY = height - centerStartY

        var visibleCount = 0
        var strongVisibleCount = 0
        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1
        var centerVisibleCount = 0
        var cornerVisibleCount = 0
        var leftEdgeCount = 0
        var rightEdgeCount = 0
        var topEdgeCount = 0
        var bottomEdgeCount = 0
        var alphaSum = 0.0
        var alphaSquaredSum = 0.0

        mask.alpha.forEachIndexed { index, alphaValue ->
            if (alphaValue < visibleThreshold) return@forEachIndexed
            val x = index % width
            val y = index / width
            val alphaNorm = alphaValue / 255.0

            visibleCount += 1
            alphaSum += alphaNorm
            alphaSquaredSum += alphaNorm * alphaNorm
            if (alphaValue >= strongThreshold) strongVisibleCount += 1
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
            if (x in centerStartX until centerEndX && y in centerStartY until centerEndY) centerVisibleCount += 1
            if ((x < cornerWidth || x >= width - cornerWidth) && (y < cornerHeight || y >= height - cornerHeight)) {
                cornerVisibleCount += 1
            }
            if (x < marginX) leftEdgeCount += 1
            if (x >= width - marginX) rightEdgeCount += 1
            if (y < marginY) topEdgeCount += 1
            if (y >= height - marginY) bottomEdgeCount += 1
        }

        if (visibleCount == 0) {
            return fallbackFor(packageName, label, reason = "empty_mask")
        }

        val bboxWidth = maxX - minX + 1
        val bboxHeight = maxY - minY + 1
        val bboxArea = bboxWidth * bboxHeight
        val fillRatio = visibleCount.toFloat() / totalPixels
        val denseFillRatio = visibleCount.toFloat() / bboxArea.coerceAtLeast(1)
        val bboxWidthRatio = bboxWidth.toFloat() / width
        val bboxHeightRatio = bboxHeight.toFloat() / height
        val cornerArea = (cornerWidth * cornerHeight * 4).coerceAtLeast(1)
        val cornerCoverage = cornerVisibleCount.toFloat() / cornerArea
        val centerArea = ((centerEndX - centerStartX) * (centerEndY - centerStartY)).coerceAtLeast(1)
        val centerCoverage = centerVisibleCount.toFloat() / centerArea
        val strongFillRatio = strongVisibleCount.toFloat() / totalPixels
        val edgeCoverages = listOf(
            leftEdgeCount.toFloat() / (marginX * height).coerceAtLeast(1),
            rightEdgeCount.toFloat() / (marginX * height).coerceAtLeast(1),
            topEdgeCount.toFloat() / (marginY * width).coerceAtLeast(1),
            bottomEdgeCount.toFloat() / (marginY * width).coerceAtLeast(1)
        )
        val edgeTouchSides = edgeCoverages.count { it >= 0.18f }
        val alphaMean = alphaSum / visibleCount
        val alphaVariance = (alphaSquaredSum / visibleCount) - (alphaMean * alphaMean)
        val aspectDelta = abs(bboxWidth - bboxHeight).toFloat() / max(bboxWidth, bboxHeight).coerceAtLeast(1)

        val isTinyOrUnreadable = fillRatio < 0.035f || (centerCoverage < 0.025f && fillRatio < 0.08f)
        if (isTinyOrUnreadable) {
            return fallbackFor(packageName, label, reason = "too_small_or_low_center_weight")
        }

        val isFullBleedSquare = bboxWidthRatio > 0.86f &&
            bboxHeightRatio > 0.86f &&
            edgeTouchSides >= 3 &&
            cornerCoverage > 0.16f
        if (isFullBleedSquare) {
            return fallbackFor(packageName, label, reason = "full_bleed_square")
        }

        val isDenseLowDetailBlock = fillRatio > 0.50f &&
            strongFillRatio > 0.42f &&
            denseFillRatio > 0.72f &&
            cornerCoverage > 0.12f &&
            aspectDelta < 0.18f &&
            alphaVariance < 0.028
        if (isDenseLowDetailBlock) {
            return fallbackFor(packageName, label, reason = "dense_low_detail_block")
        }

        return AutoIconFallback(type = AutoIconFallbackType.ORIGINAL, reason = "quality_ok")
    }

    fun evaluate(
        bitmap: Bitmap,
        packageName: String,
        label: String,
        explicitRule: AutoIconRule? = null
    ): AutoIconFallback {
        return evaluate(bitmap.toAlphaMask(), packageName, label, explicitRule)
    }

    fun resolveConfiguredRule(packageName: String, explicitRule: AutoIconRule? = null): AutoIconRuleMode? {
        return when (explicitRule?.mode) {
            AutoIconRuleMode.FOLLOW_HEURISTIC -> null
            AutoIconRuleMode.KEEP_ORIGINAL,
            AutoIconRuleMode.FORCE_FALLBACK -> explicitRule.mode
            null -> DEFAULT_AUTO_ICON_RULES[packageName]?.mode
        }
    }

    fun resolveDefaultRule(packageName: String): AutoIconRule? = DEFAULT_AUTO_ICON_RULES[packageName]

    fun resolveLucideFallbackName(packageName: String, label: String): String? {
        FALLBACK_LUCIDE_PACKAGE_MAP[packageName]?.let { return it }
        val haystack = "$packageName $label"
        return FALLBACK_LUCIDE_KEYWORDS.firstNotNullOfOrNull { (pattern, iconName) ->
            iconName.takeIf { pattern.containsMatchIn(haystack) }
        }
    }

    private fun fallbackFor(packageName: String, label: String, reason: String): AutoIconFallback {
        val lucideIconName = resolveLucideFallbackName(packageName, label)
        return if (lucideIconName != null) {
            AutoIconFallback(
                type = AutoIconFallbackType.LUCIDE,
                lucideIconName = lucideIconName,
                reason = reason
            )
        } else {
            AutoIconFallback(
                type = AutoIconFallbackType.NEUTRAL,
                reason = reason
            )
        }
    }
}

private fun Bitmap.toAlphaMask(): IconAlphaMask {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    return IconAlphaMask(
        width = width,
        height = height,
        alpha = IntArray(pixels.size) { index -> (pixels[index] ushr 24) and 0xFF }
    )
}
