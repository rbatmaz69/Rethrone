package com.example.androidlauncher.ui

import android.content.Intent
import android.app.SearchManager
import android.net.Uri
import androidx.core.net.toUri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import com.composables.icons.lucide.History
import com.composables.icons.lucide.Lucide
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.LauncherLogic
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.AppUsageStats
import com.example.androidlauncher.data.SearchHistoryEntry
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalFontSize
import com.example.androidlauncher.ui.theme.LocalLiquidGlassEnabled
import kotlin.math.min

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BottomSearch(
    apps: List<AppInfo>,
    onClose: () -> Unit,
    onAppLaunch: (AppInfo, Rect?) -> Unit,
    onWebLaunch: (Intent, Rect?, String) -> Unit,
    preferredImeWebLaunchBounds: Rect? = null,
    webHistory: List<SearchHistoryEntry> = emptyList(),
    appUsageStats: Map<String, AppUsageStats> = emptyMap(),
    smartSuggestionsEnabled: Boolean = true,
    onRemoveHistorySuggestion: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val colorTheme = LocalColorTheme.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val isLiquidGlassEnabled = LocalLiquidGlassEnabled.current
    val fontSize = LocalFontSize.current
    val density = LocalDensity.current

    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val searchSurfaceBrush = remember(colorTheme, isDarkTextEnabled) {
        colorTheme.searchBrush(isDarkTextEnabled, alpha = if (isDarkTextEnabled) 0.97f else 0.94f)
    }
    val themeBorderColor = remember(colorTheme, isDarkTextEnabled) {
        colorTheme.borderColor(isDarkTextEnabled)
    }

    val searchContainerModifier = if (isLiquidGlassEnabled) {
        Modifier
            .background(searchSurfaceBrush, RoundedCornerShape(28.dp))
            .border(BorderStroke(1.2.dp, LiquidGlass.borderBrush(isDarkTextEnabled)), RoundedCornerShape(28.dp))
    } else {
        Modifier
            .background(searchSurfaceBrush, RoundedCornerShape(28.dp))
            .border(BorderStroke(1.dp, themeBorderColor), RoundedCornerShape(28.dp))
    }
    val rowBackgroundColor = remember(mainTextColor, isDarkTextEnabled) {
        if (isDarkTextEnabled) Color.Black.copy(alpha = 0.035f) else Color.White.copy(alpha = 0.055f)
    }
    val primaryWebRowColor = remember(mainTextColor, isDarkTextEnabled) {
        if (isDarkTextEnabled) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.09f)
    }

    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var hasRequestedInitialFocus by remember { mutableStateOf(false) }
    var searchBarBounds by remember { mutableStateOf<Rect?>(null) }
    var searchBarIconBounds by remember { mutableStateOf<Rect?>(null) }
    var searchBarHeightPx by remember { mutableFloatStateOf(0f) }
    var historyWebContainerHeightPx by remember { mutableFloatStateOf(0f) }
    val searchBarSpacing = 10.dp
    val keyboardLaunchSizePx = with(density) { 40.dp.toPx() }
    val searchBarHorizontalPaddingPx = with(density) { 20.dp.toPx() }
    val searchBarIconSizePx = with(density) { 20.dp.toPx() }

    val appSuggestions = remember(query, apps, appUsageStats, smartSuggestionsEnabled) {
        if (query.isBlank()) {
            emptyList()
        } else if (smartSuggestionsEnabled) {
            LauncherLogic.rankAppSuggestions(apps, query, appUsageStats, limit = 3)
        } else {
            LauncherLogic.filterAppsByRelevance(apps, query).take(3)
        }
    }
    val historySuggestion = remember(query, webHistory, smartSuggestionsEnabled) {
        if (query.isBlank() || !smartSuggestionsEnabled) {
            null
        } else {
            LauncherLogic.rankWebSuggestions(webHistory, query, limit = 1).firstOrNull()
        }
    }
    val webSuggestionQuery = remember(query, historySuggestion) {
        query.trim().takeIf { it.isNotEmpty() && historySuggestion == null }
    }
    val hasHistoryWebSuggestion = historySuggestion != null || webSuggestionQuery != null
    val searchBarHeight = with(density) { searchBarHeightPx.toDp() }
    val historyWebContainerHeight = with(density) { historyWebContainerHeightPx.toDp() }
    val historyWebBottomPadding = searchBarHeight + searchBarSpacing
    val appSuggestionsBottomPadding = historyWebBottomPadding + if (hasHistoryWebSuggestion) {
        historyWebContainerHeight + searchBarSpacing
    } else {
        0.dp
    }

    val suggestionEnterTransition = remember {
        fadeIn(animationSpec = tween(durationMillis = 220)) +
            expandVertically(
                expandFrom = Alignment.Bottom,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    }
    val suggestionExitTransition = remember {
        fadeOut(animationSpec = tween(durationMillis = 180)) +
            shrinkVertically(
                shrinkTowards = Alignment.Bottom,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    }
    val singleSuggestionEnterTransition = remember {
        fadeIn(animationSpec = tween(durationMillis = 180)) +
            expandVertically(
                expandFrom = Alignment.Bottom,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    }
    val singleSuggestionExitTransition = remember {
        fadeOut(animationSpec = tween(durationMillis = 160)) +
            shrinkVertically(
                shrinkTowards = Alignment.Bottom,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    }

    BackHandler {
        keyboardController?.hide()
        onClose()
    }

    val overlayColor = colorTheme.overlayScrimColor(isDarkTextEnabled)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(overlayColor)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                keyboardController?.hide()
                onClose()
            }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .padding(16.dp)
        ) {
            AnimatedVisibility(
                visible = query.isNotEmpty() && appSuggestions.isNotEmpty(),
                enter = suggestionEnterTransition,
                exit = suggestionExitTransition,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = appSuggestionsBottomPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(searchContainerModifier)
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    appSuggestions.forEach { app ->
                        AppSearchItem(
                            app = app,
                            query = query,
                            rowBackgroundColor = rowBackgroundColor,
                            mainTextColor = mainTextColor,
                            fontSizeScale = fontSize.scale,
                            onClick = { bounds -> onAppLaunch(app, bounds) }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = query.isNotEmpty() && hasHistoryWebSuggestion,
                enter = singleSuggestionEnterTransition,
                exit = singleSuggestionExitTransition,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = historyWebBottomPadding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(searchContainerModifier)
                        .onGloballyPositioned { historyWebContainerHeightPx = it.size.height.toFloat() }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    when {
                        historySuggestion != null -> {
                            SearchHistoryItem(
                                entry = historySuggestion,
                                query = query,
                                rowBackgroundColor = rowBackgroundColor,
                                mainTextColor = mainTextColor,
                                isLiquidGlass = isLiquidGlassEnabled,
                                isDarkText = isDarkTextEnabled,
                                onClick = { bounds ->
                                    buildWebSearchIntent(context, historySuggestion.query)?.let { intent ->
                                        onWebLaunch(intent, bounds, historySuggestion.query)
                                    }
                                },
                                onRemove = { onRemoveHistorySuggestion(historySuggestion.query) }
                            )
                        }
                        webSuggestionQuery != null -> {
                            WebSearchItem(
                                query = webSuggestionQuery,
                                rowBackgroundColor = primaryWebRowColor,
                                mainTextColor = mainTextColor,
                                isLiquidGlass = isLiquidGlassEnabled,
                                isDarkText = isDarkTextEnabled,
                                onClick = { bounds ->
                                    buildWebSearchIntent(context, webSuggestionQuery)?.let { intent ->
                                        onWebLaunch(intent, bounds, webSuggestionQuery)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .then(searchContainerModifier)
                    .onGloballyPositioned {
                        searchBarBounds = it.boundsInRoot()
                        searchBarHeightPx = it.size.height.toFloat()
                    }
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = mainTextColor.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(20.dp)
                        .onGloballyPositioned { searchBarIconBounds = it.boundsInRoot() }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Suchen...",
                            color = mainTextColor.copy(alpha = 0.38f),
                            fontSize = 17.sp
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag("bottom_search_field")
                            .onGloballyPositioned {
                                if (!hasRequestedInitialFocus) {
                                    hasRequestedInitialFocus = true
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                            }
                            .onPreInterceptKeyBeforeSoftKeyboard { event ->
                                if (event.key == Key.Back && event.type == KeyEventType.KeyDown) {
                                    keyboardController?.hide()
                                    onClose()
                                    true
                                } else false
                            },
                        textStyle = LocalTextStyle.current.copy(color = mainTextColor, fontSize = 18.sp),
                        singleLine = true,
                        cursorBrush = SolidColor(mainTextColor),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (query.isNotEmpty()) {
                                    val trimmedQuery = query.trim()
                                    buildWebSearchIntent(context, trimmedQuery)?.let { intent ->
                                        val keyboardLaunchBounds = preferredImeWebLaunchBounds
                                            ?: searchBarIconBounds
                                            ?: createCompactLaunchBounds(
                                                containerBounds = searchBarBounds,
                                                sizePx = keyboardLaunchSizePx,
                                                horizontalInsetPx = searchBarHorizontalPaddingPx,
                                                anchorSizePx = searchBarIconSizePx
                                            )
                                        onWebLaunch(intent, keyboardLaunchBounds, trimmedQuery)
                                    }
                                }
                            }
                        )
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { query = "" },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = mainTextColor.copy(alpha = 0.42f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun createCompactLaunchBounds(
    containerBounds: Rect?,
    sizePx: Float,
    horizontalInsetPx: Float,
    anchorSizePx: Float
): Rect? {
    val bounds = containerBounds ?: return null
    val launchWidth = min(sizePx, bounds.width)
    val launchHeight = min(sizePx, bounds.height)
    val anchorCenterX = bounds.left + horizontalInsetPx + (anchorSizePx / 2f)
    val left = (anchorCenterX - launchWidth / 2f)
        .coerceIn(bounds.left, bounds.right - launchWidth)
    val top = (bounds.center.y - launchHeight / 2f)
        .coerceIn(bounds.top, bounds.bottom - launchHeight)
    return Rect(
        left = left,
        top = top,
        right = left + launchWidth,
        bottom = top + launchHeight
    )
}

private fun buildWebSearchIntent(context: android.content.Context, query: String): Intent? {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isEmpty()) return null

    val finalUrl = if (trimmedQuery.startsWith("http") || trimmedQuery.contains(".")) {
        if (!trimmedQuery.startsWith("http")) "https://$trimmedQuery" else trimmedQuery
    } else {
        "https://www.google.com/search?q=${Uri.encode(trimmedQuery)}"
    }

    val browserIntent = Intent(Intent.ACTION_VIEW, finalUrl.toUri()).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (browserIntent.resolveActivity(context.packageManager) != null) {
        return browserIntent
    }

    val webSearchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
        putExtra(SearchManager.QUERY, trimmedQuery)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return webSearchIntent.takeIf { it.resolveActivity(context.packageManager) != null }
}

@Composable
fun AppSearchItem(
    app: AppInfo,
    query: String,
    rowBackgroundColor: Color,
    mainTextColor: Color,
    fontSizeScale: Float,
    onClick: (Rect?) -> Unit
) {
    val density = LocalDensity.current
    var iconBounds by remember(app.packageName) { mutableStateOf<Rect?>(null) }
    var rowBounds by remember(app.packageName) { mutableStateOf<Rect?>(null) }
    val minLaunchSizePx = with(density) { 28.dp.toPx() }
    val preferredLaunchSizePx = with(density) { 40.dp.toPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(rowBackgroundColor)
            .onGloballyPositioned { rowBounds = it.boundsInRoot() }
            .pointerInput(app.packageName, rowBounds, iconBounds) {
                detectTapGestures { tapOffset ->
                    val currentRowBounds = rowBounds
                    val currentIconBounds = iconBounds
                    val launchBounds = if (currentRowBounds != null) {
                        val absoluteTap = Offset(
                            x = currentRowBounds.left + tapOffset.x,
                            y = currentRowBounds.top + tapOffset.y
                        )
                        when {
                            currentIconBounds?.contains(absoluteTap) == true -> currentIconBounds
                            else -> {
                                val launchWidth = min(
                                    preferredLaunchSizePx.coerceAtLeast(minLaunchSizePx),
                                    currentRowBounds.width
                                )
                                val launchHeight = min(
                                    preferredLaunchSizePx.coerceAtLeast(minLaunchSizePx),
                                    currentRowBounds.height
                                )
                                val left = (absoluteTap.x - launchWidth / 2f)
                                    .coerceIn(currentRowBounds.left, currentRowBounds.right - launchWidth)
                                val top = (absoluteTap.y - launchHeight / 2f)
                                    .coerceIn(currentRowBounds.top, currentRowBounds.bottom - launchHeight)
                                Rect(
                                    left = left,
                                    top = top,
                                    right = left + launchWidth,
                                    bottom = top + launchHeight
                                )
                            }
                        }
                    } else {
                        currentIconBounds
                    }
                    onClick(launchBounds)
                }
            }
            .padding(vertical = 11.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .onGloballyPositioned { iconBounds = it.boundsInRoot() }
        ) { AppIconView(app = app) }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "App",
                color = mainTextColor.copy(alpha = 0.46f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            HighlightedSuggestionText(
                text = app.label,
                query = query,
                color = mainTextColor,
                fontSize = 16.sp * fontSizeScale
            )
        }
    }
}

@Composable
private fun SearchHistoryItem(
    entry: SearchHistoryEntry,
    query: String,
    rowBackgroundColor: Color,
    mainTextColor: Color,
    isLiquidGlass: Boolean,
    isDarkText: Boolean,
    onClick: (Rect?) -> Unit,
    onRemove: () -> Unit
) {
    val backgroundColor = if (isLiquidGlass) {
        rowBackgroundColor
    } else {
        if (isDarkText) rowBackgroundColor.copy(alpha = 0.92f) else rowBackgroundColor
    }
    var iconBounds by remember(entry.query) { mutableStateOf<Rect?>(null) }
    var rowBounds by remember(entry.query) { mutableStateOf<Rect?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .onGloballyPositioned { rowBounds = it.boundsInRoot() }
            .clickable { onClick(iconBounds ?: rowBounds) }
            .padding(vertical = 11.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .onGloballyPositioned { iconBounds = it.boundsInRoot() }
                .background(mainTextColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.History,
                contentDescription = null,
                tint = mainTextColor.copy(alpha = 0.78f),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Verlauf",
                color = mainTextColor.copy(alpha = 0.46f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            HighlightedSuggestionText(
                text = entry.query,
                query = query,
                color = mainTextColor,
                fontSize = 15.sp
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .size(30.dp)
                .testTag("history_remove_${entry.query.hashCode()}")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Verlaufseintrag entfernen",
                tint = mainTextColor.copy(alpha = 0.42f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun WebSearchItem(
    query: String,
    rowBackgroundColor: Color,
    mainTextColor: Color,
    isLiquidGlass: Boolean,
    isDarkText: Boolean,
    onClick: (Rect?) -> Unit
) {
    val density = LocalDensity.current
    val backgroundColor = if (isLiquidGlass) {
        rowBackgroundColor
    } else {
        if (isDarkText) rowBackgroundColor.copy(alpha = 0.92f) else rowBackgroundColor
    }
    var iconBounds by remember(query) { mutableStateOf<Rect?>(null) }
    var rowBounds by remember(query) { mutableStateOf<Rect?>(null) }
    val minLaunchSizePx = with(density) { 28.dp.toPx() }
    val preferredLaunchSizePx = with(density) { 40.dp.toPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .onGloballyPositioned { rowBounds = it.boundsInRoot() }
            .pointerInput(query, rowBounds, iconBounds) {
                detectTapGestures { tapOffset ->
                    val currentRowBounds = rowBounds
                    val currentIconBounds = iconBounds
                    val launchBounds = if (currentRowBounds != null) {
                        val absoluteTap = Offset(
                            x = currentRowBounds.left + tapOffset.x,
                            y = currentRowBounds.top + tapOffset.y
                        )
                        when {
                            currentIconBounds?.contains(absoluteTap) == true -> currentIconBounds
                            else -> {
                                val launchWidth = min(
                                    preferredLaunchSizePx.coerceAtLeast(minLaunchSizePx),
                                    currentRowBounds.width
                                )
                                val launchHeight = min(
                                    preferredLaunchSizePx.coerceAtLeast(minLaunchSizePx),
                                    currentRowBounds.height
                                )
                                val left = (absoluteTap.x - launchWidth / 2f)
                                    .coerceIn(currentRowBounds.left, currentRowBounds.right - launchWidth)
                                val top = (absoluteTap.y - launchHeight / 2f)
                                    .coerceIn(currentRowBounds.top, currentRowBounds.bottom - launchHeight)
                                Rect(
                                    left = left,
                                    top = top,
                                    right = left + launchWidth,
                                    bottom = top + launchHeight
                                )
                            }
                        }
                    } else {
                        currentIconBounds
                    }
                    onClick(launchBounds)
                }
            }
            .padding(vertical = 11.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .onGloballyPositioned { iconBounds = it.boundsInRoot() }
                .background(mainTextColor.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = mainTextColor.copy(alpha = 0.9f),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Jetzt im Web suchen",
                color = mainTextColor.copy(alpha = 0.52f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            HighlightedSuggestionText(
                text = query,
                query = query,
                color = mainTextColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                highlightWeight = FontWeight.Bold
            )
        }
        Text(
            text = "ENTER",
            color = mainTextColor.copy(alpha = 0.38f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontStyle = FontStyle.Italic
        )
    }
}

@Composable
private fun HighlightedSuggestionText(
    text: String,
    query: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    highlightWeight: FontWeight = FontWeight.SemiBold
) {
    val annotatedText = remember(text, query, fontWeight, highlightWeight) {
        buildHighlightedText(text = text, query = query, baseWeight = fontWeight, highlightWeight = highlightWeight)
    }
    Text(
        text = annotatedText,
        color = color,
        fontSize = fontSize,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private fun buildHighlightedText(
    text: String,
    query: String,
    baseWeight: FontWeight,
    highlightWeight: FontWeight
) = buildAnnotatedString {
    append(text)
    addStyle(SpanStyle(fontWeight = baseWeight), 0, text.length)

    val trimmedQuery = query.trim()
    if (trimmedQuery.isEmpty()) return@buildAnnotatedString

    val matchStart = text.indexOf(trimmedQuery, ignoreCase = true)
    if (matchStart < 0) return@buildAnnotatedString

    addStyle(
        SpanStyle(fontWeight = highlightWeight),
        start = matchStart,
        end = (matchStart + trimmedQuery.length).coerceAtMost(text.length)
    )
}
