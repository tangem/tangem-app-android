package com.tangem.tap.common

import android.view.View
import android.view.ViewTreeObserver

/**
[REDACTED_AUTHOR]
 */
class GlobalLayoutStateHandler<T: View>(
        private val view: T,
        attachImmediately: Boolean = true
) : ViewTreeObserver.OnGlobalLayoutListener {

    var onStateChanged: ((T) -> Unit)? = null

    init {
        if (attachImmediately) attach()
    }

    fun attach() {
        view.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    fun detach() {
        view.viewTreeObserver.removeOnGlobalLayoutListener(this)
    }

    override fun onGlobalLayout() {
        onStateChanged?.invoke(view)
    }
}