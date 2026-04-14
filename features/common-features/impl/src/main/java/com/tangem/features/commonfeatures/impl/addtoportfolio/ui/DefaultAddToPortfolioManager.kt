package com.tangem.features.commonfeatures.impl.addtoportfolio.ui

import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager.AnalyticsParams
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager.Settings
import com.tangem.features.commonfeatures.impl.addtoportfolio.converter.AvailableToAddDataConverter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

internal class DefaultAddToPortfolioManager @AssistedInject constructor(
    private val availableToAddDataConverter: AvailableToAddDataConverter,
    @Assisted override val settings: Settings,
    @Assisted override val analyticsParams: AnalyticsParams,
    @Assisted val scope: CoroutineScope,
    dispatchers: CoroutineDispatcherProvider,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
) : AddToPortfolioManager {

    override val onDismiss: Channel<Unit> = Channel()
    override val onSuccessAdded: Channel<AddToPortfolioManager.Result> = Channel()

    override val portfolioFetcher: PortfolioFetcher = portfolioFetcherFactory.create(
        mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = true),
        scope = scope,
    )

    private val internalParamsFlow = MutableStateFlow(ParamsInternal())

    override val paramsFlow = internalParamsFlow
        .transform { internalParams ->
            val fullParams = AddToPortfolioManager.Params(
                networks = internalParams.networks ?: return@transform,
                token = internalParams.token ?: return@transform,
            )
            emit(fullParams)
        }
        .distinctUntilChanged()
        .shareIn(scope = scope, started = SharingStarted.Eagerly, replay = 1)

    override val state: MutableStateFlow<AddToPortfolioManager.State> =
        MutableStateFlow(AddToPortfolioManager.State.Loading)

    init {
        buildFlow()
            .onEach { newState -> state.update { newState } }
            .flowOn(dispatchers.default)
            .launchIn(scope)
    }

    override fun onDismiss() {
        onDismiss.trySend(Unit)
    }

    override fun onSuccessAdded(result: AddToPortfolioManager.Result) {
        onSuccessAdded.trySend(result)
    }

    override fun setTokenNetworks(networks: List<TokenMarketInfo.Network>) {
        updateInternal(networks = networks)
    }

    override fun setTokenParams(token: TokenMarketParams) {
        updateInternal(token = token)
    }

    private fun updateInternal(networks: List<TokenMarketInfo.Network>? = null, token: TokenMarketParams? = null) {
        internalParamsFlow.update { prev ->
            val newParams = ParamsInternal(
                networks = networks ?: prev.networks,
                token = token ?: prev.token,
            )
            val shouldReload = prev.networks != newParams.networks || prev.token != newParams.token
            if (shouldReload) state.update { AddToPortfolioManager.State.Loading }
            return@update newParams
        }
    }

    private fun buildFlow(): Flow<AddToPortfolioManager.State> = combine(
        flow = portfolioFetcher.data.map { it.balances }.distinctUntilChanged(),
        flow2 = paramsFlow,
    ) { balances, (availableNetworks, token) ->
        val data = availableToAddDataConverter.convert(
            balances = balances,
            availableNetworks = availableNetworks.toSet(),
            marketParams = token,
        )
        AddToPortfolioManager.State.Ready(data)
    }

    @AssistedFactory
    interface Factory : AddToPortfolioManager.Factory {
        override fun create(
            scope: CoroutineScope,
            settings: Settings,
            analyticsParams: AnalyticsParams,
        ): DefaultAddToPortfolioManager
    }

    private data class ParamsInternal(
        val networks: List<TokenMarketInfo.Network>? = null,
        val token: TokenMarketParams? = null,
    )
}