package com.tangem.feature.swap.models.market.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
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

    companion object {
        val DefaultLoading
            get() = Loading(
                marketsTitle = TextReference.Res(R.string.feed_trending_now),
                shouldAssetsCount = false,
            )
        val SearchLoading
            get() = Loading(
                marketsTitle = TextReference.Res(R.string.markets_common_title),
                shouldAssetsCount = true,
            )
    }
}