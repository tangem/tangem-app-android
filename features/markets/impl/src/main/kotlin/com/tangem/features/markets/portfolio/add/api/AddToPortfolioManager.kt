package com.tangem.features.markets.portfolio.add.api

import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.markets.portfolio.impl.loader.PortfolioData
import kotlinx.coroutines.flow.Flow

internal interface AddToPortfolioManager {

    val availableNetworks: Flow<List<TokenMarketInfo.Network>>

    fun updatePortfolioData(portfolioData: PortfolioData)
    fun updateAvailableNetworks(networks: List<TokenMarketInfo.Network>)
}