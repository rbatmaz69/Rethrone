package com.example.androidlauncher.ui

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import com.example.androidlauncher.R
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalFontWeight

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
    val linkColor = remember(colorTheme, isDarkTextEnabled) {
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
                    secondaryTextColor = secondaryTextColor
                )

                // Developer Info
                InfoSection(
                    title = stringResource(R.string.developer_label),
                    content = stringResource(R.string.developer_name),
                    mainTextColor = mainTextColor,
                    secondaryTextColor = secondaryTextColor
                ) {
                    Text(
                        text = stringResource(R.string.github_repo),
                        color = linkColor,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, "https://github.com/rbatmaz69?tab=repositories".toUri())
                                context.startActivity(intent)
                            }
                            .padding(top = 4.dp)
                    )
                }

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

private data class AppFeature(@StringRes val titleRes: Int, @StringRes val descRes: Int)

private val APP_FEATURES: List<AppFeature> = listOf(
    AppFeature(R.string.feature_favorites_title, R.string.feature_favorites_desc),
    AppFeature(R.string.feature_drawer_title, R.string.feature_drawer_desc),
    AppFeature(R.string.feature_search_title, R.string.feature_search_desc),
    AppFeature(R.string.feature_folders_title, R.string.feature_folders_desc),
    AppFeature(R.string.feature_shortcuts_title, R.string.feature_shortcuts_desc),
    AppFeature(R.string.feature_gestures_title, R.string.feature_gestures_desc),
    AppFeature(R.string.feature_icons_title, R.string.feature_icons_desc),
    AppFeature(R.string.feature_themes_title, R.string.feature_themes_desc),
    AppFeature(R.string.feature_sizes_title, R.string.feature_sizes_desc),
    AppFeature(R.string.feature_animations_title, R.string.feature_animations_desc),
    AppFeature(R.string.feature_eyedropper_title, R.string.feature_eyedropper_desc),
    AppFeature(R.string.feature_hidden_title, R.string.feature_hidden_desc),
    AppFeature(R.string.feature_uninstall_title, R.string.feature_uninstall_desc)
)

@Composable
fun FeaturesSection(
    mainTextColor: Color,
    secondaryTextColor: Color
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

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                APP_FEATURES.forEach { feature ->
                    Column(modifier = Modifier.fillMaxWidth()) {
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
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = secondaryTextColor.copy(alpha = 0.2f), thickness = 0.5.dp)
    }
}

@Composable
fun InfoSection(
    title: String,
    content: String,
    mainTextColor: Color,
    secondaryTextColor: Color,
    extraContent: @Composable (() -> Unit)? = null
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
        extraContent?.invoke()

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = secondaryTextColor.copy(alpha = 0.2f), thickness = 0.5.dp)
    }
}
