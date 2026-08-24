package com.tangem.core.ui.components

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Keeps the display awake while this composable stays in the composition. For screens the viewer only
 * watches, which the system would otherwise dim part way through.
 *
 * Requests are counted because the flag belongs to the window and not to a screen: a stack transition holds
 * the outgoing and the incoming screen in the composition at once, and clearing the flag on dispose would let
 * the screen that leaves dim the display under the one that arrives.
 */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current

    DisposableEffect(view) {
        ScreenOnRequests.acquire(view)
        onDispose { ScreenOnRequests.release(view) }
    }
}

private object ScreenOnRequests {

    private val counts = mutableMapOf<View, Int>()

    fun acquire(view: View) {
        counts[view] = counts.getOrElse(view) { 0 } + 1
        view.keepScreenOn = true
    }

    fun release(view: View) {
        val remaining = counts.getOrElse(view) { return } - 1
        if (remaining > 0) {
            counts[view] = remaining
        } else {
            counts.remove(view)
            view.keepScreenOn = false
        }
    }
}