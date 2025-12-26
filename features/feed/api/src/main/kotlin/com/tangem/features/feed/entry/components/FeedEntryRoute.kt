package com.tangem.features.feed.entry.components

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketParams
import kotlinx.serialization.Serializable

@Serializable
sealed interface FeedEntryRoute {

    @Serializable
    data class MarketTokenDetails(
        val token: TokenMarketParams,
        val appCurrency: AppCurrency,
        val shouldShowPortfolio: Boolean,
        val analyticsParams: AnalyticsParams? = null,
    ) : FeedEntryRoute {

        @Serializable
        data class AnalyticsParams(
            val blockchain: String?,
            val source: String,
        )
    }

    @Serializable
    data object MarketTokenList : FeedEntryRoute

    @Serializable
    data class NewsDetail(val articleId: Int, val preselectedArticlesId: List<Int>) : FeedEntryRoute
}