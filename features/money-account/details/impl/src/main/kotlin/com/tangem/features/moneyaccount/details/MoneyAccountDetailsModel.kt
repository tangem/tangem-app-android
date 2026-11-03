package com.tangem.features.moneyaccount.details

import androidx.compose.runtime.Stable
import arrow.core.Either
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.router.slot.navigate
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.models.ReceiveAddressModel
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountBankCredentialsUseCase
import com.tangem.domain.moneyaccount.usecase.RenewTangemPaySessionUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.wallets.usecase.GetExploreUrlUseCase
import com.tangem.features.moneyaccount.details.addfunds.options.MoneyAccountAddFundsOptionsListener
import com.tangem.features.moneyaccount.details.component.MoneyAccountDetailsComponent
import com.tangem.features.moneyaccount.details.impl.R
import com.tangem.features.txhistory.entity.TxHistoryContentUpdateEmitter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class MoneyAccountDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    moneyAccountStatusSupplier: MoneyAccountStatusSupplier,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val urlOpener: UrlOpener,
    private val getExploreUrlUseCase: GetExploreUrlUseCase,
    private val analytics: AnalyticsEventHandler,
    private val getMoneyAccountBankCredentialsUseCase: GetMoneyAccountBankCredentialsUseCase,
    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher,
    private val txHistoryContentUpdateEmitter: TxHistoryContentUpdateEmitter,
    private val renewTangemPaySessionUseCase: RenewTangemPaySessionUseCase,
    private val uiMessageSender: UiMessageSender,
) : Model(), MoneyAccountAddFundsOptionsListener {

    private val params = paramsContainer.require<MoneyAccountDetailsComponent.Params>()
    private val stateFactory = MoneyAccountDetailsStateFactory(
        onAddFundsClick = ::onAddFundsClick,
        onSendClick = ::onSendClick,
        onMenuClick = ::onMenuClick,
        onTermsClick = ::onTermsClick,
        onSupportClick = ::onSupportClick,
        onBackClick = ::onBackClick,
        onRefresh = ::onRefresh,
        onRenewSession = ::onRenewSession,
    )
    val uiState: StateFlow<MoneyAccountDetailsUM>
        field = MutableStateFlow(stateFactory.createInitialState())

    val bottomSheetNavigation: SlotNavigation<MoneyAccountDetailsNavigation> = SlotNavigation()

    private var currentState: MoneyAccountStatusValue.Account? = null

    init {
        moneyAccountStatusSupplier.invoke(params.userWalletId)
            .map { it.value }
            .onEach { value ->
                when (value) {
                    is MoneyAccountStatusValue.Account -> onAccountLoaded(value)
                    else -> if (currentState == null) router.pop()
                }
            }
            .launchIn(modelScope)

        getBalanceHidingSettingsUseCase.isBalanceHidden()
            .onEach { isHidden -> uiState.update { it.copy(isBalanceHidden = isHidden) } }
            .launchIn(modelScope)
    }

    private fun onAccountLoaded(account: MoneyAccountStatusValue.Account) {
        val previous = currentState
        currentState = account
        uiState.update { state -> stateFactory.createAccountState(prevState = state, account = account) }
        if (previous == null) analytics.send(account.toScreenOpenedEvent())
        val wasBalanceUnavailable = previous != null && previous.balance == null
        if (account.balance == null && !wasBalanceUnavailable) {
            analytics.send(MoneyAccountAnalyticsEvents.BalanceErrorBannerShown())
        }
    }

    private fun onBackClick() {
        router.pop()
    }

    private fun onRefresh() {
        if (uiState.value.pullToRefreshConfig.isRefreshing) return
        updateRefreshing(isRefreshing = true)
        modelScope.launch {
            if (uiState.value.txHistoryCurrency != null) txHistoryContentUpdateEmitter.triggerUpdate()
            moneyAccountStatusFetcher(params.userWalletId)
            updateRefreshing(isRefreshing = false)
        }
    }

    private fun onRenewSession() {
        updateRenewSessionProgress(isLoading = true)
        modelScope.launch {
            renewTangemPaySessionUseCase(params.userWalletId)
                .onRight { moneyAccountStatusFetcher(params.userWalletId) }
                .onLeft { uiMessageSender.send(SnackbarMessage(resourceReference(R.string.common_error))) }
            updateRenewSessionProgress(isLoading = false)
        }
    }

    private fun updateRenewSessionProgress(isLoading: Boolean) {
        uiState.update { state ->
            val banner = state.messageBanner
            val button = banner?.secondaryButton
            if (banner == null || button == null) {
                state
            } else {
                state.copy(messageBanner = banner.copy(secondaryButton = button.copy(isLoading = isLoading)))
            }
        }
    }

    private fun updateRefreshing(isRefreshing: Boolean) {
        uiState.update { state ->
            state.copy(pullToRefreshConfig = state.pullToRefreshConfig.copy(isRefreshing = isRefreshing))
        }
    }

    fun onExploreClick() {
        val currency = currentState?.cryptoCurrency ?: return
        modelScope.launch {
            val url = getExploreUrlUseCase(userWalletId = params.userWalletId, currency = currency)
            if (url.isNotBlank()) urlOpener.openUrl(url)
        }
    }

    fun onTransactionClick(item: TxHistoryInfo) {
        analytics.send(item.toTransactionInListClicked())
    }

    private fun onAddFundsClick() {
        val account = currentState ?: return
        analytics.send(MoneyAccountAnalyticsEvents.AddFundsClicked())
        bottomSheetNavigation.activate(
            MoneyAccountDetailsNavigation.AddFundsOptions(depositAddress = account.balance?.depositAddress),
        )
    }

    private fun onSendClick() {
        val account = currentState ?: return
        if (account.balance == null) return
        analytics.send(MoneyAccountAnalyticsEvents.SendClicked())
        val bankTransferProductInstanceId = (account as? MoneyAccountStatusValue.Account.Active)
            ?.takeIf { it.isBankOfframpAvailable == true }
            ?.productInstanceId
        router.push(MoneyAccountDetailsRoute.WithdrawHub(productInstanceId = bankTransferProductInstanceId))
    }

    override fun onSwapClick() {
        val account = currentState ?: return
        if (account.balance == null) return
        bottomSheetNavigation.dismiss()
        router.push(
            AppRoute.Swap(
                userWalletId = params.userWalletId,
                fromCryptoCurrency = account.cryptoCurrency,
                screenSource = SWAP_SCREEN_SOURCE,
                fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.TO,
                accountFlow = AppRoute.Swap.AccountFlow.MoneyTopUp,
            ),
        )
    }

    override fun onReceiveClick(depositAddress: String) {
        val currency = currentState?.cryptoCurrency ?: return
        bottomSheetNavigation.dismiss()
        val config = TokenReceiveConfig(
            shouldShowWarning = true,
            cryptoCurrency = currency,
            userWalletId = params.userWalletId,
            shouldShowMemoDisclaimer = false,
            receiveAddress = listOf(
                ReceiveAddressModel(displayType = ReceiveAddressModel.DisplayType.Default, value = depositAddress),
            ),
        )
        bottomSheetNavigation.activate(MoneyAccountDetailsNavigation.Receive(config))
    }

    override fun onBankingDetailsLoadFailed() {
        analytics.send(MoneyAccountAnalyticsEvents.DetailsLoadFailed())
        bottomSheetNavigation.dismiss()
        bottomSheetNavigation.activate(MoneyAccountDetailsNavigation.BankDetailsError)
    }

    override fun onDismissAddFunds() {
        bottomSheetNavigation.navigate { config ->
            config.takeUnless { it is MoneyAccountDetailsNavigation.AddFundsOptions }
        }
    }

    suspend fun retryBankCredentials(): Either<VisaApiError, BankCredentials> {
        analytics.send(MoneyAccountAnalyticsEvents.DetailsRetryTapped())
        return getMoneyAccountBankCredentialsUseCase(params.userWalletId)
    }

    override fun onBankDetailsLoaded(bankCredentials: BankCredentials) {
        analytics.send(MoneyAccountAnalyticsEvents.AddFundsBankDetailsShown())
        bottomSheetNavigation.dismiss()
        router.push(MoneyAccountDetailsRoute.BankDetails(bankCredentials))
    }

    fun onBankDetailsSupportClick() {
        analytics.send(MoneyAccountAnalyticsEvents.DetailsErrorSupportTapped())
        bottomSheetNavigation.dismiss()
        sendSupportEmail()
    }

    private fun onMenuClick() {
        analytics.send(MoneyAccountAnalyticsEvents.AccountMenuClicked())
    }

    private fun onTermsClick() {
        analytics.send(
            TangemPayAnalyticsEvents.TermsAndLimitsClicked(product = TangemPayAnalyticsEvents.Product.ACCOUNT),
        )
        urlOpener.openUrl(TERMS_OF_USE_URL)
    }

    private fun onSupportClick() {
        analytics.send(MoneyAccountAnalyticsEvents.SupportClicked())
        sendSupportEmail()
    }

    private fun sendSupportEmail() {
        val customerId = currentState?.customerId ?: return
        modelScope.launch {
            sendFeedbackEmailUseCase(
                type = FeedbackEmailType.Visa.MoneyAccountRequest(
                    walletMetaInfo = WalletMetaInfo(userWalletId = params.userWalletId),
                    customerId = customerId,
                ),
            )
        }
    }

    private fun MoneyAccountStatusValue.Account.toScreenOpenedEvent() = MoneyAccountAnalyticsEvents.AccountScreenOpened(
        accountState = when (this) {
            is MoneyAccountStatusValue.Account.Active -> MoneyAccountAnalyticsEvents.AccountState.ACTIVE
        },
        balanceBand = balance?.cryptoAmount?.let(MoneyAccountAnalyticsEvents.BalanceBand::of),
        currency = balance?.currencyCode,
    )

    private companion object {
        const val SWAP_SCREEN_SOURCE = "MoneyAccount"
        const val TERMS_OF_USE_URL = "https://tangem.com/docs/en/virtual-account-terms.pdf"
    }
}