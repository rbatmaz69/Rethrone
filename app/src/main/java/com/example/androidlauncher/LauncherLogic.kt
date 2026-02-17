package com.example.androidlauncher

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
}
