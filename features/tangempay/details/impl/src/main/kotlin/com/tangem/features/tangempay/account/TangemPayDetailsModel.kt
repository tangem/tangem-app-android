package com.tangem.features.tangempay.account

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRoute.Swap.AccountFlow
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
import com.tangem.domain.models.pay.TangemPayDetailsInitialRoute
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.CashbackDisplayMode
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.model.TangemPayTopUpData
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.repository.TangemPayWithdrawRepository
import com.tangem.domain.pay.usecase.*
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.TangemPayConstants
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.addfunds.AddFundsListener
import com.tangem.features.tangempay.card.issue.TangemPayIssueAdditionalCardComponent
import com.tangem.features.tangempay.cashback.impl.model.TangemPayCashbackDateFormatter
import com.tangem.features.tangempay.common.*
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.multichain.choosenetwork.ChooseNetworkListener
import com.tangem.features.tangempay.multichain.shouldUseChooseNetwork
import com.tangem.features.tangempay.tiers.select.TangemPaySelectPlanSource
import com.tangem.features.tangempay.txhistory.TangemPayTxHistoryUiActions
import com.tangem.features.tangempay.txhistory.TangemPayTxHistoryUpdateListener
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.transformer.Transformer
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
    paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analytics: AnalyticsEventHandler,
    private val router: Router,
    private val urlOpener: UrlOpener,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val uiMessageSender: UiMessageSender,
    private val txHistoryUpdateListener: TangemPayTxHistoryUpdateListener,
    private val tangemPayWithdrawRepository: TangemPayWithdrawRepository,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val tangemPayFeatureToggles: TangemPayFeatureToggles,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val produceTangemPayInitialDataUseCase: ProduceTangemPayInitialDataUseCase,
    private val onboardingRepository: OnboardingRepository,
    private val getCustomerOffers: GetCustomerOffersUseCase,
    private val getCashbackSummaryUseCase: GetCashbackSummaryUseCase,
    private val getBankCredentialsUseCase: GetBankCredentialsUseCase,
    private val getCashbackDeactivationDismissedUseCase: GetCashbackDeactivationDismissedUseCase,
    private val setCashbackDeactivationDismissedUseCase: SetCashbackDeactivationDismissedUseCase,
    tangemPayCurrencyFactory: TangemPayCurrencyFactory,
) : Model(),
    TangemPayTxHistoryUiActions,
    TangemPayDetailIntents,
    AddFundsListener,
    TangemPayIssueAdditionalCardComponent.Listener,
    ChooseNetworkListener {

    private val params: TangemPayDetailsContainerComponent.Params = paramsContainer.require()

    private val currentStatus = MutableStateFlow(params.initialStatus)

    private val userWalletId
        get() = currentStatus.value.userWalletId

    val cryptoCurrency: CryptoCurrency.Token = tangemPayCurrencyFactory.create(userWalletId)

    private val stateFactory = TangemPayDetailsStateFactory(
        onBack = router::pop,
        onOpenMenu = ::onOpenMenu,
        intents = this,
        isTiersPlusPlanEnabled = tangemPayFeatureToggles.isTiersPlusPlanEnabled,
        isMultichainEnabled = tangemPayFeatureToggles.isAccountMultichainEnabled,
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
    private val cashbackBlockJobHolder = JobHolder()
    private val planSelectionJobHolder = JobHolder()
    private val cashbackDateFormatter = TangemPayCashbackDateFormatter()

    val bottomSheetNavigation: SlotNavigation<TangemPayDetailsNavigation> = SlotNavigation()

    private var shownTiersBanner: TangemPayTiersBannerType? = null
    private var shownCashbackBlock: CashbackBlockAnalyticsType? = null
    private var cashbackTransformer: Transformer<TangemPayDetailsUM>? = null
    private var isDeliveryBannerShown = false

    private val isInitialRouteHandled = MutableStateFlow(false)

    init {
        analytics.send(TangemPayAnalyticsEvents.MainScreenOpened())
        handleBalanceHiding()

        paymentAccountStatusSupplier.invoke(userWalletId)
            .onEach { status -> currentStatus.update { status } }
            .map { it.value }
            .onEach { state ->
                sendTiersAnalytics(state)
                when (state) {
                    is PaymentAccountStatusValue.Deactivated -> {
                        uiState.update { stateFactory.getDeactivatedState(state) }
                        handleInitialRoute()
                    }
                    is PaymentAccountStatusValue.Loaded -> {
                        fetchCashbackBlock()
                        uiState.update {
                            val freshState = stateFactory.getLoadedState(state)
                            cashbackTransformer?.transform(freshState) ?: freshState
                        }
                        sendDeliveryBannerAnalytics()
                        handleInitialRoute()
                    }
                    is PaymentAccountStatusValue.Inactive -> uiState.update {
                        stateFactory.getInactiveState(state)
                    }
                    else -> uiState.update { stateFactory.getLoadingState() }
                }
            }
            .launchIn(modelScope)
    }

    private fun handleInitialRoute() {
        if (isInitialRouteHandled.value) return

        isInitialRouteHandled.update { true }

        when (params.initialRoute) {
            TangemPayDetailsInitialRoute.ACCOUNT_DETAILS,
            TangemPayDetailsInitialRoute.TIERS_ONBOARDING,
            -> Unit
            TangemPayDetailsInitialRoute.ADD_FUNDS -> openAddFunds()
            TangemPayDetailsInitialRoute.VA_ONRAMP -> openVirtualAccountOnramp()
            TangemPayDetailsInitialRoute.VA_ONRAMP_DETAILS -> openVirtualAccountBankingDetails()
            TangemPayDetailsInitialRoute.CURRENT_PLAN -> openCurrentPlan()
            TangemPayDetailsInitialRoute.CHANGE_PLAN -> openChangePlan()
            TangemPayDetailsInitialRoute.CASHBACK -> openCashback()
            TangemPayDetailsInitialRoute.ORDER_CARD -> openOrderCard()
        }
    }

    fun onStart() {
        observeAwaitingPlanSelection()
        onRefreshSwipe(refreshState = ShowRefreshState(false))
    }

    fun onStop() {
        planSelectionJobHolder.cancel()
    }

    private fun observeAwaitingPlanSelection() {
        currentStatus
            .map { it.value }
            .filterIsInstance<PaymentAccountStatusValue.AwaitingPlanSelection>()
            .onEach { status ->
                router.replaceAll(
                    TangemPayAccountDetailsInnerRoute.SelectPlan(
                        tariffPlan = status.tariffPlan,
                        source = TangemPaySelectPlanSource.TIERS_ONBOARDING,
                    ),
                )
            }
            .launchIn(modelScope)
            .saveIn(planSelectionJobHolder)
    }

    override fun onClickAddFunds() {
        analytics.send(TangemPayAnalyticsEvents.AddFundsClicked())
        openAddFunds()
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

    private fun openAddFunds() {
        val status = currentStatus.value
        val balance = status.balanceOrNull()
        if (balance == null || !status.value.canAddFunds(tangemPayFeatureToggles.isAccountMultichainEnabled)) {
            showBottomSheetError(TangemPayDetailsErrorType.Receive)
            return
        }
        bottomSheetNavigation.activate(
            TangemPayDetailsNavigation.AddFunds(
                walletId = userWalletId,
                fiatBalance = balance.availableForWithdrawal,
                cryptoBalance = balance.availableForWithdrawal,
                depositAddress = balance.cryptoBalance.depositAddress,
                cryptoCurrency = cryptoCurrency,
                virtualAccountOnramp = status.ifLoadedOrNull { it.virtualAccount },
            ),
        )
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
                accountFlow = AccountFlow.Withdraw,
            ),
        )
    }

    private fun fetchCashbackBlock() {
        if (!tangemPayFeatureToggles.isCashbackEnabled || cashbackBlockJobHolder.isActive) return
        modelScope.launch {
            getCashbackSummaryUseCase(userWalletId).onRight { summary ->
                val isDismissed = getCashbackDeactivationDismissedUseCase(userWalletId)
                sendCashbackBlockAnalytics(summary = summary, isDeactivationDismissed = isDismissed)
                val transformer = CashbackBlockTransformer(
                    summary = summary,
                    isDeactivationDismissed = isDismissed,
                    dateFormatter = cashbackDateFormatter,
                    onClick = ::onClickCashback,
                    onMenuItemClick = ::onClickCashbackMenuItem,
                    onGotIt = ::onDismissCashbackDeactivation,
                )
                cashbackTransformer = transformer
                uiState.update(transformer)
            }.onLeft {
                val current = cashbackTransformer
                val isMenuEntryShown = current is CashbackMenuItemErrorTransformer ||
                    current is CashbackBlockTransformer && current.isAltBlockMode
                when {
                    // The menu entry is the shown cashback UI — the error state lives there ([REDACTED_TASK_KEY])
                    isMenuEntryShown -> {
                        sendCashbackBlockShown(CashbackBlockAnalyticsType.SettingsButtonError)
                        applyCashbackTransformer(
                            CashbackMenuItemErrorTransformer(
                                onReload = ::onReloadCashbackMenuItem,
                                isReloading = false,
                            ),
                        )
                    }
                    // A failed refresh must not wipe the successfully shown widget ([REDACTED_TASK_KEY])
                    current is CashbackBlockTransformer -> return@onLeft
                    else -> {
                        sendCashbackBlockShown(CashbackBlockAnalyticsType.Error)
                        applyCashbackTransformer(
                            CashbackErrorBlockTransformer(
                                onReload = ::onReloadCashbackBlock,
                                isReloading = false,
                            ),
                        )
                    }
                }
            }
        }.saveIn(cashbackBlockJobHolder)
    }

    private fun applyCashbackTransformer(transformer: Transformer<TangemPayDetailsUM>) {
        cashbackTransformer = transformer
        uiState.update(transformer)
    }

    private fun onReloadCashbackBlock() {
        if (cashbackBlockJobHolder.isActive) return
        applyCashbackTransformer(
            CashbackErrorBlockTransformer(onReload = ::onReloadCashbackBlock, isReloading = true),
        )
        fetchCashbackBlock()
    }

    private fun onReloadCashbackMenuItem() {
        if (cashbackBlockJobHolder.isActive) return
        applyCashbackTransformer(
            CashbackMenuItemErrorTransformer(onReload = ::onReloadCashbackMenuItem, isReloading = true),
        )
        fetchCashbackBlock()
    }

    private fun sendCashbackBlockAnalytics(summary: CashbackSummary, isDeactivationDismissed: Boolean) {
        val block = when (summary) {
            is CashbackSummary.Enabled -> when (summary.displayMode) {
                CashbackDisplayMode.FULL -> CashbackBlockAnalyticsType.Widget
                CashbackDisplayMode.ALT_BLOCK -> CashbackBlockAnalyticsType.SettingsButton
            }

            CashbackSummary.Deactivated ->
                CashbackBlockAnalyticsType.DeactivationBanner.takeIf { !isDeactivationDismissed }

            CashbackSummary.Disabled,
            CashbackSummary.Unknown,
            -> null
        }
        sendCashbackBlockShown(block)
    }

    private fun sendCashbackBlockShown(block: CashbackBlockAnalyticsType?) {
        if (block == shownCashbackBlock) return
        shownCashbackBlock = block

        val event = when (block) {
            CashbackBlockAnalyticsType.Widget -> {
                TangemPayAnalyticsEvents.Cashback.BannerShowed()
            }
            CashbackBlockAnalyticsType.SettingsButton -> {
                TangemPayAnalyticsEvents.Cashback.ButtonInSettingsShowed()
            }
            CashbackBlockAnalyticsType.DeactivationBanner -> {
                TangemPayAnalyticsEvents.Cashback.DeactivationBannerShowed()
            }
            CashbackBlockAnalyticsType.Error -> {
                TangemPayAnalyticsEvents.Cashback.BannerErrorStateShowed()
            }
            CashbackBlockAnalyticsType.SettingsButtonError -> {
                TangemPayAnalyticsEvents.Cashback.ButtonErrorStateShowed()
            }
            null -> null
        }

        event?.let { analytics.send(it) }
    }

    private fun onDismissCashbackDeactivation() {
        analytics.send(TangemPayAnalyticsEvents.Cashback.DeactivationBannerGotItClicked())
        modelScope.launch {
            setCashbackDeactivationDismissedUseCase(userWalletId)
            cashbackTransformer = null
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
        fetchCashbackBlock()
        modelScope.launch {
            uiState.update(TangemPayDetailsRefreshTransformer(isRefreshing = refreshState.value))
            paymentAccountStatusFetcher.invoke(userWalletId)
            txHistoryUpdateListener.triggerUpdate()
            uiState.update(TangemPayDetailsRefreshTransformer(isRefreshing = false))
        }.saveIn(refreshStateJobHolder)
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
                accountFlow = AccountFlow.TopUp,
            ),
        )
    }

    override fun onClickBankTransfer() {
        openVirtualAccountOnramp(shouldSendClickAnalytics = true)
    }

    private fun openVirtualAccountOnramp(shouldSendClickAnalytics: Boolean = false) {
        val loaded = currentStatus.value.ifLoadedOrNull { it } ?: return
        when (val onramp = loaded.virtualAccount) {
            null -> return
            VirtualAccountOnramp.Processing -> showVaPreparing()
            is VirtualAccountOnramp.Available,
            VirtualAccountOnramp.Eligible,
            -> {
                if (shouldSendClickAnalytics) analytics.send(TangemPayAnalyticsEvents.VaTopupButtonClicked())
                openVirtualAccountDeposit(onramp, loaded)
            }
        }
    }

    private fun openVirtualAccountBankingDetails() {
        val loaded = currentStatus.value.ifLoadedOrNull { it } ?: return
        when (val onramp = loaded.virtualAccount) {
            null -> return
            VirtualAccountOnramp.Processing -> showVaPreparing()
            VirtualAccountOnramp.Eligible -> openVirtualAccountDeposit(onramp, loaded)
            is VirtualAccountOnramp.Available -> fetchVirtualAccountBankingDetails(onramp)
        }
    }

    private fun fetchVirtualAccountBankingDetails(onramp: VirtualAccountOnramp.Available) {
        modelScope.launch {
            getBankCredentialsUseCase(userWalletId = userWalletId, productInstanceId = onramp.productInstanceId)
                .onRight(::onShowVirtualAccountRequisites)
                .onLeft { showVaBankingDetailsError(onramp.productInstanceId) }
        }
    }

    private fun openVirtualAccountDeposit(onramp: VirtualAccountOnramp, loaded: PaymentAccountStatusValue.Loaded) {
        bottomSheetNavigation.dismiss()
        val paymentAccountAddress = loaded.balance?.cryptoBalance?.depositAddress
        if (paymentAccountAddress == null) {
            showBottomSheetError(TangemPayDetailsErrorType.Receive)
            return
        }
        bottomSheetNavigation.activate(
            TangemPayDetailsNavigation.VirtualAccountDeposit(
                virtualAccountOnramp = onramp,
                userWalletId = userWalletId,
                paymentAccountAddress = paymentAccountAddress,
            ),
        )
    }

    fun showVaBankingDetailsError(productInstanceId: String) {
        analytics.send(TangemPayAnalyticsEvents.VaDetailsErrorShowed())
        bottomSheetNavigation.dismiss()
        bottomSheetNavigation.activate(
            TangemPayDetailsNavigation.VaBankingDetailsError(
                userWalletId = userWalletId,
                productInstanceId = productInstanceId,
            ),
        )
    }

    private fun showVaPreparing() {
        analytics.send(TangemPayAnalyticsEvents.VaPreparationPopupShowed())
        bottomSheetNavigation.dismiss()
        uiMessageSender.send(message = TangemPayMessagesFactory.createVaPreparingMessage())
    }

    fun onVaBankingDetailsResolved(bankCredentials: BankCredentials) {
        // Bank credentials just loaded on retry — show the requisites straight away ([REDACTED_TASK_KEY]),
        // instead of the intro deposit sheet that would need another "Show details" tap.
        onShowVirtualAccountRequisites(bankCredentials)
    }

    fun onVirtualAccountOrderCreated() {
        analytics.send(TangemPayAnalyticsEvents.VaSuccessScreenActivation())
        bottomSheetNavigation.dismiss()
        router.push(TangemPayAccountDetailsInnerRoute.VirtualAccountDepositSuccess)
    }

    fun onShowVirtualAccountRequisites(bankCredentials: BankCredentials) {
        bottomSheetNavigation.dismiss()
        bottomSheetNavigation.activate(
            TangemPayDetailsNavigation.VirtualAccountRequisites(
                userWalletId = userWalletId,
                bankCredentials = bankCredentials,
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
        val networks = currentStatus.value.networksOrNull().orEmpty()
        if (shouldUseChooseNetwork(tangemPayFeatureToggles.isAccountMultichainEnabled, networks)) {
            bottomSheetNavigation.activate(TangemPayDetailsNavigation.ChooseNetwork(walletId = data.walletId))
        } else {
            val config = TokenReceiveConfig(
                shouldShowWarning = true,
                cryptoCurrency = data.currency,
                userWalletId = data.walletId,
                showMemoDisclaimer = false,
                receiveAddress = data.receiveAddress,
            )
            bottomSheetNavigation.activate(TangemPayDetailsNavigation.Receive(config))
        }
    }

    override fun onDismissAddFunds() {
        bottomSheetNavigation.dismiss()
    }

    override fun onSelectAvailable(networkRawId: String) {
        bottomSheetNavigation.dismiss()
        bottomSheetNavigation.activate(
            TangemPayDetailsNavigation.PaymentReceive(walletId = userWalletId, networkRawId = networkRawId),
        )
    }

    override fun onSelectDisabled() {
        bottomSheetNavigation.dismiss()
        bottomSheetNavigation.activate(TangemPayDetailsNavigation.OtherNetworks)
    }

    /**
     * Both sheets share the single bottom-sheet slot, so opening "Other networks" replaced the
     * "Choose network" sheet it was opened from instead of stacking on top of it. Closing it therefore has to
     * bring that sheet back explicitly — otherwise the user is dropped all the way out to the account screen.
     */
    fun onOtherNetworksDismiss() {
        bottomSheetNavigation.dismiss()
        bottomSheetNavigation.activate(TangemPayDetailsNavigation.ChooseNetwork(walletId = userWalletId))
    }

    override fun onDismiss() {
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

    override fun onClickCurrentPlan(tariffPlan: TangemPayTariffPlanState) {
        analytics.send(TangemPayAnalyticsEvents.Tiers.CurrentPlanClicked())
        router.push(TangemPayAccountDetailsInnerRoute.CurrentPlan(tariffPlan))
    }

    override fun onClickCashback() {
        analytics.send(TangemPayAnalyticsEvents.Cashback.BannerClicked())
        router.push(TangemPayAccountDetailsInnerRoute.Cashback)
    }

    private fun onClickCashbackMenuItem() {
        analytics.send(TangemPayAnalyticsEvents.Cashback.ButtonInSettingsClicked())
        router.push(TangemPayAccountDetailsInnerRoute.Cashback)
    }

    private fun openCurrentPlan() {
        val tariffPlan = currentStatus.value.tariffPlanState ?: return
        router.push(TangemPayAccountDetailsInnerRoute.CurrentPlan(tariffPlan))
    }

    private fun openChangePlan() {
        val tariffPlan = currentStatus.value.tariffPlan ?: return
        router.push(
            TangemPayAccountDetailsInnerRoute.SelectPlan(
                tariffPlan = tariffPlan,
                source = TangemPaySelectPlanSource.CHANGE_PLAN,
            ),
        )
    }

    private fun openCashback() {
        if (!tangemPayFeatureToggles.isCashbackEnabled) return
        currentStatus.value.ifLoadedOrNull { router.push(TangemPayAccountDetailsInnerRoute.Cashback) }
    }

    private fun openOrderCard() {
        if (!tangemPayFeatureToggles.isPlasticCardOrderEnabled) return
        currentStatus.value.ifLoadedOrNull { router.push(TangemPayAccountDetailsInnerRoute.OrderCard()) }
    }

    override fun onCardClick(cardId: String) {
        analytics.send(TangemPayAnalyticsEvents.CardIconClicked())
        router.push(TangemPayAccountDetailsInnerRoute.CardDetails(cardId = cardId))
    }

    override fun onActivateCardClick(cardId: String) {
        analytics.send(TangemPayAnalyticsEvents.Plastic.ActivateCardBannerButtonClicked())
        router.push(
            TangemPayAccountDetailsInnerRoute.CardDetails(cardId = cardId, shouldOpenActivation = true),
        )
    }

    override fun onAddCardClick(tariffState: TangemPayTariffPlanState?) {
        analytics.send(TangemPayAnalyticsEvents.AddExtraCardClicked())
        modelScope.launch {
            val offer = getCustomerOffers.additionalCardOffer(userWalletId).getOrNull()
            if (offer == null) {
                val message = if (tariffState != null && tariffState.tariff.plan.isBasicTier) {
                    TangemPayMessagesFactory.createMaximumCardsForPlanIssuedMessage(
                        onUpgradeClick = { onClickCurrentPlan(tariffState) }
                            .takeIf { !tariffState.isPlanTransitioningState },
                    )
                } else {
                    TangemPayMessagesFactory.createMaximumCardsIssuedMessage()
                }
                uiMessageSender.send(message)
                return@launch
            }
            if (tangemPayFeatureToggles.isPlasticCardOrderEnabled) {
                router.push(TangemPayAccountDetailsInnerRoute.OrderCard())
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

    private fun sendDeliveryBannerAnalytics() {
        val banner = uiState.value.balanceBlockState.cardsBlockState?.progressBanner
        val isShown = banner is CardsProgressBannerUM.Delivering

        if (isShown == isDeliveryBannerShown) return
        isDeliveryBannerShown = isShown

        if (isShown) analytics.send(TangemPayAnalyticsEvents.Plastic.CardInTransitBannerShowed())
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

private enum class CashbackBlockAnalyticsType { Widget, SettingsButton, DeactivationBanner, Error, SettingsButtonError }