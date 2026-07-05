package com.example.androidlauncher.ui

import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.androidlauncher.LauncherAccessibilityService
import com.example.androidlauncher.R
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.AppLockManager
import com.example.androidlauncher.data.DesignStyle
import com.example.androidlauncher.openAccessibilitySettings
import com.example.androidlauncher.ui.LiquidGlass.designSurface
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle
import com.example.androidlauncher.ui.theme.LocalFontWeight

/**
 * Konfigurationsmenü der App-Sperre: Apps auswählen, die geschützt werden sollen, sowie
 * Code (PIN/Muster) festlegen und Biometrie aktivieren. Struktur an [HiddenAppsMenu] angelehnt.
 */
@Composable
fun AppLockMenu(
    apps: List<AppInfo>,
    lockedPackages: Set<String>,
    lockType: String,
    biometricEnabled: Boolean,
    onToggleLocked: (String) -> Unit,
    onSetSecret: (type: String, token: String) -> Unit,
    onClearSecret: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val designStyle = LocalDesignStyle.current
    val surfaceAccent = LocalColorTheme.current.menuSurfaceColor(isDarkTextEnabled)
    val accentColor = LocalColorTheme.current.accentColor(isDarkTextEnabled)
    val fontWeight = LocalFontWeight.current
    val mainTextColor = LiquidGlass.mainTextColor(isDarkTextEnabled)
    val context = LocalContext.current

    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    // Die Durchsetzung der Sperre läuft über den Accessibility-Service. Ist er aus, greift
    // keine Sperre – Status bei jeder Rückkehr (z. B. aus den Einstellungen) neu prüfen.
    var isAccessibilityEnabled by remember {
        mutableStateOf(LauncherAccessibilityService.isAccessibilityServiceEnabled(context))
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isAccessibilityEnabled = LauncherAccessibilityService.isAccessibilityServiceEnabled(context)
    }

    var searchQuery by remember { mutableStateOf("") }
    var showSetup by remember { mutableStateOf(false) }
    // Paket, dessen Aktivierung nach erfolgreichem Setup ausgeführt werden soll.
    var pendingTogglePackage by remember { mutableStateOf<String?>(null) }

    val hasSecret = lockType != "none"

    if (showSetup) {
        LockSecretSetup(
            mainTextColor = mainTextColor,
            accentColor = accentColor,
            surfaceAccent = surfaceAccent,
            designStyle = designStyle,
            isDarkTextEnabled = isDarkTextEnabled,
            onCancel = {
                showSetup = false
                pendingTogglePackage = null
            },
            onConfirm = { type, secret ->
                onSetSecret(type, AppLockManager.hashSecret(secret))
                pendingTogglePackage?.let { onToggleLocked(it) }
                pendingTogglePackage = null
                showSetup = false
            }
        )
        return
    }

    val sortedApps = remember(apps) { apps.sortedBy { it.label.lowercase() } }
    val filteredApps = remember(sortedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            sortedApps
        } else {
            sortedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
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
                stringResource(R.string.app_lock),
                fontSize = 28.sp,
                fontWeight = fontWeight.weight,
                color = mainTextColor
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close), tint = mainTextColor)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.app_lock_desc),
            color = mainTextColor.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
        Spacer(Modifier.height(16.dp))

        if (!isAccessibilityEnabled) {
            AccessibilityHintBanner(
                mainTextColor = mainTextColor,
                accentColor = accentColor,
                designStyle = designStyle,
                isDarkTextEnabled = isDarkTextEnabled,
                onActivate = { openAccessibilitySettings(context) }
            )
            Spacer(Modifier.height(16.dp))
        }

        // Code-Verwaltung
        SettingsActionRow(
            label = if (hasSecret) {
                val typeLabel = if (lockType == "pattern") stringResource(R.string.lock_pattern) else "PIN"
                stringResource(R.string.lock_change_code, typeLabel)
            } else {
                stringResource(R.string.lock_set_code)
            },
            mainTextColor = mainTextColor,
            designStyle = designStyle,
            surfaceAccent = surfaceAccent,
            isDarkTextEnabled = isDarkTextEnabled,
            onClick = { showSetup = true }
        )

        if (hasSecret) {
            Spacer(Modifier.height(8.dp))
            // Biometrie-Schalter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .designSurface(
                        designStyle, RoundedCornerShape(20.dp), isDarkTextEnabled, surfaceAccent,
                        fillAlpha = 0.03f, glassStartAlpha = 0.06f, glassEndAlpha = 0.02f,
                        borderWidth = 1.dp, borderStartAlpha = 0.15f, borderEndAlpha = 0.03f
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.lock_unlock_biometric), color = mainTextColor, fontSize = 15.sp)
                    if (!biometricAvailable) {
                        Text(
                            stringResource(R.string.lock_no_biometric),
                            color = mainTextColor.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
                Switch(
                    checked = biometricEnabled && biometricAvailable,
                    enabled = biometricAvailable,
                    onCheckedChange = { onToggleBiometric(it) },
                    colors = LiquidGlass.switchColors(isDarkTextEnabled, designStyle.isGlassLike)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

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

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(items = filteredApps, key = { it.packageName }) { app ->
                LockedAppRow(
                    app = app,
                    isLocked = app.packageName in lockedPackages,
                    onToggle = {
                        if (!hasSecret && app.packageName !in lockedPackages) {
                            // Beim ersten Aktivieren zuerst Code einrichten.
                            pendingTogglePackage = app.packageName
                            showSetup = true
                        } else {
                            onToggleLocked(app.packageName)
                        }
                    },
                    mainTextColor = mainTextColor,
                    accentColor = accentColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled
                )
            }
        }
    }
}

