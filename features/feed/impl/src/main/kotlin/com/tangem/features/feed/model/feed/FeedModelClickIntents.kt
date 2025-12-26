package com.tangem.features.feed.model.feed

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.feed.model.market.list.state.SortByTypeUM

/**
 * Callback interface for feed model navigation actions.
 */
internal interface FeedModelClickIntents {
    fun onMarketItemClick(token: TokenMarketParams, appCurrency: AppCurrency)
    fun onMarketOpenClick(sortBy: SortByTypeUM?)
    fun onArticleClick(articleId: Int, preselectedArticlesId: List<Int> = emptyList())
    fun onOpenAllNews()
}