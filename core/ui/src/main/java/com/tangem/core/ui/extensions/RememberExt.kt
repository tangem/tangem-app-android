package com.tangem.core.ui.extensions

import androidx.compose.runtime.Composable
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