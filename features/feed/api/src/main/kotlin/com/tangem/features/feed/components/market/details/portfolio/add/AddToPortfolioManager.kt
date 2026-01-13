package com.tangem.features.feed.components.market.details.portfolio.add

import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.feed.components.market.details.MarketsPortfolioAnalyticsParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AddToPortfolioManager {

    val token: TokenMarketParams
    val analyticsParams: MarketsPortfolioAnalyticsParams?
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
            analyticsParams: MarketsPortfolioAnalyticsParams?,
        ): AddToPortfolioManager
    }
}