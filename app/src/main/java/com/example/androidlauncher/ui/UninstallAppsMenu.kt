package com.example.androidlauncher.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.example.androidlauncher.R
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.AppRepository
import com.example.androidlauncher.data.DesignStyle
import com.example.androidlauncher.ui.LiquidGlass.designSurface
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle
import com.example.androidlauncher.ui.theme.LocalFontWeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Akzentfarbe für destruktive Aktionen (wie in [IconConfigMenu]). */
private val DestructiveColor = Color(0xFFEF5350)

/**
 * Menü zum Deinstallieren installierter Apps.
 *
 * Bietet Einzel-Deinstallation per Swipe oder Lösch-Button sowie Mehrfachauswahl
 * per Checkbox mit einem Sammel-Button. Vor jeder Deinstallation erscheint ein
 * In-App-Bestätigungsdialog. System-Apps werden ausgeblendet (nur deinstallierbare
 * Nutzer-Apps und aktualisierte System-Apps erscheinen).
 *
 * Die eigentliche Deinstallation läuft über das System ([Intent.ACTION_DELETE]).
 * Bei Mehrfachauswahl werden die Apps nacheinander abgearbeitet, sobald der
 * jeweilige System-Dialog geschlossen wird (siehe [LifecycleEventEffect]).
 * Die Liste aktualisiert sich anschließend automatisch über den
 * `PACKAGE_REMOVED`-Receiver in der MainActivity.
 *
 * @param apps Alle aktuell bekannten Apps (System-Apps werden hier herausgefiltert).
 * @param onClose Callback zum Schließen des Menüs.
 */
