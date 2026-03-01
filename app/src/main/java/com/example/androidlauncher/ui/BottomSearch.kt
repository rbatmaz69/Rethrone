package com.example.androidlauncher.ui

import android.content.Intent
import android.app.SearchManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
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
import com.example.androidlauncher.ui.theme.LocalLiquidGlassEnabled
import kotlinx.coroutines.delay

/**
 * Schwebende Suchleiste am unteren Bildschirmrand.
 * Optimiert für ein extrem flüssiges, gemeinsames Erscheinen aller Suchergebnisse.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BottomSearch(
    apps: List<AppInfo>,
    onClose: () -> Unit,
    onAppLaunch: (AppInfo) -> Unit
) {
    val context = LocalContext.current
    val colorTheme = LocalColorTheme.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val isLiquidGlassEnabled = LocalLiquidGlassEnabled.current
    val fontSize = LocalFontSize.current

    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White

    // Theme-Konfiguration
    val searchContainerModifier = if (isDarkTextEnabled) {
        val primary = colorTheme.primary
        val themedLightBackground = Color(
            red = primary.red * 0.90f + 0.10f,
            green = primary.green * 0.90f + 0.10f,
            blue = primary.blue * 0.90f + 0.10f,
            alpha = 1f
        )
        val menuBgColor = themedLightBackground.copy(alpha = 0.98f)
        if (isLiquidGlassEnabled) {
            Modifier
                .background(menuBgColor, RoundedCornerShape(28.dp))
                .border(BorderStroke(1.2.dp, LiquidGlass.borderBrush(isDarkTextEnabled)), RoundedCornerShape(28.dp))
        } else {
            Modifier.background(menuBgColor, RoundedCornerShape(28.dp))
        }
    } else if (isLiquidGlassEnabled) {
        Modifier
            .background(LiquidGlass.glassBrush(isDarkTextEnabled), RoundedCornerShape(28.dp))
            .border(BorderStroke(1.2.dp, LiquidGlass.borderBrush(isDarkTextEnabled)), RoundedCornerShape(28.dp))
    } else {
        Modifier.background(mainTextColor.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
    }

    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val filteredApps = remember(query, apps) {
        LauncherLogic.filterAppsByRelevance(apps, query).take(3)
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
            val targetCount = filteredApps.size + 1
            for (i in 1..targetCount) {
                visibleItemCount = i
                delay(35)
            }
            isInitialAppearance = false
        }
    }
    
    // Stellt sicher, dass visibleItemCount aktuell bleibt, wenn sich die Liste beim Tippen vergrößert
    LaunchedEffect(filteredApps.size) {
        if (query.isNotEmpty() && !isInitialAppearance) {
            visibleItemCount = filteredApps.size + 1
        }
    }

    BackHandler { onClose() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val overlayColor = if (isDarkTextEnabled) Color.White.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.65f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(overlayColor)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() }
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
                    val reversedApps = filteredApps.reversed()
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(searchContainerModifier)
                            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Apps werden als Gruppe behandelt
                        reversedApps.forEachIndexed { index, app ->
                            val isItemVisible = visibleItemCount > index
                            
                            AnimatedVisibility(
                                visible = isItemVisible,
                                enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { 8 }),
                                exit = fadeOut(tween(100))
                            ) {
                                AppSearchItem(
                                    app = app,
                                    mainTextColor = mainTextColor,
                                    fontSizeScale = fontSize.scale,
                                    onClick = { onAppLaunch(app) }
                                )
                            }
                        }

                        // Divider und Websuche erscheinen zusammen
                        val isWebSectionVisible = visibleItemCount > reversedApps.size

                        if (reversedApps.isNotEmpty()) {
                            AnimatedVisibility(
                                visible = isWebSectionVisible,
                                enter = fadeIn(tween(200)),
                                exit = fadeOut(tween(100))
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(mainTextColor.copy(alpha = 0.08f)))
                            }
                        }

                        AnimatedVisibility(
                            visible = isWebSectionVisible,
                            enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { 8 }),
                            exit = fadeOut(tween(100))
                        ) {
                            WebSearchItem(
                                query = query,
                                mainTextColor = mainTextColor,
                                isLiquidGlass = isLiquidGlassEnabled,
                                isDarkText = isDarkTextEnabled,
                                onClick = {
                                    val finalUrl = if (query.startsWith("http") || query.contains(".")) {
                                        if (!query.startsWith("http")) "https://$query" else query
                                    } else "https://www.google.com/search?q=$query"
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                        onClose()
                                    } catch (_: Exception) {
                                        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply { putExtra(SearchManager.QUERY, query) }
                                        if (intent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(intent)
                                            onClose()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // Such-Eingabefeld (Bubble)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(searchContainerModifier)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = mainTextColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(text = "Search...", color = mainTextColor.copy(alpha = 0.4f), fontSize = 18.sp)
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                                .onPreInterceptKeyBeforeSoftKeyboard { event ->
                                    if (event.key == Key.Back && event.type == KeyEventType.KeyDown) {
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
                                         val finalUrl = if (query.startsWith("http") || query.contains(".")) {
                                             if (!query.startsWith("http")) "https://$query" else query
                                         } else "https://www.google.com/search?q=$query"
                                         try {
                                             val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                                             intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                             context.startActivity(intent)
                                             onClose()
                                         } catch (_: Exception) {
                                             val intent = Intent(Intent.ACTION_WEB_SEARCH).apply { putExtra(SearchManager.QUERY, query) }
                                             if (intent.resolveActivity(context.packageManager) != null) {
                                                 context.startActivity(intent)
                                                 onClose()
                                             }
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
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp)) { AppIconView(app = app) }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = app.label, color = mainTextColor, fontSize = 16.sp * fontSizeScale, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun WebSearchItem(
    query: String,
    mainTextColor: Color,
    isLiquidGlass: Boolean,
    isDarkText: Boolean,
    onClick: () -> Unit
) {
    val backgroundModifier = if (isLiquidGlass) {
        Modifier.background(
            color = if (isDarkText) Color.Black.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(12.dp)
        )
    } else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(backgroundModifier)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(mainTextColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = mainTextColor.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = "Search Web", color = mainTextColor.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(text = query, color = mainTextColor, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
