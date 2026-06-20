import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApp)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

// Signing-Credentials niemals im Repo: gelesen aus Umgebungsvariablen (CI) mit Vorrang,
// sonst aus der nicht-versionierten local.properties (lokale Release-Builds).
val keystoreProperties = Properties().apply {
    val localProps = rootProject.file("local.properties")
    if (localProps.exists()) {
        localProps.inputStream().use { load(it) }
    }
}
fun signingProperty(envName: String, propName: String): String? =
    System.getenv(envName) ?: keystoreProperties.getProperty(propName)

android {
    namespace = "com.example.androidlauncher"
    compileSdk = 36

    defaultConfig {
        // Eindeutige, dauerhafte Play-Store-ID (nicht mehr der com.example.*-Platzhalter).
        // namespace bleibt bewusst getrennt, um kein Package-Rename auszulösen.
        applicationId = "com.rethrone.launcher"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    androidResources {
        // Nur die offiziell gepflegten Sprachen mit ausliefern. Verhindert APK-Bloat durch
        // Bibliotheks-Übersetzungen und hält den Sprachensatz konsistent. Englisch (values/)
        // dient als Fallback für nicht unterstützte Systemsprachen.
        localeFilters += listOf("en", "de", "fr", "es", "it")
    }

    signingConfigs {
        // Nur anlegen, wenn Credentials vorhanden sind (ENV oder local.properties).
        val storeFilePath = signingProperty("KEYSTORE_FILE", "release.storeFile")
        if (storeFilePath != null) {
            create("release") {
                storeFile = file(storeFilePath)
                storePassword = signingProperty("KEYSTORE_PASSWORD", "release.storePassword")
                keyAlias = signingProperty("KEY_ALIAS", "release.keyAlias")
                keyPassword = signingProperty("KEY_PASSWORD", "release.keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Mit dem eigenen Release-Keystore signieren, sobald Credentials vorliegen
            // (CI über GitHub-Secrets oder lokal über local.properties). Andernfalls Fallback
            // auf die Debug-Signatur für reine Dev-Installationen per ADB.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

tasks.withType<Test>().configureEach {
    doFirst {
        temporaryDir.mkdirs()
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

kover {
    reports {
        filters {
            excludes {
                classes("com.example.androidlauncher.ui.theme.*")
                classes("com.example.androidlauncher.MainActivityKt")
                classes("com.example.androidlauncher.MainActivityKt\$*")
                classes("com.example.androidlauncher.MainActivity")
                classes("com.example.androidlauncher.MainActivity\$*")
                classes("com.example.androidlauncher.ui.AppDrawerKt")
                classes("com.example.androidlauncher.ui.AppDrawerKt\$*")
                classes("com.example.androidlauncher.ui.ColorConfigMenuKt")
                classes("com.example.androidlauncher.ui.ColorConfigMenuKt\$*")
                classes("com.example.androidlauncher.ui.FolderConfigMenuKt")
                classes("com.example.androidlauncher.ui.FolderConfigMenuKt\$*")
                classes("com.example.androidlauncher.ui.SettingsPaletteMenuKt")
                classes("com.example.androidlauncher.ui.SettingsPaletteMenuKt\$*")
                classes("com.example.androidlauncher.ui.SizeConfigMenuKt")
                classes("com.example.androidlauncher.ui.SizeConfigMenuKt\$*")
                classes("com.example.androidlauncher.ui.UtilsKt")
                classes("com.example.androidlauncher.ui.UtilsKt\$*")
                classes("com.example.androidlauncher.ui.ComposableSingletons*")
                classes("com.example.androidlauncher.ComposableSingletons*")
                classes("com.example.androidlauncher.ui.PaletteMenuItem")
                // Neue extrahierte UI-Dateien
                classes("com.example.androidlauncher.ui.HomeScreenKt")
                classes("com.example.androidlauncher.ui.HomeScreenKt\$*")
                classes("com.example.androidlauncher.ui.FavoritesConfigMenuKt")
                classes("com.example.androidlauncher.ui.FavoritesConfigMenuKt\$*")
                classes("com.example.androidlauncher.ui.SystemWallpaperViewKt")
                classes("com.example.androidlauncher.ui.SystemWallpaperViewKt\$*")
                classes("com.example.androidlauncher.ui.LiquidGlass")
                classes("com.example.androidlauncher.ui.LiquidGlass\$*")
                classes("com.example.androidlauncher.ui.EditConfigMenuKt")
                classes("com.example.androidlauncher.ui.EditConfigMenuKt\$*")
                classes("com.example.androidlauncher.ui.FontSelectionMenuKt")
                classes("com.example.androidlauncher.ui.FontSelectionMenuKt\$*")
                classes("com.example.androidlauncher.ui.WallpaperConfigMenuKt")
                classes("com.example.androidlauncher.ui.WallpaperConfigMenuKt\$*")
                classes("com.example.androidlauncher.ui.IconConfigMenuKt")
                classes("com.example.androidlauncher.ui.IconConfigMenuKt\$*")
                classes("com.example.androidlauncher.ui.AppContextMenuKt")
                classes("com.example.androidlauncher.ui.AppContextMenuKt\$*")
                classes("com.example.androidlauncher.ui.AppShortcutsMenuKt")
                classes("com.example.androidlauncher.ui.AppShortcutsMenuKt\$*")
                classes("com.example.androidlauncher.ui.HybridSearchKt")
                classes("com.example.androidlauncher.ui.HybridSearchKt\$*")
                classes("com.example.androidlauncher.ui.InfoDialogKt")
                classes("com.example.androidlauncher.ui.InfoDialogKt\$*")
                classes("com.example.androidlauncher.data.FavoritesManager")
                classes("com.example.androidlauncher.data.FavoritesManager\$*")
                classes("com.example.androidlauncher.data.FavoritesManagerKt")
                classes("com.example.androidlauncher.data.AppRepository")
                classes("com.example.androidlauncher.data.AppRepository\$*")
                classes("com.example.androidlauncher.data.IconManager")
                classes("com.example.androidlauncher.data.IconManager\$*")
                classes("com.example.androidlauncher.data.IconManagerKt")
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Rounded-Icon-Set (Android-15/16-Look). R8 shrinkt ungenutzte Icons im Release.
    implementation(libs.androidx.compose.material.icons.extended)
    // Google Fonts (Downloadable) – universelle Schriftarten über den Play-Services-Provider.
    implementation(libs.androidx.compose.ui.text.google.fonts)

    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // Lucide Icons
    implementation(libs.lucide.icons)

    // Biometrie (Fingerabdruck/Gesicht + Geräte-Credential-Fallback) für die App-Sperre
    implementation(libs.androidx.biometric)


    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("org.json:json:20231013")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
