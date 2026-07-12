package com.example.androidlauncher.ui.home

import androidx.annotation.StringRes
import com.example.androidlauncher.R

/**
 * Ein durchsuchbarer Einstellungs-Eintrag für das Suchfeld im Einstellungs-Hub.
 *
 * Bewusst KEIN deklaratives Menü-Modell: Die Seiten bleiben handgeschriebene
 * Composables; die Registry ist nur ein leichter Index darüber (Label,
 * Kategorie-Breadcrumb, optionale Synonyme und der Overlay-Pfad zum Ziel).
 */
data class SettingsSearchEntry(
    /** Stabile Id; dient zugleich als Highlight-Schlüssel auf der Zielseite. */
    val id: String,
    @param:StringRes val labelRes: Int,
    /** Kategorie-Name als Breadcrumb im Suchergebnis (z. B. „Startbildschirm"). */
    @param:StringRes val categoryRes: Int,
    /**
     * Overlays, die der Reihe nach geöffnet werden (ohne den Hub selbst) –
     * jedes openOverlay() pusht den Vorgänger, sodass die Back-Kette exakt der
     * manuellen Navigation entspricht.
     */
    val path: List<ActiveOverlay>,
    /** Optionale, lokalisierte Synonyme (kommagetrennt), z. B. „Hintergrund,Bild,Foto". */
    @param:StringRes val keywordsRes: Int? = null,
)

/**
 * Filtert die Einträge per Substring-Match (case-insensitive) über Label,
 * Synonyme und Kategorie-Namen. Pure Funktion – die String-Auflösung wird
 * injiziert, damit die Logik JVM-testbar bleibt.
 */
fun filterSettings(
    entries: List<SettingsSearchEntry>,
    query: String,
    resolve: (Int) -> String,
): List<SettingsSearchEntry> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    return entries.filter { entry ->
        resolve(entry.labelRes).lowercase().contains(q) ||
            resolve(entry.categoryRes).lowercase().contains(q) ||
            entry.keywordsRes?.let { res ->
                resolve(res).lowercase().split(',').any { keyword -> keyword.trim().contains(q) }
            } == true
    }
}

/** Index aller Einstellungen: eine Zeile pro Eintrag der Kategorie-Seiten. */
object SettingsSearchRegistry {

    val entries: List<SettingsSearchEntry> = buildList {
        // Aussehen
        appearance("themes", R.string.label_themes)
        appearance("colors", R.string.color_config_title, R.string.search_kw_colors)
        appearance("design_style", R.string.label_design_style)
        appearance("font_size", R.string.size_config_title, R.string.search_kw_font)
        appearance("app_icons", R.string.edit_app_icons)
        appearance("wallpaper", R.string.change_wallpaper, R.string.search_kw_wallpaper)
        appearance("animations", R.string.label_animations)

        // Startbildschirm
        homescreen("favorites", R.string.favorites_title)
        homescreen("home_layout", R.string.edit_home_layout)
        homescreen("add_widget", R.string.add_widget)
        homescreen("clock_widget", R.string.clock_widget, R.string.search_kw_clock)
        homescreen("calendar_widget", R.string.calendar_widget)
        homescreen("weather_widget", R.string.weather_widget)
        homescreen("dynamic_island", R.string.dynamic_island, R.string.search_kw_island)
        homescreen("edge_lighting", R.string.edge_lighting)

        // Apps
        apps("app_access", R.string.app_access_label)
        apps("hidden_apps", R.string.hide_apps, R.string.search_kw_hidden)
        apps("app_lock", R.string.app_lock)
        apps("uninstall_apps", R.string.uninstall_apps)

        // Suche
        search("smart_suggestions", R.string.smart_suggestions)
        search("clear_history", R.string.clear_search_history)

        // Gesten (eigene Seite, kein Highlight)
        add(
            SettingsSearchEntry(
                id = "gestures",
                labelRes = R.string.label_gestures,
                categoryRes = R.string.label_gestures,
                path = listOf(ActiveOverlay.GesturesConfig),
            )
        )

        // System
        system("haptics", R.string.haptic_feedback, R.string.search_kw_haptics)
        system("backup_export", R.string.backup_export, R.string.search_kw_backup)
        system("backup_import", R.string.backup_import, R.string.search_kw_backup)
        system("default_launcher", R.string.default_launcher)
        system("notifications", R.string.notifications_label)
        system("accessibility", R.string.accessibility_label)
        system("usage_access", R.string.usage_access_label)
        system("info", R.string.label_info)
    }

    private fun MutableList<SettingsSearchEntry>.appearance(
        id: String,
        @StringRes labelRes: Int,
        @StringRes keywordsRes: Int? = null,
    ) = add(
        SettingsSearchEntry(
            id = id,
            labelRes = labelRes,
            categoryRes = R.string.section_appearance,
            path = listOf(ActiveOverlay.AppearanceSettings),
            keywordsRes = keywordsRes,
        )
    )

    private fun MutableList<SettingsSearchEntry>.homescreen(
        id: String,
        @StringRes labelRes: Int,
        @StringRes keywordsRes: Int? = null,
    ) = add(
        SettingsSearchEntry(
            id = id,
            labelRes = labelRes,
            categoryRes = R.string.section_homescreen,
            path = listOf(ActiveOverlay.HomescreenSettings),
            keywordsRes = keywordsRes,
        )
    )

    private fun MutableList<SettingsSearchEntry>.apps(
        id: String,
        @StringRes labelRes: Int,
        @StringRes keywordsRes: Int? = null,
    ) = add(
        SettingsSearchEntry(
            id = id,
            labelRes = labelRes,
            categoryRes = R.string.section_apps,
            path = listOf(ActiveOverlay.AppsSettings),
            keywordsRes = keywordsRes,
        )
    )

    private fun MutableList<SettingsSearchEntry>.search(
        id: String,
        @StringRes labelRes: Int,
        @StringRes keywordsRes: Int? = null,
    ) = add(
        SettingsSearchEntry(
            id = id,
            labelRes = labelRes,
            categoryRes = R.string.section_search,
            path = listOf(ActiveOverlay.SearchSettings),
            keywordsRes = keywordsRes,
        )
    )

    private fun MutableList<SettingsSearchEntry>.system(
        id: String,
        @StringRes labelRes: Int,
        @StringRes keywordsRes: Int? = null,
    ) = add(
        SettingsSearchEntry(
            id = id,
            labelRes = labelRes,
            categoryRes = R.string.section_system,
            path = listOf(ActiveOverlay.SystemSettings),
            keywordsRes = keywordsRes,
        )
    )
}
