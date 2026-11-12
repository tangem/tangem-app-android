package com.tangem.features.polymarket.impl.fasttrading.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.wallet.isHotWallet
import com.tangem.domain.polymarket.sessionkey.PolymarketDeviceIdProvider
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest
import com.tangem.domain.polymarket.usecase.ObservePolymarketFastTradingUseCase
import com.tangem.domain.polymarket.usecase.RefreshPolymarketFastTradingUseCase
import com.tangem.domain.polymarket.usecase.StartPolymarketSessionKeyOperationUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.polymarket.impl.fasttrading.PolymarketFastTradingSettingsComponent
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyActionUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingRevokeConfirmationUM
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
    private val getUserWallet: GetUserWalletUseCase,
) : Model() {

    private val params = paramsContainer.require<PolymarketFastTradingSettingsComponent.Params>()
    private val converter = PolymarketFastTradingKeysUMConverter(thisDeviceName = deviceIdProvider.deviceName)
    private val isRefreshing = MutableStateFlow(false)
    private val revokeConfirmation = MutableStateFlow<FastTradingRevokeConfirmationUM?>(null)

    val uiState: StateFlow<PolymarketFastTradingSettingsUM>
        field = MutableStateFlow<PolymarketFastTradingSettingsUM>(
            PolymarketFastTradingSettingsUM.Loading(onBackClick = ::onBackClick),
        )

    init {
        combine(
            observeFastTrading(params.userWalletId),
            isRefreshing,
            revokeConfirmation,
        ) { state, isRefreshing, revokeConfirmation ->
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
                        onActivateClick = ::onActivateClick,
                        revokeConfirmation = revokeConfirmation,
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
            FastTradingKeyActionUM.Extend -> startOnThisDevice(action, PolymarketSessionKeyOperationRequest.Extend)
            FastTradingKeyActionUM.Reactivate -> {
                startOnThisDevice(action, PolymarketSessionKeyOperationRequest.Reactivate)
            }
            is FastTradingKeyActionUM.Revoke -> onRevokeClick(action)
        }
    }

    private fun startOnThisDevice(action: FastTradingKeyActionUM, request: PolymarketSessionKeyOperationRequest) {
        val keys = (uiState.value as? PolymarketFastTradingSettingsUM.Content)?.keys ?: return
        val isOffered = keys.areActionsEnabled && keys.thisDevice.any { action in it.actions }
        if (isOffered) start(request)
    }

    private fun onActivateClick() {
        if (isActivationOffered()) start(PolymarketSessionKeyOperationRequest.Activate)
    }

    private fun isActivationOffered(): Boolean {
        val content = uiState.value as? PolymarketFastTradingSettingsUM.Content ?: return false
        return content.keys.activateBanner?.isEnabled == true
    }

    private fun onRevokeClick(action: FastTradingKeyActionUM.Revoke) {
        val isHotWallet = getUserWallet(params.userWalletId).getOrNull()?.isHotWallet ?: return
        val isConfirmedByCardScan = !isHotWallet && action.subject is FastTradingKeyActionUM.RevokeSubject.Device
        if (isConfirmedByCardScan) {
            revoke(action.address)
        } else {
            confirmRevocation(action, isHoldToConfirm = isHotWallet)
        }
    }

    private fun confirmRevocation(action: FastTradingKeyActionUM.Revoke, isHoldToConfirm: Boolean) {
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
        revokeConfirmation.value = FastTradingRevokeConfirmationUM(
            description = description,
            isHoldToConfirm = isHoldToConfirm,
            onConfirm = {
                revokeConfirmation.value = null
                revoke(action.address)
            },
            onDismiss = { revokeConfirmation.value = null },
        )
    }

    private fun revoke(address: String) {
        if (isRevokeOffered(address)) start(PolymarketSessionKeyOperationRequest.Revoke(address))
    }

    private fun isRevokeOffered(address: String): Boolean {
        val keys = (uiState.value as? PolymarketFastTradingSettingsUM.Content)?.keys ?: return false
        if (!keys.areActionsEnabled) return false
        val rows = keys.thisDevice + keys.otherDevices
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