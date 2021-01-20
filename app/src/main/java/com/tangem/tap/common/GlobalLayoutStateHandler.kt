package com.tangem.tap.common

import android.view.View
import android.view.ViewTreeObserver
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
class GlobalLayoutStateHandler<T: View>(
        private val view: T,
        attachImmediately: Boolean = true
) : ViewTreeObserver.OnGlobalLayoutListener {

    var onStateChanged: ((T) -> Unit)? = null

    private var isAttached: Boolean = false

    init {
        if (attachImmediately) attach()
    }

    fun attach() {
        if (isAttached) {
            Timber.d("Already attached")
            return
        }

        isAttached = true
        view.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    fun detach() {
        view.viewTreeObserver.removeOnGlobalLayoutListener(this)
        isAttached = false
    }

    override fun onGlobalLayout() {
        onStateChanged?.invoke(view)
    }
}