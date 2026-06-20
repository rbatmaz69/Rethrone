package com.example.androidlauncher.ui

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.example.androidlauncher.R
import androidx.compose.foundation.Image
import com.example.androidlauncher.data.DesignStyle
import com.example.androidlauncher.ui.LiquidGlass.designSurface
import com.example.androidlauncher.ui.theme.LocalColorTheme
import com.example.androidlauncher.ui.theme.LocalDarkTextEnabled
import com.example.androidlauncher.ui.theme.LocalDesignStyle

/**
 * Vollbild-Sperroberfläche, die vor dem Öffnen einer geschützten App erscheint.
 * Verifiziert PIN bzw. Muster über [onVerify] und bietet optional Biometrie an.
 *
 * @param lockType "pin" oder "pattern".
 * @param onVerify prüft die Eingabe gegen das gespeicherte Geheimnis.
 * @param onSuccess wird bei korrekter Eingabe aufgerufen.
 * @param biometricEnabled blendet den Biometrie-Button ein.
 * @param onBiometric startet den BiometricPrompt.
 */
@Composable
fun AppLockScreen(
    appLabel: String,
    appIcon: ImageBitmap?,
    lockType: String,
    onVerify: (String) -> Boolean,
    onSuccess: () -> Unit,
    biometricEnabled: Boolean,
    onBiometric: () -> Unit
) {
    val isDarkText = LocalDarkTextEnabled.current
    val designStyle = LocalDesignStyle.current
    val colorTheme = LocalColorTheme.current
    val textColor = LiquidGlass.mainTextColor(isDarkText)
    val accentColor = colorTheme.accentColor(isDarkText)
    val surfaceAccent = colorTheme.menuSurfaceColor(isDarkText)
    val backgroundColor = colorTheme.backgroundColor(isDarkText)

    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    fun attempt(input: String) {
        if (onVerify(input)) {
            onSuccess()
        } else {
            showError = true
            pin = ""
            vibrateError(context)
        }
    }

    // Biometrie automatisch beim Öffnen anbieten
    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled) onBiometric()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App-Icon oder Schloss
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .designSurface(
                    designStyle, CircleShape, isDarkText, surfaceAccent,
                    fillAlpha = 0.08f, glassStartAlpha = 0.12f, glassEndAlpha = 0.04f,
                    borderWidth = 1.dp, borderStartAlpha = 0.2f, borderEndAlpha = 0.06f
                ),
            contentAlignment = Alignment.Center
        ) {
            if (appIcon != null) {
                Image(bitmap = appIcon, contentDescription = appLabel, modifier = Modifier.size(48.dp))
            } else {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = textColor, modifier = Modifier.size(36.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(appLabel, color = textColor, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            if (showError) stringResource(R.string.lock_wrong_code) else stringResource(R.string.lock_locked_hint),
            color = if (showError) Color(0xFFE0584F) else textColor.copy(alpha = 0.6f),
            fontSize = 14.sp
        )

        Spacer(Modifier.height(36.dp))

        if (lockType == "pattern") {
            PatternLockView(
                onComplete = { attempt(it) },
                nodeColor = textColor,
                lineColor = accentColor,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        } else {
            PinPad(
                value = pin,
                onValueChange = { pin = it },
                maxLength = 12,
                textColor = textColor,
                accentColor = accentColor,
                designStyle = designStyle,
                surfaceAccent = surfaceAccent,
                isDarkTextEnabled = isDarkText
            )
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .clip(RoundedCornerShape(28.dp))
                    .designSurface(
                        designStyle, RoundedCornerShape(28.dp), isDarkText, accentColor,
                        fillAlpha = 0.18f, glassStartAlpha = 0.2f, glassEndAlpha = 0.08f,
                        borderWidth = 1.dp, borderStartAlpha = 0.25f, borderEndAlpha = 0.1f
                    )
                    .clickable(enabled = pin.isNotEmpty()) { attempt(pin) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.unlock), color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }

        if (biometricEnabled) {
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onBiometric() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.use_biometric), color = textColor.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }
    }
}

private fun vibrateError(context: android.content.Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(VibratorManager::class.java)
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
    if (vibrator?.hasVibrator() == true) {
        vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
