package com.tangem.features.polymarket.impl.main.model

import com.tangem.domain.polymarket.model.PolymarketEventsBatchAction
import com.tangem.domain.polymarket.model.PolymarketEventsBatchFlow
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketFeedScrollPosition
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Everything one category of the feed owns: its own pagination, the poller keeping it fresh, and the place the
 * user left its list at.
 *
 * A category holds on to its pages for as long as the screen lives — that is what makes coming back to a tab
 * instant, since switching tabs picks another feed instead of reloading this one. Only the feed in front of the
 * user polls; the others stand still with their pages and their freshness bookkeeping intact, so a return
 * refreshes exactly what went stale while the tab was away.
 *
 * @property config request params this feed pages with — its category
 * @property actionsFlow the flow this feed's pagination takes its actions from
 * @property batchFlow this feed's pages and the results of their refreshes
 * @property poller keeps this feed's visible pages up to date while it is the active one
 */
internal class PolymarketCategoryFeed(
    val config: PolymarketEventsListConfig,
    val actionsFlow: MutableSharedFlow<PolymarketEventsBatchAction>,
    val batchFlow: PolymarketEventsBatchFlow,
    val poller: PolymarketFeedPoller,
) {

    /** Where the list stood when the user last left this tab; `null` until they scroll it. */
    var scrollPosition: PolymarketFeedScrollPosition? = null
}