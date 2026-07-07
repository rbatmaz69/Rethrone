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
 * Ergebnis eines Resize-Zooms inkl. Anschlag-Information (U3: Haptik + Chip-Puls).
 *
 * [clampedWidth]/[clampedHeight] sind nur dann `true`, wenn die Achse **frei** ist
 * (`max > min`) und der ungeklemmte Wert ausserhalb des Bereichs lag. Eine gesperrte
 * Achse (`min == max`) meldet bewusst nie "geklemmt" – sonst wuerde jede Pinch-Geste
 * an einem nur-einachsig skalierbaren Widget dauerhaft vibrieren.
 */
data class ResizeZoomResult(
    val size: WidgetSizeDp,
    val clampedWidth: Boolean,
    val clampedHeight: Boolean,
    /**
     * Auf den nuetzlichen Bereich zurueckgeklemmter Akkumulator – der Aufrufer
     * MUSS seinen kumulierten Faktor hiermit ueberschreiben. Ohne die Kappung
     * laeuft der Akkumulator am Anschlag weiter, waehrend die Groesse geklemmt
     * bleibt, und die Gegenrichtung hat eine riesige tote Zone ("einmal gross
     * gezogen, nicht mehr kleinziehbar").
     */
    val appliedZoom: Float,
)

/** Untergrenze des Zoom-Faktors – schuetzt vor degenerierten Gesten-Werten (0/negativ). */
private const val MIN_RESIZE_ZOOM = 0.05f

/**
 * Kappt [zoom] auf den Bereich, in dem sich mindestens eine freie Achse noch
 * bewegt: Untergrenze = kleinstes min/start-Verhaeltnis, Obergrenze = groesstes
 * max/start-Verhaeltnis der freien Achsen. Dadurch reagiert das Reinziehen nach
 * einem Anschlag sofort wieder.
 */
private fun clampZoomToUsefulRange(
    startSize: WidgetSizeDp,
    zoom: Float,
    limits: WidgetResizeLimits,
): Float {
    var minZoom = Float.MAX_VALUE
    var maxZoom = Float.MIN_VALUE
    if (limits.maxWidthDp > limits.minWidthDp && startSize.widthDp > 0) {
        minZoom = minOf(minZoom, limits.minWidthDp.toFloat() / startSize.widthDp)
        maxZoom = maxOf(maxZoom, limits.maxWidthDp.toFloat() / startSize.widthDp)
    }
    if (limits.maxHeightDp > limits.minHeightDp && startSize.heightDp > 0) {
        minZoom = minOf(minZoom, limits.minHeightDp.toFloat() / startSize.heightDp)
        maxZoom = maxOf(maxZoom, limits.maxHeightDp.toFloat() / startSize.heightDp)
    }
    if (minZoom > maxZoom) return zoom.coerceAtLeast(MIN_RESIZE_ZOOM)
    return zoom.coerceIn(minZoom, maxZoom)
}

/**
 * Groesse waehrend einer Pinch-Geste (U3): [startSize] ist die Groesse beim
 * Gesten-Start, [zoom] der **kumulierte** Zoom-Faktor seitdem (nicht das letzte
 * Delta – so gehen Sub-dp-Aenderungen nicht durch Rundung verloren). Beide Achsen
 * skalieren mit demselben Faktor; gesperrte Achsen (min == max) bleiben durch das
 * Clamping automatisch fest. Der Aufrufer fuehrt seinen Akkumulator mit
 * [ResizeZoomResult.appliedZoom] weiter, damit er am Anschlag nicht davonlaeuft.
 */
fun resolveResizeZoom(
    startSize: WidgetSizeDp,
    zoom: Float,
    limits: WidgetResizeLimits,
): ResizeZoomResult {
    val appliedZoom = clampZoomToUsefulRange(startSize, zoom, limits)
    val rawWidth = (startSize.widthDp * appliedZoom).roundToInt()
    val rawHeight = (startSize.heightDp * appliedZoom).roundToInt()
    val width = rawWidth.coerceIn(limits.minWidthDp, limits.maxWidthDp)
    val height = rawHeight.coerceIn(limits.minHeightDp, limits.maxHeightDp)
    return ResizeZoomResult(
        size = WidgetSizeDp(width, height),
        clampedWidth = limits.maxWidthDp > limits.minWidthDp && rawWidth != width,
        clampedHeight = limits.maxHeightDp > limits.minHeightDp && rawHeight != height,
        appliedZoom = appliedZoom,
    )
}
