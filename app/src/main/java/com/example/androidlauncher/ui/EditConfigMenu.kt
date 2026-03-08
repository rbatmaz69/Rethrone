package com.example.androidlauncher.ui

import com.example.androidlauncher.ForegroundAppResolver
import com.example.androidlauncher.LauncherAccessibilityService
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings2
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Shield
import com.composables.icons.lucide.Hand
import com.composables.icons.lucide.Smartphone
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalLiquidGlassEnabled
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Konfigurationsmenü für Anpassungen.
 *
 * Bietet Zugang zu:
 * - **App-Icons anpassen** (öffnet [IconConfigMenu])
 * - **Wallpaper ändern** (öffnet System-Bildauswahl mit UCrop)
 * - **Hintergrund anpassen** (öffnet [WallpaperConfigMenu])
 * - **Benachrichtigung** (öffnet System-Einstellungen)
 *
 * @param onOpenIconConfig Callback zum Öffnen der Icon-Konfiguration.
 * @param onChangeWallpaper Callback zum Starten der Wallpaper-Auswahl.
 * @param onResetWallpaper Callback zum Entfernen des benutzerdefinierten Wallpapers.
 * @param onOpenWallpaperAdjust Callback zum Öffnen der Wallpaper-Feinabstimmung.
 * @param isCustomWallpaperSet Ob aktuell ein benutzerdefiniertes Wallpaper gesetzt ist.
 * @param onClose Callback zum Schließen des Menüs.
 */
@Composable
fun EditConfigMenu(
    onOpenIconConfig: () -> Unit,
    onChangeWallpaper: () -> Unit,
    onResetWallpaper: () -> Unit,
    onOpenWallpaperAdjust: () -> Unit,
    isCustomWallpaperSet: Boolean,
    isShakeGesturesEnabled: Boolean,
    onShakeGesturesToggled: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val isLiquidGlassEnabled = LocalLiquidGlassEnabled.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val menuListState = rememberLazyListState()

    var isNotificationEnabled by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var isAccessibilityEnabled by remember { mutableStateOf(LauncherAccessibilityService.isAccessibilityServiceEnabled(context)) }
    var isUsageAccessEnabled by remember { mutableStateOf(ForegroundAppResolver.hasUsageAccess(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isNotificationEnabled = isNotificationServiceEnabled(context)
        isAccessibilityEnabled = LauncherAccessibilityService.isAccessibilityServiceEnabled(context)
        isUsageAccessEnabled = ForegroundAppResolver.hasUsageAccess(context)
    }

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
                "Bearbeiten",
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                color = mainTextColor
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = mainTextColor)
            }
        }

        LazyColumn(
            state = menuListState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 32.dp, bottom = 8.dp)
        ) {
            item {
                EditMenuItem(
                    icon = Lucide.Settings2,
                    label = "App-Icons anpassen",
                    onClick = onOpenIconConfig,
                    mainTextColor = mainTextColor,
                    isLiquidGlassEnabled = isLiquidGlassEnabled,
                    isDarkTextEnabled = isDarkTextEnabled
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Image,
                    label = "Wallpaper ändern",
                    onClick = onChangeWallpaper,
                    mainTextColor = mainTextColor,
                    isLiquidGlassEnabled = isLiquidGlassEnabled,
                    isDarkTextEnabled = isDarkTextEnabled,
                    trailingContent = {
                        if (isCustomWallpaperSet) {
                            IconButton(onClick = onResetWallpaper) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Wallpaper",
                                    tint = mainTextColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = mainTextColor.copy(alpha = 0.4f)
                            )
                        }
                    }
                )
            }

            if (isCustomWallpaperSet) {
                item {
                    EditMenuItem(
                        icon = Lucide.Settings2,
                        label = "Hintergrund anpassen",
                        onClick = onOpenWallpaperAdjust,
                        mainTextColor = mainTextColor,
                        isLiquidGlassEnabled = isLiquidGlassEnabled,
                        isDarkTextEnabled = isDarkTextEnabled
                    )
                }
            }

            item {
                EditSectionHeader(
                    title = "Gesten",
                    mainTextColor = mainTextColor
                )
            }

            item {
                EditToggleItem(
                    icon = Lucide.Smartphone,
                    label = "Shake-Gesten",
                    description = "1× Schütteln: Taschenlampe · 2× Schütteln: Kamera",
                    checked = isShakeGesturesEnabled,
                    onCheckedChange = onShakeGesturesToggled,
                    mainTextColor = mainTextColor,
                    isLiquidGlassEnabled = isLiquidGlassEnabled,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "shake_gestures_switch"
                )
            }

            item {
                EditSectionHeader(
                    title = "Zugriffe",
                    mainTextColor = mainTextColor
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Bell,
                    label = "Benachrichtigung",
                    onClick = {
                        openNotificationSettings(context)
                    },
                    statusLabel = if (isNotificationEnabled) "An" else "Aus",
                    mainTextColor = mainTextColor,
                    isLiquidGlassEnabled = isLiquidGlassEnabled,
                    isDarkTextEnabled = isDarkTextEnabled
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Hand,
                    label = "Bedienungshilfen",
                    onClick = {
                        openAccessibilitySettings(context)
                    },
                    statusLabel = if (isAccessibilityEnabled) "An" else "Aus",
                    mainTextColor = mainTextColor,
                    isLiquidGlassEnabled = isLiquidGlassEnabled,
                    isDarkTextEnabled = isDarkTextEnabled
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Shield,
                    label = "Nutzungszugriff",
                    onClick = {
                        ForegroundAppResolver.openUsageAccessSettings(context)
                    },
                    statusLabel = if (isUsageAccessEnabled) "An" else "Aus",
                    mainTextColor = mainTextColor,
                    isLiquidGlassEnabled = isLiquidGlassEnabled,
                    isDarkTextEnabled = isDarkTextEnabled
                )
            }
        }
    }
}

