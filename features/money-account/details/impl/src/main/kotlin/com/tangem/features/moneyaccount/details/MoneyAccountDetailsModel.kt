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
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.models.ReceiveAddressModel
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountBankCredentialsUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.wallets.usecase.GetExploreUrlUseCase
import com.tangem.features.moneyaccount.details.addfunds.options.MoneyAccountAddFundsOptionsListener
import com.tangem.features.moneyaccount.details.component.MoneyAccountDetailsComponent
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
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val urlOpener: UrlOpener,
    private val getExploreUrlUseCase: GetExploreUrlUseCase,
    private val analytics: AnalyticsEventHandler,
    private val getMoneyAccountBankCredentialsUseCase: GetMoneyAccountBankCredentialsUseCase,
) : Model(), MoneyAccountAddFundsOptionsListener {

    private val params = paramsContainer.require<MoneyAccountDetailsComponent.Params>()
    val txHistorySlotNavigation = SlotNavigation<CryptoCurrency.Token>()
    private val stateFactory = MoneyAccountDetailsStateFactory(
        onAddFundsClick = ::onAddFundsClick,
        onTermsClick = ::onTermsClick,
        onSupportClick = ::onSupportClick,
        onBackClick = ::onBackClick,
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
                    is MoneyAccountStatusValue.Account -> {
                        currentState = value
                        txHistorySlotNavigation.activate(value.cryptoCurrency)
                        uiState.update { stateFactory.createAccountState(value) }
                    }
                    else -> if (currentState == null) router.pop()
                }
            }
            .launchIn(modelScope)
    }

    private fun onBackClick() {
        router.pop()
    }

    fun onExploreClick() {
        val currency = currentState?.cryptoCurrency ?: return
        modelScope.launch {
            val url = getExploreUrlUseCase(userWalletId = params.userWalletId, currency = currency)
            if (url.isNotBlank()) urlOpener.openUrl(url)
        }
    }

    private fun onAddFundsClick() {
        val account = currentState ?: return
        analytics.send(MoneyAccountAnalyticsEvents.AddFundsClicked())
        bottomSheetNavigation.activate(
            MoneyAccountDetailsNavigation.AddFundsOptions(depositAddress = account.balance?.depositAddress),
        )
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
        bottomSheetNavigation.dismiss()
        router.push(MoneyAccountDetailsRoute.BankDetails(bankCredentials))
    }

    fun onBankDetailsSupportClick() {
        analytics.send(MoneyAccountAnalyticsEvents.DetailsErrorSupportTapped())
        bottomSheetNavigation.dismiss()
        onSupportClick()
    }

    private fun onTermsClick() {
        urlOpener.openUrl(TERMS_OF_USE_URL)
    }

    private fun onSupportClick() {
        val customerId = currentState?.customerId ?: return
        modelScope.launch {
            sendFeedbackEmailUseCase(
                type = FeedbackEmailType.Visa.DirectUserRequest(
                    walletMetaInfo = WalletMetaInfo(userWalletId = params.userWalletId),
                    customerId = customerId,
                ),
            )
        }
    }

    private companion object {
        const val SWAP_SCREEN_SOURCE = "MoneyAccount"
        const val TERMS_OF_USE_URL = "https://tangem.com/docs/en/virtual-account-terms.pdf"
    }
}