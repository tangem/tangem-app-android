package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.common.Provider
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf

internal class TokenListErrorConverter(
    private val currentStateProvider: Provider<WalletState>,
) : Converter<TokenListError, WalletMultiCurrencyState.Content> {

    // TODO: [REDACTED_JIRA]
    override fun convert(value: TokenListError): WalletMultiCurrencyState.Content {
        return requireNotNull(currentStateProvider() as? WalletMultiCurrencyState.Content).copy(
            tokensListState = WalletTokensListState.Content(items = persistentListOf(), onOrganizeTokensClick = null),
        )
    }
}