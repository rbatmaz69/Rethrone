package com.example.androidlauncher

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service that listens for notification events.
 * Provides a real-time set of package names that have active notifications.
 */
class NotificationService : NotificationListenerService() {

    companion object {
        private val _activeNotificationPackages = MutableStateFlow<Set<String>>(emptySet())
        val activeNotificationPackages = _activeNotificationPackages.asStateFlow()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateNotifications()
    }

    /**
     * Updates the flow with the current set of package names that have active notifications.
     */
    private fun updateNotifications() {
        try {
            // activeNotifications is a property of NotificationListenerService returning StatusBarNotification[]
            val notifications = activeNotifications ?: return
            val activePkgs = notifications.asSequence()
                .filterNotNull()
                .filterNot { it.isMediaNotification() }   // Mediensteuerung erzeugt keinen Punkt
                .mapNotNull { it.packageName }
                .toSet()
            _activeNotificationPackages.value = activePkgs
        } catch (e: Exception) {
            // In some cases (e.g. during binding) activeNotifications might not be available yet
        }
    }

    /**
     * Erkennt Medien-/Wiedergabe-Benachrichtigungen (Mediensteuerung). Diese bleiben nach dem Schließen
     * eines Videos pausiert bestehen und sollen daher keinen Benachrichtigungspunkt auslösen.
     * EXTRA_MEDIA_SESSION (MediaSession-Token) ist der robusteste Marker für MediaStyle-Benachrichtigungen,
     * unabhängig vom Play/Pause-Zustand; CATEGORY_TRANSPORT deckt zusätzliche Transport-Steuerungen ab.
     */
    private fun StatusBarNotification.isMediaNotification(): Boolean {
        val n = notification ?: return false
        if (n.category == Notification.CATEGORY_TRANSPORT) return true
        return n.extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true
    }
}
