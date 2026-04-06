package com.tangem.features.feed.model.search.state.transformers

import com.tangem.features.feed.ui.search.state.MarketSearchResultUM
import com.tangem.features.feed.ui.search.state.SearchContentUM
import com.tangem.features.feed.ui.search.state.SearchUM
import kotlinx.collections.immutable.persistentListOf

internal class SetSearchResultsLoadingTransformer : SearchUMTransformer {

    override fun transform(prevState: SearchUM): SearchUM {
        val currentContent = prevState.content
        val currentUserAssets = if (currentContent is SearchContentUM.Results) {
            currentContent.userAssets
        } else {
            persistentListOf()
        }
        // Keep showing the previous market list while the new query loads (stale-while-revalidate).
        // Replacing with Loading + empty batch flashes skeletons / empty section even when the API
        // returns the same tokens for a refined query.
        val nextMarketTokens = if (currentContent is SearchContentUM.Results) {
            when (val market = currentContent.marketTokens) {
                is MarketSearchResultUM.Content -> market
                else -> MarketSearchResultUM.Loading
            }
        } else {
            MarketSearchResultUM.Loading
        }
        return prevState.copy(
            content = SearchContentUM.Results(
                userAssets = currentUserAssets,
                marketTokens = nextMarketTokens,
            ),
        )
    }
}