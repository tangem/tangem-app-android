package com.tangem.tap.features.disclaimer.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

class DisclaimerReducer {
    companion object {
        fun reduce(action: Action, state: AppState): DisclaimerState = internalReduce(action, state)
    }
}

private fun internalReduce(action: Action, state: AppState): DisclaimerState {
    if (action !is DisclaimerAction) return state.disclaimerState

    val disclaimerState = state.disclaimerState

    return when (action) {
        is DisclaimerAction.Show -> disclaimerState.copy(
            callback = action.callback,
        )
        is DisclaimerAction.UpdateState -> disclaimerState.copy(
            type = action.type,
            accepted = action.accepted,
        )
        is DisclaimerAction.AcceptDisclaimer, is DisclaimerAction.OnBackPressed -> disclaimerState.copy(
            callback = null,
        )
        else -> disclaimerState
    }
}
