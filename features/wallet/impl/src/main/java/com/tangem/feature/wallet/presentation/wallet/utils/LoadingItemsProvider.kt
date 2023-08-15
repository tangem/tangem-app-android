package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal object LoadingItemsProvider {

    fun getLoadingMultiCurrencyTokens(): ImmutableList<WalletTokensListState.TokensListItemState.Token> {
        val items = mutableListOf<WalletTokensListState.TokensListItemState.Token>()
        repeat(times = 5) {
            items.add(
                WalletTokensListState.TokensListItemState.Token(
                    state = TokenItemState.Loading(id = "Loading#$it"),
                ),
            )
        }
        return items.toImmutableList()
    }
}