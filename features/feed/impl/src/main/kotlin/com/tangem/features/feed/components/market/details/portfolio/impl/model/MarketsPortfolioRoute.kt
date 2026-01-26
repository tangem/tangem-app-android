package com.tangem.features.feed.components.market.details.portfolio.impl.model

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.TokenReceiveConfig
import kotlinx.serialization.Serializable

@Serializable
sealed interface MarketsPortfolioRoute : Route {

    @Serializable
    data object AddToPortfolio : MarketsPortfolioRoute

    @Serializable
    data class TokenReceive(
        val config: TokenReceiveConfig,
    ) : MarketsPortfolioRoute
}