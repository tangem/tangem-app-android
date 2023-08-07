package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.common.Provider
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.content.WalletTokensListState
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf

internal class TokenListErrorToWalletStateConverter(
    private val currentStateProvider: Provider<WalletStateHolder>,
) : Converter<TokenListError, WalletStateHolder> {

    // TODO: https://tangem.atlassian.net/browse/AND-4021
    override fun convert(value: TokenListError): WalletStateHolder {
        val state = currentStateProvider()
        return WalletStateHolder.MultiCurrencyContent(
            onBackClick = state.onBackClick,
            topBarConfig = state.topBarConfig,
            walletsListConfig = state.walletsListConfig,
            pullToRefreshConfig = state.pullToRefreshConfig,
            notifications = state.notifications,
            bottomSheet = state.bottomSheet,
            tokensListState = WalletTokensListState.Content(items = persistentListOf(), onOrganizeTokensClick = null),
        )
    }
}
