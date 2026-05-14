package com.tangem.features.feed.components.market.details.portfolio.impl.analytics

import com.tangem.common.ui.markets.action.TokenActionsBSContentUM
import com.tangem.core.analytics.models.AnalyticsEvent

internal class PortfolioAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Markets / Chart", event = event, params = params) {

    data class EventBuilder(
        val tokenSymbol: String,
        val source: String?,
    ) {

        fun addToPortfolioClicked() = PortfolioAnalyticsEvent(
            event = "Button - Add To Portfolio",
            params = buildMap {
                put("Token", tokenSymbol)
                if (source != null) put("Source", source)
            },
        )

        fun quickActionClick(actionUM: TokenActionsBSContentUM.Action, blockchainName: String) =
            PortfolioAnalyticsEvent(
                event = when (actionUM) {
                    TokenActionsBSContentUM.Action.Buy -> "Button - Buy"
                    TokenActionsBSContentUM.Action.Receive -> "Button - Receive"
                    TokenActionsBSContentUM.Action.Exchange -> "Button - Swap"
                    TokenActionsBSContentUM.Action.Stake -> "Button - Stake"
                    TokenActionsBSContentUM.Action.YieldMode -> "Button - Yield Mode"
                    else -> "error"
                },
                params = buildMap {
                    put("Token", tokenSymbol)
                    if (source != null) put("Source", source)
                    put("blockchain", blockchainName)
                },
            )
    }
}