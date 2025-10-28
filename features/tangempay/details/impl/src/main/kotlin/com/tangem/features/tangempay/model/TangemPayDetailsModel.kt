package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig.ShowRefreshState
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.pay.DataForReceiveFactory
import com.tangem.domain.pay.repository.CardDetailsRepository
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent
import com.tangem.features.tangempay.entity.TangemPayDetailsErrorType
import com.tangem.features.tangempay.entity.TangemPayDetailsNavigation
import com.tangem.features.tangempay.entity.TangemPayDetailsStateFactory
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.features.tangempay.model.listener.CardDetailsEvent
import com.tangem.features.tangempay.model.listener.CardDetailsEventListener
import com.tangem.features.tangempay.model.transformers.DetailsBalanceTransformer
import com.tangem.features.tangempay.model.transformers.TangemPayDetailsRefreshTransformer
import com.tangem.features.tangempay.navigation.TangemPayDetailsInnerRoute
import com.tangem.features.tangempay.utils.TangemPayErrorMessageFactory
import com.tangem.features.tangempay.utils.TangemPayTxHistoryUiActions
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.transformer.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TangemPayDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val cardDetailsRepository: CardDetailsRepository,
    private val dataForReceiveFactory: DataForReceiveFactory,
    private val uiMessageSender: UiMessageSender,
    private val cardDetailsEventListener: CardDetailsEventListener,
) : Model(), TangemPayTxHistoryUiActions {

    private val params: TangemPayDetailsContainerComponent.Params = paramsContainer.require()

    private val stateFactory = TangemPayDetailsStateFactory(
        onBack = router::pop,
        onRefresh = ::onRefreshSwipe,
        onReceive = ::onClickReceive,
        onClickChangePin = ::onClickChangePin,
        onClickFreezeCard = ::onClickFreezeCard,
    )

    val uiState: StateFlow<TangemPayDetailsUM>
        field = MutableStateFlow(stateFactory.getInitialState())

    private val refreshStateJobHolder = JobHolder()
    private val fetchBalanceJobHolder = JobHolder()

    val bottomSheetNavigation: SlotNavigation<TangemPayDetailsNavigation> = SlotNavigation()

    init {
        fetchBalance()
    }

    private fun onClickChangePin() {
        router.push(TangemPayDetailsInnerRoute.ChangePIN)
    }

    private fun onClickFreezeCard() {
        // TODO [REDACTED_JIRA]
    }

    private fun onClickReceive() {
        val depositAddress = params.config.depositAddress
        if (depositAddress == null) {
            showBottomSheetError(TangemPayDetailsErrorType.Receive)
        } else {
            dataForReceiveFactory.getDataForReceive(depositAddress = depositAddress, chainId = params.config.chainId)
                .onRight {
                    val config = TokenReceiveConfig(
                        shouldShowWarning = false,
                        cryptoCurrency = it.currency,
                        userWalletId = it.walletId,
                        showMemoDisclaimer = false,
                        receiveAddress = it.receiveAddress,
                    )
                    bottomSheetNavigation.activate(TangemPayDetailsNavigation.Receive(config))
                }
                .onLeft { showBottomSheetError(TangemPayDetailsErrorType.Receive) }
        }
    }

    private fun fetchBalance(): Job {
        return modelScope.launch {
            val result = cardDetailsRepository.getCardBalance()
            uiState.update(DetailsBalanceTransformer(result))
        }.saveIn(fetchBalanceJobHolder)
    }

    private fun onRefreshSwipe(refreshState: ShowRefreshState) {
        modelScope.launch {
            cardDetailsEventListener.send(CardDetailsEvent.Hide)
            uiState.update(TangemPayDetailsRefreshTransformer(isRefreshing = refreshState.value))
            fetchBalance().join()
            uiState.update(TangemPayDetailsRefreshTransformer(isRefreshing = false))
        }.saveIn(refreshStateJobHolder)
    }

    override fun onTransactionClick(item: TangemPayTxHistoryItem) {
        bottomSheetNavigation.activate(TangemPayDetailsNavigation.TransactionDetails(item))
    }

    private fun showBottomSheetError(type: TangemPayDetailsErrorType) {
        uiMessageSender.send(message = TangemPayErrorMessageFactory.createErrorMessage(type = type))
    }
}