package com.tangem.features.feed.ui.search.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.common.ui.markets.tokenselector.UserAssetItemUM
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.features.feed.ui.feed.state.MarketChartUM
import kotlinx.collections.immutable.ImmutableList

internal data class SearchUM(
    val searchBar: SearchBarUM,
    val content: SearchContentUM,
    val topMarkets: MarketChartUM,
)

@Immutable
sealed interface SearchContentUM {

    data class History(
        val textHints: ImmutableList<TextHintItemUM>,
        val recentTokens: ImmutableList<MarketsListItemUM>,
    ) : SearchContentUM

    data class Results(
        val userAssets: ImmutableList<UserAssetItemUM>,
        val marketTokens: MarketSearchResultUM,
    ) : SearchContentUM

    data object InitialEmpty : SearchContentUM
}

@Immutable
sealed interface MarketSearchResultUM {

    data class Content(
        val items: ImmutableList<MarketsListItemUM>,
        val shouldShowUnder100kNotification: Boolean = false,
        val onShowUnder100kClick: () -> Unit = {},
    ) : MarketSearchResultUM

    data object Loading : MarketSearchResultUM
    data object NotFound : MarketSearchResultUM
    data object Empty : MarketSearchResultUM
}

data class TextHintItemUM(val text: String)