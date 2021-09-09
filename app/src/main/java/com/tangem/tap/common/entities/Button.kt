package com.tangem.tap.common.entities

import com.tangem.tap.common.toggleWidget.ProgressState
import com.tangem.tap.features.send.redux.states.ButtonState

open class Button(val enabled: Boolean)

open class IndeterminateProgressButton(
        val state: ButtonState
) : Button(state != ButtonState.DISABLED) {

    val progressState: ProgressState
        get() = when (state) {
            ButtonState.PROGRESS -> ProgressState.Progress()
            else -> ProgressState.None
        }
}