@Composable
private fun EditSectionHeader(
    title: String,
    mainTextColor: Color
) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = mainTextColor.copy(alpha = 0.7f),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

/**
 * Einzelnes Menü-Element im Bearbeiten-Menü.
 *
 * Zeigt ein Icon, Label und optionalen Status oder Trailing-Content.
 * Unterstützt den Liquid-Glass-Effekt mit abgeschwächten Alpha-Werten
 * für eine subtilere Darstellung als die Standard-Glass-Elemente.
 *
 * @param icon Lucide/Material-Icon links.
 * @param label Beschriftung des Elements.
 * @param onClick Callback bei Klick.
 * @param mainTextColor Primäre Textfarbe.
 * @param isLiquidGlassEnabled Ob Liquid Glass aktiv ist.
 * @param isDarkTextEnabled Ob der dunkle Textmodus aktiv ist.
 * @param statusLabel Optionaler Status-Text (z.B. "An" / "Aus").
 * @param trailingContent Optionaler benutzerdefinierter Trailing-Inhalt.
 */
@Composable
fun EditMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    mainTextColor: Color,
    isLiquidGlassEnabled: Boolean,
    isDarkTextEnabled: Boolean,
    statusLabel: String? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val backgroundModifier = if (isLiquidGlassEnabled) {
        // Subtilere Alpha-Werte als Standard-Glass für Menü-Items
        val glassBrush = LiquidGlass.glassBrush(isDarkTextEnabled, startAlpha = 0.10f, endAlpha = 0.03f)
        val borderBrush = LiquidGlass.borderBrush(
            isDarkTextEnabled,
            startAlpha = if (isDarkTextEnabled) 0.2f else 0.25f,
            endAlpha = 0.05f
        )
        Modifier
            .background(glassBrush, RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, borderBrush), RoundedCornerShape(16.dp))
    } else {
        Modifier.background(mainTextColor.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(backgroundModifier)
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 20.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = mainTextColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                color = mainTextColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (statusLabel != null) {
                Text(
                    text = statusLabel,
                    color = mainTextColor.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = mainTextColor.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun EditToggleItem(
    icon: ImageVector,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    mainTextColor: Color,
    isLiquidGlassEnabled: Boolean,
    isDarkTextEnabled: Boolean,
    switchTestTag: String
) {
    val backgroundModifier = if (isLiquidGlassEnabled) {
        val glassBrush = LiquidGlass.glassBrush(isDarkTextEnabled, startAlpha = 0.10f, endAlpha = 0.03f)
        val borderBrush = LiquidGlass.borderBrush(
            isDarkTextEnabled,
            startAlpha = if (isDarkTextEnabled) 0.2f else 0.25f,
            endAlpha = 0.05f
        )
        Modifier
            .background(glassBrush, RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, borderBrush), RoundedCornerShape(16.dp))
    } else {
        Modifier.background(mainTextColor.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(backgroundModifier)
            .clickable { onCheckedChange(!checked) },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 18.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = mainTextColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = mainTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = mainTextColor.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                modifier = Modifier.testTag(switchTestTag),
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = LiquidGlass.switchColors(isDarkTextEnabled, isLiquidGlassEnabled)
            )
        }
    }
}