@Composable
fun UninstallAppsMenu(
    apps: List<AppInfo>,
    onRefreshApps: (String?) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val designStyle = LocalDesignStyle.current
    val surfaceAccent = LocalColorTheme.current.menuSurfaceColor(isDarkTextEnabled)
    val fontWeight = LocalFontWeight.current
    val mainTextColor = LiquidGlass.mainTextColor(isDarkTextEnabled)
    val secondaryTextColor = LiquidGlass.secondaryTextColor(isDarkTextEnabled)

    // System-Apps off-main-thread herausfiltern.
    val uninstallableApps by produceState(initialValue = emptyList<AppInfo>(), apps.toList()) {
        value = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val filtered = apps.filter { app ->
                try {
                    val flags = pm.getApplicationInfo(app.packageName, 0).flags
                    val isSystem = (flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isUpdatedSystem = (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    // Eigene App hier separat behandeln (s.u.); reine System-Apps ausblenden,
                    // aktualisierte System-Apps (deinstallierbar) erlauben.
                    app.packageName != context.packageName && (!isSystem || isUpdatedSystem)
                } catch (_: Exception) {
                    false
                }
            }
            // Rethrone selbst soll AUSSCHLIESSLICH hier (im Deinstallieren-Menü) erscheinen –
            // in der allgemeinen App-Liste ist es als Standard-Launcher ausgeblendet.
            // Eintrag inkl. Icon eigenständig aufbauen, da er nicht in `apps` enthalten ist.
            val ownEntry = try {
                val ai = pm.getApplicationInfo(context.packageName, 0)
                val ownIcon = AppRepository(context).loadIcon(context.packageName)
                AppInfo(
                    label = pm.getApplicationLabel(ai).toString(),
                    packageName = context.packageName,
                    iconBitmap = ownIcon
                )
            } catch (_: Exception) {
                null
            }
            if (ownEntry != null) filtered + ownEntry else filtered
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(uninstallableApps, searchQuery) {
        if (searchQuery.isBlank()) {
            uninstallableApps
        } else {
            uninstallableApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

    val selectedPackages = remember { mutableStateListOf<String>() }
    // Entfernt Auswahl-Einträge, die nicht mehr deinstallierbar sind (z.B. nach Refresh).
    LaunchedEffect(uninstallableApps) {
        val available = uninstallableApps.mapTo(HashSet()) { it.packageName }
        selectedPackages.retainAll { it in available }
    }

    // Pakete, für die der Bestätigungsdialog gezeigt wird (1 = Einzel, N = Sammel).
    var pendingConfirmation by remember { mutableStateOf<List<String>?>(null) }

    // Sequentielle Abarbeitung der Deinstallations-Warteschlange.
    var uninstallQueue by remember { mutableStateOf<List<String>>(emptyList()) }
    var awaitingResume by remember { mutableStateOf(false) }

    fun fireUninstall(pkg: String) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.fromParts("package", pkg, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        } catch (_: Exception) {
            Toast.makeText(context, context.getString(R.string.uninstall_failed), Toast.LENGTH_SHORT).show()
        }
    }

    fun processNext() {
        val next = uninstallQueue.firstOrNull()
        if (next == null) {
            awaitingResume = false
            return
        }
        awaitingResume = true
        fireUninstall(next)
    }

    fun startUninstall(packages: List<String>) {
        if (packages.isEmpty()) return
        selectedPackages.removeAll(packages)
        uninstallQueue = packages
        processNext()
    }

    // Nach Rückkehr aus dem System-Dialog das nächste Paket abarbeiten und die
    // App-Liste aktualisieren (entfernt das gerade deinstallierte Paket).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (awaitingResume) {
            val justAttempted = uninstallQueue.firstOrNull()
            uninstallQueue = uninstallQueue.drop(1)
            scope.launch {
                delay(500) // System die Deinstallation abschließen lassen
                onRefreshApps(justAttempted)
            }
            processNext()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("uninstall_apps_menu")
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
                stringResource(R.string.uninstall_apps),
                fontSize = 24.sp,
                fontWeight = fontWeight.weight,
                color = mainTextColor
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close), tint = mainTextColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Suchleiste mit Liquid-Glass-Optik (wie in IconConfigMenu).
        val searchBarModifier = Modifier.designSurface(
            designStyle, RoundedCornerShape(16.dp), isDarkTextEnabled, surfaceAccent,
            fillAlpha = 0.1f, glassStartAlpha = 0.10f, glassEndAlpha = 0.03f,
            borderWidth = 1.dp, borderStartAlpha = 0.2f, borderEndAlpha = 0.05f
        )

        Box(modifier = Modifier.fillMaxWidth().then(searchBarModifier).padding(horizontal = 16.dp, vertical = 12.dp)) {
            StableSearchFieldContent(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = stringResource(R.string.search_apps),
                textStyle = androidx.compose.ui.text.TextStyle(color = mainTextColor, fontSize = 16.sp),
                textColor = mainTextColor,
                placeholderColor = mainTextColor.copy(alpha = 0.4f),
                leadingIconTint = mainTextColor.copy(alpha = 0.4f),
                leadingIconSize = 20.dp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(items = filteredApps, key = { _, app -> app.packageName }) { _, app ->
                UninstallAppRow(
                    app = app,
                    isSelected = app.packageName in selectedPackages,
                    onToggleSelected = {
                        if (app.packageName in selectedPackages) {
                            selectedPackages.remove(app.packageName)
                        } else {
                            selectedPackages.add(app.packageName)
                        }
                    },
                    onRequestUninstall = { pendingConfirmation = listOf(app.packageName) },
                    mainTextColor = mainTextColor,
                    secondaryTextColor = secondaryTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled
                )
            }
        }

        // Sammel-Aktionsleiste bei aktiver Mehrfachauswahl.
        if (selectedPackages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { selectedPackages.clear() },
                    modifier = Modifier.testTag("uninstall_clear_selection")
                ) {
                    Text(stringResource(R.string.clear_selection), color = mainTextColor.copy(alpha = 0.7f))
                }
                Button(
                    onClick = { pendingConfirmation = selectedPackages.toList() },
                    modifier = Modifier.weight(1f).testTag("uninstall_selected_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DestructiveColor,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Lucide.Trash2, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.uninstall_count, selectedPackages.size))
                }
            }
        }
    }

    pendingConfirmation?.let { packages ->
        val title: String
        val message: String
        if (packages.size == 1) {
            val label = filteredApps.firstOrNull { it.packageName == packages.first() }?.label
                ?: uninstallableApps.firstOrNull { it.packageName == packages.first() }?.label
                ?: packages.first()
            title = stringResource(R.string.uninstall_one_title)
            message = stringResource(R.string.uninstall_one_message, label)
        } else {
            title = stringResource(R.string.uninstall_many_title)
            message = stringResource(R.string.uninstall_many_message, packages.size)
        }

        AlertDialog(
            onDismissRequest = { pendingConfirmation = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toUninstall = packages
                        pendingConfirmation = null
                        startUninstall(toUninstall)
                    },
                    modifier = Modifier.testTag("uninstall_confirm_button")
                ) {
                    Text(stringResource(R.string.uninstall), color = DestructiveColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirmation = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UninstallAppRow(
    app: AppInfo,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    onRequestUninstall: () -> Unit,
    mainTextColor: Color,
    secondaryTextColor: Color,
    designStyle: DesignStyle,
    surfaceAccent: Color,
    isDarkTextEnabled: Boolean
) {
    // Swipe löst die Bestätigung aus; die Zeile bleibt erhalten (confirmValueChange == false),
    // bis die echte Deinstallation über das System erfolgt ist.
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRequestUninstall()
            }
            false
        }
    )

    val itemBackgroundModifier = Modifier.designSurface(
        designStyle, RoundedCornerShape(20.dp), isDarkTextEnabled, surfaceAccent,
        fillAlpha = 0.03f, glassStartAlpha = 0.06f, glassEndAlpha = 0.02f,
        borderWidth = 1.dp, borderStartAlpha = 0.15f, borderEndAlpha = 0.03f
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.clip(RoundedCornerShape(20.dp)),
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            // Nur während eines aktiven Swipes zeichnen – sonst würde der
            // Hintergrund durch den durchscheinenden Zeilen-Vordergrund scheinen.
            val isSwiping = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            if (isSwiping) {
                val revealModifier = Modifier.designSurface(
                    designStyle, RoundedCornerShape(20.dp), isDarkTextEnabled, surfaceAccent,
                    fillAlpha = 0.10f, glassStartAlpha = 0.18f, glassEndAlpha = 0.08f,
                    borderWidth = 1.dp, borderStartAlpha = 0.2f, borderEndAlpha = 0.05f
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .then(revealModifier)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        Lucide.Trash2,
                        contentDescription = null,
                        tint = DestructiveColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("uninstall_item_${app.packageName}")
                .clip(RoundedCornerShape(20.dp))
                .then(itemBackgroundModifier)
                .clickable { onToggleSelected() },
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelected() },
                    modifier = Modifier.testTag("uninstall_checkbox_${app.packageName}"),
                    colors = CheckboxDefaults.colors(
                        checkedColor = DestructiveColor,
                        uncheckedColor = mainTextColor.copy(alpha = 0.5f),
                        checkmarkColor = Color.White
                    )
                )
                AppIconView(app, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    app.label,
                    color = mainTextColor,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onRequestUninstall,
                    modifier = Modifier.testTag("uninstall_button_${app.packageName}")
                ) {
                    Icon(
                        Lucide.Trash2,
                        contentDescription = stringResource(R.string.uninstall),
                        tint = DestructiveColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
