package com.tangem.tap.common.haptic

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.tangem.core.ui.haptic.HapticManager

class DefaultHapticManager(private val vibrator: Vibrator) : HapticManager {

    override fun vibrateShort() {
        vibrate(VIBRATION_SHORT_DURATION)
    }

    override fun vibrateLong() {
        vibrate(VIBRATION_MEDIUM_DURATION)
    }

    private fun vibrate(durationMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(durationMs)
        }
    }

    companion object {
        const val VIBRATION_SHORT_DURATION = 50L
        const val VIBRATION_MEDIUM_DURATION = 100L
        const val VIBRATION_LONG_DURATION = 200L
    }
}
