package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.common.Converter
import com.tangem.common.Provider
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState

internal class HiddenStateConverter(
    private val currentStateProvider: Provider<WalletState>,
) : Converter<Boolean, WalletState> {

    override fun convert(value: Boolean): WalletState {
        return when (val state = currentStateProvider() as? WalletState.ContentState) {
            is WalletMultiCurrencyState.Content -> {
                state.copy(isBalanceHidden = value)
            }

            is WalletSingleCurrencyState.Content -> {
                state.copy(isBalanceHidden = value)
            }

            else -> currentStateProvider()
        }
    }
}