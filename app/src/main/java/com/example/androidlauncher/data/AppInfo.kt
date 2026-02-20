package com.example.androidlauncher.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector

@Stable
data class AppInfo(
    val label: String,
    val packageName: String,
    val iconBitmap: ImageBitmap? = null,
    val lucideIcon: ImageVector? = null,
    val customIconResId: Int? = null
)
