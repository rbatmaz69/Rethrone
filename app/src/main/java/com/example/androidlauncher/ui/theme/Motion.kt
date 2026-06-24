package com.example.androidlauncher.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset

/**
 * Zentrale Motion-Sprache im Material-3-Expressive-Stil.
 *
 * Bündelt federnde Spring-Specs und einheitliche Menü-/Sheet-Übergänge, damit
 * sich der gesamte Launcher universell „dynamisch & verspielt" anfühlt – statt
 * uneinheitlicher, harter `slideIn/Out`-Blöcke in jeder Menü-Datei.
 *
 * Alle Aufrufer sollten die Übergänge hinter [LocalAnimationsEnabled] gaten:
 * bei deaktivierten Animationen `EnterTransition.None` / `ExitTransition.None`
 * verwenden.
 */
object RethroneSprings {

    /**
     * Räumliche Bewegung (Position/Größe): spürbar federnd.
     * [stiffnessScale] skaliert das Tempo (höher = schneller).
     */
    fun <T> spatial(stiffnessScale: Float = 1f): androidx.compose.animation.core.SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow * stiffnessScale
    )

    /**
     * Effekte (Alpha/Farbe): weich, aber ohne Nachschwingen.
     * [stiffnessScale] skaliert das Tempo (höher = schneller).
     */
    fun <T> effects(stiffnessScale: Float = 1f): androidx.compose.animation.core.SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium * stiffnessScale
    )

    /**
     * Vollflächige Container-Übergänge (Drawer, große Sheets): knackig-räumlich,
     * aber nur minimaler Overshoot. Eine stark federnde Bewegung würde am
     * Container-Rand kurz eine Wallpaper-Lücke zeigen – daher höher gedämpft
     * (≈0.85) als [spatial], aber lebendiger als [effects].
     * [stiffnessScale] skaliert das Tempo (höher = schneller).
     */
    fun <T> container(stiffnessScale: Float = 1f): androidx.compose.animation.core.SpringSpec<T> = spring(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessMedium * stiffnessScale
    )

    /**
     * Größen-Morph (Breite/Höhe eines `AnimatedContent`-Containers): weich und **ohne**
     * Nachschwingen, damit ein wachsender/schrumpfender Container (z. B. die Dynamic-Island-
     * Pille beim Inhalts-Wechsel) sauber in einem Zug morpht, statt am Rand zu wippen. Bewusst
     * etwas weicher (MediumLow) als [effects], damit der Größen-Verlauf zur Überblendung passt.
     * [stiffnessScale] skaliert das Tempo (höher = schneller).
     */
    fun <T> morph(stiffnessScale: Float = 1f): androidx.compose.animation.core.SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow * stiffnessScale
    )

    /**
     * Insel-Öffnung: physischer, ausladender „Aus-der-Notch"-Schwung mit deutlichem, verspieltem
     * Überschwingen/Nachfedern. Bewusst weicher (mehr Bounce) und langsamer als [container]/[morph] –
     * die kleine, schwebende Pille darf am Rand kurz überschwingen (kein Vollbild-Container → keine
     * Wallpaper-Lücke). [stiffnessScale] skaliert das Tempo (höher = schneller).
     */
    fun <T> island(stiffnessScale: Float = 1f): androidx.compose.animation.core.SpringSpec<T> = spring(
        dampingRatio = 0.45f,
        stiffness = Spring.StiffnessMediumLow * 0.7f * stiffnessScale
    )
}

/**
 * Einheitlicher Expressive-Eingang für Menüs/Sheets: federndes „Aufpoppen"
 * aus leicht verkleinerter, von unten kommender Position + Einblenden.
 *
 * @param animationsEnabled wenn `false`, wird [EnterTransition.None] genutzt.
 * @param fromBottom wenn `true`, gleitet der Inhalt zusätzlich von unten herein.
 */
@Composable
fun rememberMenuEnter(
    animationsEnabled: Boolean,
    fromBottom: Boolean = true
): EnterTransition {
    if (!animationsEnabled) return EnterTransition.None
    val speed = LocalAnimationSpeed.current
    val scale = scaleIn(
        animationSpec = RethroneSprings.spatial(speed),
        initialScale = 0.92f,
        transformOrigin = TransformOrigin(0.5f, if (fromBottom) 1f else 0.5f)
    )
    val fade = fadeIn(animationSpec = RethroneSprings.effects(speed))
    return if (fromBottom) {
        scale + fade + slideInVertically(
            animationSpec = RethroneSprings.spatial<IntOffset>(speed),
            initialOffsetY = { it / 6 }
        )
    } else {
        scale + fade
    }
}

/**
 * Passender Expressive-Ausgang: schrumpfen + ausblenden (+ optional nach unten).
 */
@Composable
fun rememberMenuExit(
    animationsEnabled: Boolean,
    toBottom: Boolean = true
): ExitTransition {
    if (!animationsEnabled) return ExitTransition.None
    val speed = LocalAnimationSpeed.current
    val scale = scaleOut(
        animationSpec = RethroneSprings.effects(speed),
        targetScale = 0.92f,
        transformOrigin = TransformOrigin(0.5f, if (toBottom) 1f else 0.5f)
    )
    val fade = fadeOut(animationSpec = RethroneSprings.effects(speed))
    return if (toBottom) {
        scale + fade + slideOutVertically(
            animationSpec = RethroneSprings.effects<IntOffset>(speed),
            targetOffsetY = { it / 6 }
        )
    } else {
        scale + fade
    }
}
