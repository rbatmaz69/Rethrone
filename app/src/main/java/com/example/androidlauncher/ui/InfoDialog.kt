package com.example.androidlauncher.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.R
import com.example.androidlauncher.SystemWallpaperView
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled

@Composable
fun InfoDialog(
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTextEnabled = LocalDarkTextEnabled.current
    val mainTextColor = if (isDarkTextEnabled) Color(0xFF010101) else Color.White
    val secondaryTextColor = if (isDarkTextEnabled) Color(0xFF555555) else Color.White.copy(alpha = 0.7f)
    val backgroundColor = if (isDarkTextEnabled) Color(0xFFF5F5F5) else Color(0xFF121212)
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        SystemWallpaperView()
        Box(modifier = Modifier.fillMaxSize().background(backgroundColor.copy(alpha = 0.95f)))

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
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    color = mainTextColor
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = mainTextColor)
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

                // Developer Info
                InfoSection(
                    title = stringResource(R.string.developer_label),
                    content = stringResource(R.string.developer_name),
                    mainTextColor = mainTextColor,
                    secondaryTextColor = secondaryTextColor
                ) {
                    Text(
                        text = stringResource(R.string.github_repo),
                        color = if (isDarkTextEnabled) Color.Blue else Color(0xFF64B5F6),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rbatmaz69?tab=repositories"))
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
