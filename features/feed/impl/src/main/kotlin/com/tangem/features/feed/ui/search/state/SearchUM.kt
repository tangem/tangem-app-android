package com.tangem.features.feed.ui.search.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import kotlinx.collections.immutable.ImmutableList

data class SearchUM(
    val searchBar: SearchBarUM,
    val content: SearchContentUM,
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

data class UserAssetItemUM(
    val id: String,
    val tokenIconUrl: String?,
    val tokenName: String,
    val tokenSymbol: String,
    val accountName: String,
    val onClick: () -> Unit,
)