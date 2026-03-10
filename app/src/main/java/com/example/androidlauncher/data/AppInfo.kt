package com.example.androidlauncher.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data class representing an application installed on the device.
 * Used to display app icons and names in the list.
 */
@Stable
data class AppInfo(
    val label: String, // The display name of the app
    val packageName: String, // The unique package identifier (e.g., com.example.app)
    val iconBitmap: ImageBitmap? = null, // The actual app icon as a bitmap
    val lucideIcon: ImageVector? = null // Optional vector icon (if used)
)
