package com.tangem.features.polymarket.impl.main.model

import com.tangem.domain.polymarket.model.PolymarketEventsBatchAction
import com.tangem.domain.polymarket.model.PolymarketEventsBatchFlow
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketFeedScrollPosition
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Everything one category of the feed owns: its pagination, the poller keeping it fresh and the place its list
 * was left at.
 *
 * Only the feed in front of the user polls; the others keep their pages and their freshness bookkeeping, so a
 * return refreshes exactly what went stale while the tab was away.
 */
internal class PolymarketCategoryFeed(
    val config: PolymarketEventsListConfig,
    val actionsFlow: MutableSharedFlow<PolymarketEventsBatchAction>,
    val batchFlow: PolymarketEventsBatchFlow,
    val poller: PolymarketFeedPoller,
) {

    var scrollPosition: PolymarketFeedScrollPosition? = null
}