package com.tangem.features.commonfeatures.impl.choosetoken.market.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.markets.TokenMarketListConfig
import kotlinx.collections.immutable.ImmutableList

/**
 * Category of the "Market Pulse" block on the Choose asset screen.
 *
 * Each category maps to a [TokenMarketListConfig.Order] used to fetch the markets list.
 */
internal enum class SwapMarketCategory(
    val title: TextReference,
    val order: TokenMarketListConfig.Order,
) {
    Trending(
        title = resourceReference(R.string.markets_sort_by_trending_title),
        order = TokenMarketListConfig.Order.Trending,
    ),
    ExperiencedBuyers(
        title = resourceReference(R.string.markets_sort_by_experienced_buyers_title),
        order = TokenMarketListConfig.Order.Buyers,
    ),
    TopGainers(
        title = resourceReference(R.string.markets_sort_by_top_gainers_title),
        order = TokenMarketListConfig.Order.TopGainers,
    ),
    TopLosers(
        title = resourceReference(R.string.markets_sort_by_top_losers_title),
        order = TokenMarketListConfig.Order.TopLosers,
    ),
}

/**
 * UI model for the selectable category tabs of the "Market Pulse" block.
 *
 * @property items          all available categories in display order.
 * @property selected       currently selected category.
 * @property onCategoryClick invoked when a category tab is tapped.
 */
@Immutable
internal data class SwapMarketCategoriesUM(
    val items: ImmutableList<SwapMarketCategory>,
    val selected: SwapMarketCategory,
    val onCategoryClick: (SwapMarketCategory) -> Unit,
)