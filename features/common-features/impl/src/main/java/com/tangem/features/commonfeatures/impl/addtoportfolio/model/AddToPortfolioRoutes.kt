package com.tangem.features.commonfeatures.impl.addtoportfolio.model

import androidx.compose.runtime.Immutable
import com.tangem.core.decompose.navigation.Route
import com.tangem.features.commonfeatures.api.addtoportfolio.SelectedPortfolio
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
    data object UserPortfolio : AddToPortfolioRoutes

    @Serializable
    data object TokenActions : AddToPortfolioRoutes
}