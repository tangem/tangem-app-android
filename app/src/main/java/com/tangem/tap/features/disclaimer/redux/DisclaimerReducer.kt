package com.tangem.tap.features.disclaimer.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

object DisclaimerReducer {
    fun reduce(action: Action, state: AppState): DisclaimerState = internalReduce(action, state)
}

private fun internalReduce(action: Action, state: AppState): DisclaimerState {
    if (action !is DisclaimerAction) return state.disclaimerState

    val disclaimerState = state.disclaimerState

    return when (action) {
        is DisclaimerAction.SetDisclaimer -> disclaimerState.copy(
            disclaimer = action.disclaimer,
        )
        is DisclaimerAction.Show -> disclaimerState.copy(
            showedFromScreen = action.fromScreen,
            callback = action.callback,
        )
        is DisclaimerAction.AcceptDisclaimer, is DisclaimerAction.OnBackPressed -> disclaimerState.copy(
            callback = null,
            progressState = null,
        )
        is DisclaimerAction.OnProgressStateChanged -> disclaimerState.copy(
            progressState = action.state,
        )
        else -> disclaimerState
    }
}
