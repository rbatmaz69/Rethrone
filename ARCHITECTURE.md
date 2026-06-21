# Architektur

Dieses Dokument beschreibt den Aufbau von **Rethrone**, einem Android-Launcher auf Basis
von Jetpack Compose. Es richtet sich an Entwickler, die Features ergänzen oder Bugs beheben
und dabei die bestehenden Konventionen einhalten wollen.

## Überblick

Rethrone ist ein **Single-Module-Projekt** (`:app`). Der Code ist in drei Schichten plus
einigen Einstiegspunkten (Activities/Services) organisiert:

```
com.example.androidlauncher
├── (Wurzel)        Activities, Services, plattformnahe Logik
│   ├── MainActivity.kt              Haupt-Activity, Compose-Einstieg, UI-Zustand
│   ├── AppLockActivity.kt           Sperrbildschirm
│   ├── LauncherAccessibilityService Bedienungshilfen-Dienst (Bildschirm sperren)
│   ├── NotificationService.kt       Benachrichtigungs-Zugriff
│   ├── LauncherLogic.kt             zustandslose, reine Geschäftslogik (Filter/Favoriten/Folder/Merge)
│   ├── LauncherDeviceActions.kt     Kapselung der Geräte-/System-APIs (Taschenlampe, DND, Kamera …)
│   ├── ForegroundAppResolver.kt     Usage-Stats: zuletzt aktive App
│   ├── ReturnOriginStore.kt         Persistenz der Rückkehr-Animations-Ursprünge
│   └── Return*/Shake*               State-Maschinen für Rückkehr-Animation & Shake-Geste
│
├── gesture/        Gesten-Verarbeitung
│   └── GestureActionHandler.kt      führt Geräte-/System-Aktionen einer Geste aus (testbar)
│
├── data/           Datenschicht: Repositories, Manager, Modelle
│   ├── AppRepository.kt             installierte Apps + Icon-Laden (kapselt PackageManager)
│   ├── ThemeManager.kt              alle UI-Einstellungen (DataStore)
│   ├── IconManager.kt               Custom-/Auto-Icon-Regeln
│   ├── FavoritesManager.kt          Favoriten
│   ├── FolderManager.kt             Ordner (+ FolderSerializer)
│   ├── AppLockManager.kt            App-Sperre (PBKDF2) + CryptoManager
│   ├── SearchSuggestionsManager.kt  Suchverlauf & App-Nutzung
│   ├── WeatherRepository.kt         Wetter (Open-Meteo)
│   └── *Modelle*                    AppInfo, FolderInfo, GestureAction, ColorTheme, FontSize …
│
└── ui/             Compose-UI
    ├── HomeScreen.kt, AppDrawer.kt, HybridSearch.kt …   Bildschirme
    ├── *ConfigMenu.kt                                   Einstellungs-Menüs
    ├── AppDrawerViewModel.kt                            ViewModel des App-Drawers
    ├── onboarding/                                      Onboarding-Flow
    └── theme/                                           Farben, Typografie, Theme-CompositionLocals
```

## Schichten & Verantwortlichkeiten

**Datenschicht (`data/`).** Repositories und Manager kapseln Persistenz (DataStore /
SharedPreferences) und Plattform-APIs. Zustand wird reaktiv als `Flow`/`StateFlow`
herausgegeben. Diese Klassen werden per **Hilt** als prozessweite Singletons bereitgestellt
(`di/DataModule`) und in Activities/ViewModels injiziert. Die Manager besitzen weiterhin einen
`constructor(context)` **und** einen DataStore-Konstruktor – Letzterer dient Unit-Tests, die
einen Fake-DataStore einspeisen.

**Geschäftslogik (`LauncherLogic`, `gesture/`).** Reine, framework-freie Logik gehört nach
`LauncherLogic` (ein zustandsloses `object`) oder in eine eigene Klasse mit injizierten
Abhängigkeiten (Vorbild: `GestureActionHandler`). Solche Logik ist **ohne Android-Framework
unit-testbar** und ist die bevorzugte Heimat für jede Entscheidungs-/Transformationslogik.

**UI-Schicht (`ui/`).** Composables sind möglichst dumm: sie rendern Zustand und melden
Ereignisse nach oben. Komplexere Entscheidungslogik wird nicht im Composable eingebettet,
sondern an `LauncherLogic`/einen Handler/ein ViewModel delegiert.

## Zustands-Konventionen

- **Reiner UI-Zustand** (offen/zu, Animationsfortschritt) lebt als `remember { mutableStateOf(...) }`
  im Composable.
