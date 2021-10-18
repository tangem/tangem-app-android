package com.tangem.tap.features.home.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

class HomeReducer {
    companion object {
        fun reduce(action: Action, state: AppState): HomeState = internalReduce(action, state)
    }
}

private fun internalReduce(action: Action, state: AppState): HomeState {

    if (action !is HomeAction) return state.homeState

    var state = state.homeState
    when (action) {
        is HomeAction.SetFragmentShareTransition -> {
            state = state.copy(shareTransition = action.shareTransition)
        }
        is HomeAction.ShouldScanCardOnResume -> {
            state = state.copy(shouldScanCardOnResume = action.shouldScanCard)
        }
        is HomeAction.ChangeScanCardButtonState -> {
            state = state.copy(btnScanState = action.state)
        }
    }

    return state
}