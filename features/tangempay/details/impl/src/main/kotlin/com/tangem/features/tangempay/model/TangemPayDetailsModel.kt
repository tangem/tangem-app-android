package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig.ShowRefreshState
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.TangemPayTopUpData
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.pay.repository.TangemPayWithdrawRepository
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.TangemPayConstants
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.components.AddFundsListener
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent
import com.tangem.features.tangempay.entity.TangemPayDetailsErrorType
import com.tangem.features.tangempay.entity.TangemPayDetailsNavigation
import com.tangem.features.tangempay.entity.TangemPayDetailsStateFactory
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.features.tangempay.model.transformers.*
import com.tangem.features.tangempay.navigation.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.utils.*
import com.tangem.features.tokendetails.ExpressTransactionsEvent
import com.tangem.features.tokendetails.ExpressTransactionsEventListener
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass")
@Stable
@ModelScoped
internal class TangemPayDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analytics: AnalyticsEventHandler,
    private val router: Router,
    private val urlOpener: UrlOpener,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val uiMessageSender: UiMessageSender,
    private val txHistoryUpdateListener: TangemPayTxHistoryUpdateListener,
    private val tangemPayWithdrawRepository: TangemPayWithdrawRepository,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val expressTransactionsEventListener: ExpressTransactionsEventListener,
    private val tangemPayFeatureToggles: TangemPayFeatureToggles,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
) : Model(), TangemPayTxHistoryUiActions, TangemPayDetailIntents, AddFundsListener {

    private val params: TangemPayDetailsContainerComponent.Params = paramsContainer.require()

    private val isTangemPayDeactivated = params.initialStatus.isDeactivated

    private val initialCard = params.initialStatus.ifLoadedOrNull { it.cards.firstOrNull() }

    private val currentStatus = MutableStateFlow(params.initialStatus)

    private val userWalletId
        get() = currentStatus.value.userWalletId

    val cryptoCurrency
        get() = currentStatus.value.cryptoCurrency

    private val stateFactory = TangemPayDetailsStateFactory(
        onBack = router::pop,
        onOpenMenu = ::onOpenMenu,
        intents = this,
        cardFrozenState = when {
            initialCard == null -> TangemPayCardFrozenState.Unfrozen
            else -> initialCard.frozenState
        },
        isRedesignEnabled = isRedesignEnabled(),
    )

    val uiState: StateFlow<TangemPayDetailsUM>
        field = MutableStateFlow(
            stateFactory.getInitialState(
                isTangemPayDeactivated = isTangemPayDeactivated,
                cardNumberEnd = initialCard?.lastDigits.orEmpty(),
                isReissuing = initialCard == null || initialCard.state != TangemPayCardState.Active,
                isFrozen = initialCard?.frozenState == TangemPayCardFrozenState.Frozen,
            ),
        )

    private val refreshStateJobHolder = JobHolder()
    private val addToWalletBannerJobHolder = JobHolder()

    val bottomSheetNavigation: SlotNavigation<TangemPayDetailsNavigation> = SlotNavigation()

    init {
        analytics.send(TangemPayAnalyticsEvents.MainScreenOpened())
        handleBalanceHiding()

        val statusFlow = paymentAccountStatusSupplier.invoke(userWalletId)
            .onEach { status -> currentStatus.update { status } }
            .map { it.value }

        if (isTangemPayDeactivated) {
            statusFlow
                .filterIsInstance<PaymentAccountStatusValue.Deactivated>()
                .onEach { state ->
                    uiState.update(DetailsBalanceTransformer(state.balance.fiatBalance))
                }
                .launchIn(modelScope)
        } else {
            if (initialCard != null) {
                subscribeToCardFrozenState(initialCard.id)
            }
            fetchAddToWalletBanner()
            statusFlow
                .filterIsInstance<PaymentAccountStatusValue.Loaded>()
                .filter { it.source == StatusSource.ACTUAL }
                .onEach { state ->
                    uiState.update(DetailsBalanceTransformer(state.balance.fiatBalance))
                    state.cards.firstOrNull()?.let { card ->
                        uiState.update(
                            TangemPayCardDataTransformer(
                                card = card,
                                onCardClick = { onCardClick() },
                            ),
                        )
                    }
                }
                .launchIn(modelScope)
        }
    }

    fun onResume() {
        modelScope.launch {
            expressTransactionsEventListener.send(ExpressTransactionsEvent.Update)
        }
    }

    fun onPause() {
        modelScope.launch {
            expressTransactionsEventListener.send(ExpressTransactionsEvent.Clear)
        }
    }

    fun isRedesignEnabled(): Boolean = tangemPayFeatureToggles.isRedesignEnabled

    private fun subscribeToCardFrozenState(cardId: String) {
        cardDetailsRepository
            .cardFrozenState(cardId)
            .onEach { uiState.update(TangemPayFreezeUnfreezeStateTransformer(cardFrozenState = it)) }
            .launchIn(modelScope)
    }

    override fun onClickAddFunds() {
        analytics.send(TangemPayAnalyticsEvents.AddFundsClicked())
        val balance = currentStatus.value.balanceOrNull()
        if (balance == null) {
            showBottomSheetError(TangemPayDetailsErrorType.Receive)
        } else {
            bottomSheetNavigation.activate(
                TangemPayDetailsNavigation.AddFunds(
                    walletId = userWalletId,
                    fiatBalance = balance.availableForWithdrawal,
                    cryptoBalance = balance.availableForWithdrawal,
                    depositAddress = balance.cryptoBalance.depositAddress,
                    cryptoCurrency = cryptoCurrency,
                ),
            )
        }
    }

    override fun onClickWithdraw() {
        analytics.send(TangemPayAnalyticsEvents.WithdrawClicked())
        modelScope.launch {
            val hasActiveWithdrawal = tangemPayWithdrawRepository.hasWithdrawOrder(userWalletId)
            if (hasActiveWithdrawal) {
                showBottomSheetError(TangemPayDetailsErrorType.WithdrawInProgress)
            } else {
                uiMessageSender.send(
                    message = TangemPayMessagesFactory.createWithdrawWarning(
                        onGotItClick = { onConfirmWithdrawal(cryptoCurrency) },
                    ),
                )
            }
        }
    }

    private fun onConfirmWithdrawal(currency: CryptoCurrency) {
        val balance = currentStatus.value.balanceOrNull()
        if (balance == null) {
            showBottomSheetError(TangemPayDetailsErrorType.Withdraw)
            return
        }
        router.push(
            AppRoute.Swap(
                cryptoCurrency = currency,
                userWalletId = userWalletId,
                screenSource = AnalyticsParam.ScreensSources.TangemPay.value,
                currencyPosition = AppRoute.Swap.CurrencyPosition.FROM,
                tangemPayInput = AppRoute.Swap.TangemPayInput(
                    cryptoAmount = balance.availableForWithdrawal,
                    fiatAmount = balance.availableForWithdrawal,
                    depositAddress = balance.cryptoBalance.depositAddress,
                    isWithdrawal = true,
                ),
            ),
        )
    }

    private fun fetchAddToWalletBanner() {
        modelScope.launch {
            val isDone = try {
                cardDetailsRepository.isAddToWalletDone(userWalletId).getOrNull() == true
            } catch (e: Exception) {
                TangemLogger.e("Error", e)
                return@launch
            }
            uiState.update(
                transformer = DetailsAddToWalletBannerTransformer(
                    onClickBanner = ::onClickAddToWalletBlock,
                    onClickCloseBanner = ::onClickCloseAddToWalletBlock,
                    isDone = isDone,
                ),
            )
        }.saveIn(addToWalletBannerJobHolder)
    }

    private fun handleBalanceHiding() {
        getBalanceHidingSettingsUseCase().onEach {
            uiState.update(DetailBalanceVisibilityTransformer(isHidden = it.isBalanceHidden))
        }.launchIn(modelScope)
    }

    override fun onContactSupportClicked() {
        analytics.send(Basic.ButtonSupport(source = AnalyticsParam.ScreensSources.TangemPay))
        val customerId = currentStatus.value.ifLoadedOrNull { it.customerId } ?: return
        modelScope.launch {
            sendFeedbackEmailUseCase.invoke(
                type = FeedbackEmailType.Visa.FeatureIsBeta(
                    walletMetaInfo = WalletMetaInfo(userWalletId = userWalletId),
                    customerId = customerId,
                ),
            )
        }
    }

    override fun onRefreshSwipe(refreshState: ShowRefreshState) {
        modelScope.launch {
            uiState.update(TangemPayDetailsRefreshTransformer(isRefreshing = refreshState.value))
            paymentAccountStatusFetcher.invoke(userWalletId)
            expressTransactionsEventListener.send(ExpressTransactionsEvent.Update)
            txHistoryUpdateListener.triggerUpdate()
            uiState.update(TangemPayDetailsRefreshTransformer(isRefreshing = false))
        }.saveIn(refreshStateJobHolder)
    }

    private fun onClickAddToWalletBlock() {
        analytics.send(TangemPayAnalyticsEvents.AddToWalletClicked())
        router.push(TangemPayAccountDetailsInnerRoute.AddToWallet)
    }

    private fun onClickCloseAddToWalletBlock() {
        modelScope.launch {
            try {
                cardDetailsRepository.setAddToWalletAsDone(userWalletId)
            } catch (e: Exception) {
                TangemLogger.e("Error", e)
            }
            uiState.update(
                transformer = DetailsAddToWalletBannerTransformer(
                    onClickBanner = ::onClickAddToWalletBlock,
                    onClickCloseBanner = ::onClickCloseAddToWalletBlock,
                    isDone = true,
                ),
            )
        }.saveIn(addToWalletBannerJobHolder)
    }

    private fun onOpenMenu() {
        analytics.send(TangemPayAnalyticsEvents.CardSettingsClicked())
    }

    override fun onClickSwap(data: TangemPayTopUpData) {
        analytics.send(TangemPayAnalyticsEvents.SwapClicked())
        bottomSheetNavigation.dismiss()
        router.push(
            AppRoute.Swap(
                cryptoCurrency = data.currency,
                userWalletId = data.walletId,
                screenSource = AnalyticsParam.ScreensSources.TangemPay.value,
                currencyPosition = AppRoute.Swap.CurrencyPosition.TO,
                tangemPayInput = AppRoute.Swap.TangemPayInput(
                    cryptoAmount = data.cryptoBalance,
                    fiatAmount = data.fiatBalance,
                    depositAddress = data.depositAddress,
                    isWithdrawal = false,
                ),
            ),
        )
    }

    override fun onClickReceive(data: TangemPayTopUpData) {
        analytics.send(TangemPayAnalyticsEvents.ReceiveFundsClicked())
        bottomSheetNavigation.dismiss()
        val config = TokenReceiveConfig(
            shouldShowWarning = true,
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
        val (type, status) = when (item) {
            is TangemPayTxHistoryItem.Collateral -> "collateral" to "unknown"
            is TangemPayTxHistoryItem.Fee -> "fee" to "unknown"
            is TangemPayTxHistoryItem.Payment -> "payment" to "unknown"
            is TangemPayTxHistoryItem.Spend -> "spend" to item.status.name.lowercase()
        }
        analytics.send(TangemPayAnalyticsEvents.TransactionInListClicked(type = type, status = status))
        bottomSheetNavigation.activate(
            configuration = TangemPayDetailsNavigation.TransactionDetails(
                transaction = item,
                isBalanceHidden = uiState.value.isBalanceHidden,
            ),
        )
    }

    override fun onClickTermsAndLimits() {
        analytics.send(TangemPayAnalyticsEvents.TermsAndLimitsClicked())
        urlOpener.openUrl(TangemPayConstants.TERMS_AND_LIMITS_LINK)
    }

    override fun onCardClick() {
        analytics.send(TangemPayAnalyticsEvents.CardIconClicked())
        router.push(TangemPayAccountDetailsInnerRoute.CardDetails)
    }

    override fun onAddCardClick() {
        analytics.send(TangemPayAnalyticsEvents.AddExtraCardClicked())
        analytics.send(TangemPayAnalyticsEvents.FakeDoorPopupDisplayed())
        uiMessageSender.send(
            message = TangemPayMessagesFactory.createFutureFeature(
                onGotItClick = { analytics.send(TangemPayAnalyticsEvents.FakeDoorGotitClicked()) },
            ),
        )
    }

    private fun showBottomSheetError(type: TangemPayDetailsErrorType) {
        uiMessageSender.send(message = TangemPayMessagesFactory.createErrorMessage(errorType = type))
    }
}