package com.example.androidlauncher.di

import android.content.Context
import com.example.androidlauncher.LauncherShakeManager
import com.example.androidlauncher.data.AppRepository
import com.example.androidlauncher.data.FavoritesManager
import com.example.androidlauncher.data.FolderManager
import com.example.androidlauncher.data.IconManager
import com.example.androidlauncher.data.SearchSuggestionsManager
import com.example.androidlauncher.data.ThemeManager
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
}
