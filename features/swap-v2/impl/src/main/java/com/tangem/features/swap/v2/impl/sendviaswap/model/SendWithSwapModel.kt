package com.tangem.features.swap.v2.impl.sendviaswap.model

import arrow.core.getOrElse
import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationComponent
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.swap.v2.api.SendWithSwapComponent
import com.tangem.features.swap.v2.impl.amount.SwapAmountComponent
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.SwapAlertFactory
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
import com.tangem.features.swap.v2.impl.sendviaswap.SendWithSwapRoute
import com.tangem.features.swap.v2.impl.sendviaswap.confirm.SendWithSwapConfirmComponent
import com.tangem.features.swap.v2.impl.sendviaswap.entity.SendWithSwapUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

@Suppress("LongParameterList")
@ModelScoped
internal class SendWithSwapModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val swapAlertFactory: SwapAlertFactory,
    paramsContainer: ParamsContainer,
) : Model(),
    SwapAmountComponent.ModelCallback,
    SendDestinationComponent.ModelCallback,
    SendWithSwapConfirmComponent.ModelCallback {

    private val params: SendWithSwapComponent.Params = paramsContainer.require()

    val analyticCategoryName = CommonSendAnalyticEvents.SEND_CATEGORY
    val analyticsSendSource = CommonSendAnalyticEvents.CommonSendSource.SendWithSwap
    val initialRoute = SendWithSwapRoute.Amount(false)
    val currentRoute = MutableStateFlow<SendWithSwapRoute>(initialRoute)

    var userWallet: UserWallet by Delegates.notNull()
    var appCurrency: AppCurrency = AppCurrency.Default

    val uiState: StateFlow<SendWithSwapUM>
        field = MutableStateFlow(initialState())

    val isBalanceHiddenFlow: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val primaryCryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>
        field = MutableStateFlow(
            CryptoCurrencyStatus(
                currency = params.currency,
                value = CryptoCurrencyStatus.Loading,
            ),
        )

    val primaryFeePaidCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>
        field = MutableStateFlow(
            CryptoCurrencyStatus(
                currency = params.currency,
                value = CryptoCurrencyStatus.Loading,
            ),
        )

    val accountFlow: StateFlow<Account?>
        field = MutableStateFlow(null)

    val isAccountModeFlow: StateFlow<Boolean>
        field = MutableStateFlow(false)

    init {
        initUserWallet()
        initAppCurrency()
        subscribeOnBalanceHidden()
    }

    override fun onAmountResult(amountUM: SwapAmountUM) {
        uiState.update { it.copy(amountUM = amountUM) }
    }

    override fun onDestinationResult(destinationUM: DestinationUM) {
        uiState.update { it.copy(destinationUM = destinationUM) }
    }

    override fun onResult(route: SendWithSwapRoute, sendWithSwapUM: SendWithSwapUM) {
        if (currentRoute.value == route) {
            uiState.value = sendWithSwapUM
        }
    }

    override fun onNavigationResult(navigationUM: NavigationUM) {
        uiState.update { it.copy(navigationUM = navigationUM) }
    }

    override fun onSeparatorClick(lastAmount: String, isEnterInFiatSelected: Boolean) {
        params.callback?.onCloseSwap(lastAmount, isEnterInFiatSelected)
        router.popTo(initialRoute)
    }

    override fun resetSendWithSwapNavigation(resetNavigation: Boolean) {
        uiState.update { state ->
            state.copy(
                destinationUM = DestinationUM.Empty(),
                feeSelectorUM = FeeSelectorUM.Loading,
                confirmUM = ConfirmUM.Empty,
                navigationUM = NavigationUM.Empty,
            )
        }
        if (resetNavigation) {
            router.popTo(SendWithSwapRoute.Amount(false))
        }
    }

    override fun onBackClick() = router.pop()

    override fun onNextClick() {
        if (currentRoute.value.isEditMode) {
            onBackClick()
        } else {
            when (currentRoute.value) {
                is SendWithSwapRoute.Amount -> router.push(SendWithSwapRoute.Destination(isEditMode = false))
                is SendWithSwapRoute.Destination -> router.push(SendWithSwapRoute.Confirm)
                SendWithSwapRoute.Confirm -> router.push(SendWithSwapRoute.Success)
                SendWithSwapRoute.Success -> onBackClick()
            }
        }
    }

    private fun initUserWallet() {
        getUserWalletUseCase(params.userWalletId).fold(
            ifRight = { wallet ->
                userWallet = wallet
                getPrimaryCurrencyStatusUpdates(params.currency)
            },
            ifLeft = { error ->
                Timber.w(error.toString())
                swapAlertFactory.getGenericErrorState(
                    expressError = ExpressError.UnknownError,
                    onFailedTxEmailClick = {
                        modelScope.launch {
                            swapAlertFactory.onFailedTxEmailClick(
                                userWallet = userWallet,
                                cryptoCurrency = params.currency,
                                errorMessage = error.toString(),
                            )
                        }
                    },
                    popBack = ::onBackClick,
                )
            },
        )
    }

    private fun initAppCurrency() {
        modelScope.launch {
            appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }
        }
    }

    private fun initialState(): SendWithSwapUM {
        return SendWithSwapUM(
            amountUM = SwapAmountUM.Empty(swapDirection = SwapDirection.Direct),
            destinationUM = DestinationUM.Empty(),
            feeSelectorUM = FeeSelectorUM.Loading,
            confirmUM = ConfirmUM.Empty,
            navigationUM = NavigationUM.Empty,
        )
    }

    private fun getPrimaryCurrencyStatusUpdates(cryptoCurrency: CryptoCurrency) {
        getAccountCurrencyStatusUseCase(
            userWalletId = params.userWalletId,
            currency = cryptoCurrency,
        ).onEach { (account, cryptoCurrencyStatus) ->
            accountFlow.value = account
            isAccountModeFlow.value = isAccountsModeEnabledUseCase.invokeSync()

            primaryCryptoCurrencyStatusFlow.value = cryptoCurrencyStatus
            primaryFeePaidCurrencyStatusFlow.value = getFeePaidCryptoCurrencyStatusSyncUseCase(
                userWalletId = params.userWalletId,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
            ).getOrNull() ?: cryptoCurrencyStatus
        }.flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun subscribeOnBalanceHidden() {
        getBalanceHidingSettingsUseCase()
            .conflate()
            .distinctUntilChanged()
            .onEach { balanceHidingSettings ->
                isBalanceHiddenFlow.update { balanceHidingSettings.isBalanceHidden }
            }
            .launchIn(modelScope)
    }
}