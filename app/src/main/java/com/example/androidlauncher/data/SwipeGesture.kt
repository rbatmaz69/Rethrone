package com.example.androidlauncher.data

/** Wisch-Richtung auf dem Startbildschirm, der eine [GestureAction] zugeordnet werden kann. */
enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }

/** Einer Wisch-Richtung zugeordnete Aktion samt Ziel-App für [GestureAction.OPEN_APP]. */
data class SwipeBinding(
    val action: GestureAction,
    val appPackage: String? = null,
)

/**
 * Vollständige Swipe-Gesten-Belegung des Startbildschirms. Fehlende Richtungen
 * fallen auf die Defaults zurück, die dem früher fest verdrahteten Verhalten
 * entsprechen (hoch = App-Drawer, runter = Benachrichtigungen).
 */
data class SwipeGestureConfig(
    private val bindings: Map<SwipeDirection, SwipeBinding> = emptyMap(),
) {
    operator fun get(direction: SwipeDirection): SwipeBinding =
        bindings[direction] ?: SwipeBinding(defaultActionFor(direction))

    companion object {
        fun defaultActionFor(direction: SwipeDirection): GestureAction = when (direction) {
            SwipeDirection.UP -> GestureAction.APP_DRAWER
            SwipeDirection.DOWN -> GestureAction.NOTIFICATIONS
            SwipeDirection.LEFT, SwipeDirection.RIGHT -> GestureAction.NONE
        }
    }
}
