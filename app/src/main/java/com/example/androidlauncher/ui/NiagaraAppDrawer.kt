package com.example.androidlauncher.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.ui.theme.LocalAppFont
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalFontSize
import com.example.androidlauncher.ui.theme.LocalFontWeight
import com.example.androidlauncher.ui.theme.LocalHapticFeedbackEnabled
import com.example.androidlauncher.ui.theme.LocalLiquidGlassEnabled
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * NiagaraAppDrawer ist die alternative AppDrawer-Layoutvariante (Version 2).
 *
 * Zeigt alle Apps als alphabetisch sortierte Liste mit Buchstaben-Gruppenüberschriften und
 * seitlicher A–Z-Schnellnavigation (angelehnt an den Niagara Launcher). Ordner werden hier
 * bewusst nicht dargestellt – diese bleiben Version 1 (Grid) vorbehalten.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NiagaraAppDrawer(
    apps: List<AppInfo>,
    onToggleFavorite: (String) -> Unit,
    isFavorite: (String) -> Boolean,
    onClose: () -> Unit,
    onLaunchApp: (String, Intent, Rect?) -> Unit,
    returnIconPackage: String?
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colorTheme = LocalColorTheme.current
    val fontSize = LocalFontSize.current
    val fontWeight = LocalFontWeight.current
    val appFont = LocalAppFont.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val isLiquidGlassEnabled = LocalLiquidGlassEnabled.current
    val hapticEnabled = LocalHapticFeedbackEnabled.current
    val haptic = LocalHapticFeedback.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val drawerBackgroundBrush = remember(colorTheme, isDarkTextEnabled) {
        colorTheme.backgroundBrush(isDarkTextEnabled, alpha = 0.88f)
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val appDrawerVm: AppDrawerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val customIcons by appDrawerVm.customIcons.collectAsState()
    val searchQuery by appDrawerVm.searchQuery.collectAsState()
    val visibleApps by appDrawerVm.visibleApps.collectAsState()

    // apps ist eine in-place mutierte SnapshotStateList – auf den Inhalt keyen (Kopie),
    // damit Installationen/Deinstallationen den ViewModel-State tatsächlich erreichen.
    val appsSnapshot = apps.toList()
    LaunchedEffect(appsSnapshot) { appDrawerVm.updateApps(appsSnapshot) }

    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    var menuAppBounds by remember { mutableStateOf<Rect?>(null) }

    // Apps nach Anfangsbuchstabe gruppieren. Quelle ist bereits alphabetisch sortiert,
    // daher bleibt die Reihenfolge erhalten. Nicht-Buchstaben landen unter "#".
    val grouped = remember(visibleApps) {
        visibleApps.groupBy { app ->
            val first = app.label.trim().firstOrNull()?.uppercaseChar()
            if (first != null && first in 'A'..'Z') first.toString() else "#"
        }
    }

    // Item-Index jeder Sektionsüberschrift für die Sprungnavigation.
    val headerIndexByLetter = remember(grouped) {
        val map = HashMap<String, Int>()
        var idx = 0
        grouped.forEach { (letter, groupApps) ->
            map[letter] = idx
            idx += 1 + groupApps.size // Header + Apps
        }
        map
    }

    // Reihenfolge der Seitenleiste: "#" (falls vorhanden) gefolgt von A–Z.
    val sideBarLetters = remember {
        buildList {
            add("#")
            ('A'..'Z').forEach { add(it.toString()) }
        }
    }

    val listState = rememberLazyListState()
    var swipeDragY by remember { mutableStateOf(0f) }
    val swipeCloseThresholdPx = with(density) { 64.dp.toPx() }
    val swipeToCloseConnection = remember(listState, swipeCloseThresholdPx, onClose) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                if (source == NestedScrollSource.UserInput && atTop && available.y > 0f) {
                    swipeDragY += available.y
                    if (swipeDragY >= swipeCloseThresholdPx) {
                        swipeDragY = 0f
                        onClose()
                    }
                    return Offset(0f, available.y)
                }
                if (!atTop || available.y < 0f) swipeDragY = 0f
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                if (atTop && available.y > 1500f) {
                    swipeDragY = 0f
                    onClose()
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                swipeDragY = 0f
                return Velocity.Zero
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().testTag("niagara_app_drawer")) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(drawerBackgroundBrush)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Apps",
                    fontSize = 24.sp * fontSize.scale,
                    fontWeight = fontWeight.weight,
                    color = mainTextColor
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = mainTextColor)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            val searchIntSrc = remember { MutableInteractionSource() }
            val searchBarModifier = if (isLiquidGlassEnabled) {
                Modifier
                    .background(LiquidGlass.glassBrush(isDarkTextEnabled), RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.2.dp, LiquidGlass.borderBrush(isDarkTextEnabled)), RoundedCornerShape(12.dp))
            } else {
                Modifier.background(mainTextColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("niagara_app_drawer_search_field")
                    .then(searchBarModifier)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .clickable(interactionSource = searchIntSrc, indication = null) { focusRequester.requestFocus() }
            ) {
                StableSearchFieldContent(
                    value = searchQuery,
                    onValueChange = { appDrawerVm.setSearchQuery(it) },
                    placeholder = "Apps durchsuchen...",
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = mainTextColor,
                        fontSize = 16.sp * fontSize.scale
                    ),
                    textColor = mainTextColor,
                    placeholderColor = mainTextColor.copy(alpha = 0.4f),
                    focusRequester = focusRequester,
                    leadingIconTint = mainTextColor.copy(alpha = 0.4f),
                    leadingIconSize = 20.dp,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            val isSearching = searchQuery.isNotBlank()

            Box(modifier = Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(swipeToCloseConnection),
                        state = listState,
                        contentPadding = PaddingValues(end = if (isSearching) 0.dp else 24.dp, bottom = 32.dp)
                    ) {
                        if (isSearching) {
                            items(items = visibleApps, key = { it.packageName }, contentType = { "app" }) { app ->
                                NiagaraAppRow(
                                    app = app,
                                    customIcons = customIcons,
                                    bouncePackage = returnIconPackage,
                                    mainTextColor = mainTextColor,
                                    onLaunch = { bounds ->
                                        context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { intent ->
                                            onLaunchApp(app.packageName, intent, bounds)
                                        }
                                    },
                                    onLongPress = { bounds ->
                                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuApp = app
                                        menuAppBounds = bounds
                                    }
                                )
                            }
                        } else {
                            grouped.forEach { (letter, groupApps) ->
                                stickyHeader(key = "header_$letter", contentType = "header") {
                                    NiagaraSectionHeader(letter = letter, mainTextColor = mainTextColor)
                                }
                                items(items = groupApps, key = { it.packageName }, contentType = { "app" }) { app ->
                                    NiagaraAppRow(
                                        app = app,
                                        customIcons = customIcons,
                                        bouncePackage = returnIconPackage,
                                        mainTextColor = mainTextColor,
                                        onLaunch = { bounds ->
                                            context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { intent ->
                                                onLaunchApp(app.packageName, intent, bounds)
                                            }
                                        },
                                        onLongPress = { bounds ->
                                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuApp = app
                                            menuAppBounds = bounds
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Seitliche A–Z-Schnellnavigation – nur ohne aktive Suche.
                // Tippen springt zum Buchstaben, Wischen entlang der Leiste scrubbt
                // flüssig durch die Liste (Buchstabe unter dem Finger).
                if (!isSearching) {
                    var activeLetter by remember { mutableStateOf<String?>(null) }
                    // Gemessene vertikale Mitte jedes Buchstabens (relativ zur Leiste).
                    // Über SpaceEvenly verteilte Buchstaben liegen nicht in gleich großen
                    // Slabs – daher den Finger auf den tatsächlich nächsten Buchstaben mappen.
                    val letterCenters = remember(sideBarLetters) { FloatArray(sideBarLetters.size) }
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(24.dp)
                            .pointerInput(headerIndexByLetter, sideBarLetters) {
                                fun resolve(y: Float) {
                                    var best: String? = null
                                    var bestDist = Float.MAX_VALUE
                                    sideBarLetters.forEachIndexed { i, letter ->
                                        if (headerIndexByLetter[letter] == null) return@forEachIndexed
                                        val d = abs(letterCenters[i] - y)
                                        if (d < bestDist) {
                                            bestDist = d
                                            best = letter
                                        }
                                    }
                                    val letter = best ?: return
                                    if (letter != activeLetter) {
                                        activeLetter = letter
                                        if (hapticEnabled) {
                                            @Suppress("DEPRECATION")
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        }
                                        headerIndexByLetter[letter]?.let { target ->
                                            scope.launch { listState.scrollToItem(target) }
                                        }
                                    }
                                }
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    activeLetter = null
                                    resolve(down.position.y)
                                    down.consume()
                                    do {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                            ?: event.changes.firstOrNull()
                                        if (change != null && change.pressed) {
                                            resolve(change.position.y)
                                            change.consume()
                                        }
                                    } while (event.changes.any { it.pressed })
                                    activeLetter = null
                                }
                            },
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        sideBarLetters.forEachIndexed { index, letter ->
                            val enabled = headerIndexByLetter[letter] != null
                            val isActive = letter == activeLetter
                            Text(
                                text = letter,
                                fontSize = if (isActive) 13.sp else 11.sp,
                                fontFamily = appFont.fontFamily,
                                fontWeight = fontWeight.weight,
                                color = mainTextColor.copy(
                                    alpha = when {
                                        !enabled -> 0.22f
                                        isActive -> 1f
                                        else -> 0.75f
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { letterCenters[index] = it.boundsInParent().center.y }
                                    .padding(vertical = 2.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        val currentMenuApp = menuApp
        if (currentMenuApp != null) {
            AppContextMenu(
                isFavorite = isFavorite(currentMenuApp.packageName),
                targetBounds = menuAppBounds,
                onDismiss = { menuApp = null },
                onToggleFavorite = { onToggleFavorite(currentMenuApp.packageName) },
                onAppInfo = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", currentMenuApp.packageName, null)
                        }
                    )
                },
                onUninstall = {
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_DELETE).apply {
                                data = Uri.fromParts("package", currentMenuApp.packageName, null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    } catch (_: Exception) {
                        Toast.makeText(context, "Deinstallation konnte nicht gestartet werden", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

/**
 * Buchstaben-Gruppenüberschrift innerhalb der Niagara-Liste.
 */
@Composable
private fun NiagaraSectionHeader(
    letter: String,
    mainTextColor: Color
) {
    val fontWeight = LocalFontWeight.current
    // Bewusst ohne Hintergrund-Balken – nur ein dezenter, linksbündiger Buchstabe.
    Text(
        text = letter,
        fontSize = 13.sp,
        fontWeight = fontWeight.weight,
        color = mainTextColor.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 6.dp)
    )
}

/**
 * Eine einzelne App-Zeile im Niagara-Listenlayout (Icon links, Label rechts).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NiagaraAppRow(
    app: AppInfo,
    customIcons: Map<String, String>?,
    bouncePackage: String?,
    mainTextColor: Color,
    onLaunch: (Rect?) -> Unit,
    onLongPress: (Rect?) -> Unit
) {
    val fontSize = LocalFontSize.current
    val fontWeight = LocalFontWeight.current
    val intSrc = remember { MutableInteractionSource() }
    var iconBounds by remember { mutableStateOf<Rect?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(intSrc)
            .combinedClickable(
                interactionSource = intSrc,
                indication = null,
                onClick = { onLaunch(iconBounds) },
                onLongClick = { onLongPress(iconBounds) }
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.onGloballyPositioned { iconBounds = it.boundsInRoot() }) {
            AppIconView(app, customIcons = customIcons)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = app.label,
            fontSize = 16.sp * fontSize.scale,
            color = mainTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = fontWeight.weight
        )
    }
}
