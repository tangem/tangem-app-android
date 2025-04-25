package com.tangem.tap.common.redux

import com.tangem.tap.common.redux.global.globalReducer
import com.tangem.tap.features.details.redux.DetailsReducer
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectReducer
import com.tangem.tap.features.home.redux.HomeReducer
import com.tangem.tap.features.saveWallet.redux.SaveWalletReducer
import com.tangem.tap.features.welcome.redux.WelcomeReducer
import com.tangem.tap.proxy.redux.DaggerGraphReducer
import org.rekotlin.Action

fun appReducer(action: Action, state: AppState?): AppState {
    requireNotNull(state)
    if (action is AppAction.RestoreState) return action.state

    return AppState(
        globalState = globalReducer(action, state),
        homeState = HomeReducer.reduce(action, state),
        detailsState = DetailsReducer.reduce(action, state),
        walletConnectState = WalletConnectReducer.reduce(action, state.walletConnectState),
        welcomeState = WelcomeReducer.reduce(action, state),
        saveWalletState = SaveWalletReducer.reduce(action, state),
        daggerGraphState = DaggerGraphReducer.reduce(action, state),
    )
}

sealed class AppAction : Action {
    data class RestoreState(val state: AppState) : AppAction()
}
