package com.example.androidlauncher.di

import android.content.Context
import com.example.androidlauncher.LauncherShakeManager
import com.example.androidlauncher.data.AppLockManager
import com.example.androidlauncher.data.AppRepository
import com.example.androidlauncher.data.DynamicIslandManager
import com.example.androidlauncher.data.FavoritesManager
import com.example.androidlauncher.data.FolderManager
import com.example.androidlauncher.data.IconManager
import com.example.androidlauncher.data.NotificationStateStore
import com.example.androidlauncher.data.SearchSuggestionsManager
import com.example.androidlauncher.data.ThemeManager
import com.example.androidlauncher.data.WidgetHostManager
import com.example.androidlauncher.data.settings.AnimationSettings
import com.example.androidlauncher.data.settings.AppearanceSettings
import com.example.androidlauncher.data.settings.GestureSettings
import com.example.androidlauncher.data.settings.HomeLayoutSettings
import com.example.androidlauncher.data.settings.IslandAndEdgeSettings
import com.example.androidlauncher.data.settings.PrivacySettings
import com.example.androidlauncher.data.settings.WallpaperSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Stellt die Datenschicht (Repositories/Manager) als prozessweite Singletons bereit.
 *
 * Bewusst ueber `@Provides` statt `@Inject`-Konstruktoren: die Manager besitzen bereits einen
 * `constructor(context: Context)` (der intern die jeweilige DataStore-Instanz aufloest) sowie
 * einen DataStore-Konstruktor fuer Unit-Tests. Diese test-freundliche Doppelkonstruktion bleibt
 * so unangetastet – Hilt liefert in Produktion einfach die Context-Variante.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideThemeManager(@ApplicationContext context: Context): ThemeManager =
        ThemeManager(context)

    @Provides
    @Singleton
    fun provideFavoritesManager(@ApplicationContext context: Context): FavoritesManager =
        FavoritesManager(context)

    @Provides
    @Singleton
    fun provideFolderManager(@ApplicationContext context: Context): FolderManager =
        FolderManager(context)

    @Provides
    @Singleton
    fun provideIconManager(@ApplicationContext context: Context): IconManager =
        IconManager(context)

    @Provides
    @Singleton
    fun provideAppRepository(@ApplicationContext context: Context): AppRepository =
        AppRepository(context)

    @Provides
    @Singleton
    fun provideSearchSuggestionsManager(@ApplicationContext context: Context): SearchSuggestionsManager =
        SearchSuggestionsManager(context)

    @Provides
    @Singleton
    fun provideLauncherShakeManager(@ApplicationContext context: Context): LauncherShakeManager =
        LauncherShakeManager(context)

    @Provides
    @Singleton
    fun provideNotificationStateStore(): NotificationStateStore = NotificationStateStore()

    @Provides
    @Singleton
    fun provideDynamicIslandManager(
        @ApplicationContext context: Context,
        notificationStateStore: NotificationStateStore,
    ): DynamicIslandManager = DynamicIslandManager(context, notificationStateStore)

    @Provides
    @Singleton
    fun provideAppLockManager(): AppLockManager = AppLockManager()

    // A1-Split: domänen-spezifische Settings-Stores (teilen sich die "settings"-DataStore-Datei).
    @Provides
    @Singleton
    fun provideWallpaperSettings(@ApplicationContext context: Context): WallpaperSettings =
        WallpaperSettings(context)

    @Provides
    @Singleton
    fun provideGestureSettings(@ApplicationContext context: Context): GestureSettings =
        GestureSettings(context)

    @Provides
    @Singleton
    fun provideAnimationSettings(@ApplicationContext context: Context): AnimationSettings =
        AnimationSettings(context)

    @Provides
    @Singleton
    fun provideIslandAndEdgeSettings(@ApplicationContext context: Context): IslandAndEdgeSettings =
        IslandAndEdgeSettings(context)

    @Provides
    @Singleton
    fun providePrivacySettings(@ApplicationContext context: Context): PrivacySettings =
        PrivacySettings(context)

    @Provides
    @Singleton
    fun provideAppearanceSettings(@ApplicationContext context: Context): AppearanceSettings =
        AppearanceSettings(context)

    @Provides
    @Singleton
    fun provideHomeLayoutSettings(@ApplicationContext context: Context): HomeLayoutSettings =
        HomeLayoutSettings(context)

    @Provides
    @Singleton
    fun provideWidgetHostManager(
        @ApplicationContext context: Context,
        homeLayoutSettings: HomeLayoutSettings,
    ): WidgetHostManager = WidgetHostManager(context, homeLayoutSettings)
}
