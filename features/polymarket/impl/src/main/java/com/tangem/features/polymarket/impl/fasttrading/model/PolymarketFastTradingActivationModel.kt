package com.tangem.features.polymarket.impl.fasttrading.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperation
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationOutcome
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStage
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStart
import com.tangem.domain.polymarket.usecase.ObservePolymarketSessionKeyOperationUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketSessionKeyOutcomeUseCase
import com.tangem.domain.polymarket.usecase.StartPolymarketSessionKeyOperationUseCase
import com.tangem.features.polymarket.impl.fasttrading.PolymarketFastTradingActivationComponent
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
    observeOperation: ObservePolymarketSessionKeyOperationUseCase,
    observeOutcome: ObservePolymarketSessionKeyOutcomeUseCase,
) : Model() {

    private val params = paramsContainer.require<PolymarketFastTradingActivationComponent.Params>()
    private val activationId = MutableStateFlow<Long?>(null)
    private var isActivationRunningSeen = false

    val uiState: StateFlow<PolymarketFastTradingActivationUM>
        field = MutableStateFlow(
            PolymarketFastTradingActivationUM(
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
            flow2 = observeOperation(params.userWalletId),
            flow3 = observeOutcome(params.userWalletId),
            transform = ::track,
        )
            .launchIn(modelScope)
        activationId
            .onEach { id -> uiState.update { it.copy(isActivating = id != null) } }
            .launchIn(modelScope)
    }

    private fun track(
        activationId: Long?,
        running: PolymarketSessionKeyOperation?,
        finished: PolymarketSessionKeyOperationOutcome?,
    ) {
        activationId ?: return
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
        when (val start = startOperation(params.userWalletId, PolymarketSessionKeyOperationRequest.Activate)) {
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
}