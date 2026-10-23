package com.tangem.features.feed.ui.feed.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class FeedListSearchBar(
    val onBarClick: () -> Unit,
    val placeholderText: TextReference,
)

@Immutable
internal sealed interface MarketChartUM {

    data class Content(
        val items: ImmutableList<MarketsListItemUM>,
    ) : MarketChartUM

    data object Loading : MarketChartUM

    data class LoadingError(val onRetryClicked: () -> Unit) : MarketChartUM
}