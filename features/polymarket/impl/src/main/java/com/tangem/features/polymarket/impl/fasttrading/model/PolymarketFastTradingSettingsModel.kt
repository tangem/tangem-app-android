package com.tangem.features.polymarket.impl.fasttrading.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.domain.polymarket.sessionkey.PolymarketDeviceIdProvider
import com.tangem.domain.polymarket.usecase.ObservePolymarketFastTradingUseCase
import com.tangem.domain.polymarket.usecase.RefreshPolymarketFastTradingUseCase
import com.tangem.features.polymarket.impl.fasttrading.PolymarketFastTradingSettingsComponent
import com.tangem.features.polymarket.impl.fasttrading.ui.state.PolymarketFastTradingSettingsUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class PolymarketFastTradingSettingsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val router: Router,
    observeFastTrading: ObservePolymarketFastTradingUseCase,
    private val refreshFastTrading: RefreshPolymarketFastTradingUseCase,
    deviceIdProvider: PolymarketDeviceIdProvider,
) : Model() {

    private val params = paramsContainer.require<PolymarketFastTradingSettingsComponent.Params>()
    private val converter = PolymarketFastTradingKeysUMConverter(thisDeviceName = deviceIdProvider.deviceName)
    private val isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<PolymarketFastTradingSettingsUM>
        field = MutableStateFlow<PolymarketFastTradingSettingsUM>(
            PolymarketFastTradingSettingsUM.Loading(onBackClick = ::onBackClick),
        )

    init {
        combine(observeFastTrading(params.userWalletId), isRefreshing) { state, isRefreshing ->
            state.overview.fold(
                ifLeft = {
                    PolymarketFastTradingSettingsUM.Error(onBackClick = ::onBackClick, onRetryClick = ::refresh)
                },
                ifRight = { overview ->
                    PolymarketFastTradingSettingsUM.Content(
                        onBackClick = ::onBackClick,
                        keys = converter.convert(overview, state.operation),
                        pullToRefreshConfig = PullToRefreshConfig(isRefreshing = isRefreshing) { refresh() },
                    )
                },
            )
        }
            .onEach { uiState.value = it }
            .launchIn(modelScope)
    }

    private fun onBackClick() {
        router.pop()
    }

    private fun refresh() {
        if (isRefreshing.value) return
        isRefreshing.value = true
        modelScope.launch {
            try {
                refreshFastTrading(params.userWalletId)
            } finally {
                isRefreshing.value = false
            }
        }
    }
}