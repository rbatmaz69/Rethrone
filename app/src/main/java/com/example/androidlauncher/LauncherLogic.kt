package com.example.androidlauncher

import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.FolderInfo
import java.util.UUID

object LauncherLogic {
    const val MAX_FAVORITES = 8

    fun filterApps(apps: List<AppInfo>, query: String): List<AppInfo> {
        if (query.isBlank()) return apps
        return apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    fun toggleFavorite(currentFavorites: List<String>, packageName: String): List<String> {
        return if (packageName in currentFavorites) {
            currentFavorites - packageName
        } else {
            if (currentFavorites.size < MAX_FAVORITES) {
                currentFavorites + packageName
            } else {
                currentFavorites
            }
        }
    }

    fun moveFavoriteUp(currentFavorites: List<String>, index: Int): List<String> {
        if (index <= 0 || index >= currentFavorites.size) return currentFavorites
        val newList = currentFavorites.toMutableList()
        val item = newList.removeAt(index)
        newList.add(index - 1, item)
        return newList
    }

    fun moveFavoriteDown(currentFavorites: List<String>, index: Int): List<String> {
        if (index < 0 || index >= currentFavorites.size - 1) return currentFavorites
        val newList = currentFavorites.toMutableList()
        val item = newList.removeAt(index)
        newList.add(index + 1, item)
        return newList
    }
    
    fun getFavoriteApps(allApps: List<AppInfo>, favoritePackages: List<String>): List<AppInfo> {
        return favoritePackages.mapNotNull { pkg -> 
            allApps.find { it.packageName == pkg } 
        }.take(MAX_FAVORITES)
    }

    // Folder Logic
    fun getAppsInFolders(folders: List<FolderInfo>): Set<String> {
        return folders.flatMap { it.appPackageNames }.toSet()
    }

    fun getVisibleApps(allApps: List<AppInfo>, folders: List<FolderInfo>): List<AppInfo> {
        val appsInFolders = getAppsInFolders(folders)
        return allApps.filter { it.packageName !in appsInFolders }
    }

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

    fun removeAppFromFolder(folders: List<FolderInfo>, folderId: String, packageName: String): List<FolderInfo> {
        return folders.map { folder ->
            if (folder.id == folderId) {
                folder.copy(appPackageNames = folder.appPackageNames - packageName)
            } else folder
        }
    }

    fun createFolder(folders: List<FolderInfo>, name: String): List<FolderInfo> {
        val newFolder = FolderInfo(
            id = UUID.randomUUID().toString(),
            name = name,
            appPackageNames = emptyList()
        )
        return folders + newFolder
    }

    fun deleteFolder(folders: List<FolderInfo>, folderId: String): List<FolderInfo> {
        return folders.filter { it.id != folderId }
    }

    fun renameFolder(folders: List<FolderInfo>, folderId: String, newName: String): List<FolderInfo> {
        return folders.map { if (it.id == folderId) it.copy(name = newName) else it }
    }
}
