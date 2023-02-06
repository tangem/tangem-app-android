package com.tangem.tap.features.home.redux

import com.tangem.tap.common.entities.IndeterminateProgressButton
import com.tangem.tap.features.send.redux.states.ButtonState
import com.tangem.tap.features.wallet.redux.ProgressState
import org.rekotlin.StateType

data class HomeState(
    val scanInProgress: Boolean = false,
    val btnScanState: IndeterminateProgressButton = IndeterminateProgressButton(ButtonState.ENABLED),
) : StateType {

    val btnScanStateInProgress: Boolean
        get() = btnScanState.progressState == ProgressState.Loading
}
