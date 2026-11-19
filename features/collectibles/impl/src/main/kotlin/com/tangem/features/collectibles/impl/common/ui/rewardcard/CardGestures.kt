package com.tangem.features.collectibles.impl.common.ui.rewardcard

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize

/** How long a press lasts before it becomes a hold, at parity with iOS. */
private const val HOLD_DELAY_MS = 200L

private enum class PressOutcome { Released, Cancelled }

/**
 * A tap and a hold are separate gestures: a hold never ends in a tap. Neither consumes the press until it is
 * recognised, so a quick swipe still scrolls the screen; once a hold starts it keeps the finger to itself.
 */
internal fun Modifier.cardGestures(
    isTapEnabled: Boolean,
    isHoldEnabled: Boolean,
    onTap: () -> Unit,
    onHold: (position: Offset, size: IntSize) -> Unit,
    onHoldEnd: () -> Unit,
): Modifier {
    if (!isTapEnabled && !isHoldEnabled) return this

    return pointerInput(isTapEnabled, isHoldEnabled) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val outcome = if (isHoldEnabled) {
                withTimeoutOrNull(HOLD_DELAY_MS) { awaitPressOutcome() }
            } else {
                awaitPressOutcome()
            }

            when (outcome) {
                PressOutcome.Released -> if (isTapEnabled) onTap()
                PressOutcome.Cancelled -> Unit
                null -> try {
                    onHold(down.position, size)
                    trackHold(pointerId = down.id) { position -> onHold(position, size) }
                } finally {
                    onHoldEnd()
                }
            }
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitPressOutcome(): PressOutcome =
    if (waitForUpOrCancellation() != null) PressOutcome.Released else PressOutcome.Cancelled

private suspend fun AwaitPointerEventScope.trackHold(pointerId: PointerId, onMove: (Offset) -> Unit) {
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == pointerId } ?: return
        change.consume()
        if (!change.pressed) return
        onMove(change.position)
    }
}