package com.example.androidlauncher.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Sun
import com.example.androidlauncher.R
import com.example.androidlauncher.data.WeatherData
import com.example.androidlauncher.data.WeatherRepository
import com.example.androidlauncher.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Startbildschirm-Widgets (Uhr, Datum, Wetterzeile) samt zugehoeriger App-Starts
 * (A6-Split aus HomeScreen.kt; gleiches Paket, keine Aufrufer-Aenderungen).
 */

@SuppressLint("WrongConstant")
internal fun expandNotifications(context: Context) {
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val method = statusBarManager.getMethod("expandNotificationsPanel")
        method.invoke(statusBarService)
    } catch (_: Exception) {
    }
}

/**
 * Großes Uhrzeit-Element (eigenständig verschiebbar). [time] wird vom Aufrufer getickt.
 */
@Composable
fun ClockText(
    time: java.util.Date,
    isPreview: Boolean,
    returnIconPackage: String?,
    onAppLaunchForReturn: (String, Rect?) -> Unit
) {
    val context = LocalContext.current
    val fontSize = LocalFontSize.current
    val appFontWeight = LocalFontWeight.current
    val mainTextColor = LocalHomeTextColor.current

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val intSrcTime = remember { MutableInteractionSource() }
    val clockBounds = remember { mutableStateOf<Rect?>(null) }
    var clockPackage by remember { mutableStateOf<String?>(null) }

    val bounceScaleTime by animateFloatAsState(
        targetValue = when {
            !LocalAppCloseAnimationEnabled.current -> 1f
            returnIconPackage != null && returnIconPackage == clockPackage -> 1.08f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "ClockReturnBounce"
    )

    // Material-3-Expressive: Minutenwechsel per kurzem Crossfade statt hartem Swap.
    val animationsEnabled = LocalAnimationsEnabled.current
    Crossfade(
        targetState = timeFormat.format(time),
        animationSpec = tween(if (animationsEnabled) 220 else 0),
        label = "clockCrossfade",
        modifier = Modifier
            .onGloballyPositioned { clockBounds.value = it.boundsInRoot() }
            .graphicsLayer {
                scaleX = bounceScaleTime
                scaleY = bounceScaleTime
            }
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (!isPreview) {
                    Modifier
                        .bounceClick(intSrcTime)
                        .clickable(interactionSource = intSrcTime, indication = null) {
                            launchClockApp(context, clockBounds.value, onAppLaunchForReturn) { clockPackage = it }
                        }
                } else {
                    Modifier
                }
            )
    ) { timeStr ->
        Text(
            text = timeStr,
            // Seitliches/vertikales Polster INNERHALB des Clips, damit Glyphen-Überhänge
            // (kursive/dekorative Fonts, negatives letterSpacing) nicht abgeschnitten werden.
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 72.sp * fontSize.scale,
            fontWeight = appFontWeight.weight,
            letterSpacing = (-2).sp,
            lineHeight = 72.sp * fontSize.scale,
            // Entfernt das zusätzliche Font-Padding (enger Rahmen), behält aber die gewählte
            // App-Schriftart bei, indem auf den aktuellen LocalTextStyle gemergt wird.
            // Trim.Both passt die Box an die echten Glyphen an → hohe/dekorative Schriften
            // werden NICHT vom Clip abgeschnitten.
            style = LocalTextStyle.current.merge(
                TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    )
                )
            ),
            color = mainTextColor
        )
    }
}

/**
 * Datums-Element (eigenständig verschiebbar). [time] wird vom Aufrufer getickt.
 */
