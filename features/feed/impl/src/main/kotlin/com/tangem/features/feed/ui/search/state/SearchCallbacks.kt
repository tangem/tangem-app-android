package com.tangem.features.feed.ui.search.state

internal data class SearchCallbacks(
    val onLoadMore: () -> Unit,
    val onClearHintsClick: () -> Unit,
    val onTextHintClick: (hint: String) -> Unit,
    val onResultMarketTokenClick: () -> Unit,
)