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
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.account.*
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.TangemPayTopUpData
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.pay.repository.TangemPayWithdrawRepository
import com.tangem.domain.pay.usecase.*
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.TangemPayConstants
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.cashback.impl.model.TangemPayCashbackDateFormatter
import com.tangem.features.tangempay.components.AddFundsListener
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent
import com.tangem.features.tangempay.components.TangemPayIssueAdditionalCardComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.*
import com.tangem.features.tangempay.model.transformers.*
import com.tangem.features.tangempay.navigation.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.tiers.select.TangemPaySelectPlanSource
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
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass", "TooManyFunctions")
@Stable
@ModelScoped
internal class TangemPayDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
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
    private val produceTangemPayInitialDataUseCase: ProduceTangemPayInitialDataUseCase,
    private val onboardingRepository: OnboardingRepository,
    private val getCustomerOffers: GetCustomerOffersUseCase,
    private val cancelTariffTransitionUseCase: CancelTariffTransitionUseCase,
    private val getCashbackSummaryUseCase: GetCashbackSummaryUseCase,
    private val getCashbackDeactivationDismissedUseCase: GetCashbackDeactivationDismissedUseCase,
    private val setCashbackDeactivationDismissedUseCase: SetCashbackDeactivationDismissedUseCase,
    tangemPayCurrencyFactory: TangemPayCurrencyFactory,
) : Model(),
    TangemPayTxHistoryUiActions,
    TangemPayDetailIntents,
    AddFundsListener,
    TangemPayIssueAdditionalCardComponent.Listener {

    private val params: TangemPayDetailsContainerComponent.Params = paramsContainer.require()

    private val currentStatus = MutableStateFlow(params.initialStatus)

    private val userWalletId
        get() = currentStatus.value.userWalletId

    val cryptoCurrency: CryptoCurrency.Token = tangemPayCurrencyFactory.create(userWalletId)

    private val stateFactory = TangemPayDetailsStateFactory(
        onBack = router::pop,
        onOpenMenu = ::onOpenMenu,
        intents = this,
        isRemoveAccountEnabled = tangemPayFeatureToggles.isRemoveAccountEnabled,
        isTiersPlusPlanEnabled = tangemPayFeatureToggles.isTiersPlusPlanEnabled,
    )

    val uiState: StateFlow<TangemPayDetailsUM>
        field = MutableStateFlow(
            when {
                params.initialStatus.value is PaymentAccountStatusValue.Deactivated -> {
                    stateFactory.getDeactivatedState(
                        params.initialStatus.value as PaymentAccountStatusValue.Deactivated,
                    )
                }
                else -> stateFactory.getLoadingState()
            },
        )

    private val refreshStateJobHolder = JobHolder()
    private val addToWalletBannerJobHolder = JobHolder()
    private val cashbackBlockJobHolder = JobHolder()
    private val cashbackDateFormatter = TangemPayCashbackDateFormatter()

    val bottomSheetNavigation: SlotNavigation<TangemPayDetailsNavigation> = SlotNavigation()

    private var shownTiersBanner: TangemPayTiersBannerType? = null

    init {
        analytics.send(TangemPayAnalyticsEvents.MainScreenOpened())
        handleBalanceHiding()

        paymentAccountStatusSupplier.invoke(userWalletId)
            .onEach { status -> currentStatus.update { status } }
            .map { it.value }
            .onEach { state ->
                sendTiersAnalytics(state)
                when (state) {
                    is PaymentAccountStatusValue.Deactivated -> uiState.update {
                        stateFactory.getDeactivatedState(state)
                    }
                    is PaymentAccountStatusValue.Loaded -> {
                        fetchAddToWalletBanner()
                        fetchCashbackBlock()
                        uiState.update { stateFactory.getLoadedState(state) }
                    }
                    is PaymentAccountStatusValue.Inactive -> uiState.update {
                        stateFactory.getInactiveState(state)
                    }
                    is PaymentAccountStatusValue.AwaitingPlanSelection -> router.replaceAll(
                        TangemPayAccountDetailsInnerRoute.SelectPlan(
                            tariffPlan = state.tariffPlan,
                            source = TangemPaySelectPlanSource.TIERS_ONBOARDING,
                        ),
                    )
                    else -> uiState.update { stateFactory.getLoadingState() }
                }
            }
            .launchIn(modelScope)
    }

    override fun onCancelTariffTransition(orderId: String) {
        analytics.send(TangemPayAnalyticsEvents.Tiers.CancelPlusMoveToBasicClicked())
        uiState.update(TangemPayErrorNotificationTransformer(shouldShowProgress = true))
        modelScope.launch {
            cancelTariffTransitionUseCase(userWalletId = userWalletId, orderId = orderId)
                .onLeft {
                    uiMessageSender.send(TangemPayMessagesFactory.createGenericError())
                }
            uiState.update(TangemPayErrorNotificationTransformer(shouldShowProgress = false))
        }
    }

    fun onStart() {
        onRefreshSwipe(refreshState = ShowRefreshState(false))
    }

    fun onStop() {
        modelScope.launch {
            expressTransactionsEventListener.send(ExpressTransactionsEvent.Clear)
        }
    }

    override fun onClickAddFunds() {
        analytics.send(TangemPayAnalyticsEvents.AddFundsClicked())
        val balance = currentStatus.value.balanceOrNull()
        val address = currentStatus.value.ifLoadedOrNull { it.depositAddress }
        if (balance == null || address.isNullOrEmpty()) {
            showBottomSheetError(TangemPayDetailsErrorType.Receive)
        } else {
            bottomSheetNavigation.activate(
                TangemPayDetailsNavigation.AddFunds(
                    walletId = userWalletId,
                    fiatBalance = balance.availableForWithdrawal,
                    cryptoBalance = balance.availableForWithdrawal,
                    depositAddress = balance.cryptoBalance.depositAddress,
                    cryptoCurrency = cryptoCurrency,
                    virtualAccountOnramp = currentStatus.value.ifLoadedOrNull { it.virtualAccount },
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
                fromCryptoCurrency = currency,
                userWalletId = userWalletId,
                screenSource = AnalyticsParam.ScreensSources.TangemPay.value,
                fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.FROM,
                tangemPayInput = AppRoute.Swap.TangemPayInput(
                    cryptoAmount = balance.availableForWithdrawal,
                    fiatAmount = balance.availableForWithdrawal,
                    depositAddress = balance.cryptoBalance.depositAddress,
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

    private fun fetchCashbackBlock() {
        if (!tangemPayFeatureToggles.isCashbackEnabled) return
        modelScope.launch {
            getCashbackSummaryUseCase(userWalletId).onRight { summary ->
                val isDismissed = getCashbackDeactivationDismissedUseCase(userWalletId)
                uiState.update(
                    transformer = CashbackBlockTransformer(
                        summary = summary,
                        isDeactivationDismissed = isDismissed,
                        dateFormatter = cashbackDateFormatter,
                        onClick = ::onClickCashback,
                        onGotIt = ::onDismissCashbackDeactivation,
                    ),
                )
            }
        }.saveIn(cashbackBlockJobHolder)
    }

    private fun onDismissCashbackDeactivation() {
        modelScope.launch {
            setCashbackDeactivationDismissedUseCase(userWalletId)
            uiState.update { it.copy(cashbackBlockState = null) }
        }.saveIn(cashbackBlockJobHolder)
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
        val card = currentStatus.value.ifLoadedOrNull { it.cards.firstOrNull() } ?: return
        router.push(TangemPayAccountDetailsInnerRoute.AddToWallet(card))
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
                fromCryptoCurrency = data.currency,
                userWalletId = data.walletId,
                screenSource = AnalyticsParam.ScreensSources.TangemPay.value,
                fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.TO,
                tangemPayInput = AppRoute.Swap.TangemPayInput(
                    cryptoAmount = data.cryptoBalance,
                    fiatAmount = data.fiatBalance,
                    depositAddress = data.depositAddress,
                ),
            ),
        )
    }

    override fun onClickBankTransfer() {
        val loaded = currentStatus.value.ifLoadedOrNull { it } ?: return
        when (val onramp = loaded.virtualAccount) {
            null -> return
            VirtualAccountOnramp.Processing -> showVaPreparing()
            // BankCredentialsError opens the deposit intro first; the retryable error sheet is shown from
            // its "Show details" action (see onShowDetailsClick).
            is VirtualAccountOnramp.Available,
            VirtualAccountOnramp.Eligible,
            is VirtualAccountOnramp.BankCredentialsError,
            -> openVirtualAccountDeposit(onramp, loaded)
        }
    }

    private fun openVirtualAccountDeposit(onramp: VirtualAccountOnramp, loaded: PaymentAccountStatusValue.Loaded) {
        analytics.send(TangemPayAnalyticsEvents.VaTopupButtonClicked())
        bottomSheetNavigation.dismiss()
        bottomSheetNavigation.activate(
            TangemPayDetailsNavigation.VirtualAccountDeposit(
                virtualAccountOnramp = onramp,
                userWalletId = userWalletId,
                paymentAccountAddress = loaded.balance.cryptoBalance.depositAddress,
            ),
        )
    }

    fun showVaBankingDetailsError() {
        analytics.send(TangemPayAnalyticsEvents.VaDetailsErrorShowed())
        bottomSheetNavigation.dismiss()
        bottomSheetNavigation.activate(
            TangemPayDetailsNavigation.VaBankingDetailsError(userWalletId = userWalletId),
        )
    }

    private fun showVaPreparing() {
        analytics.send(TangemPayAnalyticsEvents.VaPreparationPopupShowed())
        bottomSheetNavigation.dismiss()
        uiMessageSender.send(message = TangemPayMessagesFactory.createVaPreparingMessage())
    }

    fun onVaBankingDetailsResolved(onramp: VirtualAccountOnramp) {
        when (onramp) {
            // Bank credentials just loaded on retry — show the requisites straight away ([REDACTED_TASK_KEY]),
            // instead of the intro deposit sheet that would need another "Show details" tap.
            is VirtualAccountOnramp.Available -> onShowVirtualAccountRequisites(onramp)
            else -> {
                val loaded = currentStatus.value.ifLoadedOrNull { it } ?: return
                openVirtualAccountDeposit(onramp, loaded)
            }
        }
    }

    fun onVirtualAccountOrderCreated() {
        analytics.send(TangemPayAnalyticsEvents.VaSuccessScreenActivation())
        bottomSheetNavigation.dismiss()
        router.push(TangemPayAccountDetailsInnerRoute.VirtualAccountDepositSuccess)
    }

    fun onShowVirtualAccountRequisites(onramp: VirtualAccountOnramp.Available) {
        bottomSheetNavigation.dismiss()
        bottomSheetNavigation.activate(
            TangemPayDetailsNavigation.VirtualAccountRequisites(
                userWalletId = userWalletId,
                bankCredentials = onramp.bankCredentials,
            ),
        )
    }

    fun onVaBankingDetailsShown() {
        analytics.send(TangemPayAnalyticsEvents.VaBankingDetailsShowed())
    }

    fun onVaShareDetailsClicked() {
        analytics.send(TangemPayAnalyticsEvents.VaShareDetailsButtonClicked())
    }

    fun onVaFieldCopied(field: String) {
        analytics.send(TangemPayAnalyticsEvents.VaCopyFieldClicked(field))
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
        val customerId = currentStatus.value.customerId ?: run {
            TangemLogger.withTag("TangemPayDetailsModel").w(
                "CustomerId is null, cannot open transaction details. " +
                    "Status: ${currentStatus.value.value.typeName}",
            )
            return
        }
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
                userWalletId = userWalletId,
                customerId = customerId,
            ),
        )
    }

    override fun onClickTermsAndLimits() {
        analytics.send(TangemPayAnalyticsEvents.TermsAndLimitsClicked())
        urlOpener.openUrl(TangemPayConstants.TERMS_AND_LIMITS_LINK)
    }

    override fun onClickVisaBenefits() {
        urlOpener.openUrl(TangemPayConstants.visaBenefitsLink())
    }

    override fun onClickCurrentPlan(tariffPlan: TangemPayCustomerTariffPlan) {
        analytics.send(TangemPayAnalyticsEvents.Tiers.CurrentPlanClicked())
        router.push(TangemPayAccountDetailsInnerRoute.CurrentPlan(tariffPlan))
    }

    override fun onClickCashback() {
        router.push(TangemPayAccountDetailsInnerRoute.Cashback)
    }

    override fun onCardClick(cardId: String) {
        analytics.send(TangemPayAnalyticsEvents.CardIconClicked())
        router.push(TangemPayAccountDetailsInnerRoute.CardDetails(cardId = cardId))
    }

    override fun onAddCardClick(tariffState: TangemPayTariffPlanState?) {
        analytics.send(TangemPayAnalyticsEvents.AddExtraCardClicked())
        modelScope.launch {
            val offer = getCustomerOffers.additionalCardOffer(userWalletId).getOrNull()
            if (offer == null) {
                val message = if (tariffState != null && tariffState.tariff.plan.isBasicTier) {
                    TangemPayMessagesFactory.createMaximumCardsForPlanIssuedMessage(
                        onUpgradeClick = { onClickCurrentPlan(tariffState.tariff) }
                            .takeIf { !tariffState.isPlanTransitioningState },
                    )
                } else {
                    TangemPayMessagesFactory.createMaximumCardsIssuedMessage()
                }
                uiMessageSender.send(message)
                return@launch
            }
            analytics.send(TangemPayAnalyticsEvents.IssueAdditionalCardPopupShown())
            bottomSheetNavigation.activate(
                TangemPayDetailsNavigation.IssueAdditionalCard(
                    walletId = userWalletId,
                    feeAmount = offer.fee.amount,
                    feeCurrency = offer.fee.currency,
                    fiatBalance = currentStatus.value.balanceOrNull()?.fiatBalance?.availableBalance ?: BigDecimal.ZERO,
                ),
            )
        }
    }

    override fun onIssueAdditionalCardDismissed() {
        bottomSheetNavigation.dismiss()
    }

    override fun onIssueAdditionalCardSucceeded() {
        bottomSheetNavigation.dismiss()
        modelScope.launch { paymentAccountStatusFetcher.invoke(userWalletId) }
    }

    override fun onAddFundsForCardIssue() {
        bottomSheetNavigation.dismiss()
        onClickAddFunds()
    }

    override fun onRenewSession() {
        uiState.update(TangemPayErrorNotificationTransformer(shouldShowProgress = true))
        modelScope.launch {
            produceTangemPayInitialDataUseCase(userWalletId)
                .onRight {
                    paymentAccountStatusFetcher.invoke(userWalletId)
                    uiState.update(TangemPayErrorNotificationTransformer(shouldShowProgress = false))
                }
                .onLeft {
                    uiMessageSender.send(SnackbarMessage(resourceReference(R.string.common_error)))
                    uiState.update(TangemPayErrorNotificationTransformer(shouldShowProgress = false))
                }
        }
    }

    override fun onRemoveAccount() {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(R.string.tangempay_remove_account_alert_title),
                message = resourceReference(R.string.tangempay_remove_account_alert_description),
                firstActionBuilder = {
                    EventMessageAction(
                        isWarning = true,
                        title = resourceReference(R.string.tangempay_remove_account),
                        onClick = ::removeAccount,
                    )
                },
                secondActionBuilder = { cancelAction() },
            ),
        )
    }

    private fun removeAccount() {
        modelScope.launch {
            onboardingRepository.disableTangemPay(userWalletId)
                .onRight {
                    paymentAccountStatusFetcher.invoke(userWalletId)
                    router.pop()
                }
                .onLeft {
                    uiMessageSender.send(SnackbarMessage(resourceReference(R.string.common_error)))
                }
        }
    }

    private fun showBottomSheetError(type: TangemPayDetailsErrorType) {
        uiMessageSender.send(message = TangemPayMessagesFactory.createErrorMessage(errorType = type))
    }

    private fun sendTiersAnalytics(state: PaymentAccountStatusValue) {
        val tariffPlan = when (state) {
            is PaymentAccountStatusValue.Loaded -> state.tariffPlan
            is PaymentAccountStatusValue.Inactive -> state.tariffPlan
            else -> null
        }
        val banner = tariffPlan?.let {
            TangemPayTiersBannerType.fromPlan(tangemPayFeatureToggles.isTiersPlusPlanEnabled, tariffPlan)
        }

        if (banner == shownTiersBanner) return
        shownTiersBanner = banner

        val event = when (banner) {
            TangemPayTiersBannerType.TopUpForTierUpgrade -> {
                TangemPayAnalyticsEvents.Tiers.TopUpBannerForPlusShowed()
            }
            TangemPayTiersBannerType.TierSystemDowngrade -> {
                TangemPayAnalyticsEvents.Tiers.PlusCardsClosureWarningBannerShowed()
            }
            null -> null
        }

        event?.let { analytics.send(it) }
    }
}