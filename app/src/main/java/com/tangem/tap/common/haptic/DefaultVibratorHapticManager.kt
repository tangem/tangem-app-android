package com.tangem.tap.common.haptic

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.ChecksSdkIntAtLeast
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.haptic.VibratorHapticManager

internal class DefaultVibratorHapticManager(
    private val vibrator: Vibrator,
) : VibratorHapticManager {

    private val isHapticEnabled = deviceSupportsVibrationEffects()

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    private fun deviceSupportsVibrationEffects(): Boolean = when {
        !vibrator.hasVibrator() -> false

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
            vibrator.areAllEffectsSupported(
                TangemHapticEffect.OneTime.DoubleClick.code,
                TangemHapticEffect.OneTime.HeavyClick.code,
                TangemHapticEffect.OneTime.Tick.code,
                TangemHapticEffect.OneTime.Click.code,
            ) == Vibrator.VIBRATION_EFFECT_SUPPORT_YES

        Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> true

        else -> false
    }

    override fun performOneTime(effect: TangemHapticEffect.OneTime) {
        if (!isHapticEnabled) return

        vibrator.vibrate(VibrationEffect.createPredefined(effect.code))
    }
}
