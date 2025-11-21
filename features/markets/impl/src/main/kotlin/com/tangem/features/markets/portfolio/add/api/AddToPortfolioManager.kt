package com.tangem.features.markets.portfolio.add.api

import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.markets.portfolio.api.MarketsPortfolioComponent.AnalyticsParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal interface AddToPortfolioManager {

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

    interface Factory {
        fun create(
            scope: CoroutineScope,
            token: TokenMarketParams,
            analyticsParams: AnalyticsParams?,
        ): AddToPortfolioManager
    }
}