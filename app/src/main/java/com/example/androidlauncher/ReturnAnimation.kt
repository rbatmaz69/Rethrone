package com.example.androidlauncher

enum class LaunchSource {
    HOME,
    DRAWER
}

data class ReturnAnimation(
    val bounds: androidx.compose.ui.geometry.Rect?,
    val source: LaunchSource,
    val packageName: String
)
