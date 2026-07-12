package com.example.androidlauncher.ui.home

import com.example.androidlauncher.data.FolderInfo

/**
 * Das aktuell geöffnete Vollbild-Overlay des Startbildschirms (Config-Menüs,
 * Info-Dialog, Ordner-Konfiguration). Ersetzt die früheren 17 sich gegenseitig
 * ausschließenden Booleans in der MainActivity durch einen einzigen Zustand –
 * damit ist per Konstruktion immer höchstens ein Overlay offen und die
 * Back-Navigation hat eine einzige autoritative Quelle.
 *
 * Die Top-Level-Flächen (App-Drawer, Suche, Einstellungs-Palette, Edit-Modus)
 * bleiben eigene Zustände, weil sie unter einem Overlay weiterbestehen können.
 */
sealed interface ActiveOverlay {
    /** Kein Overlay offen – der Startbildschirm ist frei. */
    data object None : ActiveOverlay

    data object FavoritesConfig : ActiveOverlay

    /** Kategorie-Seite „Aussehen" des Einstellungs-Hubs (Themen, Farben, Schrift, Icons, Wallpaper, Animationen). */
    data object AppearanceSettings : ActiveOverlay

    /** Kategorie-Seite „Startbildschirm" (Favoriten, Layout, Widgets, Dynamic Island, Edge-Beleuchtung). */
    data object HomescreenSettings : ActiveOverlay

    /** Kategorie-Seite „Apps" (App-Zugriff, Ausblenden, Sperre, Deinstallieren). */
    data object AppsSettings : ActiveOverlay

    /** Kategorie-Seite „Suche" (Vorschläge, Verlauf). */
    data object SearchSettings : ActiveOverlay

    /** Kategorie-Seite „System" (Haptik, Backup, Berechtigungen, Info). */
    data object SystemSettings : ActiveOverlay

    data object ColorConfig : ActiveOverlay
    data object AnimationsConfig : ActiveOverlay
    data object EdgeLightingConfig : ActiveOverlay
    data object GesturesConfig : ActiveOverlay
    data object DesignMenu : ActiveOverlay
    data object ThemeMenu : ActiveOverlay
    data object SizeConfig : ActiveOverlay
    data object FontSelection : ActiveOverlay
    data object EditConfig : ActiveOverlay
    data object IconConfig : ActiveOverlay
    data object UninstallApps : ActiveOverlay
    data object HiddenApps : ActiveOverlay
    data object AppLock : ActiveOverlay
    data object WallpaperConfig : ActiveOverlay
    data object Info : ActiveOverlay

    /** Auswahl eines System-Widgets für den Startbildschirm (B1, AppWidgetHost). */
    data object WidgetPicker : ActiveOverlay

    /** Konfiguration eines konkreten Ordners (früher `selectedFolderForConfig`). */
    data class FolderConfig(val folder: FolderInfo) : ActiveOverlay
}
