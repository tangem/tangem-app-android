package com.tangem.core.ui.haptic

import androidx.compose.runtime.Stable

/**
 * Haptic feedback
 * For cases when view could not be visible on screen (ex. Activity is not in foreground)
 * or there is no view context (ex. background service, Model, ViewModel)
 * @see [HapticManager] for view effects.
 */
@Stable
interface VibratorHapticManager {

    fun performOneTime(effect: TangemHapticEffect.OneTime)
}
