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
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig.ShowRefreshState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.pay.TangemPayTopUpData
import com.tangem.domain.pay.model.TangemPayCardBalance
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.TangemPayConstants
import com.tangem.features.tangempay.components.AddFundsListener
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayDetailsErrorType
import com.tangem.features.tangempay.entity.TangemPayDetailsNavigation
import com.tangem.features.tangempay.entity.TangemPayDetailsStateFactory
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.features.tangempay.model.listener.CardDetailsEvent
import com.tangem.features.tangempay.model.listener.CardDetailsEventListener
import com.tangem.features.tangempay.model.transformers.*
import com.tangem.features.tangempay.navigation.TangemPayDetailsInnerRoute
import com.tangem.features.tangempay.utils.TangemPayDetailIntents
import com.tangem.features.tangempay.utils.TangemPayMessagesFactory
import com.tangem.features.tangempay.utils.TangemPayTxHistoryUiActions
import com.tangem.features.tangempay.utils.TangemPayTxHistoryUpdateListener
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.transformer.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TangemPayDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val urlOpener: UrlOpener,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val uiMessageSender: UiMessageSender,
    private val cardDetailsEventListener: CardDetailsEventListener,
    private val txHistoryUpdateListener: TangemPayTxHistoryUpdateListener,
) : Model(), TangemPayTxHistoryUiActions, TangemPayDetailIntents, AddFundsListener {

    private val params: TangemPayDetailsContainerComponent.Params = paramsContainer.require()

    private val stateFactory = TangemPayDetailsStateFactory(
        onBack = router::pop,
        intents = this,
        isCardFrozen = params.config.isCardFrozen,
    )

    val uiState: StateFlow<TangemPayDetailsUM>
        field = MutableStateFlow(stateFactory.getInitialState())

    private val refreshStateJobHolder = JobHolder()
    private val fetchBalanceJobHolder = JobHolder()
    private val addToWalletBannerJobHolder = JobHolder()

    private var balance: TangemPayCardBalance? = null

    val bottomSheetNavigation: SlotNavigation<TangemPayDetailsNavigation> = SlotNavigation()

    init {
        handleBalanceHiding()
        fetchAddToWalletBanner()
        fetchBalance()
    }

    override fun onClickChangePin() {
        router.push(TangemPayDetailsInnerRoute.ChangePIN)
    }

    override fun onClickFreezeCard() {
        uiMessageSender.send(TangemPayMessagesFactory.createFreezeCardMessage(onFreezeClicked = ::freezeCard))
    }

    override fun onClickUnfreezeCard() {
        uiMessageSender.send(TangemPayMessagesFactory.createUnfreezeCardMessage(onUnfreezeClicked = ::unfreezeCard))
    }

    private fun freezeCard() {
        modelScope.launch {
            uiState.update(TangemPayFreezeUnfreezeProcessTransformer)
            cardDetailsRepository.freezeCard(cardId = params.config.cardId)
                .onLeft {
                    uiMessageSender.send(SnackbarMessage(resourceReference(R.string.tangem_pay_freeze_card_failed)))
                    uiState.update(
                        TangemPayFreezeUnfreezeStateTransformer(
                            frozen = false,
                            onFreezeClick = ::onClickFreezeCard,
                            onUnfreezeClick = ::onClickUnfreezeCard,
                        ),
                    )
                }
                .onRight {
                    uiMessageSender.send(SnackbarMessage(resourceReference(R.string.tangem_pay_freeze_card_success)))
                    uiState.update(
                        TangemPayFreezeUnfreezeStateTransformer(
                            frozen = true,
                            onFreezeClick = ::onClickFreezeCard,
                            onUnfreezeClick = ::onClickUnfreezeCard,
                        ),
                    )
                }
        }
    }

    private fun unfreezeCard() {
        modelScope.launch {
            uiState.update(TangemPayFreezeUnfreezeProcessTransformer)
            cardDetailsRepository.unfreezeCard(cardId = params.config.cardId)
                .onLeft {
                    uiMessageSender.send(SnackbarMessage(resourceReference(R.string.tangem_pay_unfreeze_card_failed)))
                    uiState.update(
                        TangemPayFreezeUnfreezeStateTransformer(
                            frozen = true,
                            onFreezeClick = ::onClickFreezeCard,
                            onUnfreezeClick = ::onClickUnfreezeCard,
                        ),
                    )
                }
                .onRight {
                    uiMessageSender.send(SnackbarMessage(resourceReference(R.string.tangem_pay_unfreeze_card_success)))
                    uiState.update(
                        TangemPayFreezeUnfreezeStateTransformer(
                            frozen = false,
                            onFreezeClick = ::onClickFreezeCard,
                            onUnfreezeClick = ::onClickUnfreezeCard,
                        ),
                    )
                }
        }
    }

    override fun onClickAddFunds() {
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

    private fun handleBalanceHiding() {
        getBalanceHidingSettingsUseCase().onEach {
            uiState.update(DetailBalanceVisibilityTransformer(isHidden = it.isBalanceHidden))
        }.launchIn(modelScope)
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

    override fun onRefreshSwipe(refreshState: ShowRefreshState) {
        modelScope.launch {
            uiState.update(TangemPayDetailsRefreshTransformer(isRefreshing = refreshState.value))
            cardDetailsEventListener.send(CardDetailsEvent.Hide)
            txHistoryUpdateListener.triggerUpdate()
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

    override fun onClickTermsAndLimits() {
        urlOpener.openUrl(TangemPayConstants.TERMS_AND_LIMITS_LINK)
    }

    private fun showBottomSheetError(type: TangemPayDetailsErrorType) {
        uiMessageSender.send(message = TangemPayMessagesFactory.createErrorMessage(type = type))
    }
}