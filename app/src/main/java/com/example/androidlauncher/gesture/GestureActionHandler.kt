package com.example.androidlauncher.gesture

import android.app.Activity
import com.example.androidlauncher.DndToggleResult
import com.example.androidlauncher.FlashlightToggleResult
import com.example.androidlauncher.LauncherDeviceActions
import com.example.androidlauncher.R
import com.example.androidlauncher.data.GestureAction

/**
 * Nutzerseitige Seiteneffekte einer Geste, die der [GestureActionHandler] auslöst, aber
 * bewusst nicht selbst umsetzt (Toast/Permission/Settings hängen am Activity-/Compose-Kontext).
 *
 * Durch diese Abstraktion bleibt die Dispatch-Logik im Handler frei von Android-UI-Aufrufen
 * und damit unit-testbar.
 */
interface GestureActionEffects {
    /**
     * Zeigt eine kurze Rückmeldung an.
     * @param messageRes String-Resource der Meldung.
     * @param longDuration true für [android.widget.Toast.LENGTH_LONG], sonst LENGTH_SHORT.
     */
    fun showMessage(messageRes: Int, longDuration: Boolean = false)

    /** Fordert die Kamera-Berechtigung an (Voraussetzung für die Taschenlampe). */
    fun requestCameraPermission()

    /** Öffnet die System-Bedienungshilfen (Voraussetzung für "Bildschirm sperren"). */
    fun openAccessibilitySettings()
}

/**
 * Führt die Geräte-/System-Aktion einer Geste aus (Taschenlampe, Kamera, App öffnen,
 * Bildschirm sperren, "Nicht stören", System-Einstellungen).
 *
 * Launcher-interne Aktionen (App-Drawer, Suche, Benachrichtigungen) werden hier NICHT
 * behandelt – sie setzen Compose-UI-State und bleiben im Composable. Wird eine solche
 * Aktion dennoch übergeben, ist [handle] ein No-Op.
 *
 * Alle Gerätezugriffe laufen über [deviceActions], alle UI-Rückmeldungen über
 * [GestureActionEffects]. Accessibility-Abfragen sind als Lambdas injiziert. Dadurch ist
 * die gesamte Verzweigungslogik ohne Android-Framework testbar (siehe GestureActionHandlerTest).
 *
 * @param deviceActions Kapselung der System-/Geräte-APIs.
 * @param isAccessibilityEnabled liefert true, wenn der Bedienungshilfen-Dienst aktiv ist.
 * @param requestLockScreen sperrt den Bildschirm über den Bedienungshilfen-Dienst.
 */
class GestureActionHandler(
    private val deviceActions: LauncherDeviceActions,
    private val isAccessibilityEnabled: () -> Boolean,
    private val requestLockScreen: () -> Unit,
) {
    /**
     * Setzt [action] um. Bei Erfolg einer Geräteaktion wird ein haptisches Feedback ausgelöst;
     * Fehlerfälle melden sich über [effects].
     *
     * @param appPackage Zielpaket für [GestureAction.OPEN_APP] (sonst ignoriert).
     * @param activity benötigt, um Activities zu starten (Kamera, App, Einstellungen).
     */
    // Bewusst ein flacher when-Dispatcher über alle Gesten: die hohe zyklomatische
    // Komplexität entsteht allein durch die Anzahl der Aktionen, nicht durch verschachtelte Logik.
    @Suppress("CyclomaticComplexMethod")
    fun handle(
        action: GestureAction,
        appPackage: String?,
        activity: Activity,
        effects: GestureActionEffects,
    ) {
        when (action) {
            // Launcher-interne Aktionen werden im Composable behandelt, nicht hier.
            GestureAction.NONE,
            GestureAction.APP_DRAWER,
            GestureAction.SEARCH,
            GestureAction.NOTIFICATIONS -> Unit

            GestureAction.FLASHLIGHT -> when (deviceActions.toggleFlashlight()) {
                is FlashlightToggleResult.Success -> deviceActions.vibrateGestureFeedback(activity)
                FlashlightToggleResult.Unsupported -> effects.showMessage(R.string.flashlight_unsupported)
                FlashlightToggleResult.MissingPermission -> effects.requestCameraPermission()
                FlashlightToggleResult.Error -> effects.showMessage(R.string.flashlight_error)
            }

            GestureAction.CAMERA -> {
                if (deviceActions.openCamera(activity)) {
                    deviceActions.vibrateGestureFeedback(activity)
                } else {
                    effects.showMessage(R.string.camera_app_not_found)
                }
            }

            GestureAction.OPEN_APP -> {
                when {
                    appPackage.isNullOrBlank() -> effects.showMessage(R.string.shake_no_app_selected)
                    deviceActions.openApp(activity, appPackage) -> deviceActions.vibrateGestureFeedback(activity)
                    else -> effects.showMessage(R.string.shake_app_not_found)
                }
            }

            GestureAction.LOCK_SCREEN -> {
                if (isAccessibilityEnabled()) {
                    requestLockScreen()
                    deviceActions.vibrateGestureFeedback(activity)
                } else {
                    effects.showMessage(R.string.shake_lock_needs_accessibility, longDuration = true)
                    effects.openAccessibilitySettings()
                }
            }

            GestureAction.TOGGLE_DND -> when (deviceActions.toggleDoNotDisturb()) {
                is DndToggleResult.Success -> deviceActions.vibrateGestureFeedback(activity)
                DndToggleResult.MissingPermission -> {
                    effects.showMessage(R.string.shake_dnd_needs_permission, longDuration = true)
                    deviceActions.openDndPolicySettings(activity)
                }
                DndToggleResult.Unsupported -> effects.showMessage(R.string.shake_dnd_unsupported)
            }

            GestureAction.OPEN_SETTINGS -> {
                if (deviceActions.openSystemSettings(activity)) {
                    deviceActions.vibrateGestureFeedback(activity)
                } else {
                    effects.showMessage(R.string.shake_settings_not_found)
                }
            }
        }
    }
}
