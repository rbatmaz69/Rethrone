package com.example.androidlauncher

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Schlanke Host-Activity fuer Compose-UI-Tests. `@AndroidEntryPoint`, damit darin gerenderte
 * Composables `hiltViewModel()` (z. B. in AppDrawer) aufloesen koennen.
 */
@AndroidEntryPoint
class DebugComposeHostActivity : ComponentActivity()

