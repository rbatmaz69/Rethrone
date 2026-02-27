package com.example.androidlauncher

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
            val activePkgs = notifications.mapNotNull { it.packageName }.toSet()
            _activeNotificationPackages.value = activePkgs
        } catch (e: Exception) {
            // In some cases (e.g. during binding) activeNotifications might not be available yet
        }
    }
}
