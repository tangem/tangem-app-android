package com.tangem.features.feed.components.market.details.portfolio.add

import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.account.PortfolioFetcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

interface AddToPortfolioManager {

    val token: TokenMarketParams
    val analyticsParams: AnalyticsParams?
    val portfolioFetcher: PortfolioFetcher

    val state: StateFlow<State>

    val allAvailableNetworks: Flow<List<TokenMarketInfo.Network>>
    fun setTokenNetworks(networks: List<TokenMarketInfo.Network>)

    sealed interface State {
        data object Init : State
        data class AvailableToAdd(
            val availableToAddData: AvailableToAddData,
        ) : State

        data object NothingToAdd : State
    }

    @Serializable
    data class AnalyticsParams(
        val source: String,
    )

    interface Factory {
        fun create(
            scope: CoroutineScope,
            token: TokenMarketParams,
            analyticsParams: AnalyticsParams?,
        ): AddToPortfolioManager
    }
}