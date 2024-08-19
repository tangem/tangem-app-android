package com.tangem.features.markets.portfolio.api

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import kotlinx.serialization.Serializable

@Stable
interface MarketsPortfolioComponent : ComposableContentComponent {

    @Serializable
    data class Params(
        val tokenId: String,
    )

    interface Factory : ComponentFactory<Params, MarketsPortfolioComponent>
}
