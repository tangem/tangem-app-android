package com.tangem.core.ui.haptic

import android.view.View
import androidx.core.view.ViewCompat

internal class DefaultHapticManager(
    private val view: View,
    private val vibratorHapticManager: VibratorHapticManager?,
) : HapticManager {

    override fun perform(effect: TangemHapticEffect) {
        when (effect) {
            is TangemHapticEffect.View -> {
                effect.androidHapticFeedbackCode?.let {
                    ViewCompat.performHapticFeedback(view, it)
                }
            }
            is TangemHapticEffect.OneTime -> {
                if (vibratorHapticManager != null) {
                    vibratorHapticManager.performOneTime(effect)
                } else {
                    when (effect) {
                        TangemHapticEffect.OneTime.Tick -> perform(TangemHapticEffect.View.SegmentTick)
                        TangemHapticEffect.OneTime.Click -> perform(TangemHapticEffect.View.ContextClick)
                        TangemHapticEffect.OneTime.DoubleClick -> perform(TangemHapticEffect.View.ContextClick)
                        TangemHapticEffect.OneTime.HeavyClick -> perform(TangemHapticEffect.View.LongPress)
                    }
                }
            }
        }
    }
}