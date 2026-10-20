package com.tangem.features.feed.ui.v2

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Calls [onTouch] when a gesture starts anywhere in this element, before its children see the touch.
 * Nothing is consumed: the tap or scroll underneath goes through as usual.
 */
internal fun Modifier.onEachTouch(onTouch: () -> Unit): Modifier = pointerInput(onTouch) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        onTouch()
    }
}