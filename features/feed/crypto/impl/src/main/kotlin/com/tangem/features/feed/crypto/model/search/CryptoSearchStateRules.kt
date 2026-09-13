package com.tangem.features.feed.crypto.model.search

import com.tangem.features.feed.crypto.ui.state.CryptoSearchUM
import com.tangem.features.feed.crypto.ui.state.MarketSearchUM
import com.tangem.features.feed.crypto.ui.state.PortfolioSearchUM
import com.tangem.features.feed.crypto.ui.state.MarketPulseItemUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun initialCryptoSearchState(): CryptoSearchUM = CryptoSearchUM(
    query = "",
    portfolio = PortfolioSearchUM.Empty,
    market = MarketSearchUM.Loading,
)

/**
 * Keeps the previous market results on screen while the new query loads, so refining a query does not
 * flash a shimmer over results that are about to be replaced.
 */
internal fun CryptoSearchUM.startingNewQuery(query: String): CryptoSearchUM = copy(
    query = query,
    market = market as? MarketSearchUM.Content ?: MarketSearchUM.Loading,
)

/**
 * A batch flow reports an empty list between a reload and its first batch, which reads as
 * [MarketSearchUM.Loading]. Applying that verbatim would shimmer over results the user is still
 * reading, so a loading snapshot never displaces content already on screen — but it does replace a
 * [MarketSearchUM.NotFound] or a [MarketSearchUM.Error], which is how a retry gets its shimmer back.
 */
internal fun CryptoSearchUM.applyMarketSnapshot(snapshot: MarketSearchUM): CryptoSearchUM =
    if (snapshot is MarketSearchUM.Loading && market is MarketSearchUM.Content) {
        this
    } else {
        copy(market = snapshot)
    }

/**
 * Low-capitalisation tokens are held back behind a notification until the user asks for them: a query
 * like "btc" otherwise surfaces dozens of near-worthless look-alikes above the real token.
 */
internal fun buildMarketContent(
    items: ImmutableList<MarketPulseItemUM>,
    shouldShowAllTokens: Boolean,
    onShowAllTokens: () -> Unit,
    loadMore: () -> Unit,
): MarketSearchUM.Content {
    if (shouldShowAllTokens) {
        return MarketSearchUM.Content(items = items, loadMore = loadMore)
    }

    val filtered = items.filterNot { it.isUnderMarketCapLimit }.toImmutableList()

    return MarketSearchUM.Content(
        items = filtered,
        loadMore = loadMore,
        shouldShowUnderMarketCapLimitNotification = filtered.size != items.size,
        onShowUnderMarketCapLimitClick = onShowAllTokens,
    )
}