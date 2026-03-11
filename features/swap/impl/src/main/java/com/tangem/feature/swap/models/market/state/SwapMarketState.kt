package com.tangem.feature.swap.models.market.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.core.ui.R
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class SwapMarketState {

    abstract val marketsTitle: TextReference
    abstract val shouldAssetsCount: Boolean

    data class Content(
        val items: ImmutableList<MarketsListItemUM>,
        val total: Int,
        val loadMore: () -> Unit,
        val onItemClick: (MarketsListItemUM) -> Unit,
        val visibleIdsChanged: (List<CryptoCurrency.RawID>) -> Unit,
        override val marketsTitle: TextReference,
        override val shouldAssetsCount: Boolean,
    ) : SwapMarketState()

    data class Loading(
        override val marketsTitle: TextReference,
        override val shouldAssetsCount: Boolean,
    ) : SwapMarketState()

    data class LoadingError(
        val onRetryClicked: () -> Unit,
        override val marketsTitle: TextReference,
        override val shouldAssetsCount: Boolean,
    ) : SwapMarketState()

    data object SearchNothingFound : SwapMarketState() {
        override val marketsTitle: TextReference = TextReference.Res(R.string.markets_common_title)
        override val shouldAssetsCount: Boolean = true
    }
}