package com.tangem.features.markets.details.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.EventValue
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.domain.markets.TokenMarketParams

internal class MarketDetailsAnalyticsEvent(
    event: String,
    params: Map<String, EventValue> = mapOf(),
) : AnalyticsEvent(category = "Markets / Chart", event = event, params = params) {

    data class EventBuilder(
        val token: TokenMarketParams,
    ) {
        fun screenOpened(blockchain: String?, source: String) = MarketDetailsAnalyticsEvent(
            event = "Token Chart Screen Opened",
            params = buildMap {
                put("Token", token.symbol.asStringValue())
                blockchain?.let { put("blockchain", it.asStringValue()) }
                put("Source", source.asStringValue())
            },
        )

        fun intervalChanged(intervalType: IntervalType, interval: PriceChangeInterval) = MarketDetailsAnalyticsEvent(
            event = "Button - Period",
            params = mapOf(
                "Token" to token.symbol.asStringValue(),
                "Period" to interval.toAnalyticsString().asStringValue(),
                "Source" to intervalType.source.asStringValue(),
            ),
        )

        fun readMoreClicked() = MarketDetailsAnalyticsEvent(
            event = "Button - Read More",
            params = mapOf(
                "Token" to token.symbol.asStringValue(),
            ),
        )

        fun linkClicked(linkTitle: String) = MarketDetailsAnalyticsEvent(
            event = "Button - Links",
            params = mapOf(
                "Token" to token.symbol.asStringValue(),
                "Link" to linkTitle.asStringValue(),
            ),
        )

        fun exchangesScreenOpened() = MarketDetailsAnalyticsEvent(
            event = "Exchanges Screen Opened",
            params = mapOf(
                "Token" to token.symbol.asStringValue(),
            ),
        )

        fun securityScoreOpened() = MarketDetailsAnalyticsEvent(
            event = "Security Score Info",
            params = mapOf("Token" to token.symbol.asStringValue()),
        )

        fun securityScoreProviderClicked(provider: String) = MarketDetailsAnalyticsEvent(
            event = "Security Score Provider Clicked",
            params = mapOf(
                "Token" to token.symbol.asStringValue(),
                "Provider" to provider.asStringValue(),
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