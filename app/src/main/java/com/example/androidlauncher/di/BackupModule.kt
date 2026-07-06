package com.example.androidlauncher.di

import android.content.Context
import com.example.androidlauncher.data.backup.BackupManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Stellt den [BackupManager] (B5) bereit. Eigenes Modul statt [DataModule],
 * damit dessen Provider-Anzahl nicht weiter wächst; gleiche Konvention
 * (Context-Konstruktor in Produktion, DataStore-Konstruktor in Tests).
 */
@Module
@InstallIn(SingletonComponent::class)
object BackupModule {

    @Provides
    @Singleton
    fun provideBackupManager(@ApplicationContext context: Context): BackupManager =
        BackupManager(context)
}
