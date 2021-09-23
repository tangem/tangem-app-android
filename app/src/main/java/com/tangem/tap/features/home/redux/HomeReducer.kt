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
        is HomeAction.SetTermsOfUseState -> {
            state = state.copy(isDisclaimerAccepted = action.isDisclaimerAccepted)
        }
        is HomeAction.SetOpenUrl -> {
            state = state.copy(openUrl = action.url)
        }
        is HomeAction.ChangeScanCardButtonState -> {
            state = state.copy(btnScanState = action.state)
        }
        is HomeAction.ShowDialog.ScanFails -> {
            state = state.copy(dialog = HomeDialog.ScanFailsDialog)
        }
        is HomeAction.HideDialog -> {
            state = state.copy(dialog = null)
        }
    }

    return state
}