package com.example.androidlauncher.data

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.example.androidlauncher.R

/**
 * Google-Fonts-Provider (Downloadable Fonts) über Google Play Services.
 * Lädt echte Schriftdateien → auf jedem Gerät mit Play Services byte-identisch.
 */
val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

/**
 * Baut eine [FontFamily] für eine Google-Schriftart mit den im Launcher genutzten Gewichten.
 * Nicht vorhandene Gewichte fallen serverseitig auf das nächste verfügbare zurück.
 */
fun googleFontFamily(name: String): FontFamily {
    val gf = GoogleFont(name)
    return FontFamily(
        Font(googleFont = gf, fontProvider = GoogleFontProvider, weight = FontWeight.Light),
        Font(googleFont = gf, fontProvider = GoogleFontProvider, weight = FontWeight.Normal),
        Font(googleFont = gf, fontProvider = GoogleFontProvider, weight = FontWeight.Medium),
        Font(googleFont = gf, fontProvider = GoogleFontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = gf, fontProvider = GoogleFontProvider, weight = FontWeight.Bold)
    )
}
