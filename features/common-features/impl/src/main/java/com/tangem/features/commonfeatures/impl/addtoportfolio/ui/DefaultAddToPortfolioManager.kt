package com.tangem.features.commonfeatures.impl.addtoportfolio.ui

import com.tangem.domain.markets.RawMarketToken
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager.*
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
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
    override val onSuccessAdded: Channel<Result> = Channel()
    override val onAddedTokenClick: Channel<Result> = Channel()

    override val portfolioFetcher: PortfolioFetcher = portfolioFetcherFactory.create(
        mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = true),
        scope = scope,
    )

    private val internalParamsFlow = MutableStateFlow(ParamsInternal())

    override val paramsFlow = internalParamsFlow
        .transform { internalParams ->
            val fullParams = Params(
                networks = internalParams.networks ?: return@transform,
                token = internalParams.token ?: return@transform,
                launchMode = internalParams.launchMode,
            )
            emit(fullParams)
        }
        .distinctUntilChanged()
        .shareIn(scope = scope, started = SharingStarted.Eagerly, replay = 1)

    override val state: MutableStateFlow<State> = MutableStateFlow(State.Loading)

    init {
        buildFlow()
            .onEach { newState -> state.update { newState } }
            .flowOn(dispatchers.default)
            .launchIn(scope)
    }

    override fun onDismiss() {
        onDismiss.trySend(Unit)
    }

    override fun onSuccessAdded(result: Result) {
        onSuccessAdded.trySend(result)
    }

    override fun onAddedTokenClick(result: Result) {
        onAddedTokenClick.trySend(result)
    }

    override fun setTokenNetworks(networks: List<TokenMarketInfo.Network>) {
        updateInternal(networks = networks)
    }

    override fun setTokenParams(token: RawMarketToken) {
        updateInternal(token = token)
    }

    override fun updateLaunchMode(launchMode: LaunchMode) {
        updateInternal(launchMode = launchMode)
    }

    private fun updateInternal(
        networks: List<TokenMarketInfo.Network>? = null,
        token: RawMarketToken? = null,
        launchMode: LaunchMode? = null,
    ) {
        internalParamsFlow.update { prev ->
            val newParams = ParamsInternal(
                networks = networks ?: prev.networks,
                token = token ?: prev.token,
                launchMode = launchMode ?: prev.launchMode,
            )
            val shouldReload = newParams != prev
            if (shouldReload) state.update { State.Loading }
            return@update newParams
        }
    }

    private fun buildFlow(): Flow<State> = combine(
        flow = portfolioFetcher.data.map { it.balances }.distinctUntilChanged(),
        flow2 = paramsFlow,
    ) { balances, (availableNetworks, token) ->
        val data = availableToAddDataConverter.convert(
            balances = balances,
            availableNetworks = availableNetworks.toSet(),
            marketParams = token,
        )
        State.Ready(data)
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
        val launchMode: LaunchMode = LaunchMode.DirectAdd,
        val networks: List<TokenMarketInfo.Network>? = null,
        val token: RawMarketToken? = null,
    )
}