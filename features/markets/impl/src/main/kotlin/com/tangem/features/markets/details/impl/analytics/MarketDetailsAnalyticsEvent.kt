package com.tangem.features.markets.details.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.domain.markets.TokenMarketParams

internal class MarketDetailsAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = "Markets / Chart", event = event, params = params) {

    data class EventBuilder(
        val token: TokenMarketParams,
    ) {
        fun screenOpened(blockchain: String?, source: String) = MarketDetailsAnalyticsEvent(
            event = "Token Chart Screen Opened",
            params = buildMap {
                put("Token", token.symbol)
                blockchain?.let { put("blockchain", it) }
                put("Source", source)
            },
        )

        fun intervalChanged(intervalType: IntervalType, interval: PriceChangeInterval) = MarketDetailsAnalyticsEvent(
            event = "Button - Period",
            params = mapOf(
                "Token" to token.symbol,
                "Period" to interval.toAnalyticsString(),
                "Source" to intervalType.source,
            ),
        )

        fun readMoreClicked() = MarketDetailsAnalyticsEvent(
            event = "Button - Read More",
            params = mapOf(
                "Token" to token.symbol,
            ),
        )

        fun linkClicked(linkTitle: String) = MarketDetailsAnalyticsEvent(
            event = "Button - Links",
            params = mapOf(
                "Token" to token.symbol,
                "Link" to linkTitle,
            ),
        )

        fun exchangesScreenOpened() = MarketDetailsAnalyticsEvent(
            event = "Exchanges Screen Opened",
            params = mapOf(
                "Token" to token.symbol,
            ),
        )

        fun securityScoreOpened() = MarketDetailsAnalyticsEvent(
            event = "Security Score Info",
            params = mapOf("Token" to token.symbol),
        )

        fun securityScoreProviderClicked(provider: String) = MarketDetailsAnalyticsEvent(
            event = "Security Score Provider Clicked",
            params = mapOf(
                "Token" to token.symbol,
                "Provider" to provider,
            ),
        )
    }

    enum class IntervalType(val source: String) {
        Chart("Chart"),
        PricePerformance("Price"),
        Insights("Insights"),
    }
}

private fun PriceChangeInterval.toAnalyticsString() = when (this) {
    PriceChangeInterval.H24 -> "24h"
    PriceChangeInterval.WEEK -> "7d"
    PriceChangeInterval.MONTH -> "1m"
    PriceChangeInterval.MONTH3 -> "3m"
    PriceChangeInterval.MONTH6 -> "6m"
    PriceChangeInterval.YEAR -> "1y"
    PriceChangeInterval.ALL_TIME -> "All"
}