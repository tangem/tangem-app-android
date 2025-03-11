package com.tangem.features.markets.portfolio.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.markets.portfolio.impl.ui.state.TokenActionsBSContentUM

internal class PortfolioAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = "Markets / Chart", event = event, params = params) {

    data class EventBuilder(
        val token: TokenMarketParams,
        val source: String?,
    ) {

        fun addToPortfolioClicked() = PortfolioAnalyticsEvent(
            event = "Button - Add To Portfolio",
            params = mapOf(
                "Token" to token.symbol,
            ),
        )

        fun addToPortfolioWalletChanged() = PortfolioAnalyticsEvent(event = "Wallet Selected")

        fun addToPortfolioContinue(blockchainNames: List<String>) = PortfolioAnalyticsEvent(
            event = "Token Network Selected",
            params = mapOf(
                "Count" to blockchainNames.size.toString(),
                "Token" to token.symbol,
                "blockchain" to blockchainNames.joinToString(separator = ", "),
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
                    put("Token", token.symbol)
                    source?.let { put("Source", source) }
                    put("blockchain", blockchainName)
                },
            )
    }
}