package com.tangem.feature.qrscanning.inner

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import androidx.core.os.HandlerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Runs an action on the main thread once every camera is released again, but not before [minDelayMillis]
 * and not later than [timeoutMillis]. CameraX closes the camera asynchronously after the screen is gone,
 * so a fixed delay is not enough on devices that release it slowly.
 */
internal class CameraReleaseAwaiter @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun runWhenReleased(minDelayMillis: Long, timeoutMillis: Long, action: () -> Unit) {
        val handler = Handler(Looper.getMainLooper())
        val cameraManager = context.getSystemService(CameraManager::class.java)
        val cameraIds = runCatching { cameraManager?.cameraIdList?.toSet() }.getOrNull().orEmpty()

        if (cameraManager == null || cameraIds.isEmpty()) {
            handler.postDelayed(action, minDelayMillis)
            return
        }

        val availableIds = mutableSetOf<String>()
        var isMinDelayPassed = false
        var isDone = false

        val callback = object : CameraManager.AvailabilityCallback() {
            override fun onCameraAvailable(cameraId: String) {
                availableIds += cameraId
                if (isMinDelayPassed) finishIfReleased()
            }

            override fun onCameraUnavailable(cameraId: String) {
                availableIds -= cameraId
            }

            fun finishIfReleased() {
                if (availableIds.containsAll(cameraIds)) finish()
            }

            fun finish() {
                if (isDone) return
                isDone = true
                cameraManager.unregisterAvailabilityCallback(this)
                handler.removeCallbacksAndMessages(this)
                action()
            }
        }

        // Registration reports every camera that is available right now; one still held stays missing
        cameraManager.registerAvailabilityCallback(callback, handler)
        HandlerCompat.postDelayed(
            handler,
            {
                isMinDelayPassed = true
                callback.finishIfReleased()
            },
            callback,
            minDelayMillis,
        )
        HandlerCompat.postDelayed(handler, callback::finish, callback, timeoutMillis)
    }
}