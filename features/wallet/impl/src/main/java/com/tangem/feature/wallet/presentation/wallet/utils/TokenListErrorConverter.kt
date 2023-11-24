package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.domain.tokens.error.TokenListError
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class TokenListErrorConverter(
    private val currentStateProvider: Provider<WalletState>,
) : Converter<TokenListError, WalletState> {

    override fun convert(value: TokenListError): WalletState {
        val state = currentStateProvider()
        return when (value) {
            is TokenListError.EmptyTokens -> state.mapToEmptyTokensState()
            is TokenListError.DataError,
            is TokenListError.UnableToSortTokenList,
            -> state
        }
    }

    private fun WalletState.mapToEmptyTokensState(): WalletState {
        return when (this) {
            is WalletMultiCurrencyState.Content -> copy(tokensListState = WalletTokensListState.Empty)
            is WalletMultiCurrencyState.Locked,
            is WalletSingleCurrencyState.Content,
            is WalletSingleCurrencyState.Locked,
            is WalletState.Initial,
            -> this
        }
    }
}