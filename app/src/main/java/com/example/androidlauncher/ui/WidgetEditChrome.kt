package com.example.androidlauncher.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.R
import com.example.androidlauncher.ui.LiquidGlass.designSurface
import com.example.androidlauncher.ui.theme.LocalAnimationSpeed
import com.example.androidlauncher.ui.theme.LocalAnimationsEnabled
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle
import com.example.androidlauncher.ui.theme.LocalHomeTextColor
import com.example.androidlauncher.ui.theme.RethroneSprings

/**
 * Gemeinsame Edit-Chrome der gehosteten Widgets (U3): Eck-Handles mit
 * A11y-tauglicher Treffflaeche, Live-Groessen-Chip und Vormerkungs-Overlay
 * fuers Entfernen. Ersetzt die zwei handgebauten 28.dp-Kreise aus B1-PR4.
 */

/**
 * Eck-Handle der Widget-Edit-Chrome: [touchTarget] grosse Treffbox (48.dp =
 * A11y-Minimum) um einen kleineren sichtbaren Kreis. Gesten/Klicks/testTag
 * haengt der Aufrufer per [modifier] an – die landen auf der Treffbox, sodass
 * die volle Flaeche reagiert, waehrend die Optik kompakt bleibt.
 */
@Composable
internal fun WidgetEditHandle(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    touchTarget: Dp = 48.dp,
    visualSize: Dp = 32.dp,
    iconSize: Dp = 16.dp,
) {
    val designStyle = LocalDesignStyle.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val surfaceAccent = LocalColorTheme.current.menuSurfaceColor(isDarkTextEnabled)

    Box(
        modifier = modifier.size(touchTarget),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(visualSize)
                .clip(CircleShape)
                .designSurface(designStyle, CircleShape, isDarkTextEnabled, surfaceAccent, fillAlpha = 0.5f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = LocalHomeTextColor.current,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Live-Groessen-Chip waehrend des Resize-Drags: „B × H dp". Am Min/Max-Anschlag
 * ([clamped]) pulsiert die Pille kurz und der Text springt auf die Akzentfarbe –
 * ohne Animationen nur der (instant geschaltete) Farbwechsel.
 */
@Composable
internal fun WidgetSizeChip(
    widthDp: Int,
    heightDp: Int,
    clamped: Boolean,
    modifier: Modifier = Modifier,
) {
    val designStyle = LocalDesignStyle.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val colorTheme = LocalColorTheme.current
    val surfaceAccent = colorTheme.menuSurfaceColor(isDarkTextEnabled)
    val animationsEnabled = LocalAnimationsEnabled.current
    val speed = LocalAnimationSpeed.current

    val scale by animateFloatAsState(
        targetValue = if (clamped && animationsEnabled) 1.08f else 1f,
        animationSpec = if (animationsEnabled) RethroneSprings.spatial(speed) else snap(),
        label = "widgetSizeChipScale"
    )
    val textColor by animateColorAsState(
        targetValue = if (clamped) colorTheme.accentColor(isDarkTextEnabled) else LocalHomeTextColor.current,
        animationSpec = if (animationsEnabled) RethroneSprings.effects(speed) else snap(),
        label = "widgetSizeChipColor"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(50))
            .designSurface(designStyle, RoundedCornerShape(50), isDarkTextEnabled, surfaceAccent, fillAlpha = 0.6f)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = stringResource(R.string.widget_size_label, widthDp, heightDp),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Geister-Overlay eines zum Entfernen vorgemerkten Widgets: Scrim ueber der
 * (ausgeblendeten) Widget-Flaeche, Hinweis-Label und eine Rueckgaengig-Pille.
 * Der Aufrufer liefert per [modifier] `matchParentSize` + zIndex.
 */
@Composable
internal fun WidgetPendingRemovalOverlay(
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    undoTestTag: String? = null,
) {
    val designStyle = LocalDesignStyle.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val surfaceAccent = LocalColorTheme.current.menuSurfaceColor(isDarkTextEnabled)
    val textColor = LocalHomeTextColor.current
    val overlayShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .clip(overlayShape)
            .designSurface(designStyle, overlayShape, isDarkTextEnabled, surfaceAccent, fillAlpha = 0.35f),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.widget_pending_removal),
                color = textColor.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
            Row(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .designSurface(
                        designStyle,
                        RoundedCornerShape(50),
                        isDarkTextEnabled,
                        surfaceAccent,
                        fillAlpha = 0.6f
                    )
                    .then(if (undoTestTag != null) Modifier.testTag(undoTestTag) else Modifier)
                    .clickable(onClick = onUndo)
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Undo,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.undo_remove_widget),
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}
