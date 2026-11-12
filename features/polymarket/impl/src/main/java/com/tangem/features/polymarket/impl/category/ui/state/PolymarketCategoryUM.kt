package com.tangem.features.polymarket.impl.category.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketCategoryTabUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketFeedContentUM

@Immutable
internal data class PolymarketCategoryUM(
    val title: TextReference,
    val selection: PolymarketEventsListConfig,
    val pullToRefreshConfig: PullToRefreshConfig,
    val subcategories: ImmutableList<PolymarketCategoryTabUM> = persistentListOf(),
    val filters: ImmutableList<TangemTabItemUM> = persistentListOf(),
    val content: PolymarketFeedContentUM = PolymarketFeedContentUM.Loading,
)