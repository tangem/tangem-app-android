package com.tangem.features.feed.model.search.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_CODE
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_MESSAGE
import com.tangem.core.analytics.models.AnalyticsParam.Key.SOURCE
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM
import com.tangem.core.analytics.models.IS_NOT_HTTP_ERROR

internal sealed class SearchAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Search", event = event, params = params) {

    data class SearchScreenOpened(
        private val screensSource: String,
    ) : SearchAnalyticsEvent(
        event = "Search Screen Opened",
        params = mapOf(SOURCE to screensSource),
    )

    class SearchStarted : SearchAnalyticsEvent(event = "Search Started")

    data class ResultsShown(
        private val totalResultsCount: Int,
        private val marketsResultsCount: Int,
        private val userTokensResultsCount: Int,
    ) : SearchAnalyticsEvent(
        event = "Results Shown",
        params = mapOf(
            "Total Results" to totalResultsCount.toString(),
            "User Tokens Count" to userTokensResultsCount.toString(),
            "Market Tokens Count" to marketsResultsCount.toString(),
        ),
    )

    data class ErrorMarketsData(
        private val code: Int?,
        private val message: String,
    ) : SearchAnalyticsEvent(
        event = "Error - Markets Data",
        params = mapOf(
            ERROR_CODE to (code ?: IS_NOT_HTTP_ERROR).toString(),
            ERROR_MESSAGE to message,
        ),
    )

    data class PortfolioItemClicked(
        private val tokenSymbol: String,
    ) : SearchAnalyticsEvent(
        event = "Portfolio Item Clicked",
        params = mapOf(TOKEN_PARAM to tokenSymbol),
    )

    data class HintClicked(
        private val hint: String,
    ) : SearchAnalyticsEvent(
        event = "Hint Clicked",
        params = mapOf("Text" to hint),
    )

    data class RecentItemClicked(
        private val tokenSymbol: String,
    ) : SearchAnalyticsEvent(
        event = "Recent Item Clicked",
        params = mapOf(TOKEN_PARAM to tokenSymbol),
    )

    data class MarketItemClicked(
        private val tokenSymbol: String,
    ) : SearchAnalyticsEvent(
        event = "Market Item Clicked",
        params = mapOf(TOKEN_PARAM to tokenSymbol),
    )

    data class GroupClicked(
        private val tokenSymbol: String,
    ) : SearchAnalyticsEvent(
        event = "Group Clicked",
        params = mapOf(TOKEN_PARAM to tokenSymbol),
    )

    class ButtonClearHistoryClick : SearchAnalyticsEvent(event = "Button - Clear History")
}