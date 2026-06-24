package com.example.androidlauncher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.unit.dp

/**
 * Edge-Lighting-Overlay im Samsung-Stil: Bei jeder Änderung von [pulseId] läuft **einmal** ein
 * leuchtender Lichtpunkt mit nachziehendem Schweif am abgerundeten Bildschirmrand entlang.
 *
 * Rein zeichnerisch – das Composable hängt keinen `pointerInput`/`clickable` an, fängt also **keine**
 * Touches ab (Home-Screen bleibt während des Leuchtens voll bedienbar). Im Ruhezustand (keine aktive
 * Runde) wird nichts gezeichnet und nichts neu komponiert.
 *
 * @param pulseId Zähler; jede Erhöhung startet eine neue Runde. Der Initialwert `0` startet nichts.
 * @param color Leuchtfarbe (frei wählbar über das Farben-Menü).
 */
@Composable
fun EdgeLighting(
    modifier: Modifier = Modifier,
    color: Color,
    pulseId: Int
) {
    val sweep = remember { Animatable(0f) }
    var active by remember { mutableStateOf(false) }

    LaunchedEffect(pulseId) {
        if (pulseId == 0) return@LaunchedEffect
        active = true
        sweep.snapTo(0f)
        sweep.animateTo(1f, tween(durationMillis = 1400, easing = FastOutSlowInEasing))
        active = false
    }

    Canvas(modifier = modifier) {
        if (!active) return@Canvas

        val head = sweep.value
        // Sanftes Erscheinen/Verschwinden über die Runde (Kopf taucht auf, Schweif klingt aus).
        val envelope = when {
            head < 0.08f -> head / 0.08f
            head > 0.82f -> (1f - head) / 0.18f
            else -> 1f
        }.coerceIn(0f, 1f)
        if (envelope <= 0f) return@Canvas

        val glowPx = 18.dp.toPx()
        val corePx = 6.dp.toPx()
        val radiusPx = 44.dp.toPx()
        val inset = glowPx / 2f

        // Abgerundetes Rechteck als geschlossene Schleife = Laufpfad des Lichts.
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    left = inset,
                    top = inset,
                    right = size.width - inset,
                    bottom = size.height - inset,
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
            )
        }
        val measure = android.graphics.PathMeasure(path.asAndroidPath(), true)
        val length = measure.length
        if (length <= 0f) return@Canvas

        val posBuf = FloatArray(2)
        fun pointAt(dist: Float): Offset {
            var d = dist % length
            if (d < 0f) d += length
            measure.getPosTan(d, posBuf, null)
            return Offset(posBuf[0], posBuf[1])
        }

        val headDist = head * length
        val cometLen = length * 0.32f
        val segments = 26
        // Vom Schweif (f=0) zum Kopf (f=1): kurze Teilstücke mit nach vorn steigender Helligkeit.
        for (i in 0 until segments) {
            val f0 = i / segments.toFloat()
            val f1 = (i + 1) / segments.toFloat()
            val p0 = pointAt(headDist - (1f - f0) * cometLen)
            val p1 = pointAt(headDist - (1f - f1) * cometLen)
            val a = (f1 * f1) * envelope
            // Weicher, breiter Glow darunter.
            drawLine(color.copy(alpha = a * 0.25f), p0, p1, strokeWidth = glowPx, cap = StrokeCap.Round)
            // Heller Kern, zum Kopf hin Richtung Weiß für einen „heißen" Lichtpunkt.
            val coreColor = lerpColor(color, Color.White, (f1 * f1) * 0.5f)
            drawLine(coreColor.copy(alpha = a), p0, p1, strokeWidth = corePx, cap = StrokeCap.Round)
        }
    }
}
