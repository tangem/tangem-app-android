package com.tangem.feature.swap.models

import com.tangem.feature.swap.choosetoken.api.model.TokenListUMData
import com.tangem.feature.swap.models.market.state.SwapMarketState

internal data class SwapSelectTokenStateHolder(
    val marketsState: SwapMarketState,
    val tokensListData: TokenListUMData,
    val isBalanceHidden: Boolean,
    val isAfterSearch: Boolean,
    val onSearchEntered: (String) -> Unit,
)

internal val SwapSelectTokenStateHolder.isNotFoundState: Boolean
    get() =
        tokensListData.tokensList.isEmpty() && isAfterSearch &&
            marketsState !is SwapMarketState.Content && marketsState !is SwapMarketState.Loading

internal val SwapSelectTokenStateHolder.isEmptyState: Boolean
    get() =
        tokensListData.tokensList.isEmpty() && !isAfterSearch &&
            marketsState !is SwapMarketState.Content && marketsState !is SwapMarketState.Loading