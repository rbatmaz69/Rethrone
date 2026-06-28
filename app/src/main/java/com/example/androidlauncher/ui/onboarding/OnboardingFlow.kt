package com.example.androidlauncher.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.composables.icons.lucide.ArrowUp
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.ChartColumn
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Hand
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.PartyPopper
import com.composables.icons.lucide.PersonStanding
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Vibrate
import com.example.androidlauncher.ForegroundAppResolver
import com.example.androidlauncher.LauncherAccessibilityService
import com.example.androidlauncher.LauncherLogic
import com.example.androidlauncher.R
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.DesignStyle
import com.example.androidlauncher.data.ThemeManager
import com.example.androidlauncher.ui.AppIconView
import com.example.androidlauncher.ui.isDefaultLauncher
import com.example.androidlauncher.ui.isNotificationServiceEnabled
import com.example.androidlauncher.ui.openAccessibilitySettings
import com.example.androidlauncher.ui.openNotificationSettings
import com.example.androidlauncher.ui.theme.ColorTheme
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle
import com.example.androidlauncher.ui.theme.RethroneShape
import com.example.androidlauncher.ui.theme.rememberAppHaptics
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.grid.items as gridItems

/**
 * Erststart-Onboarding: ruhiger Vollbild-Wizard im Material-3-Expressive-Look.
 *
 * Wird in [com.example.androidlauncher.MainActivity] als oberste Ebene gerendert,
 * solange [ThemeManager.isOnboardingCompleted] `false` ist. Begrüßt den Nutzer,
 * hilft aktiv beim Setzen als Standard-Launcher, führt durch die wichtigsten
 * Berechtigungen (jeweils überspringbar), lässt Theme/Design wählen, Favoriten
 * aussuchen und die Gesten verstehen. [onComplete] schreibt schließlich das Flag.
 *
 * Wird innerhalb des `AndroidLauncherTheme`-Scopes gerendert, liest also Farbe/Text
 * über die `Local*`-Provider und respektiert ein ggf. bereits gewähltes Theme.
 *
 * Die Flächen (Icon-Kreise/Karten) nutzen bewusst NICHT `designSurface`, sondern eine
 * eigene, flache [onboardingSurface] – so bleibt der Wizard auf jedem gewählten
 * Design-Stil sauber (kein durchscheinender Schatten / keine getönte Doppelfläche).
 */
