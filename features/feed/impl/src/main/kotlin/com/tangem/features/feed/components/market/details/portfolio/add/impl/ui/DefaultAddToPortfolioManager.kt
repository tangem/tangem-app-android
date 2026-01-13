package com.tangem.features.feed.components.market.details.portfolio.add.impl.ui

import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.feed.components.market.details.MarketsPortfolioAnalyticsParams
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioManager
import com.tangem.features.feed.components.market.details.portfolio.add.impl.converter.AvailableToAddDataConverter
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
    @Assisted override val analyticsParams: MarketsPortfolioAnalyticsParams?,
    @Assisted val scope: CoroutineScope,
    dispatchers: CoroutineDispatcherProvider,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
) : AddToPortfolioManager {

    private val _allAvailableNetworks = MutableSharedFlow<List<TokenMarketInfo.Network>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val allAvailableNetworks: Flow<List<TokenMarketInfo.Network>> = _allAvailableNetworks.asSharedFlow()
    override val portfolioFetcher: PortfolioFetcher = portfolioFetcherFactory.create(
        mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = true),
        scope = scope,
    )

    override val state: StateFlow<AddToPortfolioManager.State> =
        combine(
            flow = portfolioFetcher.data.map { it.balances }.distinctUntilChanged(),
            flow2 = allAvailableNetworks.map { it.toSet() }.distinctUntilChanged(),
        ) { balances, availableNetworks ->
            val data = availableToAddDataConverter.convert(
                balances = balances,
                availableNetworks = availableNetworks,
                marketParams = token,
            )
            if (data.isAvailableToAdd) {
                AddToPortfolioManager.State.AvailableToAdd(data)
            } else {
                AddToPortfolioManager.State.NothingToAdd
            }
        }
            .flowOn(dispatchers.default)
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = AddToPortfolioManager.State.Init,
            )

    override fun setTokenNetworks(networks: List<TokenMarketInfo.Network>) {
        _allAvailableNetworks.tryEmit(networks)
    }

    @AssistedFactory
    interface Factory : AddToPortfolioManager.Factory {
        override fun create(
            scope: CoroutineScope,
            token: TokenMarketParams,
            analyticsParams: MarketsPortfolioAnalyticsParams?,
        ): DefaultAddToPortfolioManager
    }
}