@Composable
fun DateText(
    time: java.util.Date,
    isPreview: Boolean,
    returnIconPackage: String?,
    onAppLaunchForReturn: (String, Rect?) -> Unit
) {
    val context = LocalContext.current
    val fontSize = LocalFontSize.current
    val appFontWeight = LocalFontWeight.current
    val mainTextColor = LocalHomeTextColor.current

    val dateFormat = remember { SimpleDateFormat("EEEE, d. MMMM", Locale.getDefault()) }
    val intSrcDate = remember { MutableInteractionSource() }
    val calendarBounds = remember { mutableStateOf<Rect?>(null) }
    var calendarPackage by remember { mutableStateOf<String?>(null) }

    val bounceScaleDate by animateFloatAsState(
        targetValue = when {
            !LocalAppCloseAnimationEnabled.current -> 1f
            returnIconPackage != null && returnIconPackage == calendarPackage -> 1.08f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "CalendarReturnBounce"
    )

    // Material-3-Expressive: Datumswechsel (zur Mitternacht) per Crossfade.
    val animationsEnabled = LocalAnimationsEnabled.current
    Crossfade(
        targetState = dateFormat.format(time),
        animationSpec = tween(if (animationsEnabled) 300 else 0),
        label = "dateCrossfade",
        modifier = Modifier
            .onGloballyPositioned { calendarBounds.value = it.boundsInRoot() }
            .graphicsLayer {
                scaleX = bounceScaleDate
                scaleY = bounceScaleDate
            }
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (!isPreview) {
                    Modifier
                        .bounceClick(intSrcDate)
                        .clickable(interactionSource = intSrcDate, indication = null) {
                            launchCalendarApp(
                                context,
                                calendarBounds.value,
                                onAppLaunchForReturn
                            ) { calendarPackage = it }
                        }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) { dateStr ->
        Text(
            text = dateStr,
            fontSize = 18.sp * fontSize.scale,
            fontWeight = appFontWeight.weight,
            lineHeight = 18.sp * fontSize.scale,
            // Trim.Both passt die Box an die echten Glyphen an (kein Abschneiden).
            style = LocalTextStyle.current.merge(
                TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    )
                )
            ),
            color = mainTextColor.copy(alpha = 0.7f)
        )
    }
}

/**
 * Schmale Wetterzeile unter Uhr/Datum: ein Symbol plus aktuelle Temperatur.
 *
 * Holt den groben Standort über [WeatherRepository] und ruft das Wetter von Open-Meteo ab.
 * Solange kein Standort/keine Daten vorliegen (z. B. Berechtigung noch nicht erteilt),
 * wird in kurzen Abständen erneut versucht; danach im 30-Minuten-Takt aktualisiert.
 * Im Vorschau-/Edit-Modus wird ein statischer Platzhalter gezeigt (kein Netzwerkzugriff).
 */
@Composable
fun WeatherRow(isPreview: Boolean = false) {
    val context = LocalContext.current
    val fontSize = LocalFontSize.current
    val appFontWeight = LocalFontWeight.current
    val mainTextColor = LocalHomeTextColor.current

    // Startwert aus dem prozessweiten Cache: zeigt beim Zurückkehren aus dem App-Drawer
    // sofort den letzten Wert, statt kurz zu verschwinden.
    val lifecycleOwner = LocalLifecycleOwner.current
    val weather by produceState<WeatherData?>(initialValue = WeatherRepository.cached, isPreview) {
        if (isPreview) return@produceState
        val repo = WeatherRepository(context)
        val refreshIntervalMs = 30 * 60_000L
        // Nur bei sichtbarem Launcher abrufen: im Hintergrund pausiert der Loop und
        // prüft beim nächsten ON_START zuerst die Drossel (WeatherRepository.shouldRefresh).
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                if (WeatherRepository.shouldRefresh(refreshIntervalMs)) {
                    // GPS nur, falls die Berechtigung ohnehin erteilt ist; sonst grobe Position
                    // über die IP-Adresse (ohne Standortdienst/Berechtigung).
                    val location = repo.awaitLocation() ?: repo.ipLocation()
                    if (location != null) {
                        repo.fetch(location.first, location.second)?.let { value = it }
                    }
                } else {
                    // Cache noch aktuell – übernehmen, kein erneuter Netzwerkabruf.
                    value = WeatherRepository.cached
                }
                // Schneller erneut versuchen, solange noch keine Daten vorliegen,
                // sonst regulär alle 30 Minuten aktualisieren.
                delay(if (value == null) 20_000L else refreshIntervalMs)
            }
        }
    }

    val icon: ImageVector
    val temperatureText: String
    if (isPreview) {
        icon = Lucide.Sun
        temperatureText = "21°"
    } else {
        val data = weather ?: return
        icon = WeatherRepository.iconFor(data.weatherCode)
        temperatureText = "${data.temperatureC}°"
    }

    // Material-3-Expressive: Wetterwert wechselt per Crossfade statt hartem Swap.
    val animationsEnabled = LocalAnimationsEnabled.current
    Crossfade(
        targetState = icon to temperatureText,
        animationSpec = tween(if (animationsEnabled) 300 else 0),
        label = "weatherCrossfade",
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) { (stateIcon, stateTemp) ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = stateIcon,
                contentDescription = null,
                tint = mainTextColor.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp * fontSize.scale)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stateTemp,
                fontSize = 18.sp * fontSize.scale,
                fontWeight = appFontWeight.weight,
                color = mainTextColor.copy(alpha = 0.7f)
            )
        }
    }
}

