package com.example.androidlauncher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.view.HapticFeedbackConstants

sealed interface FlashlightToggleResult {
    data class Success(val isEnabled: Boolean) : FlashlightToggleResult
    data object Unsupported : FlashlightToggleResult
    data object MissingPermission : FlashlightToggleResult
    data object Error : FlashlightToggleResult
}

/**
 * Kapselt Geräteaktionen für Shake-Gesten.
 */
class LauncherDeviceActions(context: Context) {
    companion object {
        private const val APP_CAMERA_CATEGORY = "android.intent.category.APP_CAMERA"
        private const val SUCCESS_VIBRATION_DURATION_MS = 70L
    }

    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val cameraManager = appContext.getSystemService(CameraManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val torchCameraId = findTorchCameraId()

    @Volatile
    private var isTorchEnabled = false
    private var isTorchCallbackRegistered = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId) {
                isTorchEnabled = enabled
            }
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId == torchCameraId) {
                isTorchEnabled = false
            }
        }
    }

    fun supportsFlashlight(): Boolean {
        return torchCameraId != null && packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    fun startTorchMonitoring() {
        val manager = cameraManager ?: return
        if (torchCameraId == null || isTorchCallbackRegistered) return
        try {
            manager.registerTorchCallback(torchCallback, mainHandler)
            isTorchCallbackRegistered = true
        } catch (_: SecurityException) {
            isTorchCallbackRegistered = false
        } catch (_: RuntimeException) {
            isTorchCallbackRegistered = false
        }
    }

    fun stopTorchMonitoring() {
        val manager = cameraManager ?: return
        if (!isTorchCallbackRegistered) return
        try {
            manager.unregisterTorchCallback(torchCallback)
        } catch (_: RuntimeException) {
            // Ignore; callback might already be detached by the platform.
        }
        isTorchCallbackRegistered = false
    }

    fun toggleFlashlight(): FlashlightToggleResult {
        val manager = cameraManager ?: return FlashlightToggleResult.Unsupported
        val cameraId = torchCameraId ?: return FlashlightToggleResult.Unsupported

        return try {
            val newTorchState = !isTorchEnabled
            manager.setTorchMode(cameraId, newTorchState)
            isTorchEnabled = newTorchState
            FlashlightToggleResult.Success(newTorchState)
        } catch (_: SecurityException) {
            FlashlightToggleResult.MissingPermission
        } catch (_: CameraAccessException) {
            FlashlightToggleResult.Error
        } catch (_: IllegalArgumentException) {
            FlashlightToggleResult.Error
        }
    }

    fun openCamera(activity: Activity): Boolean {
        val candidateIntents = listOf(
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            Intent(Intent.ACTION_MAIN).addCategory(APP_CAMERA_CATEGORY),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        )

        return candidateIntents.any { baseIntent ->
            val resolvedActivity = resolveActivity(baseIntent) ?: return@any false
            val explicitIntent = Intent(baseIntent).apply {
                component = ComponentName(
                    resolvedActivity.activityInfo.packageName,
                    resolvedActivity.activityInfo.name
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                activity.startActivity(explicitIntent)
                true
            } catch (_: ActivityNotFoundException) {
                false
            } catch (_: SecurityException) {
                false
            } catch (_: RuntimeException) {
                false
            }
        }
    }

    fun vibrateGestureFeedback(activity: Activity? = null) {
        val targetView = activity?.currentFocus ?: activity?.window?.decorView

        runCatching {
            val feedbackConstant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
            targetView?.performHapticFeedback(
                feedbackConstant,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
        }

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        if (!vibrator.hasVibrator()) return

        runCatching {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            SUCCESS_VIBRATION_DURATION_MS,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                }
                else -> {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(SUCCESS_VIBRATION_DURATION_MS)
                }
            }
        }
    }

    private fun resolveActivity(intent: Intent): ResolveInfo? {
        return packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?: packageManager.resolveActivity(intent, 0)
    }

    private fun findTorchCameraId(): String? {
        val manager = cameraManager ?: return null
        return try {
            manager.cameraIdList.firstOrNull { cameraId ->
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                hasFlash && lensFacing == CameraCharacteristics.LENS_FACING_BACK
            } ?: manager.cameraIdList.firstOrNull { cameraId ->
                manager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: SecurityException) {
            null
        } catch (_: CameraAccessException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }
}
