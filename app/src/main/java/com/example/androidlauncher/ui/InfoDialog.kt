package com.example.androidlauncher.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ArrowUp
import com.composables.icons.lucide.Ban
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.Hand
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Maximize2
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Trash2
import com.example.androidlauncher.R
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalFontWeight
import com.example.androidlauncher.ui.theme.RethroneSprings

@Composable
fun InfoDialog(
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val colorTheme = LocalColorTheme.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val fontWeight = LocalFontWeight.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val secondaryTextColor = if (isDarkTextEnabled) Color(0xFF2B2B2B) else Color.White.copy(alpha = 0.72f)
    val accentColor = remember(colorTheme, isDarkTextEnabled) {
        colorTheme.accentColor(isDarkTextEnabled)
    }
    val scrollState = rememberScrollState()

    // Hintergrund (Wallpaper + Theme-Verlauf) stellt das gemeinsame MenuOverlay bereit –
    // identisch zu allen anderen Einstellungsmenüs (kein eigener Verlauf, kein Hero-Motiv).
    Box(modifier = Modifier.fillMaxSize()) {
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
                    text = stringResource(R.string.info_title),
                    fontSize = 28.sp,
                    fontWeight = fontWeight.weight,
                    color = mainTextColor
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close), tint = mainTextColor)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // App Info
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val versionName = packageInfo.versionName ?: "1.0"

                @Suppress("DEPRECATION")
                val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    packageInfo.versionCode.toLong()
                }

                InfoSection(
                    title = stringResource(R.string.app_name),
                    content = stringResource(R.string.app_version, versionName, versionCode),
                    mainTextColor = mainTextColor,
                    secondaryTextColor = secondaryTextColor
                )

                // Features (aufklappbar)
                FeaturesSection(
                    mainTextColor = mainTextColor,
                    secondaryTextColor = secondaryTextColor,
                    accentColor = accentColor
                )

                // Developer Info
                InfoSection(
                    title = stringResource(R.string.developer_label),
                    content = stringResource(R.string.developer_name),
                    mainTextColor = mainTextColor,
                    secondaryTextColor = secondaryTextColor
                )

                // License
                InfoSection(
                    title = stringResource(R.string.license_label),
                    content = stringResource(R.string.license_text),
                    mainTextColor = mainTextColor,
                    secondaryTextColor = secondaryTextColor
                )

                // Libraries
                InfoSection(
                    title = stringResource(R.string.libraries_label),
                    content = "Jetpack Compose, Kotlin, AndroidX, Material3, Lucide Icons",
                    mainTextColor = mainTextColor,
                    secondaryTextColor = secondaryTextColor
                )

                // Privacy
                InfoSection(
                    title = stringResource(R.string.privacy_label),
                    content = stringResource(R.string.privacy_text),
                    mainTextColor = mainTextColor,
                    secondaryTextColor = secondaryTextColor
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private data class AppFeature(
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    val icon: ImageVector
)

private val APP_FEATURES: List<AppFeature> = listOf(
    AppFeature(R.string.feature_favorites_title, R.string.feature_favorites_desc, Lucide.Star),
    AppFeature(R.string.feature_drawer_title, R.string.feature_drawer_desc, Lucide.LayoutGrid),
    AppFeature(R.string.feature_search_title, R.string.feature_search_desc, Lucide.Search),
    AppFeature(R.string.feature_folders_title, R.string.feature_folders_desc, Lucide.Folder),
    AppFeature(R.string.feature_shortcuts_title, R.string.feature_shortcuts_desc, Lucide.Hand),
    AppFeature(R.string.feature_gestures_title, R.string.feature_gestures_desc, Lucide.ArrowUp),
    AppFeature(R.string.feature_icons_title, R.string.feature_icons_desc, Lucide.Pencil),
    AppFeature(R.string.feature_themes_title, R.string.feature_themes_desc, Lucide.Palette),
    AppFeature(R.string.feature_sizes_title, R.string.feature_sizes_desc, Lucide.Maximize2),
    AppFeature(R.string.feature_animations_title, R.string.feature_animations_desc, Lucide.Sparkles),
    AppFeature(R.string.feature_eyedropper_title, R.string.feature_eyedropper_desc, Lucide.Image),
    AppFeature(R.string.feature_lock_title, R.string.feature_lock_desc, Lucide.Lock),
    AppFeature(R.string.feature_widgets_title, R.string.feature_widgets_desc, Lucide.Clock),
    AppFeature(R.string.feature_doubletap_title, R.string.feature_doubletap_desc, Lucide.Smartphone),
    AppFeature(R.string.feature_hidden_title, R.string.feature_hidden_desc, Lucide.Ban),
    AppFeature(R.string.feature_uninstall_title, R.string.feature_uninstall_desc, Lucide.Trash2)
)

@Composable
fun FeaturesSection(
    mainTextColor: Color,
    secondaryTextColor: Color,
    accentColor: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "featuresArrow")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.label_features),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = mainTextColor
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = mainTextColor,
                modifier = Modifier.rotate(arrowRotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = RethroneSprings.spatial(), expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                APP_FEATURES.forEach { feature ->
                    FeatureRow(
                        feature = feature,
                        mainTextColor = mainTextColor,
                        secondaryTextColor = secondaryTextColor,
                        accentColor = accentColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = secondaryTextColor.copy(alpha = 0.2f), thickness = 0.5.dp)
    }
}

@Composable
private fun FeatureRow(
    feature: AppFeature,
    mainTextColor: Color,
    secondaryTextColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon im getönten Kreis (Material-3-Expressive): Akzent-Tint, dezenter Hintergrund.
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(feature.titleRes),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = mainTextColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(feature.descRes),
                fontSize = 13.sp,
                color = secondaryTextColor,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun InfoSection(
    title: String,
    content: String,
    mainTextColor: Color,
    secondaryTextColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = mainTextColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            fontSize = 14.sp,
            color = secondaryTextColor,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = secondaryTextColor.copy(alpha = 0.2f), thickness = 0.5.dp)
    }
}
