package com.tangem.tap.common.redux

import com.tangem.tap.common.redux.global.globalReducer
import com.tangem.tap.common.redux.navigation.navigationReducer
import com.tangem.tap.features.wallet.redux.walletReducer
import org.rekotlin.Action

fun appReducer(action: Action, state: AppState?): AppState {
    requireNotNull(state)
    if (action is AppAction.RestoreState) return action.state
    return AppState(
            navigationState = navigationReducer(action, state),
            globalState = globalReducer(action, state),
            walletState = walletReducer(action, state),
    )
}

sealed class AppAction : Action {
    data class RestoreState(val state: AppState) : AppAction()
}