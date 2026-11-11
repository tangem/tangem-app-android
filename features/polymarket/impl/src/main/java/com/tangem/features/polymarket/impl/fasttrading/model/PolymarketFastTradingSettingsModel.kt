package com.tangem.features.polymarket.impl.fasttrading.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.modalMessage
import com.tangem.domain.polymarket.sessionkey.PolymarketDeviceIdProvider
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest
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
        when (action) {
            is FastTradingKeyActionUM.Revoke -> confirmRevocation(action)
        }
    }

    private fun confirmRevocation(action: FastTradingKeyActionUM.Revoke) {
        val description = when (val subject = action.subject) {
            is FastTradingKeyActionUM.RevokeSubject.Device -> resourceReference(
                id = R.string.prediction_session_key_revoke_confirm_text,
                formatArgs = wrappedList(subject.name),
            )
            FastTradingKeyActionUM.RevokeSubject.UnknownSigner -> resourceReference(
                id = R.string.prediction_session_key_revoke_confirm_unknown_signer_text,
                formatArgs = wrappedList(action.address),
            )
        }
        messageSender.send(
            modalMessage {
                status {
                    variant = TangemModalStatus.Variant.Warning
                    title = resourceReference(R.string.prediction_session_key_revoke_confirm_title)
                    this.description = description
                }
                primaryButton {
                    text = resourceReference(R.string.prediction_session_key_revoke)
                    onClick {
                        dismiss()
                        if (isRevokeOffered(action.address)) {
                            start(PolymarketSessionKeyOperationRequest.Revoke(action.address))
                        }
                    }
                }
                secondaryButton {
                    text = resourceReference(R.string.common_cancel)
                }
            },
        )
    }

    private fun isRevokeOffered(address: String): Boolean {
        val keys = (uiState.value as? PolymarketFastTradingSettingsUM.Content)?.keys ?: return false
        if (!keys.areActionsEnabled) return false
        val rows = listOfNotNull(keys.thisDevice) + keys.otherDevices
        val actions = rows.flatMap { it.actions } + keys.unknownSigners.flatMap { it.actions }
        return actions.any { it is FastTradingKeyActionUM.Revoke && it.address.equals(address, ignoreCase = true) }
    }

    private fun start(request: PolymarketSessionKeyOperationRequest) {
        val start = startOperation(params.userWalletId, request)
        PolymarketSessionKeyMessagesFactory.create(start)?.let(messageSender::send)
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