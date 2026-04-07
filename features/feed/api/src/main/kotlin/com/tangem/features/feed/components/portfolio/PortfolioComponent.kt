package com.tangem.features.feed.components.portfolio

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency

interface PortfolioComponent : ComposableContentComponent {

    data class Params(
        val id: CryptoCurrency.ID,
    )

    interface Factory : ComponentFactory<Params, PortfolioComponent>
}