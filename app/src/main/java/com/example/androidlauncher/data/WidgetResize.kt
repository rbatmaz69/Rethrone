package com.example.androidlauncher.data

import kotlin.math.roundToInt

/**
 * Framework-freie Groessen-Logik fuer das interaktive Widget-Resize im Edit-Modus
 * (B1-PR4). Wie [WidgetBindFlow]: nur Primitive rein, Entscheidungen raus –
 * `AppWidgetProviderInfo` selbst ist nicht JVM-testbar, daher spiegeln die
 * Aufrufstellen dessen Felder in die Parameter.
 */

/** `AppWidgetProviderInfo.RESIZE_HORIZONTAL/VERTICAL`, framework-frei gespiegelt. */
const val WIDGET_RESIZE_HORIZONTAL = 1
const val WIDGET_RESIZE_VERTICAL = 2

/**
 * Erlaubter Groessenbereich eines Widgets in dp. Eine nicht skalierbare Achse
 * wird ueber `min == max` ausgedrueckt – der Drag auf ihr ist dann ein No-Op,
 * wodurch ein einzelnes Eck-Handle alle vier resizeMode-Faelle abdeckt.
 */
data class WidgetResizeLimits(
    val minWidthDp: Int,
    val maxWidthDp: Int,
    val minHeightDp: Int,
    val maxHeightDp: Int,
) {
    /** `false` bei RESIZE_NONE (oder degeneriertem Bereich) → kein Handle anzeigen. */
    val isResizable: Boolean get() = maxWidthDp > minWidthDp || maxHeightDp > minHeightDp
}

/**
 * Leitet die Resize-Grenzen aus den Provider-Angaben und der Screen-Groesse ab.
 *
 * - Provider-Minima von 0 fallen auf die App-Untergrenzen zurueck (greifbares Widget).
 * - `maxResize*Dp == 0` heisst "vom Provider nicht begrenzt" (API-31-Semantik) –
 *   dann begrenzt nur der Screen (gleiche Kappung wie [defaultWidgetSizeDp]).
 * - Degeneriert (Provider-Minimum ueber der Screen-Kappung): Achse wird auf dem
 *   Minimum festgesetzt statt zu invertieren.
 */
@Suppress("LongParameterList") // spiegelt bewusst die AppWidgetProviderInfo-Felder als Primitive
fun resolveResizeLimits(
    resizeMode: Int,
    minResizeWidthDp: Int,
    minResizeHeightDp: Int,
    maxResizeWidthDp: Int,
    maxResizeHeightDp: Int,
    currentWidthDp: Int,
    currentHeightDp: Int,
    screenWidthDp: Int,
    screenHeightDp: Int,
): WidgetResizeLimits {
    val screenMaxWidth = (screenWidthDp - SCREEN_EDGE_MARGIN_DP).coerceAtLeast(MIN_WIDGET_WIDTH_DP)
    val screenMaxHeight = (screenHeightDp * MAX_HEIGHT_SCREEN_FRACTION).toInt()
        .coerceAtLeast(MIN_WIDGET_HEIGHT_DP)

    val (minWidth, maxWidth) = axisRange(
        allowed = resizeMode and WIDGET_RESIZE_HORIZONTAL != 0,
        providerMinDp = minResizeWidthDp,
        providerMaxDp = maxResizeWidthDp,
        floorDp = MIN_WIDGET_WIDTH_DP,
        screenCapDp = screenMaxWidth,
        currentDp = currentWidthDp,
    )
    val (minHeight, maxHeight) = axisRange(
        allowed = resizeMode and WIDGET_RESIZE_VERTICAL != 0,
        providerMinDp = minResizeHeightDp,
        providerMaxDp = maxResizeHeightDp,
        floorDp = MIN_WIDGET_HEIGHT_DP,
        screenCapDp = screenMaxHeight,
        currentDp = currentHeightDp,
    )
    return WidgetResizeLimits(minWidth, maxWidth, minHeight, maxHeight)
}

private fun axisRange(
    allowed: Boolean,
    providerMinDp: Int,
    providerMaxDp: Int,
    floorDp: Int,
    screenCapDp: Int,
    currentDp: Int,
): Pair<Int, Int> {
    if (!allowed) return currentDp to currentDp
    val min = if (providerMinDp > 0) providerMinDp else floorDp
    val providerMax = if (providerMaxDp > 0) providerMaxDp else Int.MAX_VALUE
    val max = minOf(providerMax, screenCapDp).coerceAtLeast(min)
    return min to max
}

/**
 * Groesse waehrend eines Resize-Drags: [startSize] ist die Groesse beim Gesten-Start,
 * [totalDragXDp]/[totalDragYDp] der **kumulierte** Drag seitdem (nicht das letzte
 * Delta – so gehen Sub-dp-Bewegungen nicht durch Rundung verloren).
 */
fun applyResizeDrag(
    startSize: WidgetSizeDp,
    totalDragXDp: Float,
    totalDragYDp: Float,
    limits: WidgetResizeLimits,
): WidgetSizeDp = WidgetSizeDp(
    widthDp = (startSize.widthDp + totalDragXDp).roundToInt()
        .coerceIn(limits.minWidthDp, limits.maxWidthDp),
    heightDp = (startSize.heightDp + totalDragYDp).roundToInt()
        .coerceIn(limits.minHeightDp, limits.maxHeightDp),
)
