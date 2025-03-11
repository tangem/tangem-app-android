package com.tangem.core.ui.haptic

import androidx.compose.runtime.Stable

/**
 * Haptic feedback.
 * @see [TangemHapticEffect.OneTime] for one-time effects.
 * @see [TangemHapticEffect.View] for view effects.
 */
@Stable
interface HapticManager {

    fun perform(effect: TangemHapticEffect)
}