package com.example.androidlauncher

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application-Einstiegspunkt fuer Hilt. `@HiltAndroidApp` erzeugt den
 * Dependency-Injection-Graphen (SingletonComponent), aus dem Activities, Services und
 * ViewModels ihre Abhaengigkeiten beziehen.
 */
@HiltAndroidApp
class RethroneApplication : Application()
