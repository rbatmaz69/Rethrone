<div align="center">

<img src="Tulpe_minimalist.png" alt="Rethrone Logo" width="160" />

```
  ____      _   _                          
 |  _ \ ___| |_| |__  _ __ ___  _ __   ___ 
 | |_) / _ \ __| '_ \| '__/ _ \| '_ \ / _ \
 |  _ <  __/ |_| | | | | | (_) | | | |  __/
 |_| \_\___|\__|_| |_|_|  \___/|_| |_|\___|
```

### 👑 Lang lebe der Homescreen.

*Ein eleganter, blitzschneller Minimalist-Launcher für Android — komplett in **Jetpack Compose** von Grund auf neu gebaut. Kein Ballast, keine Werbung, nur eine extrem saubere Oberfläche mit Fokus auf Ästhetik und Geschwindigkeit.*

<br/>

![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.09-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%203-757575?style=for-the-badge&logo=materialdesign&logoColor=white)

![Min SDK](https://img.shields.io/badge/min%20SDK-26-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Target SDK](https://img.shields.io/badge/target%20SDK-36-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![License](https://img.shields.io/badge/License-Non--Commercial-FF6B6B?style=for-the-badge)

</div>

---

## ✨ Was Rethrone kann

> 🪶 **Minimalistisches Design** — Fokus auf Typografie, Platz und klare Linien. Nichts lenkt ab.

> 🧊 **Liquid Glass & Themes** — Fünf elegante Farbthemen (Gelb, Blau, Rot, Grün, Lila), ein freies **Color-Wheel** für jede beliebige Farbe und der moderne *Liquid-Glass*-Effekt.

> 🌗 **Dynamische Kontraste** — Dark- und Light-Text-Modus für optimale Lesbarkeit auf jedem Wallpaper.

> 🔤 **Volle Typo-Kontrolle** — Eigene Schriftarten, Schriftstärke und global einstellbare Icon- & Schriftgrößen.

> 🖼️ **Wallpaper-Studio** — Hintergrund setzen und direkt im Launcher zuschneiden (uCrop).

> ✒️ **Custom Line-Art Icons** — Integrierte minimalistische Lucide-Symbole für ein einheitliches Erscheinungsbild.

> 🎬 **Return-Animation** — Hochwertige, iOS-ähnliche Animationen, die Icons exakt an ihre Ursprungsposition zurückführen.

<details>
<summary><b>📂 Smart App Drawer & Suche</b> — zum Aufklappen</summary>

<br/>

- **Niagara-Style A–Z-Scrubber** für blitzschnelle Navigation durch alle Apps.
- **Ordner** zum Organisieren deiner Apps.
- **Favoriten-Liste** direkt auf dem Home-Screen — optional mit oder ohne Labels.
- **Hybrid-Suche** mit intelligenten Vorschlägen.

</details>

<details>
<summary><b>🗂️ App-Verwaltung</b> — zum Aufklappen</summary>

<br/>

- Apps **direkt aus dem Launcher deinstallieren**.
- **App-Shortcuts** per Long-Press (Deep-Links direkt in App-Funktionen).
- Kontextmenü pro App für schnelle Aktionen.

</details>

<details>
<summary><b>🤙 Gesten & Geräte-Aktionen</b> — zum Aufklappen</summary>

<br/>

- **Wisch-Gesten:** nach oben für den App-Drawer, nach unten für das Benachrichtigungsfeld.
- **Shake-Gesten** mit frei konfigurierbaren Aktionen (z.B. Taschenlampe).
- **Benachrichtigungs-Integration** auf dem Home-Screen.

</details>

> ℹ️ **Info-Bereich** — Transparenter Zugriff auf App-Version, Lizenzen und Entwicklerinformationen.

---

## 🛠 Tech Stack

| | |
|---|---|
| 🟣 **Sprache** | Kotlin `2.2.10` |
| 🎨 **UI-Framework** | Jetpack Compose (BOM `2024.09.00`) |
| 🧱 **Design** | Material 3 Components |
| 🏗️ **Build** | Android Gradle Plugin `9.2.1` |
| ✒️ **Icons** | [Lucide Icons](https://lucide.dev/) `1.1.0` |
| ✂️ **Bild-Zuschnitt** | [uCrop](https://github.com/Yalantis/uCrop) `2.2.8` |
| 📱 **SDK** | compile/target `36` · min `26` |
| 🔍 **Linter** | [detekt](https://detekt.dev/) `1.23.8` |
| 📊 **Coverage** | [Kover](https://github.com/Kotlin/kotlinx-kover) `0.9.8` |

---

## 👥 Entwickler

| 🧑‍💻 | |
|---|---|
| **Refik Bilal Batmaz** | |
| **Vinisan Kunasegaran** | |

---

## 📄 Lizenz

Dieses Projekt unterliegt einer **Custom Non-Commercial License**.

- ✅ **Erlaubt:** Kostenlose Nutzung und Installation der APK für private Zwecke. Einsicht und Modifikation des Quellcodes für private Zwecke.
- ❌ **Verboten:** Jegliche kommerzielle Nutzung, Verkauf, kostenpflichtige Weitergabe oder Monetarisierung (z.B. Werbung).

---

## 📁 Projektstruktur

```text
.
├── app                      # 👑 Hauptmodul der Anwendung
│   ├── src
│   │   ├── main
│   │   │   ├── java         # Kotlin Quellcode
│   │   │   │   └── com.example.androidlauncher
│   │   │   │       ├── data       # Datenmodelle & Manager (AppInfo, FolderManager, ThemeManager …)
│   │   │   │       ├── ui         # UI-Komponenten in Compose (AppDrawer, Menüs, Wallpaper-Crop …)
│   │   │   │       │   └── theme  # Design System (Farben, Typografie)
│   │   │   │       ├── *Service   # Notification- & Accessibility-Services, Shake-/Return-Logik
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
└── README.md                # Du bist hier 👋
```

---

## 🚀 Entwicklung

### 🧰 Voraussetzungen
- Android Studio (aktuelle stabile Version)
- JDK 17
- Gradle-Wrapper/Daemon ebenfalls mit JDK 17

### 🔨 Projekt bauen
```bash
./gradlew assembleDebug
```

### ▶️ Debug-App installieren (Standard)
```bash
./gradlew run        # Alias für :app:installDebug
```

### ⚡ Release-App testen — schnell & über WLAN

Der Release-Build läuft mit R8/Minify und ohne `debuggable`-Overhead und ist daher **spürbar schneller** als Debug. Er wird mit dem Debug-Keystore signiert → über WLAN installierbar und als In-Place-Update *neben* dem Debug-Build (keine Deinstallation nötig).

```bash
# 1) Handy per WLAN-Debugging verbinden (Android 11+)
#    Codes aus: Entwickleroptionen → WLAN-Debugging → Mit Pairing-Code koppeln
adb pair    <handy-ip>:<pair-port> <pairing-code>
adb connect <handy-ip>:<debug-port>
adb devices                                   # Gerät sollte gelistet sein

# 2) Release bauen & installieren
./gradlew installRelease

# 3) Launcher starten (oder einfach die Home-Taste drücken)
adb shell am start -n com.example.androidlauncher/.MainActivity
```

> 💡 Wegen aktivem ProGuard/R8 die App nach einem Release-Build einmal komplett durchklicken. Fehlt zur Laufzeit eine Klasse, eine passende `-keep`-Regel in `app/proguard-rules.pro` ergänzen.

### 🧪 Tests ausführen
```bash
./gradlew :app:testDebugUnitTest
```

### 📊 Coverage erzeugen
```bash
./gradlew koverXmlReportDebug koverLogDebug
```

### 🔍 Linter ausführen
```bash
./gradlew detekt
```

---

## 🩺 Troubleshooting

- `./gradlew clean build` ist erfolgreich, auch wenn `stripDebugDebugSymbols`/`stripReleaseDebugSymbols` meldet, dass bestimmte `.so`-Dateien nicht gestript werden können. Das sind in diesem Kontext nur Warnungen.
- Wenn `./gradlew clean run` vorher mit `Task 'run' not found` abgebrochen ist: der Root-Task `run` ist nun als Alias für `:app:installDebug` vorhanden.
- Nutze immer den Wrapper (`./gradlew`) statt `gradle`, damit die im Projekt festgelegte Version aus `gradle/wrapper/gradle-wrapper.properties` verwendet wird.
- Die Warnung zu `HAPTIC_FEEDBACK_ENABLED` in `ThemeManager.kt` ist eine Deprecation-Warnung und kein Build-Blocker.
- Die zahlreichen AGP-Deprecation-`WARNING`s bei `installRelease` sind harmlos — solange `BUILD SUCCESSFUL` erscheint, ist alles in Ordnung.
- Für detaillierte Plugin-/Gradle-Warnungen:
```bash
./gradlew --warning-mode all help
```

---

## 🤖 CI (GitHub Actions)

- **`android.yml`** — läuft bei **Push auf `main`** und bei **Pull Requests auf `main` / `develop`**. Enthält Detekt-Lint, Unit-Tests und eine Coverage-Summary im Actions-Tab.
- **`release.yml`** — separater Workflow rund um Release-Artefakte.

<div align="center">

<br/>

*Made with 🪷 & Jetpack Compose.*

</div>
