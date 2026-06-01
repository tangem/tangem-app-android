package com.tangem.core.ui.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

private typealias Action = () -> Unit

/**
 * Returns action with Haptic Feedback
 *
 * @param state of the content
 * @param hapticType haptic effect
 * @param onAction action that needs haptic feedback
 */
@Composable
fun rememberHapticFeedback(
    state: Any,
    onAction: () -> Unit,
    hapticType: HapticFeedbackType = HapticFeedbackType.LongPress,
): Action {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(state) {
        {
            hapticFeedback.performHapticFeedback(hapticType)
            onAction.invoke()
        }
    }
}

/**
 * Returns [value] when it is non-null, otherwise the most recent non-null value seen at this call
 * site. The cached value is updated in a [SideEffect] so this function never writes to a snapshot
 * state during composition.
 *
 * Typical use case: pairing transient nullable inputs with `AnimatedVisibility` (or any other
 * exit-animating wrapper). When the caller flips the input back to `null` to trigger an exit
 * transition, the last non-null value is still available for the wrapped content to render until
 * the transition finishes — without it, the content would disappear instantly and the exit
 * animation would have nothing to animate.
 *
 * Example:
 * ```
 * val displayedIcon = rememberLastNonNull(iconStart)
 * AnimatedVisibility(visible = iconStart != null) {
 *     displayedIcon?.let { TangemIcon(it) }
 * }
 * ```
 *
 * Note: the cache is per call site, so calling this multiple times in the same composable yields
 * independent caches.
 */
@Composable
fun <T : Any> rememberLastNonNull(value: T?): T? {
    val cache = remember { mutableStateOf(value) }
    SideEffect {
        if (value != null && cache.value !== value) cache.value = value
    }
    return value ?: cache.value
}