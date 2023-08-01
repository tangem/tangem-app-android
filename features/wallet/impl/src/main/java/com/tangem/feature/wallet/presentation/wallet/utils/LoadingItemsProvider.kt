package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.wallet.state.content.WalletTokensListState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal object LoadingItemsProvider {

    fun getLoadingMultiCurrencyTokens(): ImmutableList<WalletTokensListState.TokensListItemState.Token> {
        return buildList(capacity = 5) {
            add(WalletTokensListState.TokensListItemState.Token(state = TokenItemState.Loading))
        }.toImmutableList()
    }
}