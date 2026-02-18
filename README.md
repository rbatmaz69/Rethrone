# Minimalist Android Launcher

Ein eleganter, performanter Launcher für Android, der mit **Jetpack Compose** von Grund auf neu geschrieben wurde. Dieser Launcher verzichtet auf unnötigen Ballast und bietet eine extrem saubere Benutzeroberfläche.

## ✨ Features
- **Minimalistisches Design:** Fokus auf Typografie und Platz.
- **Custom Line-Art Icons:** Integrierte minimalistische Symbole für System-Apps (Telefon, Kamera, Browser, Cloud-Dienste etc.).
- **Smart App Drawer:** Schneller Zugriff auf alle Apps mit integrierter Suchfunktion.
- **Favoriten-Liste:** Deine wichtigsten Apps direkt auf dem Home-Screen.
- **System-Integration:** Unterstützt das aktuelle System-Wallpaper und bietet schnellen Zugriff auf Benachrichtigungen und Einstellungen.
- **Gesten-Steuerung:** Wische nach oben für den App-Drawer oder nach unten für das Benachrichtigungsfeld.

## 🛠 Tech Stack
- **Sprache:** Kotlin
- **UI-Framework:** Jetpack Compose
- **Design:** Material 3
- **Icons:** Custom Vector Drawables (Minimalist Style)
- **Linter:** [detekt](https://detekt.dev/) für Code-Qualität und Stil.

## 📁 Projektstruktur
Hier ist ein Überblick über die wichtigsten Verzeichnisse und Dateien des Projekts:

```text
.
├── .github/workflows        # CI/CD Workflows (GitHub Actions)
├── app                      # Hauptmodul der Anwendung
│   ├── src
│   │   ├── main
│   │   │   ├── java         # Kotlin Quellcode
│   │   │   │   └── com.example.androidlauncher
│   │   │   │       ├── ui.theme  # Jetpack Compose Themes
│   │   │   │       └── MainActivity.kt
│   │   │   └── res          # Android Ressourcen (Icons, XML, etc.)
│   └── build.gradle.kts     # Modulspezifische Gradle Konfiguration
├── config                   # Konfigurationsdateien
│   └── detekt               # Detekt (Linter) Konfiguration
├── gradle                   # Gradle Wrapper und Version Catalog
│   └── libs.versions.toml   # Zentrale Verwaltung von Abhängigkeiten (Version Catalog)
├── build.gradle.kts         # Projektweite Gradle Konfiguration
└── README.md                # Projektdokumentation
```

## 🚀 Entwicklung

### Linter ausführen
Um den Linter lokal auszuführen, nutze folgenden Befehl:
```bash
./gradlew detekt
```
Der Linter ist auch in die CI-Pipeline integriert und läuft bei jedem Push und Pull-Request.
