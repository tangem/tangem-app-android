package com.tangem.tap.common.redux

import com.tangem.tap.common.redux.global.globalReducer
import com.tangem.tap.common.redux.navigation.NavigationReducer
import com.tangem.tap.features.details.redux.DetailsReducer
import com.tangem.tap.features.disclaimer.redux.DisclaimerReducer
import com.tangem.tap.features.home.redux.HomeReducer
import com.tangem.tap.features.send.redux.reducers.SendScreenReducer
import com.tangem.tap.features.wallet.redux.WalletReducer
import org.rekotlin.Action

fun appReducer(action: Action, state: AppState?): AppState {
    requireNotNull(state)
    if (action is AppAction.RestoreState) return action.state

    return AppState(
            navigationState = NavigationReducer.reduce(action, state),
            globalState = globalReducer(action, state),
            homeState = HomeReducer.reduce(action, state),
            walletState = WalletReducer.reduce(action, state),
            sendState = SendScreenReducer.reduce(action, state.sendState),
            detailsState = DetailsReducer.reduce(action, state),
            disclaimerState = DisclaimerReducer.reduce(action, state)
    )
}

sealed class AppAction : Action {
    data class RestoreState(val state: AppState) : AppAction()
}