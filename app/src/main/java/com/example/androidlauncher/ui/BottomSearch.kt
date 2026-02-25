package com.example.androidlauncher.ui

import android.content.Intent
import android.app.SearchManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.LauncherLogic
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalFontSize

@Composable
fun BottomSearch(
    apps: List<AppInfo>,
    onClose: () -> Unit,
    onAppLaunch: (AppInfo) -> Unit
) {
    val context = LocalContext.current
    val colorTheme = LocalColorTheme.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val fontSize = LocalFontSize.current

    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    // Slightly transparent background for the search sheet
    val backgroundColor = if (isDarkTextEnabled) {
        Color.White.copy(alpha = 0.95f)
    } else {
        Color(0xFF121212).copy(alpha = 0.95f)
    }

    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Filter apps based on query
    val filteredApps = remember(query, apps) {
        LauncherLogic.filterAppsByRelevance(apps, query)
    }

    // Handle back press
    BackHandler {
        onClose()
    }

    // Auto-focus the search field
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)) // Dim background
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                 onClose()
            }
    ) {
        // Content container that aligns to bottom and handles IME padding
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime) // Move up with keyboard
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { /* Consume clicks */ }
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Results List (only if query is not empty)
            if (query.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp), // Limit height
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = true // Show best matches at the bottom, closer to the thumb
                ) {
                    // Web Search Option (Always visible when querying)
                    item {
                         WebSearchItem(
                             query = query,
                             mainTextColor = mainTextColor,
                             onClick = {
                                 val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                                     putExtra(SearchManager.QUERY, query)
                                 }
                                 if (intent.resolveActivity(context.packageManager) != null) {
                                     context.startActivity(intent)
                                     onClose()
                                 }
                             }
                         )
                    }

                    // App Results
                    items(filteredApps) { app ->
                        AppSearchItem(
                            app = app,
                            mainTextColor = mainTextColor,
                            fontSizeScale = fontSize.scale,
                            onClick = { onAppLaunch(app) }
                        )
                    }
                }
            }

            // Search Input Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        color = if (isDarkTextEnabled) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search apps and web...",
                        color = mainTextColor.copy(alpha = 0.5f),
                        fontSize = 16.sp
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = LocalTextStyle.current.copy(
                        color = mainTextColor,
                        fontSize = 18.sp
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(colorTheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (filteredApps.isNotEmpty()) {
                                onAppLaunch(filteredApps.first())
                            } else if (query.isNotEmpty()) {
                                 val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                                     putExtra(SearchManager.QUERY, query)
                                 }
                                 if (intent.resolveActivity(context.packageManager) != null) {
                                     context.startActivity(intent)
                                     onClose()
                                 }
                            }
                        }
                    )
                )
            }
            // Add a little bottom spacer for visual balance
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun AppSearchItem(
    app: AppInfo,
    mainTextColor: Color,
    fontSizeScale: Float,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp)) {
            AppIconView(app = app)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = app.label,
            color = mainTextColor,
            fontSize = 16.sp * fontSizeScale,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun WebSearchItem(
    query: String,
    mainTextColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = mainTextColor.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = "Search Web",
                color = mainTextColor.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            Text(
                text = query,
                color = mainTextColor,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


