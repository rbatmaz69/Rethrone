package com.example.androidlauncher.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidlauncher.LauncherLogic
import com.example.androidlauncher.data.AppInfo
import com.example.androidlauncher.data.FolderInfo
import com.example.androidlauncher.data.IconManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.geometry.Offset
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap

@OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AppDrawerViewModel(application: Application) : AndroidViewModel(application) {

    // IconManager im ViewModel halten (Application context)
    private val iconManager = IconManager(application)

    // expose custom icons as StateFlow
    val customIcons: StateFlow<Map<String, String>> =
        iconManager.customIcons.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // Inputs: apps and folders (werden vom Composable gesetzt)
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _folders = MutableStateFlow<List<FolderInfo>>(emptyList())

    // Search query mit StateFlow
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Public API zum setzten
    fun updateApps(apps: List<AppInfo>) { _apps.value = apps }
    fun updateFolders(folders: List<FolderInfo>) { _folders.value = folders }
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    // Visible apps: instantly filter on background thread to prevent UI flickering
    val visibleApps: StateFlow<List<AppInfo>> = combine(
        _apps,
        _folders,
        _searchQuery
    ) { apps, folders, query ->
        if (query.isBlank()) {
            LauncherLogic.getVisibleApps(apps, folders)
        } else {
            LauncherLogic.filterApps(apps, query)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // --- A: LRU Cache for Loaded Bitmaps & Intents ---
    private val _iconBitmapCache = LruCache<String, ImageBitmap>(80)
    
    private val _launchIntentCache = MutableStateFlow<Map<String, Intent?>>(emptyMap())
    val launchIntentCache: StateFlow<Map<String, Intent?>> = _launchIntentCache.asStateFlow()

    // Prefetch launch intents on apps update
    init {
        viewModelScope.launch {
            _apps.collect { apps ->
                if (apps.isEmpty()) {
                    _launchIntentCache.value = emptyMap()
                    return@collect
                }
                val map = withContext(Dispatchers.IO) {
                    val pm = application.packageManager
                    apps.associate { app ->
                        val intent = try {
                            pm.getLaunchIntentForPackage(app.packageName)
                        } catch (e: Exception) {
                            null
                        }
                        app.packageName to intent
                    }
                }
                _launchIntentCache.value = map
            }
        }
    }

    // Helper to get cached launch intent; fallback to packageManager if missing
    suspend fun getLaunchIntent(packageName: String): Intent? {
        val cached = _launchIntentCache.value[packageName]
        if (cached != null) return cached
        return withContext(Dispatchers.IO) {
            try {
                getApplication<Application>().packageManager.getLaunchIntentForPackage(packageName)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun getCachedIcon(packageName: String): ImageBitmap? {
        return _iconBitmapCache.get(packageName)
    }
    fun putCachedIcon(packageName: String, bitmap: ImageBitmap) {
        _iconBitmapCache.put(packageName, bitmap)
    }

    // --- B: Folder Logic / Drag & Drop State Hoisting ---
    private val _draggingItemPkg = MutableStateFlow<String?>(null)
    val draggingItemPkg: StateFlow<String?> = _draggingItemPkg.asStateFlow()

    private val _touchPosition = MutableStateFlow(Offset.Zero)
    val touchPosition: StateFlow<Offset> = _touchPosition.asStateFlow()

    private val _initialTouchOffset = MutableStateFlow(Offset.Zero)
    val initialTouchOffset: StateFlow<Offset> = _initialTouchOffset.asStateFlow()

    fun onDragStart(pkg: String, touchPos: Offset, initialOffset: Offset) {
        _draggingItemPkg.value = pkg
        _touchPosition.value = touchPos
        _initialTouchOffset.value = initialOffset
    }

    fun onDragUpdate(dragAmount: Offset) {
        _touchPosition.value = _touchPosition.value + dragAmount
    }

    fun onDragEnd() {
        _draggingItemPkg.value = null
    }
}
