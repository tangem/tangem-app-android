package com.tangem.tap.features.wallet.redux.reducers

import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletState

class MultiWalletReducer {
    @Suppress("LongMethod", "ComplexMethod")
    fun reduce(action: WalletAction.MultiWallet, state: WalletState): WalletState {
        return when (action) {
            is WalletAction.MultiWallet.SelectWallet -> {
                state.copy(selectedCurrency = action.currency)
            }
            is WalletAction.MultiWallet.TryToRemoveWallet -> state
            is WalletAction.MultiWallet.AddMissingDerivations -> state.copy(
                missingDerivations = action.blockchains,
            )
            is WalletAction.MultiWallet.BackupWallet -> state
            is WalletAction.MultiWallet.ScanToGetDerivations -> state.copy(state = ProgressState.Loading)
            else -> state
        }
    }
}