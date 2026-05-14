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
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.TangemPayCryptoCurrencyFactory
import com.tangem.domain.pay.model.TangemPayCardBalance
import com.tangem.domain.pay.model.TangemPayTopUpData
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.pay.repository.TangemPayWithdrawRepository
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.tangempay.TangemPayConstants
import com.tangem.features.tangempay.components.AddFundsListener
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent
import com.tangem.features.tangempay.entity.TangemPayDetailsErrorType
import com.tangem.features.tangempay.entity.TangemPayDetailsNavigation
import com.tangem.features.tangempay.entity.TangemPayDetailsStateFactory
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.features.tangempay.model.listener.CardDetailsEvent
import com.tangem.features.tangempay.model.listener.CardDetailsEventListener
import com.tangem.features.tangempay.model.transformers.*
import com.tangem.features.tangempay.navigation.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.utils.TangemPayDetailIntents
import com.tangem.features.tangempay.utils.TangemPayMessagesFactory
import com.tangem.features.tangempay.utils.TangemPayTxHistoryUiActions
import com.tangem.features.tangempay.utils.TangemPayTxHistoryUpdateListener
import com.tangem.features.tokendetails.ExpressTransactionsEvent
import com.tangem.features.tokendetails.ExpressTransactionsEventListener
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.transformer.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass")
@Stable
@ModelScoped
internal class TangemPayDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analytics: AnalyticsEventHandler,
    private val router: Router,
    private val urlOpener: UrlOpener,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val uiMessageSender: UiMessageSender,
    private val cardDetailsEventListener: CardDetailsEventListener,
    private val txHistoryUpdateListener: TangemPayTxHistoryUpdateListener,
    private val tangemPayCryptoCurrencyFactory: TangemPayCryptoCurrencyFactory,
    private val tangemPayWithdrawRepository: TangemPayWithdrawRepository,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val expressTransactionsEventListener: ExpressTransactionsEventListener,
) : Model(), TangemPayTxHistoryUiActions, TangemPayDetailIntents, AddFundsListener {

    private val params: TangemPayDetailsContainerComponent.Params = paramsContainer.require()

    private val stateFactory = TangemPayDetailsStateFactory(
        onBack = router::pop,
        onOpenMenu = ::onOpenMenu,
        intents = this,
        cardFrozenState = params.config.cardFrozenState,
    )

    val uiState: StateFlow<TangemPayDetailsUM>
        field = MutableStateFlow(
            stateFactory.getInitialState(
                isTangemPayDeactivated = params.config.isTangemPayDeactivated,
                cardNumberEnd = params.config.cardNumberEnd,
            ),
        )

    private val refreshStateJobHolder = JobHolder()
    private val fetchBalanceJobHolder = JobHolder()
    private val addToWalletBannerJobHolder = JobHolder()

    private var balance: TangemPayCardBalance? = null

    private val userWallet: UserWallet? = getUserWalletUseCase(params.userWalletId).getOrNull()
    val cryptoCurrency: CryptoCurrency? = userWallet?.let { wallet ->
        tangemPayCryptoCurrencyFactory.create(userWallet = wallet, chainId = params.config.chainId).getOrNull()
    }

    val bottomSheetNavigation: SlotNavigation<TangemPayDetailsNavigation> = SlotNavigation()

    init {
        analytics.send(TangemPayAnalyticsEvents.MainScreenOpened())
        handleBalanceHiding()
        fetchBalance()
        if (!params.config.isTangemPayDeactivated) {
            subscribeToCardFrozenState()
            fetchAddToWalletBanner()
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

    private fun subscribeToCardFrozenState() {
        cardDetailsRepository
            .cardFrozenState(params.config.cardId)
            .onEach { uiState.update(TangemPayFreezeUnfreezeStateTransformer(cardFrozenState = it)) }
            .launchIn(modelScope)
    }

    override fun onClickAddFunds() {
        analytics.send(TangemPayAnalyticsEvents.AddFundsClicked())
        val currentBalance = balance
        val depositAddress = currentBalance?.depositAddress
        if (currentBalance == null || depositAddress == null) {
            showBottomSheetError(TangemPayDetailsErrorType.Receive)
        } else {
            bottomSheetNavigation.activate(
                TangemPayDetailsNavigation.AddFunds(
                    walletId = params.userWalletId,
                    fiatBalance = currentBalance.availableForWithdrawal,
                    cryptoBalance = currentBalance.availableForWithdrawal,
                    depositAddress = depositAddress,
                    chainId = params.config.chainId,
                ),
            )
        }
    }

    override fun onClickWithdraw() {
        analytics.send(TangemPayAnalyticsEvents.WithdrawClicked())
        val currentBalance = balance
        val depositAddress = currentBalance?.depositAddress
        if (currentBalance == null || depositAddress == null) {
            showBottomSheetError(TangemPayDetailsErrorType.Withdraw)
        } else {
            val userWallet = userWallet ?: getUserWalletUseCase(params.userWalletId).getOrNull()
            if (userWallet == null) {
                showBottomSheetError(TangemPayDetailsErrorType.Withdraw)
            } else {
                modelScope.launch {
                    val hasActiveWithdrawal = tangemPayWithdrawRepository.hasWithdrawOrder(userWallet = userWallet)
                    if (hasActiveWithdrawal) {
                        showBottomSheetError(TangemPayDetailsErrorType.WithdrawInProgress)
                    } else {
                        val currency = cryptoCurrency ?: tangemPayCryptoCurrencyFactory.create(
                            userWallet = userWallet,
                            chainId = params.config.chainId,
                        ).getOrNull()
                        if (currency != null) {
                            uiMessageSender.send(
                                message = TangemPayMessagesFactory.createWithdrawWarning(
                                    onGotItClick = { onConfirmWithdrawal(currency, currentBalance, depositAddress) },
                                ),
                            )
                        } else {
                            showBottomSheetError(TangemPayDetailsErrorType.Withdraw)
                        }
                    }
                }
            }
        }
    }

    private fun onConfirmWithdrawal(
        currency: CryptoCurrency,
        currentBalance: TangemPayCardBalance,
        depositAddress: String,
    ) {
        router.push(
            AppRoute.Swap(
                cryptoCurrency = currency,
                userWalletId = params.userWalletId,
                screenSource = AnalyticsParam.ScreensSources.TangemPay.value,
                currencyPosition = AppRoute.Swap.CurrencyPosition.FROM,
                tangemPayInput = AppRoute.Swap.TangemPayInput(
                    cryptoAmount = currentBalance.availableForWithdrawal,
                    fiatAmount = currentBalance.availableForWithdrawal,
                    depositAddress = depositAddress,
                    isWithdrawal = true,
                ),
            ),
        )
    }

    private fun fetchBalance(): Job {
        return modelScope.launch {
            val result = try {
                cardDetailsRepository.getCardBalance(params.userWalletId).onRight { balance = it }
            } catch (e: Exception) {
                TangemLogger.e("Error", e)
                return@launch
            }
            uiState.update(
                transformer = DetailsBalanceTransformer(
                    balance = result,
                    userWallet = getUserWalletUseCase(params.userWalletId).getOrNull(),
                    cryptoCurrencyFactory = tangemPayCryptoCurrencyFactory,
                ),
            )
        }.saveIn(fetchBalanceJobHolder)
    }

    private fun fetchAddToWalletBanner() {
        modelScope.launch {
            val isDone = try {
                cardDetailsRepository.isAddToWalletDone(params.userWalletId).getOrNull() == true
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
        modelScope.launch {
            sendFeedbackEmailUseCase.invoke(
                type = FeedbackEmailType.Visa.FeatureIsBeta(
                    walletMetaInfo = WalletMetaInfo(userWalletId = params.userWalletId),
                    customerId = params.config.customerId,
                ),
            )
        }
    }

    override fun onRefreshSwipe(refreshState: ShowRefreshState) {
        modelScope.launch {
            uiState.update(TangemPayDetailsRefreshTransformer(isRefreshing = refreshState.value))
            cardDetailsEventListener.send(CardDetailsEvent.Hide)
            expressTransactionsEventListener.send(ExpressTransactionsEvent.Update)
            txHistoryUpdateListener.triggerUpdate()
            fetchBalance().join()
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
                cardDetailsRepository.setAddToWalletAsDone(params.userWalletId)
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