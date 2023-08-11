package com.tangem.feature.wallet.presentation.wallet.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tangem.feature.wallet.presentation.wallet.state.WalletState

/**
 * Wallet state holder
 *
 * @param initialState initial ui state
 *
* [REDACTED_AUTHOR]
 */
internal class WalletStateHolder(initialState: WalletState) {

    /** Screen state */
    var uiState: WalletState by mutableStateOf(initialState)
        private set

    /** Set screen [state] */
    fun setState(state: WalletState) {
        when (state) {
            is WalletState.ContentState -> {
                cache(state = state)

                uiState = state
            }
            is WalletState.Initial -> Unit
        }
    }

    /** Cache [state] [WalletState.ContentState] to [WalletStateCache] */
    private fun cache(state: WalletState.ContentState) {
        val selectedWalletIndex = state.walletsListConfig.selectedWalletIndex
        val selectedWalletId = state.walletsListConfig.wallets[selectedWalletIndex].id

        WalletStateCache.update(userWalletId = selectedWalletId, state = state)
    }
}
