package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.common.Provider
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf

internal class TokenListErrorConverter(
    private val currentStateProvider: Provider<WalletState>,
) : Converter<TokenListError, WalletState> {

    override fun convert(value: TokenListError): WalletState {
        return when (val state = currentStateProvider()) {
            is WalletMultiCurrencyState.Content -> {
                state.copy(
                    tokensListState = WalletTokensListState.Content(
                        items = persistentListOf(),
                        organizeTokensButton = WalletTokensListState.OrganizeTokensButtonState.Hidden,
                    ),
                )
            }
            is WalletMultiCurrencyState.Locked,
            is WalletSingleCurrencyState.Content,
            is WalletSingleCurrencyState.Locked,
            is WalletState.Initial,
            -> state
        }
    }
}