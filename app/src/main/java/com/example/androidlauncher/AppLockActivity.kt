package com.example.androidlauncher

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.FragmentActivity
import com.example.androidlauncher.data.AppFont
import com.example.androidlauncher.data.AppLockManager
import com.example.androidlauncher.data.DesignStyle
import com.example.androidlauncher.data.FontWeightLevel
import com.example.androidlauncher.data.ThemeManager
import com.example.androidlauncher.ui.AppLockScreen
import com.example.androidlauncher.ui.theme.AndroidLauncherTheme
import com.example.androidlauncher.ui.theme.ColorTheme

/**
 * Vollbild-Sperr-Activity, die über einer geschützten App erscheint und vor dem Zugriff eine
 * Authentifizierung (PIN/Muster/Biometrie) verlangt. Wird vom [LauncherAccessibilityService]
 * gestartet, sobald ein gesperrtes Paket in den Vordergrund kommt.
 */
@dagger.hilt.android.AndroidEntryPoint
class AppLockActivity : FragmentActivity() {

    @javax.inject.Inject
    lateinit var themeManager: ThemeManager

    @javax.inject.Inject
    lateinit var appLockManager: AppLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sperrbildschirm nicht in Screenshots/Recents-Vorschau zeigen.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        val targetPackage = intent.getStringExtra(EXTRA_PACKAGE)
        if (targetPackage.isNullOrBlank()) {
            finish()
            return
        }

        // Falls das Paket in dieser Sitzung bereits entsperrt ist, nicht erneut nach PIN
        // fragen (Absicherung gegen verbleibende Relaunch-Fälle).
        if (appLockManager.isUnlocked(targetPackage)) {
            finish()
            return
        }

        // Zurück-Geste verlässt die geschützte App zum Startbildschirm (App bleibt gesperrt).
        onBackPressedDispatcher.addCallback(
            this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    goHome()
                }
            }
        )

        // themeManager wird von Hilt injiziert (siehe Feld oben).
        val pm = packageManager
        val appLabel = runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(targetPackage, 0)).toString()
        }.getOrDefault(targetPackage)
        val appIcon = runCatching {
            pm.getApplicationIcon(targetPackage).toBitmap(96, 96).asImageBitmap()
        }.getOrNull()

        setContent {
            val colorTheme by themeManager.selectedTheme.collectAsState(initial = ColorTheme.SOFT_SAND)
            val darkText by themeManager.isDarkTextEnabled.collectAsState(initial = true)
            val designStyle by themeManager.designStyle.collectAsState(initial = DesignStyle.GLASS)
            val appFont by themeManager.selectedAppFont.collectAsState(initial = AppFont.SYSTEM_DEFAULT)
            val fontWeight by themeManager.selectedFontWeight.collectAsState(initial = FontWeightLevel.NORMAL)

            val lockType by themeManager.lockType.collectAsState(initial = "none")
            val lockSecret by themeManager.lockSecret.collectAsState(initial = "")
            val biometricEnabled by themeManager.isLockBiometricEnabled.collectAsState(initial = false)

            AndroidLauncherTheme(
                colorTheme = colorTheme,
                darkTextEnabled = darkText,
                designStyle = designStyle,
                appFont = appFont,
                fontWeight = fontWeight
            ) {
                AppLockScreen(
                    appLabel = appLabel,
                    appIcon = appIcon,
                    lockType = lockType,
                    onVerify = { input ->
                        lockSecret.isNotEmpty() && AppLockManager.verify(input, lockSecret)
                    },
                    onSuccess = { unlockAndFinish(targetPackage) },
                    biometricEnabled = biometricEnabled,
                    onBiometric = { showBiometricPrompt(targetPackage) }
                )
            }
        }
    }

    private fun showBiometricPrompt(targetPackage: String) {
        val canAuth = BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) return

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlockAndFinish(targetPackage)
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_lock_unlock_title))
            .setSubtitle(getString(R.string.app_lock_unlock_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        prompt.authenticate(info)
    }

    private fun unlockAndFinish(targetPackage: String) {
        appLockManager.markUnlocked(targetPackage)
        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(home)
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "extra_locked_package"
    }
}
