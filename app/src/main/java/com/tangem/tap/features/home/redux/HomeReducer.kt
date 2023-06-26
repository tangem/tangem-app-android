package com.tangem.tap.features.home.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

object HomeReducer {
    fun reduce(action: Action, state: AppState): HomeState = internalReduce(action, state)
}

private fun internalReduce(action: Action, state: AppState): HomeState {
    if (action !is HomeAction) return state.homeState

    var state = state.homeState
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
        else -> {}
    }

    return state
}
