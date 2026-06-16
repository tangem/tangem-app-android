package com.tangem.features.feed.ui.search.state

import com.tangem.common.ui.markets.models.MarketsListItemUM

internal data class SearchCallbacks(
    val onLoadMore: () -> Unit,
    val onClearHintsClick: () -> Unit,
    val onTextHintClick: (hint: String) -> Unit,
    val onResultMarketTokenClick: (MarketsListItemUM) -> Unit,
    val onHistoryTokenClick: (MarketsListItemUM) -> Unit,
    val onTopMarketSeeAllClick: () -> Unit,
    val onTopMarketItemClick: (MarketsListItemUM) -> Unit,
)