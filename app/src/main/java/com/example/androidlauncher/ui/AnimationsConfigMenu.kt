package com.example.androidlauncher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Maximize2
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.Minimize2
import com.composables.icons.lucide.Sparkles
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle
import com.example.androidlauncher.ui.theme.LocalFontWeight

/**
 * Untermenü zum gezielten Ein-/Ausschalten einzelner Animationsarten.
 *
 * Oben steht der Master-Schalter „Alle Animationen". Ist er aus, sind die
 * Einzelschalter ausgegraut und ohne Wirkung (die eigentliche UND-Verknüpfung
 * mit dem Master passiert in AndroidLauncherTheme).
 */
@Composable
fun AnimationsConfigMenu(
    isAnimationsEnabled: Boolean,
    onAnimationsToggled: (Boolean) -> Unit,
    isAppOpenAnimationEnabled: Boolean,
    onAppOpenAnimationToggled: (Boolean) -> Unit,
    isAppCloseAnimationEnabled: Boolean,
    onAppCloseAnimationToggled: (Boolean) -> Unit,
    isMenuAnimationEnabled: Boolean,
    onMenuAnimationToggled: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val designStyle = LocalDesignStyle.current
    val surfaceAccent = LocalColorTheme.current.menuSurfaceColor(isDarkTextEnabled)
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val fontWeight = LocalFontWeight.current
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("animations_config_menu")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Animationen", fontSize = 28.sp, fontWeight = fontWeight.weight, color = mainTextColor)
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = mainTextColor)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 32.dp, bottom = 8.dp)
        ) {
            // Master-Schalter.
            item {
                EditToggleItem(
                    icon = Lucide.Sparkles,
                    label = "Alle Animationen",
                    description = "Hauptschalter für sämtliche Animationen",
                    checked = isAnimationsEnabled,
                    onCheckedChange = { onAnimationsToggled(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "animations_master_switch"
                )
            }

            item {
                Text(
                    "Einzelne Animationen",
                    color = mainTextColor.copy(alpha = if (isAnimationsEnabled) 0.7f else 0.35f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            item {
                EditToggleItem(
                    icon = Lucide.Maximize2,
                    label = "App öffnen",
                    description = "Aufzieh-Animation beim Starten einer App",
                    checked = isAppOpenAnimationEnabled,
                    onCheckedChange = { onAppOpenAnimationToggled(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "animation_app_open_switch",
                    enabled = isAnimationsEnabled
                )
            }

            item {
                EditToggleItem(
                    icon = Lucide.Minimize2,
                    label = "App schließen / Rückkehr",
                    description = "Zurückschrumpfen zum Icon und Bounce-Effekt",
                    checked = isAppCloseAnimationEnabled,
                    onCheckedChange = { onAppCloseAnimationToggled(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "animation_app_close_switch",
                    enabled = isAnimationsEnabled
                )
            }

            item {
                EditToggleItem(
                    icon = Lucide.Menu,
                    label = "Menüs & Einstellungsmenü",
                    description = "Übergänge der Menüs und das kreisförmige Einstellungsmenü",
                    checked = isMenuAnimationEnabled,
                    onCheckedChange = { onMenuAnimationToggled(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "animation_menus_switch",
                    enabled = isAnimationsEnabled
                )
            }
        }
    }
}
