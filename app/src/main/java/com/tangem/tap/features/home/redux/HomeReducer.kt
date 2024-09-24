package com.tangem.tap.features.home.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

object HomeReducer {
    fun reduce(action: Action, state: AppState): HomeState = internalReduce(action, state)
}

private fun internalReduce(action: Action, appState: AppState): HomeState {
    if (action !is HomeAction) return appState.homeState

    return when (action) {
        is HomeAction.ScanInProgress -> {
            appState.homeState.copy(scanInProgress = action.scanInProgress)
        }
        else -> appState.homeState
    }
}