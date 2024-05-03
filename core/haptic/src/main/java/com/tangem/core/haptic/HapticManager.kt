package com.tangem.core.haptic

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

class HapticManager(private val vibrator: Vibrator) {

    fun vibrateShort() {
        vibrate(VIBRATION_SHORT_DURATION)
    }

    fun vibrateLong() {
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
        const val VIBRATION_SHORT_DURATION = 100L
        const val VIBRATION_MEDIUM_DURATION = 400L
        const val VIBRATION_LONG_DURATION = 700L
    }
}
