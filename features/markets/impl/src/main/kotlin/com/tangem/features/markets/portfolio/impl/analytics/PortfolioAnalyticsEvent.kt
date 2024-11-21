package com.tangem.features.markets.portfolio.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsEvent.Companion.asStringValue
import com.tangem.core.analytics.models.EventValue
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.markets.portfolio.impl.ui.state.TokenActionsBSContentUM

internal class PortfolioAnalyticsEvent(
    event: String,
    params: Map<String, EventValue> = mapOf(),
) : AnalyticsEvent(category = "Markets / Chart", event = event, params = params) {

    data class EventBuilder(
        val token: TokenMarketParams,
        val source: String?,
    ) {

        fun addToPortfolioClicked() = PortfolioAnalyticsEvent(
            event = "Button - Add To Portfolio",
            params = mapOf(
                "Token" to token.symbol.asStringValue(),
            ),
        )

        fun addToPortfolioWalletChanged() = PortfolioAnalyticsEvent(event = "Wallet Selected")

        fun addToPortfolioContinue(blockchainNames: List<String>) = PortfolioAnalyticsEvent(
            event = "Token Network Selected",
            params = mapOf(
                "Count" to blockchainNames.size.asStringValue(),
                "Token" to token.symbol.asStringValue(),
                "blockchain" to blockchainNames.asListValue(), // TODO analytics
            ),
        )

        fun quickActionClick(actionUM: TokenActionsBSContentUM.Action, blockchainName: String) =
            PortfolioAnalyticsEvent(
                event = when (actionUM) {
                    TokenActionsBSContentUM.Action.Buy -> "Button - Buy"
                    TokenActionsBSContentUM.Action.Receive -> "Button - Receive"
                    TokenActionsBSContentUM.Action.Exchange -> "Button - Swap"
                    else -> "error"
                },
                params = buildMap {
                    put("Token", token.symbol.asStringValue())
                    source?.let { put("Source", source.asStringValue()) }
                    put("blockchain", blockchainName.asStringValue())
                },
            )
    }
}