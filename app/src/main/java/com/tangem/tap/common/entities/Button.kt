package com.tangem.tap.common.entities

import com.tangem.tap.features.send.redux.states.ButtonState
import com.tangem.tap.features.wallet.redux.ProgressState

open class Button(val enabled: Boolean)

open class IndeterminateProgressButton(
    val state: ButtonState
) : Button(state != ButtonState.DISABLED) {

    val progressState: ProgressState
        get() = when (state) {
            ButtonState.PROGRESS -> ProgressState.Loading
            else -> ProgressState.Done
        }
}