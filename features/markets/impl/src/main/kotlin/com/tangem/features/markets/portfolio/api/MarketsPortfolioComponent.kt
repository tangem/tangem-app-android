package com.tangem.features.markets.portfolio.api

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import kotlinx.serialization.Serializable

@Stable
interface MarketsPortfolioComponent : ComposableContentComponent {

    @Serializable
    data class Params(
        val token: TokenMarketParams,
        val analyticsParams: AnalyticsParams?,
    )

    @Serializable
    data class AnalyticsParams(
        val source: String,
    )

    fun setTokenNetworks(networks: List<TokenMarketInfo.Network>)

    fun setNoNetworksAvailable()

    interface Factory : ComponentFactory<Params, MarketsPortfolioComponent>
}