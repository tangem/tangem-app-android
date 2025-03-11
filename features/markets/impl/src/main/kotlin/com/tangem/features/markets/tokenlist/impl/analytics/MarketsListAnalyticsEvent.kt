package com.tangem.features.markets.tokenlist.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.features.markets.tokenlist.impl.ui.state.MarketsListUM
import com.tangem.features.markets.tokenlist.impl.ui.state.SortByTypeUM

internal sealed class MarketsListAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = "Markets", event = event, params = params) {

    data object BottomSheetOpened : MarketsListAnalyticsEvent(event = "Markets Screen Opened")

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
            },
            "Period" to when (interval) {
                MarketsListUM.TrendInterval.H24 -> "24h"
                MarketsListUM.TrendInterval.D7 -> "7d"
                MarketsListUM.TrendInterval.M1 -> "1m"
            },
        ),
    )
}