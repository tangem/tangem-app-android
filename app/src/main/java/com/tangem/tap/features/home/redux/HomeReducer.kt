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

    var homeState = state.homeState
    when (action) {
        is HomeAction.CheckIfFirstLaunch.Result -> {
            homeState = homeState.copy(firstLaunch = action.firstLaunch)
        }
    }

    return homeState
}