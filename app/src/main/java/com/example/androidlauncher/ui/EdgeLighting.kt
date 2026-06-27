package com.example.androidlauncher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.unit.dp
import com.example.androidlauncher.data.EdgeLightingStyle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Edge-Lighting-Overlay im Samsung-Stil: Bei jeder Änderung von [pulseId] leuchtet der abgerundete
 * Bildschirmrand für [laps] Runden auf – je nach [style] als umlaufendes Licht, als zwei von der
 * Kamera ausgehende Lichter oder als pulsierender Voll-Rand.
 *
 * Rein zeichnerisch – kein `pointerInput`/`clickable`, fängt also **keine** Touches ab. Im Ruhezustand
 * wird nichts gezeichnet und nichts neu komponiert.
 *
 * @param pulseId Zähler; jede Erhöhung startet einen neuen Lauf. Initialwert `0` startet nichts.
 * @param color Leuchtfarbe (Farben-Menü).
 * @param style Visueller Stil ([EdgeLightingStyle]).
 * @param lapDurationMs Dauer **einer** Runde in ms (aus dem Tempo abgeleitet).
 * @param laps Anzahl Runden pro Benachrichtigung (1..5).
 * @param thickness Stärke-Faktor (skaliert Strich-/Glow-Breite).
 */
@Composable
fun EdgeLighting(
    modifier: Modifier = Modifier,
    color: Color,
    pulseId: Int,
    style: EdgeLightingStyle = EdgeLightingStyle.SWEEP,
    lapDurationMs: Int = 1400,
    laps: Int = 1,
    thickness: Float = 1f
) {
    val progress = remember { Animatable(0f) }
    var active by remember { mutableStateOf(false) }

    LaunchedEffect(pulseId) {
        if (pulseId == 0) return@LaunchedEffect
        active = true
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = laps.toFloat(),
            animationSpec = tween(
                durationMillis = (lapDurationMs * laps).coerceAtLeast(1),
                easing = LinearEasing
            )
        )
        active = false
    }

    Canvas(modifier = modifier) {
        if (!active) return@Canvas

        val p = progress.value
        val overall = (p / laps).coerceIn(0f, 1f)
        // Hülle: am Anfang auf, am Ende ab – über den Gesamtlauf.
        val envelope = when {
            overall < 0.08f -> overall / 0.08f
            overall > 0.88f -> (1f - overall) / 0.12f
            else -> 1f
        }.coerceIn(0f, 1f)
        if (envelope <= 0f) return@Canvas

        val glowPx = 18.dp.toPx() * thickness
        val corePx = 6.dp.toPx() * thickness
        val radiusPx = 44.dp.toPx()
        // Bündig am äußersten Bildschirmrand: nur die Kern-Linie minimal nach innen setzen,
        // der Glow darf nach außen über die Kante hinaus geclippt werden.
        val inset = corePx / 2f

        // Abgerundetes Rechteck als geschlossene Schleife = Laufpfad / Voll-Rand.
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

        if (style == EdgeLightingStyle.GLOW_PULSE) {
            // Voller Rand pulsiert: ein „Atem"-Puls pro Runde.
            val pulse = abs(sin(p * Math.PI).toFloat())
            val a = pulse * envelope
            drawPath(path, color.copy(alpha = a * 0.35f), style = Stroke(width = glowPx, cap = StrokeCap.Round))
            drawPath(path, color.copy(alpha = a), style = Stroke(width = corePx, cap = StrokeCap.Round))
            return@Canvas
        }

        val posBuf = FloatArray(2)
        fun pointAt(dist: Float): Offset {
            var d = dist % length
            if (d < 0f) d += length
            measure.getPosTan(d, posBuf, null)
            return Offset(posBuf[0], posBuf[1])
        }

        // Zusammenhängender Teilpfad von [startDist] bis [endDist] entlang der geschlossenen
        // Schleife. Bei Umlauf (endDist > length) wird in zwei getSegment-Aufrufe gesplittet,
        // damit eine **durchgehende** Linie statt einzelner Segmente entsteht.
        fun subPath(startDist: Float, endDist: Float): Path {
            val out = Path()
            val android = out.asAndroidPath()
            var s = startDist % length
            if (s < 0f) s += length
            val span = (endDist - startDist).coerceIn(0f, length)
            val e = s + span
            if (e <= length) {
                measure.getSegment(s, e, android, true)
            } else {
                measure.getSegment(s, length, android, true)
                measure.getSegment(0f, e - length, android, true)
            }
            return out
        }

        // Komet als durchgehende Linie: voller Schweif (Glow + Kern) plus heller Kopf-Abschnitt.
        // Kopf bei [headDist], Schweif entgegen der Laufrichtung [dir].
        fun drawComet(headDist: Float, dir: Float, baseAlpha: Float) {
            val cometLen = length * 0.32f
            val tail = headDist - dir * cometLen
            val fullStart = min(headDist, tail)
            val fullEnd = max(headDist, tail)
            val fullPath = subPath(fullStart, fullEnd)
            // Weicher Glow-Saum.
            drawPath(fullPath, color.copy(alpha = 0.18f * baseAlpha), style = Stroke(width = glowPx, cap = StrokeCap.Round))
            // Durchgehende Kern-Linie.
            drawPath(fullPath, color.copy(alpha = 0.55f * baseAlpha), style = Stroke(width = corePx, cap = StrokeCap.Round))
            // Heller Kopf (~35 % der Kometenlänge), Richtung Weiß.
            val headTail = headDist - dir * cometLen * 0.35f
            val headPath = subPath(min(headDist, headTail), max(headDist, headTail))
            drawPath(headPath, lerpColor(color, Color.White, 0.4f).copy(alpha = baseAlpha), style = Stroke(width = corePx, cap = StrokeCap.Round))
        }

        val t = p % 1f
        when (style) {
            EdgeLightingStyle.SWEEP -> {
                drawComet(headDist = t * length, dir = 1f, baseAlpha = envelope)
            }
            EdgeLightingStyle.FROM_CAMERA -> {
                // Top-Center (≈ Kamera) per Grob-Scan finden.
                val topCenter = Offset(size.width / 2f, inset)
                var topCenterDist = 0f
                var best = Float.MAX_VALUE
                val scan = 360
                for (i in 0 until scan) {
                    val d = length * i / scan
                    val distSq = (pointAt(d) - topCenter).getDistanceSquared()
                    if (distSq < best) { best = distSq; topCenterDist = d }
                }
                val half = length / 2f
                drawComet(topCenterDist + t * half, dir = 1f, baseAlpha = envelope)   // im Uhrzeigersinn
                drawComet(topCenterDist - t * half, dir = -1f, baseAlpha = envelope)  // gegen den Uhrzeigersinn
            }
            EdgeLightingStyle.GLOW_PULSE -> Unit // oben behandelt
        }
    }
}
