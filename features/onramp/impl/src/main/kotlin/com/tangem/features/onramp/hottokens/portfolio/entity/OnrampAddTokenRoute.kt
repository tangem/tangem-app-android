package com.tangem.features.onramp.hottokens.portfolio.entity

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface OnrampAddTokenRoute {

    @Serializable
    data object Empty : OnrampAddTokenRoute

    @Serializable
    data object PortfolioSelector : OnrampAddTokenRoute

    @Serializable
    data object AddToken : OnrampAddTokenRoute
}