private fun launchClockApp(
    context: Context,
    bounds: Rect?,
    onAppLaunchForReturn: (String, Rect?) -> Unit,
    onPackageFound: (String) -> Unit
) {
    val pm = context.packageManager
    // Bekannte Uhr-Pakete + Auswahl liegen framework-frei in LauncherLogic (unit-getestet).
    var foundPkg: String? = com.example.androidlauncher.LauncherLogic.firstLaunchablePackage(
        com.example.androidlauncher.LauncherLogic.KNOWN_CLOCK_PACKAGES
    ) { pkg -> pm.getLaunchIntentForPackage(pkg) != null }

    if (foundPkg == null) {
        try {
            val stdIntent = Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.APP_CLOCK")
            val res = pm.resolveActivity(stdIntent, 0)
            if (res != null) foundPkg = res.activityInfo.packageName
        } catch (_: Exception) {
        }
    }
    if (foundPkg == null) {
        try {
            val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            val res = pm.resolveActivity(alarmIntent, 0)
            if (res != null) foundPkg = res.activityInfo.packageName
        } catch (_: Exception) {
        }
    }

    if (foundPkg != null) {
        onPackageFound(foundPkg)
        val launchIntent = pm.getLaunchIntentForPackage(foundPkg)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            onAppLaunchForReturn(foundPkg, bounds)
            return
        }
    }

    try {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory("android.intent.category.APP_CLOCK")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, context.getString(R.string.clock_app_not_found), Toast.LENGTH_SHORT).show()
    }
}

private fun launchCalendarApp(
    context: Context,
    bounds: Rect?,
    onAppLaunchForReturn: (String, Rect?) -> Unit,
    onPackageFound: (String) -> Unit
) {
    val pm = context.packageManager
    var calendarIntent = Intent(Intent.ACTION_VIEW).apply {
        data = CalendarContract.CONTENT_URI.buildUpon()
            .appendPath("time")
            .appendPath(System.currentTimeMillis().toString())
            .build()
    }
    val res = pm.resolveActivity(calendarIntent, 0)
    if (res == null) {
        calendarIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
    }

    val foundPkg = pm.resolveActivity(calendarIntent, 0)?.activityInfo?.packageName
    if (foundPkg != null) {
        onPackageFound(foundPkg)
        val launchIntent = pm.getLaunchIntentForPackage(foundPkg)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            onAppLaunchForReturn(foundPkg, bounds)
        } else {
            calendarIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            onAppLaunchForReturn(foundPkg, bounds)
        }
        return
    }

    try {
        calendarIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(calendarIntent)
    } catch (_: Exception) {
        val selectorIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(selectorIntent)
        } catch (_: Exception) {
        }
    }
}
