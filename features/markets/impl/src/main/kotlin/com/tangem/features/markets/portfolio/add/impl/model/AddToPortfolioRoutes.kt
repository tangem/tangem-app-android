package com.tangem.features.markets.portfolio.add.impl.model

import androidx.compose.runtime.Immutable
import com.tangem.core.decompose.navigation.Route
import com.tangem.features.markets.portfolio.add.api.SelectedPortfolio
import kotlinx.serialization.Serializable

@Serializable
@Immutable
internal sealed interface AddToPortfolioRoutes : Route {

    @Serializable
    data object Empty : AddToPortfolioRoutes

    @Serializable
    data object PortfolioSelector : AddToPortfolioRoutes

    @Serializable
    data class NetworkSelector(
        val selectedPortfolio: SelectedPortfolio,
    ) : AddToPortfolioRoutes

    @Serializable
    data object AddToken : AddToPortfolioRoutes

    @Serializable
    data object TokenActions : AddToPortfolioRoutes
}