package com.example.androidlauncher.ui.settings

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Hand
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Shield
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Upload
import com.example.androidlauncher.ForegroundAppResolver
import com.example.androidlauncher.LauncherAccessibilityService
import com.example.androidlauncher.R
import com.example.androidlauncher.isDefaultLauncher
import com.example.androidlauncher.isNotificationServiceEnabled
import com.example.androidlauncher.openAccessibilitySettings
import com.example.androidlauncher.openNotificationSettings
import com.example.androidlauncher.ui.EditMenuItem
import com.example.androidlauncher.ui.EditSectionHeader
import com.example.androidlauncher.ui.EditToggleItem
import com.example.androidlauncher.ui.home.ActiveOverlay
import com.example.androidlauncher.ui.home.EditConfigActions
import com.example.androidlauncher.ui.home.EditConfigViewModel
import com.example.androidlauncher.ui.home.HomeViewModel
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle

/**
 * Kategorie-Seite „System" des Einstellungs-Hubs: Haptik, Backup (Export/Import),
 * System-Berechtigungen und App-Info.
 */
@Composable
fun SystemSettingsPage(
    actions: EditConfigActions
) {
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val editViewModel: EditConfigViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    val isHapticFeedbackEnabled by editViewModel.isHapticFeedbackEnabled.collectAsState(initial = true)

    // Haptik schreibt zusätzlich das System-Setting → ohne WRITE_SETTINGS-Berechtigung
    // stattdessen den System-Dialog öffnen (Verhalten wie zuvor im Bearbeiten-Menü).
    val onHapticFeedbackToggled: (Boolean) -> Unit = { enabled ->
        if (Settings.System.canWrite(context)) {
            editViewModel.setHapticFeedbackEnabled(enabled)
        } else {
            val writeSettingsIntent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = ("package:" + context.packageName).toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(writeSettingsIntent)
            Toast.makeText(
                context,
                context.getString(R.string.write_settings_permission_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val designStyle = LocalDesignStyle.current
    val surfaceAccent = LocalColorTheme.current.menuSurfaceColor(isDarkTextEnabled)
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White

    var isDefaultLauncherSet by remember { mutableStateOf(isDefaultLauncher(context)) }
    var isNotificationEnabled by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var isAccessibilityEnabled by remember {
        mutableStateOf(
            LauncherAccessibilityService.isAccessibilityServiceEnabled(context)
        )
    }
    var isUsageAccessEnabled by remember { mutableStateOf(ForegroundAppResolver.hasUsageAccess(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isDefaultLauncherSet = isDefaultLauncher(context)
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
                stringResource(R.string.section_system),
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = mainTextColor
            )
            IconButton(onClick = { homeViewModel.closeOverlay() }) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close), tint = mainTextColor)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 32.dp, bottom = 8.dp)
        ) {
            item {
                EditToggleItem(
                    icon = Lucide.Smartphone,
                    label = stringResource(R.string.haptic_feedback),
                    description = stringResource(R.string.haptic_feedback_desc),
                    checked = isHapticFeedbackEnabled,
                    onCheckedChange = { onHapticFeedbackToggled(it) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    switchTestTag = "haptic_feedback_switch"
                )
            }

            item {
                EditSectionHeader(
                    title = stringResource(R.string.section_backup),
                    mainTextColor = mainTextColor
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Download,
                    label = stringResource(R.string.backup_export),
                    onClick = { actions.exportBackup() },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "backup_export_item"
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Upload,
                    label = stringResource(R.string.backup_import),
                    onClick = { actions.importBackup() },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "backup_import_item"
                )
            }

            // Klartext-Hinweis: Exporte enthalten u. a. die Liste der ausgeblendeten
            // Apps unverschlüsselt (gerätegebundener Ciphertext wäre im Backup nutzlos).
            item {
                Text(
                    text = stringResource(R.string.backup_plaintext_hint),
                    fontSize = 12.sp,
                    color = mainTextColor.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                EditSectionHeader(
                    title = stringResource(R.string.section_permissions),
                    mainTextColor = mainTextColor
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.House,
                    label = stringResource(R.string.default_launcher),
                    onClick = { actions.openDefaultLauncherPrompt() },
                    statusLabel = if (isDefaultLauncherSet) {
                        stringResource(R.string.status_on)
                    } else {
                        stringResource(R.string.status_off)
                    },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "default_launcher_item"
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Bell,
                    label = stringResource(R.string.notifications_label),
                    onClick = { openNotificationSettings(context) },
                    statusLabel = if (isNotificationEnabled) {
                        stringResource(R.string.status_on)
                    } else {
                        stringResource(R.string.status_off)
                    },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Hand,
                    label = stringResource(R.string.accessibility_label),
                    onClick = { openAccessibilitySettings(context) },
                    statusLabel = if (isAccessibilityEnabled) {
                        stringResource(R.string.status_on)
                    } else {
                        stringResource(R.string.status_off)
                    },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled
                )
            }

            item {
                EditMenuItem(
                    icon = Lucide.Shield,
                    label = stringResource(R.string.usage_access_label),
                    onClick = { ForegroundAppResolver.openUsageAccessSettings(context) },
                    statusLabel = if (isUsageAccessEnabled) {
                        stringResource(R.string.status_on)
                    } else {
                        stringResource(R.string.status_off)
                    },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled
                )
            }

            item {
                EditMenuItem(
                    icon = Icons.Rounded.Info,
                    label = stringResource(R.string.label_info),
                    onClick = { homeViewModel.openOverlay(ActiveOverlay.Info) },
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    testTag = "system_info_item"
                )
            }
        }
    }
}
