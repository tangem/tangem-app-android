package com.tangem.features.markets.portfolio.add.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.markets.TokenMarketParams

internal interface AddToPortfolioComponent : ComposableContentComponent {

    data class Params(
        val token: TokenMarketParams,
        val manager: AddToPortfolioManager,
    )

    interface Factory : ComponentFactory<Params, AddToPortfolioComponent>
}