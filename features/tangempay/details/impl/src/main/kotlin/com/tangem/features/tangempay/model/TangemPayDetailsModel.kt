package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig.ShowRefreshState
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.pay.TangemPayTopUpData
import com.tangem.domain.pay.model.TangemPayCardBalance
import com.tangem.domain.pay.repository.CardDetailsRepository
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.components.AddFundsListener
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent
import com.tangem.features.tangempay.entity.TangemPayDetailsErrorType
import com.tangem.features.tangempay.entity.TangemPayDetailsNavigation
import com.tangem.features.tangempay.entity.TangemPayDetailsStateFactory
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.features.tangempay.model.listener.CardDetailsEvent
import com.tangem.features.tangempay.model.listener.CardDetailsEventListener
import com.tangem.features.tangempay.model.transformers.DetailsAddToWalletBannerTransformer
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
    private val uiMessageSender: UiMessageSender,
    private val cardDetailsEventListener: CardDetailsEventListener,
) : Model(), TangemPayTxHistoryUiActions, AddFundsListener {

    private val params: TangemPayDetailsContainerComponent.Params = paramsContainer.require()

    private val stateFactory = TangemPayDetailsStateFactory(
        onBack = router::pop,
        onRefresh = ::onRefreshSwipe,
        onAddFunds = ::onClickAddFunds,
        onClickChangePin = ::onClickChangePin,
        onClickFreezeCard = ::onClickFreezeCard,
    )

    val uiState: StateFlow<TangemPayDetailsUM>
        field = MutableStateFlow(stateFactory.getInitialState())

    private val refreshStateJobHolder = JobHolder()
    private val fetchBalanceJobHolder = JobHolder()
    private val addToWalletBannerJobHolder = JobHolder()

    private var balance: TangemPayCardBalance? = null

    val bottomSheetNavigation: SlotNavigation<TangemPayDetailsNavigation> = SlotNavigation()

    init {
        fetchAddToWalletBanner()
        fetchBalance()
    }

    private fun onClickChangePin() {
        router.push(TangemPayDetailsInnerRoute.ChangePIN)
    }

    private fun onClickFreezeCard() {
        // TODO [REDACTED_JIRA]
    }

    private fun onClickAddFunds() {
        if (params.config.depositAddress == null || balance == null) {
            showBottomSheetError(TangemPayDetailsErrorType.Receive)
        } else {
            bottomSheetNavigation.activate(
                TangemPayDetailsNavigation.AddFunds(
                    walletId = params.userWalletId,
                    fiatBalance = requireNotNull(balance).balance,
                    // Using the fiat balance as crypto balance. This will be changed when back returns crypto balance
                    cryptoBalance = requireNotNull(balance).balance,
                    depositAddress = requireNotNull(params.config.depositAddress),
                    chainId = params.config.chainId,
                ),
            )
        }
    }

    private fun fetchBalance(): Job {
        return modelScope.launch {
            val result = cardDetailsRepository.getCardBalance().onRight { balance = it }
            uiState.update(DetailsBalanceTransformer(balance = result))
        }.saveIn(fetchBalanceJobHolder)
    }

    private fun fetchAddToWalletBanner() {
        modelScope.launch {
            val isDone = cardDetailsRepository.isAddToWalletDone().getOrNull() ?: false
            uiState.update(
                transformer = DetailsAddToWalletBannerTransformer(
                    onClickBanner = ::onClickAddToWalletBlock,
                    onClickCloseBanner = ::onClickCloseAddToWalletBlock,
                    isDone = isDone,
                ),
            )
        }.saveIn(addToWalletBannerJobHolder)
    }

    private fun onRefreshSwipe(refreshState: ShowRefreshState) {
        modelScope.launch {
            cardDetailsEventListener.send(CardDetailsEvent.Hide)
            uiState.update(TangemPayDetailsRefreshTransformer(isRefreshing = refreshState.value))
            fetchBalance().join()
            uiState.update(TangemPayDetailsRefreshTransformer(isRefreshing = false))
        }.saveIn(refreshStateJobHolder)
    }

    private fun onClickAddToWalletBlock() {
        router.push(TangemPayDetailsInnerRoute.AddToWallet)
    }

    private fun onClickCloseAddToWalletBlock() {
        modelScope.launch {
            cardDetailsRepository.setAddToWalletAsDone()
            uiState.update(
                transformer = DetailsAddToWalletBannerTransformer(
                    onClickBanner = ::onClickAddToWalletBlock,
                    onClickCloseBanner = ::onClickCloseAddToWalletBlock,
                    isDone = true,
                ),
            )
        }.saveIn(addToWalletBannerJobHolder)
    }

    override fun onClickSwap(data: TangemPayTopUpData) {
        bottomSheetNavigation.dismiss()
        router.push(
            AppRoute.Swap(
                currencyFrom = data.currency,
                userWalletId = data.walletId,
                isInitialReverseOrder = true,
                screenSource = AnalyticsParam.ScreensSources.TangemPay.value,
                tangemPayInput = AppRoute.Swap.TangemPayInput(
                    cryptoAmount = data.cryptoBalance,
                    fiatAmount = data.fiatBalance,
                    depositAddress = data.depositAddress,
                ),
            ),
        )
    }

    override fun onClickReceive(data: TangemPayTopUpData) {
        bottomSheetNavigation.dismiss()
        val config = TokenReceiveConfig(
            shouldShowWarning = false,
            cryptoCurrency = data.currency,
            userWalletId = data.walletId,
            showMemoDisclaimer = false,
            receiveAddress = data.receiveAddress,
        )
        bottomSheetNavigation.activate(TangemPayDetailsNavigation.Receive(config))
    }

    override fun onDismissAddFunds() {
        bottomSheetNavigation.dismiss()
    }

    override fun onTransactionClick(item: TangemPayTxHistoryItem) {
        bottomSheetNavigation.activate(TangemPayDetailsNavigation.TransactionDetails(item))
    }

    private fun showBottomSheetError(type: TangemPayDetailsErrorType) {
        uiMessageSender.send(message = TangemPayErrorMessageFactory.createErrorMessage(type = type))
    }
}