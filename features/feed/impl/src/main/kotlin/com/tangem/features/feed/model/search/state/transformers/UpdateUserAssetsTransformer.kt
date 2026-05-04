package com.tangem.features.feed.model.search.state.transformers

import com.tangem.features.feed.ui.search.state.MarketSearchResultUM
import com.tangem.features.feed.ui.search.state.SearchContentUM
import com.tangem.features.feed.ui.search.state.SearchUM
import com.tangem.features.feed.ui.search.state.UserAssetItemUM
import kotlinx.collections.immutable.ImmutableList

internal class UpdateUserAssetsTransformer(
    private val userAssets: ImmutableList<UserAssetItemUM>,
) : SearchUMTransformer {

    override fun transform(prevState: SearchUM): SearchUM {
        val currentContent = prevState.content
        val currentMarketTokens = if (currentContent is SearchContentUM.Results) {
            currentContent.marketTokens
        } else {
            MarketSearchResultUM.Loading
        }
        return prevState.copy(
            content = SearchContentUM.Results(
                userAssets = userAssets,
                marketTokens = currentMarketTokens,
            ),
        )
    }
}