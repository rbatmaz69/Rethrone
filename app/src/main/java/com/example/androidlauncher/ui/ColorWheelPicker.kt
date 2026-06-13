package com.example.androidlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.Text
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Wandelt HSV (h:0..360, s/v:0..1) in eine Compose-Farbe. */
private fun hsvToColor(h: Float, s: Float, v: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))))

/** Zerlegt eine Farbe in HSV-Komponenten. */
private fun Color.toHsv(): FloatArray {
    val out = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), out)
    return out
}

private val hueColors: List<Color> = listOf(
    Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
    Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFFFF0000)
)

private val quickSwatches: List<Color> = listOf(
    Color.White, Color(0xFF010101), Color(0xFFEF4444), Color(0xFFF59E0B),
    Color(0xFF22C55E), Color(0xFF0EA5E9), Color(0xFF6366F1), Color(0xFFEC4899)
)

/**
 * Freier HSV-Farbwähler in purem Compose: Sättigungs-/Helligkeitsfeld, Hue-Slider,
 * Live-Vorschau und Schnellwahl-Swatches (inkl. Weiß als Standard).
 *
 * Während des Ziehens wird nur die lokale Vorschau aktualisiert; [onColorChange]
 * wird beim Loslassen bzw. Tippen aufgerufen, um unnötige Persistenz-Schreibvorgänge
 * zu vermeiden.
 */
@Composable
fun ColorWheelPicker(
    color: Color,
    onColorChange: (Color) -> Unit,
    mainTextColor: Color,
    modifier: Modifier = Modifier
) {
    // Lokaler HSV-Zustand, initial aus der übergebenen Farbe abgeleitet.
    val initialHsv = remember(color) { color.toHsv() }
    var hue by remember(color) { mutableStateOf(initialHsv[0]) }
    var sat by remember(color) { mutableStateOf(initialHsv[1]) }
    var value by remember(color) { mutableStateOf(initialHsv[2]) }

    val currentColor = hsvToColor(hue, sat, value)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Sättigungs-/Helligkeitsfeld
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, mainTextColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        sat = (offset.x / size.width).coerceIn(0f, 1f)
                        value = (1f - offset.y / size.height).coerceIn(0f, 1f)
                        onColorChange(hsvToColor(hue, sat, value))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { onColorChange(hsvToColor(hue, sat, value)) }
                    ) { change, _ ->
                        sat = (change.position.x / size.width).coerceIn(0f, 1f)
                        value = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                    }
                }
        ) {
            drawSaturationValueField(hue)
            // Auswahl-Indikator
            val cx = sat * size.width
            val cy = (1f - value) * size.height
            drawCircle(color = Color.White, radius = 10f, center = Offset(cx, cy), style = Stroke(width = 3f))
            drawCircle(color = Color.Black.copy(alpha = 0.6f), radius = 13f, center = Offset(cx, cy), style = Stroke(width = 1.5f))
        }

        // Hue-Slider
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(999.dp))
                .border(1.dp, mainTextColor.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        hue = (offset.x / size.width).coerceIn(0f, 1f) * 360f
                        onColorChange(hsvToColor(hue, sat, value))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { onColorChange(hsvToColor(hue, sat, value)) }
                    ) { change, _ ->
                        hue = (change.position.x / size.width).coerceIn(0f, 1f) * 360f
                    }
                }
        ) {
            drawRect(brush = Brush.horizontalGradient(hueColors))
            val x = (hue / 360f) * size.width
            drawCircle(color = Color.White, radius = size.height / 2f - 2f, center = Offset(x.coerceIn(0f, size.width), size.height / 2f), style = Stroke(width = 3f))
        }

        // Vorschau + Hex
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(1.dp, mainTextColor.copy(alpha = 0.3f), CircleShape)
            )
            val hex = remember(currentColor) {
                "#%06X".format(0xFFFFFF and currentColor.toArgb())
            }
            Text(hex, color = mainTextColor, fontSize = 16.sp)
        }

        // Schnellwahl-Swatches
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickSwatches.forEach { swatch ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(swatch)
                        .border(1.dp, mainTextColor.copy(alpha = 0.25f), CircleShape)
                        .pointerInput(swatch) {
                            detectTapGestures {
                                val hsv = swatch.toHsv()
                                hue = hsv[0]; sat = hsv[1]; value = hsv[2]
                                onColorChange(swatch)
                            }
                        }
                )
            }
        }
    }
}

/** Zeichnet das Sättigungs-/Helligkeitsfeld für den gegebenen Farbton. */
private fun DrawScope.drawSaturationValueField(hue: Float) {
    val hueColor = hsvToColor(hue, 1f, 1f)
    // Horizontal: Weiß -> reiner Farbton (Sättigung)
    drawRect(brush = Brush.horizontalGradient(listOf(Color.White, hueColor)))
    // Vertikal: Transparent -> Schwarz (Helligkeit)
    drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
}
