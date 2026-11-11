package com.tangem.features.polymarket.impl.main.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketCategoryTabUM
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketFeedContentUM
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketFeedScrollPosition
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * @property categories empty while they load and when the backend could not serve them — the feed is then not
 *  requested and [content] shows the reload prompt
 * @property pendingScrollPosition applied by the screen once the returned-to category is laid out, then cleared
 */
@Immutable
internal data class PolymarketMainUM(
    val balance: PredictionBalanceUM,
    val categories: ImmutableList<PolymarketCategoryTabUM>,
    val content: PolymarketFeedContentUM,
    val pullToRefreshConfig: PullToRefreshConfig,
    val filters: ImmutableList<TangemTabItemUM.Content> = persistentListOf(),
    val selection: PolymarketEventsListConfig? = null,
    val onSeeAllClick: (() -> Unit)? = null,
    val isCategoriesLoading: Boolean = false,
    val pendingScrollPosition: PolymarketFeedScrollPosition? = null,
    val isAwaitingFunds: Boolean = false,
    val isRegionRestricted: Boolean = false,
    val isFastTradingSettingsAvailable: Boolean = false,
)