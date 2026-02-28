import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApp)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.example.androidlauncher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.androidlauncher"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
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
                classes("com.example.androidlauncher.data.FolderManager")
                classes("com.example.androidlauncher.data.FolderManager\$*")
                classes("com.example.androidlauncher.data.FolderManagerKt")
                classes("com.example.androidlauncher.data.ThemeManager")
                classes("com.example.androidlauncher.data.ThemeManager\$*")
                classes("com.example.androidlauncher.data.ThemeManagerKt")
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
                classes("com.example.androidlauncher.ui.BottomSearchKt")
                classes("com.example.androidlauncher.ui.BottomSearchKt\$*")
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
    
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // Lucide Icons
    implementation(libs.lucide.icons)

    // uCrop
    implementation(libs.ucrop)

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
