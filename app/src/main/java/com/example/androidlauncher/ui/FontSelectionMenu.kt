package com.example.androidlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.data.AppFont
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalLiquidGlassEnabled

/**
 * A dedicated menu for selecting a font from a larger list.
 * Includes a search bar to filter fonts.
 */
@Composable
fun FontSelectionMenu(
    currentAppFont: AppFont,
    onAppFontSelected: (AppFont) -> Unit,
    onBack: () -> Unit
) {
    val colorTheme = LocalColorTheme.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val isLiquidGlassEnabled = LocalLiquidGlassEnabled.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val grayTone = if (isDarkTextEnabled) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f)
    
    var searchQuery by remember { mutableStateOf("") }
    val filteredFonts = remember(searchQuery) {
        AppFont.entries.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = mainTextColor)
            }
            Text(
                text = "Schriftart auswählen",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = mainTextColor,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Search Bar
        val searchBarModifier = if (isLiquidGlassEnabled) {
            val glassBrush = if (isDarkTextEnabled) {
                Brush.linearGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.05f))
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))
                )
            }
            val borderBrush = if (isDarkTextEnabled) {
                Brush.linearGradient(colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.3f)))
            } else {
                Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f)))
            }
            Modifier
                .background(glassBrush, RoundedCornerShape(12.dp))
                .border(BorderStroke(1.2.dp, borderBrush), RoundedCornerShape(12.dp))
        } else {
            Modifier.background(mainTextColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(searchBarModifier)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clickable { focusRequester.requestFocus() }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = grayTone, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    textStyle = androidx.compose.ui.text.TextStyle(color = mainTextColor, fontSize = 16.sp),
                    cursorBrush = SolidColor(mainTextColor),
                    singleLine = true,
                    decorationBox = { 
                        if (searchQuery.isEmpty()) Text("Schriftart suchen...", color = grayTone, fontSize = 16.sp)
                        it()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(filteredFonts) { font ->
                val isSelected = font == currentAppFont
                
                val itemModifier = if (isSelected) {
                    if (isLiquidGlassEnabled) {
                        val glassBrush = if (isDarkTextEnabled) {
                            Brush.linearGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.1f))
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.15f))
                            )
                        }
                        val borderBrush = if (isDarkTextEnabled) {
                            Brush.linearGradient(colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.4f)))
                        } else {
                            Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.2f)))
                        }
                        Modifier
                            .background(glassBrush, RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.2.dp, borderBrush), RoundedCornerShape(12.dp))
                    } else {
                        Modifier.background(mainTextColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    }
                } else {
                    Modifier
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(itemModifier)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAppFontSelected(font) }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = font.label,
                            color = mainTextColor,
                            fontSize = 18.sp,
                            fontFamily = font.fontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            RadioButton(
                                selected = true,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = mainTextColor)
                            )
                        }
                    }
                }
            }
        }
    }
}
