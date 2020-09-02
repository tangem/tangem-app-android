package com.tangem.tap.common.redux.global

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

fun globalReducer(action: Action, state: AppState): GlobalState {

    if (action !is GlobalAction) return state.globalState

    var newState = state.globalState

    when (action) {
        is GlobalAction.LoadCard -> newState = newState.copy(card = action.card)
        is GlobalAction.SetFiatRate -> {
            val rates = newState.fiatRates.rates.toMutableMap()
            rates[action.fiatRates.first] = action.fiatRates.second
            newState = newState.copy(fiatRates = FiatRates(rates))
        }
        is GlobalAction.LoadWalletManager ->
            newState = newState.copy(walletManager = action.walletManager)

    }
    return newState
}