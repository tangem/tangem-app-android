package com.tangem.features.feed.model.market.list.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.features.feed.model.market.list.state.MarketsListUM
import com.tangem.features.feed.model.market.list.state.SortByTypeUM

internal sealed class MarketsListAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Markets", event = event, params = params) {

    class BottomSheetOpened : MarketsListAnalyticsEvent(event = "Markets Screen Opened")

    data class SortBy(
        val sortByTypeUM: SortByTypeUM,
        val interval: MarketsListUM.TrendInterval,
    ) : MarketsListAnalyticsEvent(
        event = "Sort By",
        params = mapOf(
            "Type" to when (sortByTypeUM) {
                SortByTypeUM.Rating -> "Rating"
                SortByTypeUM.Trending -> "Trending"
                SortByTypeUM.ExperiencedBuyers -> "Buyers"
                SortByTypeUM.TopGainers -> "Gainers"
                SortByTypeUM.TopLosers -> "Losers"
                SortByTypeUM.Staking -> "Staking"
                SortByTypeUM.YieldSupply -> "Yield Supply"
            },
            "Period" to when (interval) {
                MarketsListUM.TrendInterval.H24 -> "24h"
                MarketsListUM.TrendInterval.D7 -> "7d"
                MarketsListUM.TrendInterval.M1 -> "1m"
            },
        ),
    )

    class YieldModePromoShown : MarketsListAnalyticsEvent(event = "Notice - Yield Mode Promo")

    class YieldModePromoClosed : MarketsListAnalyticsEvent(event = "Yield Mode Promo Closed")

    class YieldModeMoreInfoClicked : MarketsListAnalyticsEvent(event = "Yield Mode More Info")

    data class TokenSearched(val isTokenFound: Boolean) : MarketsListAnalyticsEvent(
        event = "Token Searched",
        params = mapOf(
            "Result" to if (isTokenFound) "Yes" else "No",
        ),
    )

    class ShowTokens : MarketsListAnalyticsEvent(event = "Button - Show Tokens")
}