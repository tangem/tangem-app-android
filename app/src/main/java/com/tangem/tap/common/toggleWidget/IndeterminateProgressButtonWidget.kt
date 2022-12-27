package com.tangem.tap.common.toggleWidget

import android.graphics.drawable.Drawable
import android.view.View
import com.google.android.material.button.MaterialButton
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.features.wallet.redux.ProgressState

/**
 * Created by Anton Zhilenkov on 09/04/2021.
 */
open class IndeterminateProgressButtonWidget(
    private val button: MaterialButton,
    private val progress: View,
    initialState: ProgressState = ProgressState.Done,
) : ViewStateWidget {

    private var text: CharSequence = button.text
    private var icon: Drawable? = button.icon
    private var iconGravity: Int? = button.iconGravity

    init {
        if (initialState != ProgressState.Done) changeState(initialState)
    }

    var isEnabled: Boolean
        get() = button.isEnabled
        set(value) {
            button.isEnabled = value
        }

    override val mainView: View = button

    override fun changeState(state: WidgetState) {
        val progressState = state as? ProgressState ?: return

        when (progressState) {
            ProgressState.Done, ProgressState.Error -> switchToNone()
            ProgressState.Loading -> switchToProgress()
            else -> {}
        }
    }

    protected open fun switchToNone() {
        button.isClickable = true
        button.text = text
        button.icon = icon
        iconGravity?.let { button.iconGravity = it }

        progress.hide()
    }

    protected open fun switchToProgress() {
        button.isClickable = false
        button.text = ""
        button.icon = null

        progress.show()
    }
}
