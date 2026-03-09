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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
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
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * Schwebende Suchleiste am unteren Bildschirmrand.
 * Optimiert für ein extrem flüssiges, gemeinsames Erscheinen aller Suchergebnisse.
 */
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

    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var isSearchBarVisible by remember { mutableStateOf(false) }
    var hasRequestedInitialFocus by remember { mutableStateOf(false) }
    var searchBarBounds by remember { mutableStateOf<Rect?>(null) }
    var searchBarIconBounds by remember { mutableStateOf<Rect?>(null) }
    val searchBarHiddenLiftPx = with(density) { 34.dp.toPx() }
    val searchBarScale by animateFloatAsState(
        targetValue = if (isSearchBarVisible) 1f else 0.86f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "BottomSearchBarScale"
    )
    val searchBarTranslationY by animateFloatAsState(
        targetValue = if (isSearchBarVisible) 0f else searchBarHiddenLiftPx,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "BottomSearchBarTranslationY"
    )
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
    val webSuggestions = remember(query, webHistory, smartSuggestionsEnabled) {
        if (query.isBlank() || !smartSuggestionsEnabled) {
            emptyList()
        } else {
            LauncherLogic.rankWebSuggestions(webHistory, query, limit = 3)
        }
    }
    val suggestionRows = remember(query, appSuggestions, webSuggestions) {
        if (query.isBlank()) {
            emptyList()
        } else {
            buildList {
                if (appSuggestions.isNotEmpty()) {
                    add(SearchSuggestionRow.SectionHeader("Apps"))
                    appSuggestions.forEach { add(SearchSuggestionRow.AppItem(it)) }
                }
                add(SearchSuggestionRow.SectionHeader(if (webSuggestions.isNotEmpty()) "Web & Verlauf" else "Web"))
                webSuggestions.forEach { add(SearchSuggestionRow.HistoryItem(it)) }
                add(SearchSuggestionRow.WebAction(query.trim()))
            }
        }
    }

    // Koordinierte Animation für den Container-Inhalt
    // Der Stagger wird nur beim ERSTEN Erscheinen der Ergebnisse getriggert
    var isInitialAppearance by remember { mutableStateOf(true) }
    var visibleItemCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(query.isEmpty()) {
        if (query.isEmpty()) {
            visibleItemCount = 0
            isInitialAppearance = true
        } else if (isInitialAppearance) {
            // Nur beim ersten Mal (von leer zu Text) animieren wir die Items nacheinander
            val targetCount = suggestionRows.size
            for (i in 1..targetCount) {
                visibleItemCount = i
                delay(28)
            }
            isInitialAppearance = false
        }
    }

    // Stellt sicher, dass visibleItemCount aktuell bleibt, wenn sich die Liste beim Tippen vergrößert
    LaunchedEffect(suggestionRows.size) {
        if (query.isNotEmpty() && !isInitialAppearance) {
            visibleItemCount = suggestionRows.size
        }
    }

    BackHandler {
        keyboardController?.hide()
        onClose()
    }
    LaunchedEffect(Unit) {
        isSearchBarVisible = true
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Suchergebnis-Block
                AnimatedVisibility(
                    visible = query.isNotEmpty(),
                    enter = fadeIn(tween(250)) + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                    exit = fadeOut(tween(150)) + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(searchContainerModifier)
                            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        suggestionRows.forEachIndexed { index, row ->
                            val isItemVisible = visibleItemCount > index
                            AnimatedVisibility(
                                visible = isItemVisible,
                                enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { 8 }),
                                exit = fadeOut(tween(100))
                            ) {
                                when (row) {
                                    is SearchSuggestionRow.SectionHeader -> SearchSectionHeader(
                                        title = row.title,
                                        mainTextColor = mainTextColor
                                    )
                                    is SearchSuggestionRow.AppItem -> AppSearchItem(
                                        app = row.app,
                                        mainTextColor = mainTextColor,
                                        fontSizeScale = fontSize.scale,
                                        onClick = { bounds -> onAppLaunch(row.app, bounds) }
                                    )
                                    is SearchSuggestionRow.HistoryItem -> SearchHistoryItem(
                                        entry = row.entry,
                                        mainTextColor = mainTextColor,
                                        isLiquidGlass = isLiquidGlassEnabled,
                                        isDarkText = isDarkTextEnabled,
                                        onClick = { bounds ->
                                            buildWebSearchIntent(context, row.entry.query)?.let { intent ->
                                                onWebLaunch(intent, bounds, row.entry.query)
                                            }
                                        },
                                        onRemove = { onRemoveHistorySuggestion(row.entry.query) }
                                    )
                                    is SearchSuggestionRow.WebAction -> WebSearchItem(
                                        query = row.query,
                                        mainTextColor = mainTextColor,
                                        isLiquidGlass = isLiquidGlassEnabled,
                                        isDarkText = isDarkTextEnabled,
                                        onClick = { bounds ->
                                            buildWebSearchIntent(context, row.query)?.let { intent ->
                                                onWebLaunch(intent, bounds, row.query)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Such-Eingabefeld (Bubble)
                AnimatedVisibility(
                    visible = isSearchBarVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 140, delayMillis = 10)) +
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ),
                    exit = fadeOut(animationSpec = tween(120)) +
                        slideOutVertically(
                            targetOffsetY = { it / 3 },
                            animationSpec = tween(durationMillis = 140, easing = EaseInCubic)
                        ) +
                        scaleOut(
                            targetScale = 0.98f,
                            animationSpec = tween(durationMillis = 140, easing = EaseInCubic)
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = searchBarScale
                                scaleY = searchBarScale
                                translationY = searchBarTranslationY
                            }
                            .then(searchContainerModifier)
                            .onGloballyPositioned { searchBarBounds = it.boundsInRoot() }
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
                                Text(text = "Search...", color = mainTextColor.copy(alpha = 0.4f), fontSize = 18.sp)
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .testTag("bottom_search_field")
                                    .onGloballyPositioned {
                                        if (isSearchBarVisible && !hasRequestedInitialFocus) {
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
                            IconButton(onClick = { query = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = mainTextColor.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed interface SearchSuggestionRow {
    data class SectionHeader(val title: String) : SearchSuggestionRow
    data class AppItem(val app: AppInfo) : SearchSuggestionRow
    data class HistoryItem(val entry: SearchHistoryEntry) : SearchSuggestionRow
    data class WebAction(val query: String) : SearchSuggestionRow
}

@Composable
private fun SearchSectionHeader(
    title: String,
    mainTextColor: Color
) {
    Text(
        text = title,
        color = mainTextColor.copy(alpha = 0.55f),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp, start = 4.dp)
    )
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
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .onGloballyPositioned { iconBounds = it.boundsInRoot() }
        ) { AppIconView(app = app) }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = app.label, color = mainTextColor, fontSize = 16.sp * fontSizeScale, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SearchHistoryItem(
    entry: SearchHistoryEntry,
    mainTextColor: Color,
    isLiquidGlass: Boolean,
    isDarkText: Boolean,
    onClick: (Rect?) -> Unit,
    onRemove: () -> Unit
) {
    val backgroundModifier = if (isLiquidGlass) {
        Modifier.background(
            color = if (isDarkText) Color.Black.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(12.dp)
        )
    } else Modifier
    var iconBounds by remember(entry.query) { mutableStateOf<Rect?>(null) }
    var rowBounds by remember(entry.query) { mutableStateOf<Rect?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(backgroundModifier)
            .onGloballyPositioned { rowBounds = it.boundsInRoot() }
            .clickable { onClick(iconBounds ?: rowBounds) }
            .padding(vertical = 10.dp, horizontal = 8.dp),
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
                tint = mainTextColor.copy(alpha = 0.82f),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Zuletzt gesucht",
                color = mainTextColor.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = entry.query,
                color = mainTextColor,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .size(28.dp)
                .testTag("history_remove_${entry.query.hashCode()}")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Verlaufseintrag entfernen",
                tint = mainTextColor.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun WebSearchItem(
    query: String,
    mainTextColor: Color,
    isLiquidGlass: Boolean,
    isDarkText: Boolean,
    onClick: (Rect?) -> Unit
) {
    val density = LocalDensity.current
    val backgroundModifier = if (isLiquidGlass) {
        Modifier.background(
            color = if (isDarkText) Color.Black.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(12.dp)
        )
    } else Modifier
    var iconBounds by remember(query) { mutableStateOf<Rect?>(null) }
    var rowBounds by remember(query) { mutableStateOf<Rect?>(null) }
    val minLaunchSizePx = with(density) { 28.dp.toPx() }
    val preferredLaunchSizePx = with(density) { 40.dp.toPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(backgroundModifier)
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
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .onGloballyPositioned { iconBounds = it.boundsInRoot() }
                .background(mainTextColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = mainTextColor.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = "Im Web suchen", color = mainTextColor.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(text = query, color = mainTextColor, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
