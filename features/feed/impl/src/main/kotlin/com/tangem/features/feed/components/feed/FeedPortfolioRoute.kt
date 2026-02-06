package com.tangem.features.feed.components.feed

import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioPreselectedDataComponent
import kotlinx.serialization.Serializable

@Serializable
sealed interface FeedPortfolioRoute {

    @Serializable
    data class AddToPortfolio(val tokenToAdd: AddToPortfolioPreselectedDataComponent.TokenToAdd) : FeedPortfolioRoute
}