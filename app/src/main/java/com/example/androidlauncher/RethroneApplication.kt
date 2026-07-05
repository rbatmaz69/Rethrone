package com.example.androidlauncher

import android.app.Application
import android.os.StrictMode
import dagger.hilt.android.HiltAndroidApp

/**
 * Application-Einstiegspunkt fuer Hilt. `@HiltAndroidApp` erzeugt den
 * Dependency-Injection-Graphen (SingletonComponent), aus dem Activities, Services und
 * ViewModels ihre Abhaengigkeiten beziehen.
 */
@HiltAndroidApp
class RethroneApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
    }

    /**
     * StrictMode nur in Debug-Builds: macht Disk-/Netzwerk-Zugriffe auf dem Main-Thread
     * und Ressourcen-Lecks (Activities, Closeables, Receiver) im Logcat sichtbar
     * (Tag `StrictMode`), statt sie erst als Jank/ANR beim Nutzer zu bemerken.
     */
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectActivityLeaks()
                .penaltyLog()
                .build()
        )
    }
}
