package com.example.androidlauncher.gesture

import android.app.Activity
import com.example.androidlauncher.DndToggleResult
import com.example.androidlauncher.FlashlightToggleResult
import com.example.androidlauncher.LauncherDeviceActions
import com.example.androidlauncher.R
import com.example.androidlauncher.data.GestureAction
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit-Tests für [GestureActionHandler]. Decken jede Verzweigung pro [GestureAction] ab,
 * ohne Android-Framework – Gerätezugriffe sind gemockt, UI-Rückmeldungen werden über eine
 * aufzeichnende [GestureActionEffects]-Implementierung geprüft.
 */
class GestureActionHandlerTest {

    /** Zeichnet Effekt-Aufrufe auf, statt echte UI-Aktionen auszulösen. */
    private class RecordingEffects : GestureActionEffects {
        val messages = mutableListOf<Pair<Int, Boolean>>()
        var cameraPermissionRequested = false
        var accessibilitySettingsOpened = false

        override fun showMessage(messageRes: Int, longDuration: Boolean) {
            messages.add(messageRes to longDuration)
        }

        override fun requestCameraPermission() {
            cameraPermissionRequested = true
        }

        override fun openAccessibilitySettings() {
            accessibilitySettingsOpened = true
        }
    }

    private lateinit var deviceActions: LauncherDeviceActions
    private lateinit var activity: Activity
    private lateinit var effects: RecordingEffects
    private var accessibilityEnabled = false
    private var lockScreenRequested = false

    private fun handler() = GestureActionHandler(
        deviceActions = deviceActions,
        isAccessibilityEnabled = { accessibilityEnabled },
        requestLockScreen = { lockScreenRequested = true },
    )

    @Before
    fun setUp() {
        deviceActions = mockk(relaxed = true)
        activity = mockk(relaxed = true)
        effects = RecordingEffects()
        accessibilityEnabled = false
        lockScreenRequested = false
    }

    @Test
    fun `internal actions are no-ops`() {
        listOf(
            GestureAction.NONE,
            GestureAction.APP_DRAWER,
            GestureAction.SEARCH,
            GestureAction.NOTIFICATIONS,
        ).forEach { action ->
            handler().handle(action, appPackage = null, activity = activity, effects = effects)
        }
        verify(exactly = 0) { deviceActions.toggleFlashlight() }
        assertTrue(effects.messages.isEmpty())
    }

    @Test
    fun `flashlight success vibrates without message`() {
        every { deviceActions.toggleFlashlight() } returns FlashlightToggleResult.Success(isEnabled = true)
        handler().handle(GestureAction.FLASHLIGHT, null, activity, effects)
        verify { deviceActions.vibrateGestureFeedback(activity) }
        assertTrue(effects.messages.isEmpty())
    }

    @Test
    fun `flashlight unsupported shows message`() {
        every { deviceActions.toggleFlashlight() } returns FlashlightToggleResult.Unsupported
        handler().handle(GestureAction.FLASHLIGHT, null, activity, effects)
        assertEquals(listOf(R.string.flashlight_unsupported to false), effects.messages)
    }

    @Test
    fun `flashlight missing permission requests camera permission`() {
        every { deviceActions.toggleFlashlight() } returns FlashlightToggleResult.MissingPermission
        handler().handle(GestureAction.FLASHLIGHT, null, activity, effects)
        assertTrue(effects.cameraPermissionRequested)
        assertTrue(effects.messages.isEmpty())
    }

    @Test
    fun `flashlight error shows message`() {
        every { deviceActions.toggleFlashlight() } returns FlashlightToggleResult.Error
        handler().handle(GestureAction.FLASHLIGHT, null, activity, effects)
        assertEquals(listOf(R.string.flashlight_error to false), effects.messages)
    }

