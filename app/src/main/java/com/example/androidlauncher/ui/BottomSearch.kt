package com.example.androidlauncher.ui

import android.content.Intent
import android.app.SearchManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.example.androidlauncher.ui.theme.LocalLiquidGlassEnabled

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

    // Theme-Konfiguration für die schwebende Leiste
    val searchContainerModifier = if (isDarkTextEnabled) {
        // "Dark Mode" (Schrift Schwarz): Gleicher Hintergrund wie AppContextMenu (fast undurchsichtig, Pastell/Hell)
        // Wir nutzen hier themedLightBackground Logik analog zu AppContextMenu, falls verfügbar, oder drawerBackground
        val primary = colorTheme.primary
        val themedLightBackground = Color(
            red = primary.red * 0.90f + 0.10f,
            green = primary.green * 0.90f + 0.10f,
            blue = primary.blue * 0.90f + 0.10f,
            alpha = 1f
        )

        val menuBgColor = themedLightBackground.copy(alpha = 0.98f)

        if (isLiquidGlassEnabled) {
             val borderBrush = Brush.linearGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.8f),
                    Color.Black.copy(alpha = 0.3f)
                )
            )
            Modifier
                .background(menuBgColor, RoundedCornerShape(28.dp))
                .border(BorderStroke(1.2.dp, borderBrush), RoundedCornerShape(28.dp))
        } else {
            Modifier.background(menuBgColor, RoundedCornerShape(28.dp))
        }
    } else if (isLiquidGlassEnabled) {
        // "Light Mode" (Schrift Weiß) & Liquid Glass (Bleibt wie vorher, passend zum Button)
        val glassBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.05f)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )

        val borderBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.6f),
                Color.White.copy(alpha = 0.1f)
            )
        )

        Modifier
            .background(glassBrush, RoundedCornerShape(28.dp))
            .border(BorderStroke(1.2.dp, borderBrush), RoundedCornerShape(28.dp))
    } else {
        // "Light Mode" (Schrift Weiß) & Standard (Bleibt wie vorher)
        Modifier.background(mainTextColor.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
    }

    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Filter apps based on query
    val filteredApps = remember(query, apps) {
        LauncherLogic.filterAppsByRelevance(apps, query).take(3)
    }

    // Handle back press
    BackHandler {
        onClose()
    }

    // Auto-focus the search field
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Overlay-Hintergrund anpassen
    val overlayColor = if (isDarkTextEnabled) {
        // "Dark Mode" (Schrift Schwarz): Weißes Milchglas-Dimming
        Color.White.copy(alpha = 0.55f)
    } else {
        // "Light Mode" (Schrift Weiß): Stärkeres Abdunkeln des Hintergrunds, damit weiße Schrift lesbar bleibt
        Color.Black.copy(alpha = 0.65f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(overlayColor) // Angepasster Dim-Hintergrund
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                 onClose()
            }
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Floating Container logic
        // Wir nutzen eine Box am unteren Bildschirmrand, die durch IME nach oben geschoben wird.
        // Darin befindet sich unsere "Floating Search Bar" mit etwas Padding.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { /* Consume clicks outside */ }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), // Abstand zum Rand/Tastatur
                verticalArrangement = Arrangement.spacedBy(16.dp) // Abstand zwischen Ergebnis-Blase und Suchfeld-Blase
            ) {
                // Search Results Bubble (only if query is not empty)
                if (query.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(searchContainerModifier) // Gleiches Design wie Suchfeld
                            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) // Smoothe Größenänderung des Containers
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // App Results (Max 3, no scroll)
                        // Reverse Layout simulieren: Liste umdrehen, damit Top-Treffer unten ist (näher am Finger)
                        val reversedApps = filteredApps.reversed()

                        reversedApps.forEach { app ->
                            key(app.packageName) { // Key ist wichtig damit Compose weiß was animiert werden soll
                                AnimatedVisibility(
                                    visible = true,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    AppSearchItem(
                                        app = app,
                                        mainTextColor = mainTextColor,
                                        fontSizeScale = fontSize.scale,
                                        onClick = { onAppLaunch(app) }
                                    )
                                }
                            }
                        }

                        // Divider (nur wenn Apps da sind)
                        // Auch diesen animieren wir rein/raus
                        AnimatedVisibility(
                            visible = reversedApps.isNotEmpty(),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                             Box(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .padding(vertical = 4.dp)
                                     .height(1.dp)
                                     .background(
                                         color = if (isDarkTextEnabled) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f)
                                     )
                             )
                        }

                        // Web Search Option (ganz unten in der Ergebnis-Liste)
                         WebSearchItem(
                             query = query,
                             mainTextColor = mainTextColor,
                             isLiquidGlass = isLiquidGlassEnabled,
                             isDarkText = isDarkTextEnabled,
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
                }

                // Search Input Bubble
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(searchContainerModifier) // Gleiches Design wie oben
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { /* Consume clicks */ }
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
                            Text(
                                text = "Search...",
                                color = mainTextColor.copy(alpha = 0.4f),
                                fontSize = 18.sp
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
                            cursorBrush = SolidColor(mainTextColor),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    // Immer Websuche bei Enter/Suche auf der Tastatur
                                    if (query.isNotEmpty()) {
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

                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { query = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = mainTextColor.copy(alpha = 0.5f)
                            )
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp)) {
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
    isLiquidGlass: Boolean,
    isDarkText: Boolean,
    onClick: () -> Unit
) {
    val backgroundModifier = if (isLiquidGlass) {
        Modifier.background(
            color = if (isDarkText) Color.Black.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(12.dp)
        )
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(backgroundModifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(mainTextColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = mainTextColor.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = "Search Web",
                color = mainTextColor.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = query,
                color = mainTextColor,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

