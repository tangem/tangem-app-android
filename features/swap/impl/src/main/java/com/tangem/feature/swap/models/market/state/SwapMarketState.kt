package com.tangem.feature.swap.models.market.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.domain.models.currency.CryptoCurrency
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class SwapMarketState {

    data class Content(
        val items: ImmutableList<MarketsListItemUM>,
        val total: Int,
        val loadMore: () -> Unit,
        val onItemClick: (MarketsListItemUM) -> Unit,
        val visibleIdsChanged: (List<CryptoCurrency.RawID>) -> Unit,
    ) : SwapMarketState()

    data object Loading : SwapMarketState()

    data class LoadingError(
        val onRetryClicked: () -> Unit,
    ) : SwapMarketState()

    data object SearchNothingFound : SwapMarketState()
}