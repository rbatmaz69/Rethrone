package com.example.androidlauncher.data

import android.content.ComponentName

/**
 * Ein auf dem Startbildschirm platziertes System-Widget (AppWidgetHost, B1).
 *
 * Wird als JSON-Liste in den HomeLayoutSettings persistiert (siehe [WidgetSerializer]);
 * die Offsets liegen bewusst mit im Eintrag, damit Hinzufuegen/Verschieben/Entfernen
 * ein einziger atomarer DataStore-Write ist.
 */
data class HostedWidget(
    /** Vom AppWidgetHost allokierte, systemweite Widget-ID. */
    val appWidgetId: Int,
    /** Provider als `ComponentName.flattenToString()` – JSON-freundlich. */
    val provider: String,
    val widthDp: Int,
    val heightDp: Int,
    /** Verschiebung in px relativ zur natuerlichen Ankerposition (gleiche Semantik wie HomeLayout). */
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    fun providerComponent(): ComponentName? = ComponentName.unflattenFromString(provider)
}
