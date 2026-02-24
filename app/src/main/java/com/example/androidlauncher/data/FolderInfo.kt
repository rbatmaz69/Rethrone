package com.example.androidlauncher.data

/**
 * Represents a user-created folder containing apps.
 * Stored as a JSON-like structure (or via preferences).
 */
data class FolderInfo(
    val id: String, // Unique identifier (UUID string)
    val name: String, // Display name of the folder
    val appPackageNames: List<String> // List of packages inside this folder
)