- **Persistenter/abgeleiteter Zustand** kommt als `Flow`/`StateFlow` aus der Datenschicht und
  wird per `collectAsState()` konsumiert.
- **Neue, nicht-triviale Logik** gehört in ein ViewModel (`AppDrawerViewModel` als Vorbild)
  oder in eine reine Logik-Klasse – **nicht** als verschachtelte Funktion in `setContent`.

> Hinweis: `MainActivity` ist historisch groß und hält viel UI-Zustand direkt im
> `setContent`-Block. Neue Features sollten diesem Muster **nicht** folgen, sondern Logik
> nach außen ziehen (siehe `GestureActionHandler` und `LauncherLogic.mergeInstalledApps`
> als jüngste Beispiele für die bevorzugte Richtung).

## Ein Feature hinzufügen – Leitfaden

1. **Daten?** Wenn etwas persistiert/geladen wird → Manager/Repository in `data/` (oder
   bestehenden erweitern), Zustand als `Flow` herausgeben.
2. **Logik?** Entscheidungs-/Transformationslogik → reine Funktion in `LauncherLogic` oder
   eine eigene Klasse mit injizierten Abhängigkeiten. **Dafür einen Unit-Test schreiben.**
3. **UI?** Composable in `ui/`, das den Zustand rendert und Ereignisse nach oben meldet.
4. **Verdrahtung** in `MainActivity`/dem zuständigen Screen – so dünn wie möglich.

## Test- & Qualitätsstrategie

- **Unit-Tests** (`app/src/test`, JUnit4 + MockK + Turbine + coroutines-test) decken die
  Logik-/Datenschicht ab. Reine Logik wird direkt getestet; Plattform-Abhängigkeiten werden
  gemockt (z. B. `LauncherDeviceActions`, `PackageManager`) oder hinter Callbacks abstrahiert
  (z. B. `GestureActionEffects`).
- **Instrumentierte Tests** (`app/src/androidTest`, Compose-UI-Test/Espresso) decken
  ausgewählte UI-Flows ab und laufen in CI im **Emulator-Job** (`connectedDebugAndroidTest`,
  vorerst `continue-on-error`).
- **Coverage** misst Kover; ein Gate (`koverVerifyDebug`) verhindert Regression unter die
  konfigurierte Mindest-Zeilenabdeckung. UI-/Theme-/DI-Boilerplate ist aus der Messung
  ausgenommen (siehe `app/build.gradle.kts`). Ergänzend prüft ein **Diff-Coverage-Gate**
  (`.github/scripts/diff_coverage.py`, nur in PRs), dass *geänderte* Zeilen getestet sind.
- **Test-Reports** erscheinen als PR-Annotationen (dorny/test-reporter) und werden als
  CI-Artefakt hochgeladen.
- **Statische Analyse** via Detekt inkl. ktlint-Formatierung (`config/detekt/detekt.yml`).
  Bestehende Alt-Verstöße sind in `config/detekt/baseline.xml` eingefroren – **neuer Code
  muss die Regeln erfüllen.** Lokal autoformatieren: `./gradlew detekt --auto-correct`.

### Nützliche Befehle

```bash
./gradlew detekt              # Linting/Formatierung (CI-Gate)
./gradlew testDebugUnitTest   # Unit-Tests
./gradlew koverVerifyDebug    # Coverage-Gate
./gradlew koverXmlReportDebug # Coverage-Report
./gradlew assembleDebug       # Debug-Build
./gradlew run                 # Debug-APK auf Gerät/Emulator installieren
```

## Bekannte Schulden / nächste Schritte

- `MainActivity` (~1900 Zeilen) weiter entflechten: Logik schrittweise in ViewModels/Handler
  ziehen. Mit Hilt steht die Verdrahtung dafür nun bereit (Konstruktor-Injection in ViewModels).
- ~~Kein DI-Framework~~ **Erledigt:** Hilt ist eingeführt (`di/DataModule`, `@HiltViewModel`,
  `@AndroidEntryPoint`). Datenschicht-Singletons werden injiziert statt `remember { Manager(context) }`.
- Plattform-nahe Klassen (z. B. `AppRepository` → `PackageManager`) hinter Interfaces/Seams
  legen, um sie ohne Emulator unit-testbar zu machen (Vorbild: `LauncherDeviceActions`).
- Keine `domain/`-Use-Case-Schicht; reine Logik liegt aktuell in `LauncherLogic`.
- Große `*ConfigMenu.kt`/`HomeScreen.kt`-Composables könnten in kleinere Komponenten zerlegt
  werden.
