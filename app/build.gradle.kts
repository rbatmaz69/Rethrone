import io.gitlab.arturbosch.detekt.Detekt
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApp)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
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

        // Eigener Runner liefert HiltTestApplication, damit @AndroidEntryPoint-Activities in
        // Instrumented-Tests starten koennen (siehe androidTest/HiltTestRunner.kt).
        testInstrumentationRunner = "com.example.androidlauncher.HiltTestRunner"
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

detekt {
    toolVersion = libs.versions.detekt.get()
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    // Alt-Verstoesse des Bestandscodes sind eingefroren; nur NEUER Code laesst den Build
    // fehlschlagen. Baseline neu erzeugen via `./gradlew detektBaseline`.
    baseline = file("$rootDir/config/detekt/baseline.xml")
    buildUponDefaultConfig = true
    allRules = false
    // Im CI nicht automatisch korrigieren (kein Datei-Rewrite). Lokal: `./gradlew detekt --auto-correct`.
    autoCorrect = false
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(true)
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
                // Hinweis: data.AppRepository, data.IconManager und data.FavoritesManager sind
                // bewusst NICHT mehr ausgeschlossen – fuer sie existieren Unit-Tests
                // (AppRepositoryTest, IconManagerTest, FavoritesManagerTest), die jetzt ehrlich
                // in die Coverage einfliessen.
                // DI-Boilerplate (Hilt): nicht sinnvoll unit-testbar, analog zu MainActivity.
                classes("com.example.androidlauncher.RethroneApplication")
                classes("com.example.androidlauncher.di.*")
                classes("*_Factory")
                classes("*_MembersInjector")
                classes("*_HiltModules*")
                classes("*Hilt_*")
            }
        }
        // Coverage-Gate gegen Regression: faellt der Anteil getesteter Zeilen unter die
        // Schwelle, schlaegt `koverVerifyDebug` (und damit der CI-Schritt) fehl. Bewusst
        // knapp unter dem aktuellen Ist-Wert angesetzt – beim Ausbau der Tests anhebbar.
        verify {
            rule("Mindest-Zeilenabdeckung") {
                bound {
                    minValue = 21
                    coverageUnits = CoverageUnit.LINE
                }
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

    // Dependency Injection (Hilt) – Manager/Repositories werden als Singletons bereitgestellt.
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)


    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation("org.json:json:20231013")

    // Formatting-Ruleset (ktlint-Wrapper) fuer Detekt – siehe config/detekt/detekt.yml.
    detektPlugins(libs.detekt.formatting)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    // Hilt-Test-Infrastruktur: @HiltAndroidTest + HiltTestApplication (Instrumented-Tests starten
    // @AndroidEntryPoint-Activities und nutzen hiltViewModel()).
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
