package com.example.androidlauncher.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.R
import com.example.androidlauncher.data.AppFont
import com.example.androidlauncher.ui.theme.LocalAnimationsEnabled
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle
import com.example.androidlauncher.ui.theme.RethroneSprings
import com.example.androidlauncher.ui.LiquidGlass.designSurface
import kotlinx.coroutines.delay

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
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val designStyle = LocalDesignStyle.current
    val surfaceAccent = LocalColorTheme.current.menuSurfaceColor(isDarkTextEnabled)
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
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = mainTextColor)
            }
            Text(
                text = stringResource(R.string.choose_font),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = mainTextColor,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        val searchBarModifier = Modifier.designSurface(
            designStyle, RoundedCornerShape(16.dp), isDarkTextEnabled, surfaceAccent, fillAlpha = 0.1f
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(searchBarModifier)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clickable { focusRequester.requestFocus() }
        ) {
            StableSearchFieldContent(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = stringResource(R.string.font_search_hint),
                textStyle = androidx.compose.ui.text.TextStyle(color = mainTextColor, fontSize = 16.sp),
                textColor = mainTextColor,
                placeholderColor = grayTone,
                focusRequester = focusRequester,
                leadingIconTint = grayTone,
                leadingIconSize = 18.dp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            itemsIndexed(items = filteredFonts, key = { _, font -> font.name }) { index, font ->
                val isSelected = font == currentAppFont
                
                val isSearching = searchQuery.isNotBlank()
                var isVisible by remember(font.name, isSearching) { mutableStateOf(!isSearching) }
                
                LaunchedEffect(font.name, isSearching) {
                    if (isSearching) {
                        delay((index % 15) * 25L)
                        isVisible = true
                    }
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(400)) + 
                            scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)) +
                            slideInVertically(initialOffsetY = { 15 }, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    // Material-3-Expressive: weicher Auswahl-Übergang statt hartem Umspringen.
                    val animationsEnabled = LocalAnimationsEnabled.current
                    val sel by animateFloatAsState(
                        if (isSelected) 1f else 0f,
                        if (animationsEnabled) RethroneSprings.effects<Float>() else snap<Float>(),
                        label = "fontSel"
                    )
                    val itemModifier = Modifier.designSurface(
                        designStyle, RoundedCornerShape(16.dp), isDarkTextEnabled, surfaceAccent,
                        fillAlpha = 0.15f * sel, glassStartAlpha = 0.25f * sel, glassEndAlpha = 0.1f * sel
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(itemModifier)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onAppFontSelected(font) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
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
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = if (animationsEnabled) scaleIn(RethroneSprings.spatial(), initialScale = 0.6f) + fadeIn() else EnterTransition.None,
                                exit = if (animationsEnabled) scaleOut(RethroneSprings.effects(), targetScale = 0.6f) + fadeOut() else ExitTransition.None
                            ) {
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
}
