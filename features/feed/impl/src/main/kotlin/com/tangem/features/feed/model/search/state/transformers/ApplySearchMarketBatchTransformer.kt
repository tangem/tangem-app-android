package com.tangem.features.feed.model.search.state.transformers

import com.tangem.features.feed.ui.search.state.MarketSearchResultUM
import com.tangem.features.feed.ui.search.state.SearchContentUM
import com.tangem.features.feed.ui.search.state.SearchUM

internal class ApplySearchMarketBatchTransformer(
    private val snapshot: SearchMarketBatchUiSnapshot,
) : SearchUMTransformer {

    override fun transform(prevState: SearchUM): SearchUM {
        if (shouldIgnoreTransientMarketEmpty(prevState, snapshot)) return prevState
        return UpdateMarketItemsTransformer(snapshot.marketResult).transform(prevState)
    }

    private fun shouldIgnoreTransientMarketEmpty(prevState: SearchUM, snapshot: SearchMarketBatchUiSnapshot): Boolean {
        if (snapshot.marketResult !is MarketSearchResultUM.Empty) return false
        if (snapshot.isSearchNotFound || snapshot.isInErrorState) return false
        val prev = prevState.content as? SearchContentUM.Results ?: return false
        return when (prev.marketTokens) {
            is MarketSearchResultUM.Content, is MarketSearchResultUM.Loading -> true
            else -> false
        }
    }
}

/** Inputs from the search market list flow before merging into [SearchUM]. */
internal data class SearchMarketBatchUiSnapshot(
    val marketResult: MarketSearchResultUM,
    val isSearchNotFound: Boolean,
    val isInErrorState: Boolean,
)