    @Test
    fun `camera opens and vibrates on success`() {
        every { deviceActions.openCamera(activity) } returns true
        handler().handle(GestureAction.CAMERA, null, activity, effects)
        verify { deviceActions.vibrateGestureFeedback(activity) }
        assertTrue(effects.messages.isEmpty())
    }

    @Test
    fun `camera shows message when not found`() {
        every { deviceActions.openCamera(activity) } returns false
        handler().handle(GestureAction.CAMERA, null, activity, effects)
        assertEquals(listOf(R.string.camera_app_not_found to false), effects.messages)
    }

    @Test
    fun `open app without package shows message`() {
        handler().handle(GestureAction.OPEN_APP, appPackage = "  ", activity = activity, effects = effects)
        assertEquals(listOf(R.string.shake_no_app_selected to false), effects.messages)
        verify(exactly = 0) { deviceActions.openApp(any(), any()) }
    }

    @Test
    fun `open app vibrates on success`() {
        every { deviceActions.openApp(activity, "com.example.app") } returns true
        handler().handle(GestureAction.OPEN_APP, "com.example.app", activity, effects)
        verify { deviceActions.vibrateGestureFeedback(activity) }
        assertTrue(effects.messages.isEmpty())
    }

    @Test
    fun `open app shows message when not found`() {
        every { deviceActions.openApp(activity, "com.example.app") } returns false
        handler().handle(GestureAction.OPEN_APP, "com.example.app", activity, effects)
        assertEquals(listOf(R.string.shake_app_not_found to false), effects.messages)
    }

    @Test
    fun `lock screen locks when accessibility enabled`() {
        accessibilityEnabled = true
        handler().handle(GestureAction.LOCK_SCREEN, null, activity, effects)
        assertTrue(lockScreenRequested)
        verify { deviceActions.vibrateGestureFeedback(activity) }
        assertTrue(effects.messages.isEmpty())
    }

    @Test
    fun `lock screen guides to settings when accessibility disabled`() {
        accessibilityEnabled = false
        handler().handle(GestureAction.LOCK_SCREEN, null, activity, effects)
        assertTrue(!lockScreenRequested)
        assertTrue(effects.accessibilitySettingsOpened)
        assertEquals(listOf(R.string.shake_lock_needs_accessibility to true), effects.messages)
    }

    @Test
    fun `dnd success vibrates`() {
        every { deviceActions.toggleDoNotDisturb() } returns DndToggleResult.Success(isEnabled = true)
        handler().handle(GestureAction.TOGGLE_DND, null, activity, effects)
        verify { deviceActions.vibrateGestureFeedback(activity) }
        assertTrue(effects.messages.isEmpty())
    }

    @Test
    fun `dnd missing permission opens policy settings`() {
        every { deviceActions.toggleDoNotDisturb() } returns DndToggleResult.MissingPermission
        handler().handle(GestureAction.TOGGLE_DND, null, activity, effects)
        verify { deviceActions.openDndPolicySettings(activity) }
        assertEquals(listOf(R.string.shake_dnd_needs_permission to true), effects.messages)
    }

    @Test
    fun `dnd unsupported shows message`() {
        every { deviceActions.toggleDoNotDisturb() } returns DndToggleResult.Unsupported
        handler().handle(GestureAction.TOGGLE_DND, null, activity, effects)
        assertEquals(listOf(R.string.shake_dnd_unsupported to false), effects.messages)
    }

    @Test
    fun `open settings vibrates on success`() {
        every { deviceActions.openSystemSettings(activity) } returns true
        handler().handle(GestureAction.OPEN_SETTINGS, null, activity, effects)
        verify { deviceActions.vibrateGestureFeedback(activity) }
        assertTrue(effects.messages.isEmpty())
    }

    @Test
    fun `open settings shows message on failure`() {
        every { deviceActions.openSystemSettings(activity) } returns false
        handler().handle(GestureAction.OPEN_SETTINGS, null, activity, effects)
        assertEquals(listOf(R.string.shake_settings_not_found to false), effects.messages)
    }
}
