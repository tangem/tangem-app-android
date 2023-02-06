package com.tangem.core.ui.utils

import android.os.SystemClock
import android.view.View

/**
 * Implementation of click listener for preventing multiple click events
 *
 * @property action action that called when a view has been clicked
 */
class OneTouchClickListener(private val action: () -> Unit) : View.OnClickListener {

    private var lastClickTimeMs: Long = 0L

    override fun onClick(v: View?) {
        if (SystemClock.elapsedRealtime() - lastClickTimeMs > CLICK_DELAY_MS) {
            lastClickTimeMs = SystemClock.elapsedRealtime()
            action()
        }
    }

    companion object {
        private const val CLICK_DELAY_MS = 500L
    }
}
