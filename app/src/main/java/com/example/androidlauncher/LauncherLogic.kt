package com.example.androidlauncher

import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.FolderInfo
import java.util.UUID

/**
 * Logic controller for the launcher operations.
 * Handles filtering apps, managing favorites (add/remove/reorder),
 * and folder management (create/delete/rename/add app/remove app).
 * This object is stateless and operates on immutable lists/objects passed to it.
 */
object LauncherLogic {
    const val MAX_FAVORITES = 8

    /**
     * Filters the list of apps based on a search query.
     * Case-insensitive.
     */
    fun filterApps(apps: List<AppInfo>, query: String): List<AppInfo> {
        if (query.isBlank()) return apps
        return apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    /**
     * Filters apps by relevance:
     * 1. Exact match (case insensitive)
     * 2. Starts with query (case insensitive)
     * 3. Contains query (case insensitive)
     */
    fun filterAppsByRelevance(apps: List<AppInfo>, query: String): List<AppInfo> {
        if (query.isBlank()) return emptyList()
        val lowerQuery = query.lowercase()

        return apps.filter { it.label.contains(lowerQuery, ignoreCase = true) }
            .sortedWith(compareBy<AppInfo> {
                val lowerLabel = it.label.lowercase()
                when {
                    lowerLabel == lowerQuery -> 0
                    lowerLabel.startsWith(lowerQuery) -> 1
                    else -> 2
                }
            }.thenBy { it.label })
    }

    /**
     * Toggles the favorite status of an app.
     * Adds to favorites if not already a favorite and max favorites limit is not reached.
     * Removes from favorites if it is already a favorite.
     */
    fun toggleFavorite(currentFavorites: List<String>, packageName: String): List<String> {
        return if (packageName in currentFavorites) {
            // Remove if already favorite
            currentFavorites - packageName
        } else {
            // Add if not maxed out
            if (currentFavorites.size < MAX_FAVORITES) {
                currentFavorites + packageName
            } else {
                currentFavorites
            }
        }
    }

    /**
     * Moves a favorite item up in the list (decreases index).
     */
    fun moveFavoriteUp(currentFavorites: List<String>, index: Int): List<String> {
        if (index <= 0 || index >= currentFavorites.size) return currentFavorites
        val newList = currentFavorites.toMutableList()
        val item = newList.removeAt(index)
        newList.add(index - 1, item)
        return newList
    }

    /**
     * Moves a favorite item down in the list (increases index).
     */
    fun moveFavoriteDown(currentFavorites: List<String>, index: Int): List<String> {
        if (index < 0 || index >= currentFavorites.size - 1) return currentFavorites
        val newList = currentFavorites.toMutableList()
        val item = newList.removeAt(index)
        newList.add(index + 1, item)
        return newList
    }

    /**
     * Resolves the list of favorite packages to actual AppInfo objects.
     * Maintains the order of the favorite packages list.
     */
    fun getFavoriteApps(allApps: List<AppInfo>, favoritePackages: List<String>): List<AppInfo> {
        return favoritePackages.mapNotNull { pkg ->
            allApps.find { it.packageName == pkg }
        }.take(MAX_FAVORITES)
    }

    // Folder Logic

    /**
     * returns a set of all package names that are currently inside any folder.
     */
    fun getAppsInFolders(folders: List<FolderInfo>): Set<String> {
        return folders.flatMap { it.appPackageNames }.toSet()
    }

    /**
     * Returns apps that are NOT inside any folder.
     * Used for the main app drawer list to hide folder contents.
     */
    fun getVisibleApps(allApps: List<AppInfo>, folders: List<FolderInfo>): List<AppInfo> {
        val appsInFolders = getAppsInFolders(folders)
        return allApps.filter { it.packageName !in appsInFolders }
    }

    /**
     * Adds an app to a specific folder.
     * Ensures an app exists only in one folder at a time by removing it from others.
     */
    fun addAppToFolder(folders: List<FolderInfo>, folderId: String, packageName: String): List<FolderInfo> {
        return folders.map { folder ->
            if (folder.id == folderId) {
                if (packageName !in folder.appPackageNames) {
                    folder.copy(appPackageNames = folder.appPackageNames + packageName)
                } else folder
            } else {
                // Ensure app is not in other folders
                folder.copy(appPackageNames = folder.appPackageNames - packageName)
            }
        }
    }

    /**
     * Removes an app from a specific folder.
     */
    fun removeAppFromFolder(folders: List<FolderInfo>, folderId: String, packageName: String): List<FolderInfo> {
        return folders.map { folder ->
            if (folder.id == folderId) {
                folder.copy(appPackageNames = folder.appPackageNames - packageName)
            } else folder
        }
    }

    /**
     * Creates a new folder with a generated ID.
     */
    fun createFolder(folders: List<FolderInfo>, name: String): List<FolderInfo> {
        val newFolder = FolderInfo(
            id = UUID.randomUUID().toString(),
            name = name,
            appPackageNames = emptyList()
        )
        return folders + newFolder
    }

    /**
     * Deletes a folder by ID. Apps inside will become visible in the main list again
     * (because they are no longer in `getAppsInFolders`).
     */
    fun deleteFolder(folders: List<FolderInfo>, folderId: String): List<FolderInfo> {
        return folders.filter { it.id != folderId }
    }

    /**
     * Renames a folder.
     */
    fun renameFolder(folders: List<FolderInfo>, folderId: String, newName: String): List<FolderInfo> {
        return folders.map { if (it.id == folderId) it.copy(name = newName) else it }
    }
}
