package com.example.androidlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Zentrales Hilfs-Objekt für den "Liquid Glass"-Effekt.
 *
 * Kapselt die wiederverwendbare Logik für halbtransparente Glas-Hintergründe
 * und Rahmen, die sich an den Dark-Text-Modus anpassen.
 * Eliminiert die vielfache Duplikation des gleichen Brush/Border-Codes
 * über die gesamte UI-Schicht hinweg.
 */
object LiquidGlass {

    // ── Brush-Factories ──────────────────────────────────────────────

    /**
     * Erzeugt den halbtransparenten Hintergrund-Brush für Glass-Elemente.
     * @param isDarkText Ob der dunkle Textmodus aktiv ist.
     * @param startAlpha Alpha des Startpunkts (Standard: 0.15).
     * @param endAlpha Alpha des Endpunkts (Standard: 0.05).
     */
    fun glassBrush(
        isDarkText: Boolean,
        startAlpha: Float = 0.15f,
        endAlpha: Float = 0.05f
    ): Brush {
        val baseColor = if (isDarkText) Color.Black else Color.White
        return Brush.linearGradient(
            colors = listOf(
                baseColor.copy(alpha = startAlpha),
                baseColor.copy(alpha = endAlpha)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    /**
     * Erzeugt den Rahmen-Brush für Glass-Elemente.
     * @param isDarkText Ob der dunkle Textmodus aktiv ist.
     * @param startAlpha Alpha-Wert des stärkeren Rahmens.
     * @param endAlpha Alpha-Wert des schwächeren Rahmens.
     */
    fun borderBrush(
        isDarkText: Boolean,
        startAlpha: Float = if (isDarkText) 0.8f else 0.6f,
        endAlpha: Float = if (isDarkText) 0.3f else 0.1f
    ): Brush {
        val baseColor = if (isDarkText) Color.Black else Color.White
        return Brush.linearGradient(
            colors = listOf(
                baseColor.copy(alpha = startAlpha),
                baseColor.copy(alpha = endAlpha)
            )
        )
    }

    // ── Modifier ─────────────────────────────────────────────────────

    /**
     * Wendet den vollständigen Liquid-Glass-Effekt auf ein Element an
     * (Hintergrund-Brush + Rahmen).
     *
     * @param shape Die Form des Elements (z. B. RoundedCornerShape, CircleShape).
     * @param isDarkText Ob der dunkle Textmodus aktiv ist.
     * @param borderWidth Breite des Rahmens.
     */
    fun Modifier.liquidGlass(
        shape: Shape,
        isDarkText: Boolean,
        borderWidth: Dp = 1.2.dp
    ): Modifier = this
        .background(glassBrush(isDarkText), shape)
        .border(BorderStroke(borderWidth, borderBrush(isDarkText)), shape)

    /**
     * Wendet entweder den Liquid-Glass-Effekt oder einen einfachen halbtransparenten
     * Hintergrund an, abhängig davon ob Liquid Glass aktiviert ist.
     *
     * @param shape Die Form des Elements.
     * @param isDarkText Ob der dunkle Textmodus aktiv ist.
     * @param isEnabled Ob Liquid Glass aktiviert ist.
     * @param fallbackAlpha Alpha-Wert für den einfachen Hintergrund wenn Glass deaktiviert ist.
     * @param borderWidth Breite des Glass-Rahmens.
     */
    fun Modifier.conditionalGlass(
        shape: Shape,
        isDarkText: Boolean,
        isEnabled: Boolean,
        fallbackAlpha: Float = 0.1f,
        borderWidth: Dp = 1.2.dp
    ): Modifier = if (isEnabled) {
        this.liquidGlass(shape, isDarkText, borderWidth)
    } else {
        val baseColor = if (isDarkText) Color.Black else Color.White
        this.background(baseColor.copy(alpha = fallbackAlpha), shape)
    }

    // ── Switch-Farben ────────────────────────────────────────────────

    /**
     * Erzeugt [SwitchColors] passend zum Liquid-Glass-Design.
     * Wird überall dort verwendet, wo Switches im Glass-Stil erscheinen.
     *
     * @param isDarkText Ob der dunkle Textmodus aktiv ist.
     * @param isGlassEnabled Ob Liquid Glass aktiviert ist.
     */
    @Composable
    fun switchColors(
        isDarkText: Boolean,
        isGlassEnabled: Boolean
    ): SwitchColors {
        return if (isGlassEnabled) {
            if (isDarkText) {
                SwitchDefaults.colors(
                    checkedTrackColor = Color.Black.copy(alpha = 0.15f),
                    uncheckedTrackColor = Color.Black.copy(alpha = 0.05f),
                    checkedThumbColor = Color.Black,
                    uncheckedThumbColor = Color.Black.copy(alpha = 0.8f),
                    checkedBorderColor = Color.Black.copy(alpha = 0.2f),
                    uncheckedBorderColor = Color.Black.copy(alpha = 0.2f)
                )
            } else {
                SwitchDefaults.colors(
                    checkedTrackColor = Color.White.copy(alpha = 0.25f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                    checkedThumbColor = Color.White,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.9f),
                    checkedBorderColor = Color.White.copy(alpha = 0.2f),
                    uncheckedBorderColor = Color.White.copy(alpha = 0.15f)
                )
            }
        } else {
            SwitchDefaults.colors(
                checkedTrackColor = Color.White.copy(alpha = 0.2f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.2f),
                checkedThumbColor = if (isDarkText) Color.Black else Color.White,
                uncheckedThumbColor = if (isDarkText) Color.Black.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f),
                checkedBorderColor = if (isDarkText) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f),
                uncheckedBorderColor = if (isDarkText) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f)
            )
        }
    }

    // ── Hilfsfunktionen ──────────────────────────────────────────────

    /**
     * Gibt die Haupt-Textfarbe basierend auf dem Dark-Text-Modus zurück.
     */
    fun mainTextColor(isDarkText: Boolean): Color =
        if (isDarkText) Color(0xFF010101) else Color.White

    /**
     * Gibt eine sekundäre (gedämpfte) Textfarbe zurück.
     */
    fun secondaryTextColor(isDarkText: Boolean): Color =
        if (isDarkText) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f)
}

