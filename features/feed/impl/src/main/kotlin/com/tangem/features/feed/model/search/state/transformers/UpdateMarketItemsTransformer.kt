package com.tangem.features.feed.model.search.state.transformers

import com.tangem.features.feed.ui.search.state.MarketSearchResultUM
import com.tangem.features.feed.ui.search.state.SearchContentUM
import com.tangem.features.feed.ui.search.state.SearchUM
import kotlinx.collections.immutable.persistentListOf

internal class UpdateMarketItemsTransformer(
    private val marketResult: MarketSearchResultUM,
) : SearchUMTransformer {

    override fun transform(prevState: SearchUM): SearchUM {
        val currentContent = prevState.content

        val currentUserAssets = if (currentContent is SearchContentUM.Results) {
            currentContent.userAssets
        } else {
            persistentListOf()
        }
        return prevState.copy(
            content = SearchContentUM.Results(
                userAssets = currentUserAssets,
                marketTokens = marketResult,
            ),
        )
    }
}