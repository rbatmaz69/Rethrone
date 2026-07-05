package com.example.androidlauncher.di

import com.example.androidlauncher.data.AppLockManager
import com.example.androidlauncher.data.ThemeManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt-Zugang für System-Services, die kein `@AndroidEntryPoint` unterstützen
 * (z. B. [android.accessibilityservice.AccessibilityService]). Aufgelöst über
 * `EntryPointAccessors.fromApplication(...)`, damit auch Services dieselben
 * Singletons wie der Rest der App verwenden statt eigene Instanzen zu bauen.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LauncherServiceEntryPoint {
    fun themeManager(): ThemeManager
    fun appLockManager(): AppLockManager
}
