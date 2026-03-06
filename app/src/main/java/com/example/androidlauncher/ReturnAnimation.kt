package com.example.androidlauncher

/**
 * Defines where an app launch originated from.
 * Used to determine the target position for the return animation.
 */
enum class LaunchSource {
    HOME, // Launched from the home screen favorites list
    DRAWER // Launched from the main app drawer
}

/**
 * Data class holding state for the "return animation" (when an app closes).
 * Used to animate the screen shrinking back into the app icon.
 */
data class ReturnAnimation(
    val bounds: androidx.compose.ui.geometry.Rect?, // The screen bounds of the icon that launched the app
    val source: LaunchSource, // Where the launch came from
    val packageName: String, // The return target identifier (icon/search button bounce target)
    val launchedPackageName: String = packageName // The actually launched app package for foreground matching
)
