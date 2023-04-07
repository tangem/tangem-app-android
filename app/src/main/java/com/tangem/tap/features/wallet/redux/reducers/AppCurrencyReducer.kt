package com.tangem.tap.features.wallet.redux.reducers

import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletState

class AppCurrencyReducer {
    fun reduce(action: WalletAction.AppCurrencyAction, state: WalletState): WalletState {
        return when (action) {
            is WalletAction.AppCurrencyAction.SelectAppCurrency,
            is WalletAction.AppCurrencyAction.ChooseAppCurrency,
            -> state
        }
    }
}
