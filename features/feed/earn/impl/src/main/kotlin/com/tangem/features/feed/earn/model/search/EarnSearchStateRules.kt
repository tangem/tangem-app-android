package com.tangem.features.feed.earn.model.search

import com.tangem.features.feed.earn.ui.state.EarnSearchListUM
import com.tangem.features.feed.earn.ui.state.EarnSearchPortfolioUM
import com.tangem.features.feed.earn.ui.state.EarnSearchUM

internal fun initialEarnSearchState(): EarnSearchUM = EarnSearchUM(
    query = "",
    portfolio = EarnSearchPortfolioUM.Empty,
    earn = EarnSearchListUM.Loading,
)

/**
 * Keeps the previous results on screen while the new query resolves, so refining a query does not
 * flash a shimmer over rows that are about to be replaced. Only the first query of a session pays for
 * the catalogue, so this matters mostly for that one.
 */
internal fun EarnSearchUM.startingNewQuery(query: String): EarnSearchUM = copy(
    query = query,
    earn = earn as? EarnSearchListUM.Content ?: EarnSearchListUM.Loading,
)