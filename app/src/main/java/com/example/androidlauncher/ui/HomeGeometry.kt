package com.example.androidlauncher.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Reine Geometrie-Helfer fuer die Kollisions-/Treffer-Erkennung beim Verschieben der
 * Home-Elemente (Uhr/Datum/Wetter/Favoriten im Edit-Modus).
 *
 * Bewusst aus dem grossen `HomeScreen`-Composable herausgezogen: framework-frei (operiert nur
 * auf Compose-`Rect`/`Offset`-Wertobjekten) und damit ohne Emulator unit-testbar.
 */

/**
 * Verbotszone am unteren Bildschirmrand fuer die System-Navigationsleiste – Home-Elemente
 * duerfen nicht hineinragen. Leeres Rechteck bei ungueltiger Container-Groesse.
 */
internal fun navigationBarForbiddenZone(
    rootWidth: Int,
    rootHeight: Int,
    navigationBarHeightPx: Float,
): Rect {
    if (rootWidth <= 0 || rootHeight <= 0) return Rect(0f, 0f, 0f, 0f)
    return Rect(
        left = 0f,
        top = rootHeight.toFloat() - navigationBarHeightPx,
        right = rootWidth.toFloat(),
        bottom = rootHeight.toFloat(),
    )
}

/** Verschiebt [rect] um [x]/[y]. */
internal fun translateRect(rect: Rect, x: Float, y: Float): Rect =
    Rect(
        left = rect.left + x,
        top = rect.top + y,
        right = rect.right + x,
        bottom = rect.bottom + y,
    )

/** Vergroessert [rect] allseitig um [padding] (negatives Padding verkleinert). */
internal fun expandRect(rect: Rect, padding: Float): Rect =
    Rect(
        left = rect.left - padding,
        top = rect.top - padding,
        right = rect.right + padding,
        bottom = rect.bottom + padding,
    )

/** true, wenn sich [first] und [second] echt ueberschneiden (blosses Beruehren zaehlt nicht). */
internal fun intersects(first: Rect, second: Rect): Boolean =
    first.left < second.right &&
        first.right > second.left &&
        first.top < second.bottom &&
        first.bottom > second.top

/** true, wenn [point] innerhalb (inkl. Rand) von [rect] liegt. */
internal fun rectContains(rect: Rect, point: Offset): Boolean =
    point.x >= rect.left &&
        point.x <= rect.right &&
        point.y >= rect.top &&
        point.y <= rect.bottom

/**
 * Flaecheninhalt der Ueberlappung von [first] und [second]. 0, wenn sie sich nicht
 * ueberschneiden (negative Breite/Hoehe).
 */
internal fun intersectionArea(first: Rect, second: Rect): Float {
    val width = minOf(first.right, second.right) - maxOf(first.left, second.left)
    val height = minOf(first.bottom, second.bottom) - maxOf(first.top, second.top)
    return if (width > 0f && height > 0f) width * height else 0f
}

/**
 * Begrenzt die Verschiebe-Position ([x]/[y]) eines Home-Elements so, dass das um [bounds]
 * beschriebene Element innerhalb des Wurzel-Containers ([rootWidth] x [rootHeight]) bleibt.
 *
 * [topLimit] hebt die obere Grenze an (z. B. fuer die Uhr), [navigationBarHeightPx] haelt das
 * Element ueber der System-Navigationsleiste. Bei ungueltiger Container-Groesse wird die
 * Eingabe unveraendert zurueckgegeben. Reine Mathematik – ohne Emulator unit-testbar.
 */
internal fun clampOffsetToBounds(
    bounds: Rect,
    rootWidth: Int,
    rootHeight: Int,
    topLimit: Float,
    navigationBarHeightPx: Float,
    x: Float,
    y: Float,
): Offset {
    if (rootWidth <= 0 || rootHeight <= 0) return Offset(x, y)

    val minX = -bounds.left
    val maxX = rootWidth.toFloat() - bounds.right
    val minY = -bounds.top + topLimit
    val maxY = (rootHeight.toFloat() - bounds.bottom - navigationBarHeightPx).coerceAtLeast(minY)

    return Offset(x.coerceIn(minX, maxX), y.coerceIn(minY, maxY))
}
