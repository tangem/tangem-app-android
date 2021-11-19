package com.tangem.tap.common.toggleWidget

import android.graphics.drawable.Drawable
import android.view.View
import com.google.android.material.button.MaterialButton
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.features.wallet.redux.ProgressState

/**
[REDACTED_AUTHOR]
 */
open class IndeterminateProgressButtonWidget(
        private val button: MaterialButton,
        private val progress: View,
        initialState: ProgressState = ProgressState.Done
) : ViewStateWidget {

    private var initialButtonText: CharSequence = button.text
    private var icon: Drawable? = null

    init {
        icon = button.icon
        changeState(initialState)
    }

    override val mainView: View = button

    override fun changeState(state: WidgetState) {
        val progressState = state as? ProgressState ?: return

        when (progressState) {
            ProgressState.Done, ProgressState.Error -> switchToNone()
            ProgressState.Loading -> switchToProgress()
        }
    }

    protected open fun switchToNone() {
        button.isClickable = true
        button.icon = icon
        button.text = initialButtonText
        progress.hide()
    }

    protected open fun switchToProgress() {
        button.isClickable = false
        button.icon = null
        button.text = ""
        progress.show()
    }
}