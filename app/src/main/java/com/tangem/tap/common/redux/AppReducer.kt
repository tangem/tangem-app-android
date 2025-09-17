package com.tangem.tap.common.redux

import com.tangem.tap.common.redux.global.globalReducer
import com.tangem.tap.features.details.redux.DetailsReducer
import com.tangem.tap.features.welcome.redux.WelcomeReducer
import com.tangem.tap.proxy.redux.DaggerGraphReducer
import org.rekotlin.Action

fun appReducer(action: Action, state: AppState?): AppState {
    requireNotNull(state)
    if (action is AppAction.RestoreState) return action.state

    return AppState(
        globalState = globalReducer(action, state),
        detailsState = DetailsReducer.reduce(action, state),
        welcomeState = WelcomeReducer.reduce(action, state),
        daggerGraphState = DaggerGraphReducer.reduce(action, state),
    )
}

sealed class AppAction : Action {
    data class RestoreState(val state: AppState) : AppAction()
}