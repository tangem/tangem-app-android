package com.tangem.core.ui.extensions

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput

val LocalUserInteractionTracker = staticCompositionLocalOf<UserInteractionTracker?> { null }

interface UserInteractionTracker {
    fun onUserInteraction()
}

fun Modifier.trackUserInteraction(tracker: UserInteractionTracker?): Modifier {
    if (tracker == null) return this

    return this
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)

                if (down.pressed) {
                    tracker.onUserInteraction()
                }

                waitForUpOrCancellation()
            }
        }
        .onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                tracker.onUserInteraction()
            }
            false
        }
}