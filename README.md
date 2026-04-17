# Minimalist Android Launcher

Ein eleganter, performanter Launcher für Android, der mit **Jetpack Compose** von Grund auf neu geschrieben wurde. Dieser Launcher verzichtet auf unnötigen Ballast und bietet eine extrem saubere Benutzeroberfläche mit Fokus auf Ästhetik und Benutzerfreundlichkeit.

## ✨ Features
- **Minimalistisches Design:** Fokus auf Typografie, Platz und klaren Linien.
- **Liquid Glass & Themes:** Wähle aus fünf eleganten Farbthemen (Gelb, Blau, Rot, Grün, Lila) und aktiviere den modernen "Liquid Glass"-Effekt.
- **Dynamische Kontraste:** Unterstützung für Dark- und Light-Text-Modus für optimale Lesbarkeit auf jedem Wallpaper.
- **Custom Line-Art Icons:** Integrierte minimalistische Symbole (Lucide Icons) für ein einheitliches Erscheinungsbild.
- **Smart App Drawer mit Ordnern:** Organisiere deine Apps in Ordnern, erstelle Favoriten und nutze die integrierte Suchfunktion.
- **Return Animation:** Hochwertige, iOS-ähnliche Animationen, die Icons exakt an ihre Ursprungsposition zurückführen.
- **Favoriten-Liste:** Deine wichtigsten Apps direkt auf dem Home-Screen, optional mit oder ohne Labels.
- **Anpassbarkeit:** Konfiguriere Icon-Größe und Schriftgröße global für den gesamten Launcher.
- **Gesten-Steuerung:** Wische nach oben für den App-Drawer oder nach unten für das Benachrichtigungsfeld.
- **Info-Bereich:** Transparenter Zugriff auf App-Version, Lizenzen und Entwicklerinformationen.

## 🛠 Tech Stack
- **Sprache:** Kotlin 2.1.0
- **UI-Framework:** Jetpack Compose (BOM 2024.09.00)
- **Design:** Material 3 Components
- **Icons:** [Lucide Icons](https://lucide.dev/) (v1.1.0)
- **SDK:** Android SDK 36 (Target)
- **Linter:** [detekt](https://detekt.dev/) (v1.23.8) für statische Code-Analyse.
- **Test Coverage:** [Kover](https://github.com/Kotlin/kotlinx-kover) (v0.9.0) für Code-Coverage-Reports.

## 👥 Entwickler
- **Refik Bilal Batmaz**
- **Vinisan Kunasegaran**

## 📄 Lizenz
Dieses Projekt unterliegt einer **Custom Non-Commercial License**.
- ✅ **Erlaubt:** Kostenlose Nutzung und Installation der APK für private Zwecke. Einsicht und Modifikation des Quellcodes für private Zwecke.
- ❌ **Verboten:** Jegliche kommerzielle Nutzung, Verkauf, kostenpflichtige Weitergabe oder Monetarisierung (z.B. Werbung).

## 📁 Projektstruktur
Hier ist ein Überblick über die wichtigsten Verzeichnisse und Dateien des Projekts:

```text
.
├── app                      # Hauptmodul der Anwendung
│   ├── src
│   │   ├── main
│   │   │   ├── java         # Kotlin Quellcode
│   │   │   │   └── com.example.androidlauncher
│   │   │   │       ├── data       # Datenmodelle & Manager (AppInfo, FolderInfo, ThemeManager)
│   │   │   │       ├── ui         # UI-Komponenten (Compose)
│   │   │   │       │   ├── theme  # Design System (Farben, Typografie)
│   │   │   │       │   └── ...    # Screens & Menüs (AppDrawer, SettingsPaletteMenu, InfoDialog)
│   │   │   │       └── MainActivity.kt  # Einstiegspunkt & Navigation
│   │   │   └── res          # Android Ressourcen (Icons, Strings, XML)
│   │   └── test             # JVM Unit-Tests
│   └── build.gradle.kts     # Modulspezifische Gradle Konfiguration
├── config                   # Konfigurationsdateien
│   └── detekt               # Detekt (Linter) Konfiguration
├── gradle                   # Gradle Wrapper und Version Catalog
│   └── libs.versions.toml   # Zentrale Verwaltung von Abhängigkeiten (Version Catalog)
├── build.gradle.kts         # Projektweite Gradle Konfiguration
├── settings.gradle.kts      # Projekt-Einstellungen & inkludierte Module
└── README.md                # Projektdokumentation
```

## 🚀 Entwicklung

### Voraussetzungen
- Android Studio (aktuelle stabile Version)
- JDK 17
- Gradle-Wrapper/Daemon ebenfalls mit JDK 17

### Projekt bauen
```bash
./gradlew assembleDebug
```

### App auf Geraet/Emulator installieren
```bash
./gradlew run
```

### Tests ausführen
```bash
./gradlew :app:testDebugUnitTest
```

### Coverage erzeugen
```bash
./gradlew koverXmlReportDebug koverLogDebug
```

### Linter ausführen
Um den Linter lokal auszuführen, nutze folgenden Befehl:
```bash
./gradlew detekt
```

## Troubleshooting
- `./gradlew clean build` ist erfolgreich, auch wenn `stripDebugDebugSymbols`/`stripReleaseDebugSymbols` meldet, dass bestimmte `.so`-Dateien nicht gestript werden koennen. Das sind in diesem Kontext Warnungen.
- Wenn `./gradlew clean run` vorher mit `Task 'run' not found` abgebrochen ist: der Root-Task `run` ist nun als Alias fuer `:app:installDebug` vorhanden.
- Nutze immer den Wrapper (`./gradlew`) statt `gradle`, damit die im Projekt festgelegte Version aus `gradle/wrapper/gradle-wrapper.properties` verwendet wird.
- Die Warnung zu `HAPTIC_FEEDBACK_ENABLED` in `ThemeManager.kt` ist eine Deprecation-Warnung und kein Build-Blocker.
- Fuer detailierte Plugin-/Gradle-Warnungen:
```bash
./gradlew --warning-mode all help
```

## 🤖 CI (GitHub Actions)
- Läuft **nur** bei Pull Requests auf den Branch `develop`.
- Enthält Lint, Unit-Tests und Coverage-Summary im Actions-Tab.