/**
 * Einrichtungs-Flow für den Sperr-Code: erst Typ wählen (PIN/Muster), dann zweimal eingeben.
 * Stimmen beide Eingaben überein, wird [onConfirm] mit Typ und Klartext-Geheimnis aufgerufen.
 */
@Composable
private fun LockSecretSetup(
    mainTextColor: Color,
    accentColor: Color,
    surfaceAccent: Color,
    designStyle: DesignStyle,
    isDarkTextEnabled: Boolean,
    onCancel: () -> Unit,
    onConfirm: (type: String, secret: String) -> Unit
) {
    // null = Typ-Auswahl, sonst "pin"/"pattern"
    var type by remember { mutableStateOf<String?>(null) }
    var firstEntry by remember { mutableStateOf<String?>(null) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    // Fehler-/Hinweistexte vorab auflösen, da sie in Laufzeit-Callbacks (nicht in der
    // Composition) verwendet werden, wo stringResource() nicht aufgerufen werden darf.
    val minPointsMessage = stringResource(R.string.lock_min_4_points)
    val patternMismatchMessage = stringResource(R.string.lock_pattern_mismatch)
    val minDigitsMessage = stringResource(R.string.lock_min_4_digits)
    val pinMismatchMessage = stringResource(R.string.lock_pin_mismatch)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.lock_set_code), fontSize = 24.sp, color = mainTextColor)
            IconButton(onClick = onCancel) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cancel), tint = mainTextColor)
            }
        }

        Spacer(Modifier.height(24.dp))

        when (type) {
            null -> {
                Text(
                    stringResource(R.string.lock_choose_method),
                    color = mainTextColor.copy(alpha = 0.7f),
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(20.dp))
                SettingsActionRow(
                    label = stringResource(R.string.lock_pin_code),
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    onClick = { type = "pin" }
                )
                Spacer(Modifier.height(12.dp))
                SettingsActionRow(
                    label = stringResource(R.string.lock_pattern),
                    mainTextColor = mainTextColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled,
                    onClick = { type = "pattern" }
                )
            }
            "pattern" -> {
                Text(
                    if (firstEntry == null) {
                        stringResource(
                            R.string.lock_draw_pattern
                        )
                    } else {
                        stringResource(R.string.lock_confirm_pattern)
                    },
                    color = error?.let { Color(0xFFE0584F) } ?: mainTextColor,
                    fontSize = 16.sp
                )
                error?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = Color(0xFFE0584F), fontSize = 13.sp)
                }
                Spacer(Modifier.height(28.dp))
                PatternLockView(
                    onComplete = { entered ->
                        handleEntry(
                            entered = entered,
                            firstEntry = firstEntry,
                            minLength = 4,
                            tooShortMessage = minPointsMessage,
                            onFirst = {
                                firstEntry = it
                                error = null
                            },
                            onMismatch = {
                                firstEntry = null
                                error = patternMismatchMessage
                            },
                            onTooShort = { error = it },
                            onMatch = { onConfirm("pattern", it) }
                        )
                    },
                    nodeColor = mainTextColor,
                    lineColor = accentColor,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }
            else -> { // "pin"
                Text(
                    if (firstEntry == null) {
                        stringResource(
                            R.string.lock_enter_pin
                        )
                    } else {
                        stringResource(R.string.lock_confirm_pin)
                    },
                    color = mainTextColor,
                    fontSize = 16.sp
                )
                error?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = Color(0xFFE0584F), fontSize = 13.sp)
                }
                Spacer(Modifier.height(28.dp))
                PinPad(
                    value = pin,
                    onValueChange = { pin = it },
                    maxLength = 12,
                    textColor = mainTextColor,
                    accentColor = accentColor,
                    designStyle = designStyle,
                    surfaceAccent = surfaceAccent,
                    isDarkTextEnabled = isDarkTextEnabled
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .clip(RoundedCornerShape(28.dp))
                        .designSurface(
                            designStyle, RoundedCornerShape(28.dp), isDarkTextEnabled, accentColor,
                            fillAlpha = 0.18f, glassStartAlpha = 0.2f, glassEndAlpha = 0.08f,
                            borderWidth = 1.dp, borderStartAlpha = 0.25f, borderEndAlpha = 0.1f
                        )
                        .clickable(enabled = pin.isNotEmpty()) {
                            handleEntry(
                                entered = pin,
                                firstEntry = firstEntry,
                                minLength = 4,
                                tooShortMessage = minDigitsMessage,
                                onFirst = {
                                    firstEntry = it
                                    pin = ""
                                    error = null
                                },
                                onMismatch = {
                                    firstEntry = null
                                    pin = ""
                                    error = pinMismatchMessage
                                },
                                onTooShort = {
                                    pin = ""
                                    error = it
                                },
                                onMatch = { onConfirm("pin", it) }
                            )
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (firstEntry == null) stringResource(R.string.next) else stringResource(R.string.confirm),
                        color = mainTextColor,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

/** Gemeinsame Logik für Erst-/Bestätigungseingabe von PIN und Muster. */
internal inline fun handleEntry(
    entered: String,
    firstEntry: String?,
    minLength: Int,
    tooShortMessage: String,
    onTooShort: (String) -> Unit,
    onFirst: (String) -> Unit,
    onMismatch: () -> Unit,
    onMatch: (String) -> Unit
) {
    // Punkte des Musters sind komma-getrennt; PIN ist eine Ziffernfolge.
    val length = if (entered.contains(",")) entered.split(",").size else entered.length
    if (length < minLength) {
        onTooShort(tooShortMessage)
        return
    }
    if (firstEntry == null) {
        onFirst(entered)
    } else if (firstEntry == entered) {
        onMatch(entered)
    } else {
        onMismatch()
    }
}

/**
 * Hinweis-Banner, das erscheint, solange der Accessibility-Service deaktiviert ist. Ohne ihn
 * kann die Sperre nicht durchgesetzt werden. Ein Tippen öffnet die Bedienungshilfen-Einstellungen.
 */
@Composable
private fun AccessibilityHintBanner(
    mainTextColor: Color,
    accentColor: Color,
    designStyle: DesignStyle,
    isDarkTextEnabled: Boolean,
    onActivate: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .designSurface(
                designStyle, RoundedCornerShape(20.dp), isDarkTextEnabled, accentColor,
                fillAlpha = 0.12f, glassStartAlpha = 0.16f, glassEndAlpha = 0.06f,
                borderWidth = 1.dp, borderStartAlpha = 0.3f, borderEndAlpha = 0.1f
            )
            .clickable { onActivate() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Warning, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.lock_accessibility_required), color = mainTextColor, fontSize = 15.sp)
                Text(
                    stringResource(R.string.lock_accessibility_desc),
                    color = mainTextColor.copy(alpha = 0.65f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    label: String,
    mainTextColor: Color,
    designStyle: DesignStyle,
    surfaceAccent: Color,
    isDarkTextEnabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .designSurface(
                designStyle, RoundedCornerShape(20.dp), isDarkTextEnabled, surfaceAccent,
                fillAlpha = 0.05f, glassStartAlpha = 0.1f, glassEndAlpha = 0.03f,
                borderWidth = 1.dp, borderStartAlpha = 0.18f, borderEndAlpha = 0.05f
            )
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = mainTextColor, fontSize = 15.sp)
        }
    }
}

@Composable
private fun LockedAppRow(
    app: AppInfo,
    isLocked: Boolean,
    onToggle: () -> Unit,
    mainTextColor: Color,
    accentColor: Color,
    designStyle: DesignStyle,
    surfaceAccent: Color,
    isDarkTextEnabled: Boolean
) {
    val itemBackgroundModifier = Modifier.designSurface(
        designStyle, RoundedCornerShape(20.dp), isDarkTextEnabled, surfaceAccent,
        fillAlpha = 0.03f, glassStartAlpha = 0.06f, glassEndAlpha = 0.02f,
        borderWidth = 1.dp, borderStartAlpha = 0.15f, borderEndAlpha = 0.03f
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(itemBackgroundModifier)
            .clickable { onToggle() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isLocked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = accentColor,
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
        }
    }
}
