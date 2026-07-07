package com.example.androidlauncher.ui

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.androidlauncher.R
import com.example.androidlauncher.ui.LiquidGlass.designSurface
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Ein anbietbares Widget eines Providers (Label vorab geladen, Preview lazy pro Zeile). */
internal data class WidgetPickerEntry(
    val info: AppWidgetProviderInfo,
    val label: String,
)

/** Alle Widgets einer App, gruppiert für die aufklappbare Listendarstellung. */
internal data class WidgetPickerGroup(
    val packageName: String,
    val appLabel: String,
    val entries: List<WidgetPickerEntry>,
)

/** Lädt alle Homescreen-Widget-Provider, gruppiert nach App und alphabetisch sortiert. */
internal fun loadWidgetPickerGroups(context: Context): List<WidgetPickerGroup> {
    val pm = context.packageManager
    val providers = runCatching {
        AppWidgetManager.getInstance(context).installedProviders
    }.getOrDefault(emptyList())
    return providers
        .filter { it.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0 }
        .groupBy { it.provider.packageName }
        .map { (packageName, infos) ->
            val appLabel = runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            }.getOrDefault(packageName)
            WidgetPickerGroup(
                packageName = packageName,
                appLabel = appLabel,
                entries = infos
                    .map { info ->
                        WidgetPickerEntry(
                            info = info,
                            label = runCatching { info.loadLabel(pm) }.getOrNull() ?: appLabel,
                        )
                    }
                    .sortedBy { it.label.lowercase() },
            )
        }
        .sortedBy { it.appLabel.lowercase() }
}

/**
 * Widget-Auswahl (B1): listet alle installierten Homescreen-Widget-Provider,
 * gruppiert nach App; ein Tap auf ein Widget startet den Bind-Flow in der
 * MainActivity. Wird über das `WidgetPicker`-Overlay im MenuOverlay gerendert.
 */
@Composable
fun WidgetPickerSheet(
    onWidgetChosen: (AppWidgetProviderInfo) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val surfaceAccent = LocalColorTheme.current.menuSurfaceColor(isDarkTextEnabled)
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White

    // Provider-Liste einmal pro Öffnen off-main laden; null = noch am Laden.
    val groups by produceState<List<WidgetPickerGroup>?>(initialValue = null, context) {
        value = withContext(Dispatchers.Default) { loadWidgetPickerGroups(context) }
    }
    var expandedPackage by remember { mutableStateOf<String?>(null) }

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
                stringResource(R.string.widget_picker_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = mainTextColor
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close), tint = mainTextColor)
            }
        }

        when {
            groups == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = mainTextColor.copy(alpha = 0.6f))
            }

            groups.orEmpty().isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.widget_picker_empty),
                    color = mainTextColor.copy(alpha = 0.7f),
                    fontSize = 16.sp
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 32.dp, bottom = 8.dp)
            ) {
                items(groups.orEmpty(), key = { it.packageName }) { group ->
                    WidgetPickerAppRow(
                        group = group,
                        expanded = expandedPackage == group.packageName,
                        onToggle = {
                            expandedPackage =
                                if (expandedPackage == group.packageName) null else group.packageName
                        },
                        onWidgetChosen = onWidgetChosen,
                        mainTextColor = mainTextColor,
                        surfaceAccent = surfaceAccent,
                        isDarkTextEnabled = isDarkTextEnabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetPickerAppRow(
    group: WidgetPickerGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
    onWidgetChosen: (AppWidgetProviderInfo) -> Unit,
    mainTextColor: Color,
    surfaceAccent: Color,
    isDarkTextEnabled: Boolean,
) {
    val context = LocalContext.current
    val designStyle = LocalDesignStyle.current
    val backgroundModifier = Modifier.designSurface(
        designStyle, RoundedCornerShape(20.dp), isDarkTextEnabled, surfaceAccent,
        fillAlpha = 0.05f, glassStartAlpha = 0.10f, glassEndAlpha = 0.03f,
        borderWidth = 1.dp, borderStartAlpha = if (isDarkTextEnabled) 0.2f else 0.25f, borderEndAlpha = 0.05f
    )
    val appIcon by produceState<ImageBitmap?>(initialValue = null, group.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(group.packageName) }
                .getOrNull()
                ?.toSafeImageBitmap()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(backgroundModifier)
            .testTag("widget_picker_app_${group.packageName}"),
        color = Color.Transparent
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(vertical = 16.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                appIcon?.let {
                    Image(bitmap = it, contentDescription = null, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Text(
                    text = group.appLabel,
                    color = mainTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = group.entries.size.toString(),
                    color = mainTextColor.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
                Icon(
                    imageVector = if (expanded) {
                        Icons.Rounded.KeyboardArrowDown
                    } else {
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight
                    },
                    contentDescription = null,
                    tint = mainTextColor.copy(alpha = 0.4f)
                )
            }

            if (expanded) {
                group.entries.forEach { entry ->
                    WidgetPickerWidgetRow(
                        entry = entry,
                        onClick = { onWidgetChosen(entry.info) },
                        mainTextColor = mainTextColor,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun WidgetPickerWidgetRow(
    entry: WidgetPickerEntry,
    onClick: () -> Unit,
    mainTextColor: Color,
) {
    val context = LocalContext.current
    val preview by produceState<ImageBitmap?>(initialValue = null, entry.info) {
        value = withContext(Dispatchers.IO) {
            runCatching { entry.info.loadPreviewImage(context, 0) }
                .getOrNull()
                ?.toSafeImageBitmap()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("widget_picker_entry_${entry.info.provider.flattenToString()}")
    ) {
        Text(
            text = entry.label,
            color = mainTextColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
        Text(
            // minWidth/minHeight sind in px – fuer die dp-Anzeige zurueckrechnen.
            text = run {
                val dens = context.resources.displayMetrics.density
                "${(entry.info.minWidth / dens).roundToInt()} × " +
                    "${(entry.info.minHeight / dens).roundToInt()} dp"
            },
            color = mainTextColor.copy(alpha = 0.5f),
            fontSize = 13.sp
        )
        preview?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 140.dp),
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart
            )
        }
    }
}

/** Drawable → ImageBitmap; Drawables ohne intrinsische Größe liefern null statt zu werfen. */
private fun Drawable.toSafeImageBitmap(): ImageBitmap? =
    if (intrinsicWidth > 0 && intrinsicHeight > 0) {
        runCatching { toBitmap().asImageBitmap() }.getOrNull()
    } else {
        null
    }
