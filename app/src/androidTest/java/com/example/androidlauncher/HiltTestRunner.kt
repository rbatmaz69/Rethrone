package com.example.androidlauncher

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Test-Runner, der statt [RethroneApplication] die von Hilt generierte [HiltTestApplication]
 * verwendet. Nur so lassen sich in Instrumented-Tests `@AndroidEntryPoint`-Activities starten
 * (z. B. MainActivity, DebugComposeHostActivity) und `hiltViewModel()` aufloesen.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
