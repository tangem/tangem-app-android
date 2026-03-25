package com.tangem.core.ui.haptic

import android.view.View
import androidx.core.view.ViewCompat

internal class DefaultHapticManager(
    private val view: View,
    private val vibratorHapticManager: VibratorHapticManager?,
) : HapticManager {

    override fun perform(effect: TangemHapticEffect) {
        when (effect) {
            is TangemHapticEffect.View -> performViewEffect(effect)
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

    private fun performViewEffect(effect: TangemHapticEffect.View) {
        val code = effect.androidHapticFeedbackCode ?: return
        if (ViewCompat.performHapticFeedback(view, code)) return

        val fallbackCode = VIEW_FALLBACKS[effect]?.androidHapticFeedbackCode ?: return
        ViewCompat.performHapticFeedback(view, fallbackCode)
    }

    private companion object {
        val VIEW_FALLBACKS = mapOf(
            TangemHapticEffect.View.ClockTick to TangemHapticEffect.View.KeyboardPress,
            TangemHapticEffect.View.Confirm to TangemHapticEffect.View.ContextClick,
            TangemHapticEffect.View.Reject to TangemHapticEffect.View.LongPress,
        )
    }
}