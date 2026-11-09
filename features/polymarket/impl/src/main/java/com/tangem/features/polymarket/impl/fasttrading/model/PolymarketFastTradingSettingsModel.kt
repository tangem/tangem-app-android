package com.tangem.features.polymarket.impl.fasttrading.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.domain.polymarket.sessionkey.PolymarketDeviceIdProvider
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperation
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStage
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStart
import com.tangem.domain.polymarket.usecase.ObservePolymarketFastTradingUseCase
import com.tangem.domain.polymarket.usecase.RefreshPolymarketFastTradingUseCase
import com.tangem.domain.polymarket.usecase.StartPolymarketSessionKeyOperationUseCase
import com.tangem.features.polymarket.impl.fasttrading.PolymarketFastTradingSettingsComponent
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyActionUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.PolymarketFastTradingSettingsUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class PolymarketFastTradingSettingsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val router: Router,
    observeFastTrading: ObservePolymarketFastTradingUseCase,
    private val refreshFastTrading: RefreshPolymarketFastTradingUseCase,
    private val startOperation: StartPolymarketSessionKeyOperationUseCase,
    private val messageSender: UiMessageSender,
    deviceIdProvider: PolymarketDeviceIdProvider,
) : Model() {

    private val params = paramsContainer.require<PolymarketFastTradingSettingsComponent.Params>()
    private val converter = PolymarketFastTradingKeysUMConverter(thisDeviceName = deviceIdProvider.deviceName)
    private val isRefreshing = MutableStateFlow(false)
    private var revocationAwaitingSubmission: Long? = null

    val uiState: StateFlow<PolymarketFastTradingSettingsUM>
        field = MutableStateFlow<PolymarketFastTradingSettingsUM>(
            PolymarketFastTradingSettingsUM.Loading(onBackClick = ::onBackClick),
        )

    init {
        val fastTrading = observeFastTrading(params.userWalletId).onEach { announceSubmittedRevocation(it.operation) }
        combine(fastTrading, isRefreshing) { state, isRefreshing ->
            state.overview.fold(
                ifLeft = {
                    PolymarketFastTradingSettingsUM.Error(onBackClick = ::onBackClick, onRetryClick = ::refresh)
                },
                ifRight = { overview ->
                    PolymarketFastTradingSettingsUM.Content(
                        onBackClick = ::onBackClick,
                        keys = converter.convert(overview, state.operation),
                        pullToRefreshConfig = PullToRefreshConfig(isRefreshing = isRefreshing) { refresh() },
                        onActionClick = ::onActionClick,
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

    private fun onActionClick(action: FastTradingKeyActionUM) {
        val request = when (action) {
            is FastTradingKeyActionUM.Revoke -> PolymarketSessionKeyOperationRequest.Revoke(action.address)
        }
        val start = startOperation(params.userWalletId, request)
        PolymarketSessionKeyToastFactory.create(start)?.let(messageSender::send)
        if (start is PolymarketSessionKeyOperationStart.Started) {
            revocationAwaitingSubmission = start.id
        }
    }

    private fun announceSubmittedRevocation(operation: PolymarketSessionKeyOperation?) {
        val operationId = revocationAwaitingSubmission ?: return
        val isOurs = operation?.id == operationId
        if (isOurs && operation.stage == PolymarketSessionKeyOperationStage.SIGNING) return
        revocationAwaitingSubmission = null
        if (isOurs) messageSender.send(PolymarketSessionKeyToastFactory.revokingToast())
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