package com.example.androidlauncher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.example.androidlauncher.ui.theme.LocalAnimationsEnabled
import com.example.androidlauncher.ui.theme.RethroneSprings

/**
 * 3×3-Muster-Sperre. Der Nutzer verbindet per Wischgeste die Knoten; beim Loslassen wird
 * die Knotenfolge (z. B. "0,4,8") über [onComplete] gemeldet. Knoten sind 0..8 (zeilenweise).
 */
@Composable
fun PatternLockView(
    onComplete: (String) -> Unit,
    nodeColor: Color,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val selected = remember { mutableStateListOf<Int>() }
    var currentDrag by remember { mutableStateOf<Offset?>(null) }
    var canvasWidth by remember { mutableStateOf(0f) }

    // Material-3-Expressive: Knoten wachsen weich an, statt hart umzuspringen.
    val animationsEnabled = LocalAnimationsEnabled.current
    val nodeActivation = (0 until 9).map { index ->
        animateFloatAsState(
            targetValue = if (index in selected) 1f else 0f,
            animationSpec = if (animationsEnabled) RethroneSprings.spatial<Float>() else snap<Float>(),
            label = "patternNode$index"
        )
    }

    fun nodeCenter(index: Int, size: Float): Offset {
        val col = index % 3
        val row = index / 3
        val step = size / 3f
        return Offset(step * col + step / 2f, step * row + step / 2f)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        selected.clear()
                        currentDrag = offset
                        hitNode(offset, canvasWidth)?.let { if (it !in selected) selected.add(it) }
                    },
                    onDrag = { change, _ ->
                        currentDrag = change.position
                        hitNode(change.position, canvasWidth)?.let { if (it !in selected) selected.add(it) }
                    },
                    onDragEnd = {
                        currentDrag = null
                        if (selected.isNotEmpty()) {
                            onComplete(selected.joinToString(","))
                        }
                        selected.clear()
                    },
                    onDragCancel = {
                        currentDrag = null
                        selected.clear()
                    }
                )
            }
    ) {
        canvasWidth = size.width
        val side = size.minDimension

        // Verbindungslinien zwischen gewählten Knoten
        for (i in 0 until selected.size - 1) {
            drawLine(
                color = lineColor,
                start = nodeCenter(selected[i], side),
                end = nodeCenter(selected[i + 1], side),
                strokeWidth = 10f,
                cap = StrokeCap.Round
            )
        }
        // Linie vom letzten Knoten zum Finger
        val drag = currentDrag
        if (selected.isNotEmpty() && drag != null) {
            drawLine(
                color = lineColor.copy(alpha = 0.6f),
                start = nodeCenter(selected.last(), side),
                end = drag,
                strokeWidth = 10f,
                cap = StrokeCap.Round
            )
        }

        // Knoten – Radius/Farbe/Aura folgen der weichen Aktivierung.
        for (index in 0 until 9) {
            val center = nodeCenter(index, side)
            val act = nodeActivation[index].value
            if (act > 0.001f) {
                drawCircle(
                    color = lineColor.copy(alpha = 0.2f * act),
                    radius = (side / 28f) + (side / 12f - side / 28f) * act,
                    center = center
                )
            }
            drawCircle(
                color = lerp(nodeColor.copy(alpha = 0.4f), lineColor, act),
                radius = (side / 28f) + (side / 22f - side / 28f) * act,
                center = center
            )
        }
    }
}

/** Liefert den Knotenindex (0..8) nahe der Berührung oder null. */
internal fun hitNode(offset: Offset, size: Float): Int? {
    if (size <= 0f) return null
    val step = size / 3f
    val col = (offset.x / step).toInt().coerceIn(0, 2)
    val row = (offset.y / step).toInt().coerceIn(0, 2)
    val centerX = step * col + step / 2f
    val centerY = step * row + step / 2f
    val dx = offset.x - centerX
    val dy = offset.y - centerY
    val hitRadius = step / 3f
    return if (dx * dx + dy * dy <= hitRadius * hitRadius) row * 3 + col else null
}
