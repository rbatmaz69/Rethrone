// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApp) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinCompose) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover) apply false
}

// Detekt wird im :app-Modul konfiguriert (app/build.gradle.kts), da dort der Quellcode liegt –
// das ist die einzige Stelle, an der die Config + Baseline gepflegt werden.

tasks.register("run") {
    group = "application"
    description = "Installiert die Debug-APK auf einem verbundenen Geraet/Emulator (Alias fuer :app:installDebug)."
    dependsOn(":app:installDebug")
}

