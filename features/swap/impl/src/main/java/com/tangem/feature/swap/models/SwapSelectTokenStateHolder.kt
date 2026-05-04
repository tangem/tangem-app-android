package com.tangem.feature.swap.models

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.feature.swap.models.market.state.SwapMarketState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class SwapSelectTokenStateHolder(
    val marketsState: SwapMarketState,
    val tokensListData: TokenListUMData,
    val isBalanceHidden: Boolean,
    val isAfterSearch: Boolean,
    val onSearchEntered: (String) -> Unit,
)

@Immutable
internal sealed interface TokenListUMData {

    val tokensList: ImmutableList<TokensListItemUM>
    val totalTokensCount: Int

    data class AccountList(
        override val tokensList: ImmutableList<TokensListItemUM.Portfolio>,
        override val totalTokensCount: Int,
    ) : TokenListUMData

    data class TokenList(
        override val tokensList: ImmutableList<TokensListItemUM>,
        override val totalTokensCount: Int,
    ) : TokenListUMData

    data object EmptyList : TokenListUMData {
        override val tokensList: ImmutableList<TokensListItemUM> = persistentListOf()
        override val totalTokensCount: Int = EMPTY_TOKENS_COUNT
    }

    private companion object {
        const val EMPTY_TOKENS_COUNT = 0
    }
}

internal val SwapSelectTokenStateHolder.isNotFoundState: Boolean
    get() =
        tokensListData.tokensList.isEmpty() && isAfterSearch &&
            marketsState !is SwapMarketState.Content && marketsState !is SwapMarketState.Loading

internal val SwapSelectTokenStateHolder.isEmptyState: Boolean
    get() =
        tokensListData.tokensList.isEmpty() && !isAfterSearch &&
            marketsState !is SwapMarketState.Content && marketsState !is SwapMarketState.Loading