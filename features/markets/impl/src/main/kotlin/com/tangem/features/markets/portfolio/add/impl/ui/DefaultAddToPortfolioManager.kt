package com.tangem.features.markets.portfolio.add.impl.ui

import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.markets.portfolio.add.api.AddToPortfolioManager
import com.tangem.features.markets.portfolio.add.api.AddToPortfolioManager.State
import com.tangem.features.markets.portfolio.add.impl.converter.AvailableToAddDataConverter
import com.tangem.features.markets.portfolio.api.MarketsPortfolioComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

internal class DefaultAddToPortfolioManager @AssistedInject constructor(
    private val availableToAddDataConverter: AvailableToAddDataConverter,
    @Assisted override val token: TokenMarketParams,
    @Assisted override val analyticsParams: MarketsPortfolioComponent.AnalyticsParams?,
    @Assisted val scope: CoroutineScope,
    dispatchers: CoroutineDispatcherProvider,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
) : AddToPortfolioManager {

    override val allAvailableNetworks: Flow<List<TokenMarketInfo.Network>>
        field = MutableSharedFlow(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    override val portfolioFetcher: PortfolioFetcher by lazy {
        portfolioFetcherFactory.create(
            mode = PortfolioFetcher.Mode.All(onlyMultiCurrency = true),
            scope = scope,
        )
    }

    override val state: StateFlow<State> =
        combine(
            flow = portfolioFetcher.data.map { it.balances }.distinctUntilChanged(),
            flow2 = allAvailableNetworks.map { it.toSet() }.distinctUntilChanged(),
        ) { balances, availableNetworks ->
            val data = availableToAddDataConverter.convert(
                balances = balances,
                availableNetworks = availableNetworks,
                marketParams = token,
            )
            if (data.availableToAdd) {
                State.AvailableToAdd(data)
            } else {
                State.NothingToAdd
            }
        }
            .flowOn(dispatchers.default)
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = State.Init,
            )

    override fun setTokenNetworks(networks: List<TokenMarketInfo.Network>) {
        allAvailableNetworks.tryEmit(networks)
    }

    @AssistedFactory
    interface Factory : AddToPortfolioManager.Factory {
        override fun create(
            scope: CoroutineScope,
            token: TokenMarketParams,
            analyticsParams: MarketsPortfolioComponent.AnalyticsParams?,
        ): DefaultAddToPortfolioManager
    }
}