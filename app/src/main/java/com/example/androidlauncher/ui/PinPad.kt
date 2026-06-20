package com.example.androidlauncher.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.R
import com.example.androidlauncher.data.DesignStyle
import com.example.androidlauncher.ui.LiquidGlass.designSurface

/**
 * Ziffernblock zur PIN-Eingabe. Zeigt die Anzahl eingegebener Stellen als Punkte und
 * darunter ein 3×4-Tastenfeld (1–9, 0 und Löschen). Die [value]-Verwaltung liegt beim Aufrufer.
 */
@Composable
fun PinPad(
    value: String,
    onValueChange: (String) -> Unit,
    maxLength: Int,
    textColor: Color,
    accentColor: Color,
    designStyle: DesignStyle,
    surfaceAccent: Color,
    isDarkTextEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dynamische Punkte-Anzeige: pro eingetippter Ziffer erscheint genau ein Punkt
        // (keine statischen Platzhalter). Feste Höhe + animateContentSize halten das Layout ruhig.
        Row(
            modifier = Modifier
                .height(12.dp)
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dots = value.length.coerceAtMost(maxLength)
            repeat(dots) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .designSurface(
                            designStyle, CircleShape, isDarkTextEnabled, accentColor,
                            fillAlpha = 0.9f, glassStartAlpha = 0.9f, glassEndAlpha = 0.9f,
                            borderWidth = 1.dp, borderStartAlpha = 0.25f, borderEndAlpha = 0.1f
                        )
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "<")
        )
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                for (key in row) {
                    when (key) {
                        "" -> Spacer(Modifier.size(72.dp))
                        "<" -> PinKey(
                            modifier = Modifier.size(72.dp),
                            textColor = textColor,
                            designStyle = designStyle,
                            surfaceAccent = surfaceAccent,
                            isDarkTextEnabled = isDarkTextEnabled,
                            onClick = { if (value.isNotEmpty()) onValueChange(value.dropLast(1)) }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Backspace,
                                contentDescription = stringResource(R.string.cd_delete),
                                tint = textColor
                            )
                        }
                        else -> PinKey(
                            modifier = Modifier.size(72.dp),
                            textColor = textColor,
                            designStyle = designStyle,
                            surfaceAccent = surfaceAccent,
                            isDarkTextEnabled = isDarkTextEnabled,
                            onClick = { if (value.length < maxLength) onValueChange(value + key) }
                        ) {
                            Text(key, color = textColor, fontSize = 26.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKey(
    modifier: Modifier,
    textColor: Color,
    designStyle: DesignStyle,
    surfaceAccent: Color,
    isDarkTextEnabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .designSurface(
                designStyle, CircleShape, isDarkTextEnabled, surfaceAccent,
                fillAlpha = 0.06f, glassStartAlpha = 0.1f, glassEndAlpha = 0.03f,
                borderWidth = 1.dp, borderStartAlpha = 0.18f, borderEndAlpha = 0.05f
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