@Composable
fun OnboardingFlow(
    themeManager: ThemeManager,
    apps: List<AppInfo>,
    favoritePackages: List<String>,
    onFavoritesChange: (List<String>) -> Unit,
    onRequestDefaultLauncher: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberAppHaptics()

    val colorTheme = LocalColorTheme.current
    val isDarkText = LocalDarkTextEnabled.current
    val designStyle = LocalDesignStyle.current

    val mainTextColor = if (isDarkText) Color(0xFF101010) else Color.White
    val secondaryTextColor = mainTextColor.copy(alpha = 0.72f)

    // Live-Status der Setup-Schritte; wird bei jedem ON_RESUME neu gelesen, damit die
    // Badges nach Rückkehr aus den Systemeinstellungen sofort grün werden.
    var defaultLauncherOn by remember { mutableStateOf(isDefaultLauncher(context)) }
    var usageAccessOn by remember { mutableStateOf(ForegroundAppResolver.hasUsageAccess(context)) }
    var accessibilityOn by remember {
        mutableStateOf(LauncherAccessibilityService.isAccessibilityServiceEnabled(context))
    }
    var notificationAccessOn by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var locationOn by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                defaultLauncherOn = isDefaultLauncher(context)
                usageAccessOn = ForegroundAppResolver.hasUsageAccess(context)
                accessibilityOn = LauncherAccessibilityService.isAccessibilityServiceEnabled(context)
                notificationAccessOn = isNotificationServiceEnabled(context)
                locationOn = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> locationOn = granted }

    val pageCount = 7
    val pagerState = rememberPagerState(pageCount = { pageCount })

    fun goTo(page: Int) {
        scope.launch { pagerState.animateScrollToPage(page) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colorTheme.menuGradient))
            // Streutaps abfangen, damit der Startbildschirm darunter nicht reagiert.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
    ) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            // Kopfzeile: „Überspringen" (auf der letzten Seite ausgeblendet).
            Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                if (pagerState.currentPage < pageCount - 1) {
                    TextButton(
                        onClick = {
                            haptics.tap()
                            goTo(pageCount - 1)
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_skip),
                            color = secondaryTextColor,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> ScrollPage {
                        WelcomePage(mainTextColor, secondaryTextColor)
                    }
                    1 -> ScrollPage {
                        DefaultLauncherPage(
                            mainTextColor,
                            secondaryTextColor,
                            isActive = defaultLauncherOn,
                            onAction = {
                                haptics.tap()
                                onRequestDefaultLauncher()
                            }
                        )
                    }
                    2 -> ScrollPage {
                        PermissionsPage(
                            mainTextColor, secondaryTextColor,
                            usageAccessOn = usageAccessOn,
                            accessibilityOn = accessibilityOn,
                            notificationAccessOn = notificationAccessOn,
                            locationOn = locationOn,
                            onUsageAccess = {
                                haptics.tap()
                                ForegroundAppResolver.openUsageAccessSettings(context)
                            },
                            onAccessibility = {
                                haptics.tap()
                                openAccessibilitySettings(context)
                            },
                            onNotificationAccess = {
                                haptics.tap()
                                openNotificationSettings(context)
                            },
                            onLocation = {
                                haptics.tap()
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                        )
                    }
                    3 -> ScrollPage {
                        PersonalizePage(
                            mainTextColor,
                            secondaryTextColor,
                            selectedTheme = colorTheme,
                            selectedDesign = designStyle,
                            onSelectTheme = { theme ->
                                haptics.select()
                                scope.launch { themeManager.setTheme(theme) }
                            },
                            onSelectDesign = { style ->
                                haptics.select()
                                scope.launch { themeManager.setDesignStyle(style) }
                            }
                        )
                    }
                    4 -> FavoritesPage(
                        mainTextColor,
                        secondaryTextColor,
                        apps = apps,
                        favoritePackages = favoritePackages,
                        onFavoritesChange = onFavoritesChange
                    )
                    5 -> ScrollPage {
                        GesturesPage(mainTextColor, secondaryTextColor)
                    }
                    else -> ScrollPage {
                        FinishPage(mainTextColor, secondaryTextColor)
                    }
                }
            }

            // Fußzeile: Fortschrittspunkte + Haupt-Button.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PageIndicator(
                    pageCount = pageCount,
                    current = pagerState.currentPage,
                    activeColor = mainTextColor,
                    inactiveColor = mainTextColor.copy(alpha = 0.25f)
                )
                Spacer(Modifier.height(20.dp))
                val isLast = pagerState.currentPage == pageCount - 1
                Button(
                    onClick = {
                        if (isLast) {
                            haptics.confirm()
                            onComplete()
                        } else {
                            haptics.tap()
                            goTo(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RethroneShape.Pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = mainTextColor,
                        contentColor = if (isDarkText) Color.White else colorTheme.drawerBackground
                    )
                ) {
                    Text(
                        text = stringResource(
                            if (isLast) R.string.onboarding_finish else R.string.onboarding_next
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Standard-Container für die meisten Wizard-Seiten: vertikal scrollbar und zentriert.
 * (Die Favoriten-Seite bringt einen eigenen Container mit, da ein scrollbares Grid
 * nicht in einen `verticalScroll`-Container geschachtelt werden darf.)
 */
@Composable
private fun ScrollPage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

/* ----------------------------- Seiten ----------------------------- */

@Composable
private fun WelcomePage(mainTextColor: Color, secondaryTextColor: Color) {
    PageHeader(icon = Lucide.House, mainTextColor = mainTextColor)
    Spacer(Modifier.height(28.dp))
    Text(
        text = stringResource(R.string.onboarding_welcome_title),
        style = MaterialTheme.typography.displaySmall,
        color = mainTextColor,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(14.dp))
    Text(
        text = stringResource(R.string.onboarding_welcome_desc),
        style = MaterialTheme.typography.bodyLarge,
        color = secondaryTextColor,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun DefaultLauncherPage(
    mainTextColor: Color,
    secondaryTextColor: Color,
    isActive: Boolean,
    onAction: () -> Unit
) {
    PageHeader(icon = Lucide.Sparkles, mainTextColor = mainTextColor)
    Spacer(Modifier.height(28.dp))
    Text(
        text = stringResource(R.string.onboarding_default_title),
        style = MaterialTheme.typography.headlineMedium,
        color = mainTextColor,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(14.dp))
    Text(
        text = stringResource(R.string.onboarding_default_desc),
        style = MaterialTheme.typography.bodyLarge,
        color = secondaryTextColor,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(28.dp))
    if (isActive) {
        StatusBadge(
            text = stringResource(R.string.onboarding_status_active),
            mainTextColor = mainTextColor
        )
    } else {
        SecondaryActionButton(
            text = stringResource(R.string.onboarding_default_action),
            onClick = onAction,
            mainTextColor = mainTextColor
        )
    }
}

@Composable
private fun PermissionsPage(
    mainTextColor: Color,
    secondaryTextColor: Color,
    usageAccessOn: Boolean,
    accessibilityOn: Boolean,
    notificationAccessOn: Boolean,
    locationOn: Boolean,
    onUsageAccess: () -> Unit,
    onAccessibility: () -> Unit,
    onNotificationAccess: () -> Unit,
    onLocation: () -> Unit
) {
    Text(
        text = stringResource(R.string.onboarding_permissions_title),
        style = MaterialTheme.typography.headlineMedium,
        color = mainTextColor,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = stringResource(R.string.onboarding_permissions_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = secondaryTextColor,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(24.dp))
    PermissionCard(
        icon = Lucide.ChartColumn,
        title = stringResource(R.string.onboarding_perm_usage_title),
        description = stringResource(R.string.onboarding_perm_usage_desc),
        granted = usageAccessOn,
        onGrant = onUsageAccess,
        mainTextColor = mainTextColor,
        secondaryTextColor = secondaryTextColor
    )
    Spacer(Modifier.height(14.dp))
    PermissionCard(
        icon = Lucide.PersonStanding,
        title = stringResource(R.string.onboarding_perm_accessibility_title),
        description = stringResource(R.string.onboarding_perm_accessibility_desc),
        granted = accessibilityOn,
        onGrant = onAccessibility,
        mainTextColor = mainTextColor,
        secondaryTextColor = secondaryTextColor
    )
    Spacer(Modifier.height(14.dp))
    PermissionCard(
        icon = Lucide.Bell,
        title = stringResource(R.string.onboarding_perm_notifications_title),
        description = stringResource(R.string.onboarding_perm_notifications_desc),
        granted = notificationAccessOn,
        onGrant = onNotificationAccess,
        mainTextColor = mainTextColor,
        secondaryTextColor = secondaryTextColor
    )
    Spacer(Modifier.height(14.dp))
    PermissionCard(
        icon = Lucide.MapPin,
        title = stringResource(R.string.onboarding_perm_location_title),
        description = stringResource(R.string.onboarding_perm_location_desc),
        granted = locationOn,
        onGrant = onLocation,
        mainTextColor = mainTextColor,
        secondaryTextColor = secondaryTextColor
    )
}

@Composable
private fun PersonalizePage(
    mainTextColor: Color,
    secondaryTextColor: Color,
    selectedTheme: ColorTheme,
    selectedDesign: DesignStyle,
    onSelectTheme: (ColorTheme) -> Unit,
    onSelectDesign: (DesignStyle) -> Unit
) {
    Text(
        text = stringResource(R.string.onboarding_personalize_title),
        style = MaterialTheme.typography.headlineMedium,
        color = mainTextColor,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = stringResource(R.string.onboarding_personalize_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = secondaryTextColor,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(24.dp))

    // Kuratierte, repräsentative Auswahl an Themes.
    val showcaseThemes = remember {
        listOf(
            ColorTheme.SOFT_SAND,
            ColorTheme.SIGNATURE,
            ColorTheme.OCEAN_DEPTHS,
            ColorTheme.DESERT_GLOW,
            ColorTheme.MISTY_VALLEY,
            ColorTheme.ROSE_DUST,
            ColorTheme.AURORA_VEIL,
            ColorTheme.NEON_NOIR
        )
    }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(showcaseThemes) { theme ->
            ThemeSwatch(
                theme = theme,
                selected = theme == selectedTheme,
                ringColor = mainTextColor,
                onClick = { onSelectTheme(theme) }
            )
        }
    }

    Spacer(Modifier.height(28.dp))
    Text(
        text = stringResource(R.string.onboarding_personalize_design),
        style = MaterialTheme.typography.titleSmall,
        color = secondaryTextColor,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(12.dp))
    val designChoices = remember {
        listOf(DesignStyle.GLASS, DesignStyle.FLAT, DesignStyle.MINIMAL, DesignStyle.OUTLINE)
    }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(designChoices) { style ->
            ChoiceChip(
                label = stringResource(style.titleRes),
                selected = style == selectedDesign,
                mainTextColor = mainTextColor,
                onClick = { onSelectDesign(style) }
            )
        }
    }
}

/**
 * Favoriten-Auswahl: App-Raster, Auswahl bis max. [LauncherLogic.MAX_FAVORITES].
 * Bringt einen eigenen Container mit (kein `ScrollPage`), da das `LazyVerticalGrid`
 * nicht in einen `verticalScroll`-Container darf.
 */
@Composable
private fun FavoritesPage(
    mainTextColor: Color,
    secondaryTextColor: Color,
    apps: List<AppInfo>,
    favoritePackages: List<String>,
    onFavoritesChange: (List<String>) -> Unit
) {
    val haptics = rememberAppHaptics()
    var selected by remember { mutableStateOf(favoritePackages) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_favorites_title),
            style = MaterialTheme.typography.headlineMedium,
            color = mainTextColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.onboarding_favorites_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryTextColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.favorites_count, selected.size, LauncherLogic.MAX_FAVORITES),
            style = MaterialTheme.typography.labelLarge,
            color = secondaryTextColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            gridItems(apps, key = { it.packageName }) { app ->
                val isSelected = selected.contains(app.packageName)
                FavoriteCell(
                    app = app,
                    selected = isSelected,
                    mainTextColor = mainTextColor,
                    onClick = {
                        val pkg = app.packageName
                        selected = when {
                            isSelected -> {
                                haptics.tap()
                                selected - pkg
                            }
                            selected.size < LauncherLogic.MAX_FAVORITES -> {
                                haptics.select()
                                selected + pkg
                            }
                            else -> selected
                        }
                        onFavoritesChange(selected)
                    }
                )
            }
        }
    }
}

@Composable
private fun GesturesPage(mainTextColor: Color, secondaryTextColor: Color) {
    Text(
        text = stringResource(R.string.onboarding_gestures_title),
        style = MaterialTheme.typography.headlineMedium,
        color = mainTextColor,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = stringResource(R.string.onboarding_gestures_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = secondaryTextColor,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(24.dp))
    GestureRow(
        icon = Lucide.ArrowUp,
        title = stringResource(R.string.onboarding_gesture_swipe_title),
        description = stringResource(R.string.onboarding_gesture_swipe_desc),
        mainTextColor = mainTextColor,
        secondaryTextColor = secondaryTextColor
    )
    Spacer(Modifier.height(14.dp))
    GestureRow(
        icon = Lucide.Hand,
        title = stringResource(R.string.onboarding_gesture_doubletap_title),
        description = stringResource(R.string.onboarding_gesture_doubletap_desc),
        mainTextColor = mainTextColor,
        secondaryTextColor = secondaryTextColor
    )
    Spacer(Modifier.height(14.dp))
    GestureRow(
        icon = Lucide.Vibrate,
        title = stringResource(R.string.onboarding_gesture_shake_title),
        description = stringResource(R.string.onboarding_gesture_shake_desc),
        mainTextColor = mainTextColor,
        secondaryTextColor = secondaryTextColor
    )
}

@Composable
private fun FinishPage(mainTextColor: Color, secondaryTextColor: Color) {
    PageHeader(icon = Lucide.PartyPopper, mainTextColor = mainTextColor)
    Spacer(Modifier.height(28.dp))
    Text(
        text = stringResource(R.string.onboarding_finish_title),
        style = MaterialTheme.typography.displaySmall,
        color = mainTextColor,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(14.dp))
    Text(
        text = stringResource(R.string.onboarding_finish_desc),
        style = MaterialTheme.typography.bodyLarge,
        color = secondaryTextColor,
        textAlign = TextAlign.Center
    )
}

/* --------------------------- Bausteine --------------------------- */

/**
 * Ruhige, flache Onboarding-Fläche – unabhängig vom gewählten Design-Stil und ohne
 * Schatten. Dezente Füllung + dünner Rand, lesbar auf hellem wie dunklem Untergrund.
 */
private fun Modifier.onboardingSurface(mainTextColor: Color, shape: Shape): Modifier = this
    .background(mainTextColor.copy(alpha = 0.07f), shape)
    .border(BorderStroke(1.dp, mainTextColor.copy(alpha = 0.14f)), shape)

@Composable
private fun PageHeader(icon: ImageVector, mainTextColor: Color) {
    Box(
        modifier = Modifier
            .size(104.dp)
            .onboardingSurface(mainTextColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = mainTextColor,
            modifier = Modifier.size(46.dp)
        )
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onGrant: () -> Unit,
    mainTextColor: Color,
    secondaryTextColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onboardingSurface(mainTextColor, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = mainTextColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = mainTextColor,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor
            )
        }
        Spacer(Modifier.width(12.dp))
        if (granted) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(mainTextColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Check,
                    contentDescription = stringResource(R.string.onboarding_status_active),
                    tint = mainTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            TextButton(onClick = onGrant) {
                Text(
                    text = stringResource(R.string.onboarding_allow),
                    color = mainTextColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun GestureRow(
    icon: ImageVector,
    title: String,
    description: String,
    mainTextColor: Color,
    secondaryTextColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onboardingSurface(mainTextColor, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(mainTextColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = mainTextColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = mainTextColor,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor
            )
        }
    }
}

@Composable
private fun FavoriteCell(
    app: AppInfo,
    selected: Boolean,
    mainTextColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(18.dp))
                .then(
                    if (selected) {
                        Modifier
                            .background(mainTextColor.copy(alpha = 0.18f))
                            .border(BorderStroke(2.dp, mainTextColor.copy(alpha = 0.9f)), RoundedCornerShape(18.dp))
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            AppIconView(app)
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(mainTextColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Check,
                        contentDescription = null,
                        tint = if (mainTextColor.luminanceIsDark()) Color.White else Color.Black,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = mainTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ThemeSwatch(
    theme: ColorTheme,
    selected: Boolean,
    ringColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(theme.artGradient))
            .then(
                if (selected) {
                    Modifier.border(BorderStroke(3.dp, ringColor), CircleShape)
                } else {
                    Modifier.border(BorderStroke(1.dp, ringColor.copy(alpha = 0.25f)), CircleShape)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(ringColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Check,
                    contentDescription = null,
                    tint = if (theme.artGradient.firstOrNull()?.luminanceIsDark() == true) Color.White else Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    mainTextColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RethroneShape.Pill)
            .then(
                if (selected) {
                    Modifier.background(mainTextColor.copy(alpha = 0.18f))
                } else {
                    Modifier
                }
            )
            .border(
                BorderStroke(
                    if (selected) 2.dp else 1.dp,
                    mainTextColor.copy(alpha = if (selected) 0.9f else 0.3f)
                ),
                RethroneShape.Pill
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = mainTextColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    mainTextColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RethroneShape.Pill)
            .onboardingSurface(mainTextColor, RethroneShape.Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = mainTextColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatusBadge(text: String, mainTextColor: Color) {
    Row(
        modifier = Modifier
            .clip(RethroneShape.Pill)
            .background(mainTextColor.copy(alpha = 0.14f))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Lucide.Check,
            contentDescription = null,
            tint = mainTextColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = mainTextColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    current: Int,
    activeColor: Color,
    inactiveColor: Color
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(pageCount) { index ->
            val isActive = index == current
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (isActive) 22.dp else 8.dp)
                    .clip(RethroneShape.Pill)
                    .background(if (isActive) activeColor else inactiveColor)
            )
        }
    }
}

/** Grobe Helligkeitsabschätzung, um einen lesbaren Check-Tint auf einer Fläche zu wählen. */
private fun Color.luminanceIsDark(): Boolean =
    (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f
