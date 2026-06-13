package com.example.androidlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.data.DesignStyle
import com.example.androidlauncher.ui.LiquidGlass.designSurface
import com.example.androidlauncher.ui.theme.ColorTheme
import com.example.androidlauncher.ui.theme.LocalFontWeight

/**
 * Untermenü zur Auswahl des [DesignStyle]. Zeigt für jeden Stil eine Live-Vorschau-Kachel
 * in einem 2-Spalten-Grid; die aktive Auswahl erhält ein Häkchen. Tippen wählt den Stil
 * und schließt das Menü.
 */
@Composable
fun DesignStyleMenu(
    currentStyle: DesignStyle,
    selectedTheme: ColorTheme,
    isDarkTextEnabled: Boolean,
    onStyleSelected: (DesignStyle) -> Unit,
    onClose: () -> Unit
) {
    val fontWeight = LocalFontWeight.current
    val mainTextColor = LiquidGlass.mainTextColor(isDarkTextEnabled)
    val secondaryTextColor = LiquidGlass.secondaryTextColor(isDarkTextEnabled)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("design_style_menu")
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Design", fontSize = 24.sp, fontWeight = fontWeight.weight, color = mainTextColor)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = mainTextColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(DesignStyle.entries, key = { it.name }) { style ->
                DesignStyleTile(
                    style = style,
                    isSelected = style == currentStyle,
                    selectedTheme = selectedTheme,
                    isDarkTextEnabled = isDarkTextEnabled,
                    mainTextColor = mainTextColor,
                    secondaryTextColor = secondaryTextColor,
                    onClick = { onStyleSelected(style) }
                )
            }
        }
    }
}

@Composable
private fun DesignStyleTile(
    style: DesignStyle,
    isSelected: Boolean,
    selectedTheme: ColorTheme,
    isDarkTextEnabled: Boolean,
    mainTextColor: Color,
    secondaryTextColor: Color,
    onClick: () -> Unit
) {
    val accent = selectedTheme.menuSurfaceColor(isDarkTextEnabled)
    val previewBrush = selectedTheme.menuBrush(isDarkTextEnabled, alpha = 0.96f)
    val outerShape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(outerShape)
            .clickable { onClick() }
            .background(mainTextColor.copy(alpha = if (isSelected) 0.12f else 0.05f), outerShape)
            .testTag("design_tile_${style.name}")
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Live-Vorschau: die Stil-Oberfläche über einem Theme-Hintergrund.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(previewBrush, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 96.dp, height = 44.dp)
                    .designSurface(style, RoundedCornerShape(12.dp), isDarkTextEnabled, accent),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = mainTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            style.displayName,
            color = mainTextColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            style.description,
            color = secondaryTextColor,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
