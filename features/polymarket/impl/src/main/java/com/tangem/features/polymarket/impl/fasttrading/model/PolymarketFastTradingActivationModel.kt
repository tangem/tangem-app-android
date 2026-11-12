package com.tangem.features.polymarket.impl.fasttrading.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.wallet.isHotWallet
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStage
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStart
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationState
import com.tangem.domain.polymarket.usecase.ObservePolymarketSessionKeyOperationStateUseCase
import com.tangem.domain.polymarket.usecase.StartPolymarketSessionKeyOperationUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.polymarket.impl.fasttrading.PolymarketFastTradingActivationComponent
import com.tangem.features.polymarket.impl.fasttrading.PolymarketFastTradingActivationComponent.Action
import com.tangem.features.polymarket.impl.fasttrading.ui.state.PolymarketFastTradingActivationUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class PolymarketFastTradingActivationModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val startOperation: StartPolymarketSessionKeyOperationUseCase,
    private val messageSender: UiMessageSender,
    getUserWalletUseCase: GetUserWalletUseCase,
    observeOperationState: ObservePolymarketSessionKeyOperationStateUseCase,
) : Model() {

    private val params = paramsContainer.require<PolymarketFastTradingActivationComponent.Params>()
    private val activationId = MutableStateFlow<Long?>(null)
    private var isActivationRunningSeen = false

    val uiState: StateFlow<PolymarketFastTradingActivationUM>
        field = MutableStateFlow(
            PolymarketFastTradingActivationUM(
                buttonText = params.action.buttonText(),
                isHoldToConfirm = getUserWalletUseCase(params.userWalletId).getOrNull()?.isHotWallet == true,
                isActivating = false,
                isFaqShown = false,
                onBackClick = ::onBackClick,
                onActivateClick = ::onActivateClick,
                onFaqClick = { setFaqShown(isShown = true) },
                onFaqDismiss = { setFaqShown(isShown = false) },
            ),
        )

    init {
        combine(
            flow = activationId,
            flow2 = observeOperationState(params.userWalletId),
            transform = ::track,
        )
            .launchIn(modelScope)
        activationId
            .onEach { id -> uiState.update { it.copy(isActivating = id != null) } }
            .launchIn(modelScope)
    }

    private fun track(activationId: Long?, state: PolymarketSessionKeyOperationState) {
        activationId ?: return
        val running = state.running
        val finished = state.finished
        when {
            running?.id == activationId -> {
                isActivationRunningSeen = true
                if (running.stage != PolymarketSessionKeyOperationStage.SIGNING) close()
            }
            finished?.id == activationId -> if (finished.result.isRight()) close() else resetActivation()
            isActivationRunningSeen -> resetActivation()
        }
    }

    private fun resetActivation() {
        isActivationRunningSeen = false
        activationId.value = null
    }

    private fun onActivateClick() {
        if (activationId.value != null) return
        isActivationRunningSeen = false
        when (val start = startOperation(params.userWalletId, params.action.request)) {
            is PolymarketSessionKeyOperationStart.Started -> activationId.value = start.id
            PolymarketSessionKeyOperationStart.Busy -> PolymarketSessionKeyMessagesFactory.create(start)
                ?.let(messageSender::send)
        }
    }

    private fun setFaqShown(isShown: Boolean) {
        uiState.update { it.copy(isFaqShown = isShown) }
    }

    private fun close() {
        resetActivation()
        router.pop()
    }

    private fun onBackClick() {
        router.pop()
    }

    private fun Action.buttonText(): TextReference = when (this) {
        Action.Activate -> resourceReference(R.string.common_activate)
        is Action.Reactivate -> resourceReference(R.string.prediction_session_key_reactivate)
        Action.Extend -> resourceReference(R.string.prediction_session_key_banner_extend_button)
    }
}