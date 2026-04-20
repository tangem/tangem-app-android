package com.tangem.features.feed.model.search.analytics

import com.tangem.core.analytics.api.AnalyticsEventHandler
import javax.inject.Inject

class SearchAnalyticsHelper @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    private var isSearchStartedWasSent = false

    fun sendSearchScreenOpened(source: String) {
        analyticsEventHandler.send(SearchAnalyticsEvent.SearchScreenOpened(source))
    }

    fun sendMarketItemClicked(tokenSymbol: String) {
        analyticsEventHandler.send(SearchAnalyticsEvent.MarketItemClicked(tokenSymbol))
    }

    fun sendClearButtonClicked() {
        analyticsEventHandler.send(SearchAnalyticsEvent.ButtonClearHistoryClick())
    }

    fun sendHintClicked(text: String) {
        analyticsEventHandler.send(SearchAnalyticsEvent.HintClicked(text))
    }

    fun sendRecentItemClicked(tokenSymbol: String) {
        analyticsEventHandler.send(SearchAnalyticsEvent.RecentItemClicked(tokenSymbol))
    }

    fun sendSearchStarted() {
        if (isSearchStartedWasSent) return
        analyticsEventHandler.send(SearchAnalyticsEvent.SearchStarted())
        isSearchStartedWasSent = true
    }

    fun sendPortfolioItemClicked(tokenSymbol: String) {
        analyticsEventHandler.send(SearchAnalyticsEvent.PortfolioItemClicked(tokenSymbol))
    }

    fun sendGroupClicked(tokenSymbol: String) {
        analyticsEventHandler.send(SearchAnalyticsEvent.GroupClicked(tokenSymbol))
    }

    fun sendResultShown(totalResultsCount: Int, marketsResultsCount: Int, userTokensResultsCount: Int) {
        analyticsEventHandler.send(
            SearchAnalyticsEvent.ResultsShown(
                totalResultsCount = totalResultsCount,
                marketsResultsCount = marketsResultsCount,
                userTokensResultsCount = userTokensResultsCount,
            ),
        )
    }

    fun sendErrorMarketsData(code: Int?, message: String) {
        analyticsEventHandler.send(
            SearchAnalyticsEvent.ErrorMarketsData(
                code = code,
                message = message,
            ),
        )
    }
}