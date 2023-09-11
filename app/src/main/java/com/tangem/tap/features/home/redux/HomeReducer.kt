package com.tangem.tap.features.home.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

object HomeReducer {
    fun reduce(action: Action, state: AppState): HomeState = internalReduce(action, state)
}

private fun internalReduce(action: Action, appState: AppState): HomeState {
    if (action !is HomeAction) return appState.homeState

    var state = appState.homeState
    when (action) {
        is HomeAction.InsertStory -> {
            state = state.copy(
                stories = state.stories.toMutableList().apply {
                    add(action.position, action.story)
                },
            )
        }
        is HomeAction.ScanInProgress -> {
            state = state.copy(scanInProgress = action.scanInProgress)
        }
        is HomeAction.ChangeScanCardButtonState -> {
            state = state.copy(btnScanState = action.state)
        }
        is HomeAction.UpdateCountryCode -> {
            state.onCountryCodeUpdate(state, action.userCountryCode)
        }
        else -> {}
    }

    return state
}
