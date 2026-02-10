package com.tangem.features.feed.model.feed

import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.news.model.NewsListConfig
import com.tangem.features.feed.model.market.list.state.SortByTypeUM

/**
 * Callback interface for feed model navigation actions.
 */
internal interface FeedModelClickIntents {
    fun onMarketItemClick(token: TokenMarketParams, appCurrency: AppCurrency, source: String, newsId: Int? = null)
    fun onMarketOpenClick(sortBy: SortByTypeUM?)

    /**
     * Method to open article of news from different places.
     * @param articleId - id of article, which should be opened.
     * @param preselectedArticlesId - ids of articles, which were prefetched before opening news details.
     * @param screenSource - source of screen, from which article was opened.
     * @param paginationConfig - config of current pagination from news list. Must be null, when open from other places.
     */
    fun onArticleClick(
        articleId: Int,
        preselectedArticlesId: List<Int> = emptyList(),
        screenSource: AnalyticsParam.ScreensSources,
        paginationConfig: NewsListConfig? = null,
    )
    fun onOpenAllNews()

    fun onOpenEarnPage()
}