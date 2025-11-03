package com.tangem.features.send.v2.subcomponents.amount.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.common.ui.amountScreen.converters.*
import com.tangem.common.ui.amountScreen.converters.field.AmountBoundaryUpdateTransformer
import com.tangem.common.ui.amountScreen.converters.field.AmountFieldChangeTransformer
import com.tangem.common.ui.amountScreen.converters.field.AmountFieldSetMaxAmountTransformer
import com.tangem.common.ui.amountScreen.models.AmountParameters
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.WrappedList
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.tokens.GetMinimumTransactionAmountSyncUseCase
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.send.v2.api.entity.PredefinedValues
import com.tangem.features.send.v2.api.subcomponents.amount.analytics.CommonSendAmountAnalyticEvents
import com.tangem.features.send.v2.api.subcomponents.amount.analytics.CommonSendAmountAnalyticEvents.SelectedCurrencyType
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.features.send.v2.common.CommonSendRoute
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.subcomponents.amount.SendAmountComponentParams
import com.tangem.features.send.v2.subcomponents.amount.SendAmountReduceListener
import com.tangem.features.send.v2.subcomponents.amount.SendAmountUpdateListener
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.orZero
import com.tangem.utils.isNullOrZero
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates

@Suppress("LongParameterList", "LargeClass")
@Stable
@ModelScoped
internal class SendAmountModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getMinimumTransactionAmountSyncUseCase: GetMinimumTransactionAmountSyncUseCase,
    private val sendAmountReduceListener: SendAmountReduceListener,
    private val sendAmountUpdateListener: SendAmountUpdateListener,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val feeSelectorReloadTrigger: FeeSelectorReloadTrigger,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val rampStateManager: RampStateManager,
    private val sendAmountAlertFactory: SendAmountAlertFactory,
    private val getWalletsUseCase: GetWalletsUseCase,
) : Model(), SendAmountClickIntents {

    private val params: SendAmountComponentParams = paramsContainer.require()
    private var appCurrency: AppCurrency = AppCurrency.Default
    private var userWallet: UserWallet? = null

    private val _uiState = MutableStateFlow(params.state)
    val uiState = _uiState.asStateFlow()

    private var isAvailableForSwap: Boolean = false
    val isSendWithSwapAvailable: StateFlow<Boolean>
        field = MutableStateFlow(false)

    private val analyticsCategoryName = params.analyticsCategoryName
    private var cryptoCurrencyStatus: CryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = params.cryptoCurrency,
        value = CryptoCurrencyStatus.Loading,
    )

    private var minAmountBoundary: EnterAmountBoundary? = null
    private var maxAmountBoundary: EnterAmountBoundary by Delegates.notNull()

    init {
        modelScope.launch {
            isAvailableForSwap = rampStateManager.availableForSwap(
                userWalletId = params.userWalletId,
                cryptoCurrency = params.cryptoCurrency,
            ) == ScenarioUnavailabilityReason.None
        }
        configAmountNavigation()
        initAppCurrency()
        subscribeOnCryptoCurrencyStatusFlow()
        subscribeOnAmountReduceByTriggerUpdates()
        subscribeOnAmountReduceToTriggerUpdates()
        subscribeOnAmountIgnoreReduceTriggerUpdates()
        subscribeOnAmountUpdateTriggerUpdates()
        subscribeOnBalanceHiddenUpdates()
    }

    private fun initAppCurrency() {
        getUserWalletUseCase.invokeFlow(params.userWalletId)
            .onEach { either ->
                either.fold(
                    ifLeft = { error ->
                        val amountParams = params as? SendAmountComponentParams.AmountParams
                        amountParams?.callback?.onError(error)
                    },
                    ifRight = { wallet ->
                        userWallet = wallet
                    },
                )
            }.launchIn(modelScope)
    }

    private fun subscribeOnBalanceHiddenUpdates() {
        params.isBalanceHidingFlow.onEach { isBalanceHidden ->
            if (cryptoCurrencyStatus.value != CryptoCurrencyStatus.Loading) {
                _uiState.update(
                    AmountBoundaryUpdateTransformer(
                        cryptoCurrencyStatus = cryptoCurrencyStatus,
                        maxEnterAmount = maxAmountBoundary,
                        appCurrency = appCurrency,
                        isBalanceHidden = params.isBalanceHidingFlow.value,
                    ),
                )
            }
        }.launchIn(modelScope)
    }

    private fun subscribeOnCryptoCurrencyStatusFlow() {
        combine(
            flow = params.cryptoCurrencyStatusFlow.distinctUntilChanged { old, new ->
                old.value.amount == new.value.amount
            }, // Check only balance changes,
            flow2 = params.accountFlow,
            flow3 = params.isAccountModeFlow,
        ) { newCryptoCurrencyStatus, account, isAccountsMode ->
            maxAmountBoundary = MaxEnterAmountConverter().convert(newCryptoCurrencyStatus)
            cryptoCurrencyStatus = newCryptoCurrencyStatus
            initMinBoundary(newCryptoCurrencyStatus, account, isAccountsMode)
        }.flowOn(dispatchers.main)
            .launchIn(modelScope)
    }

    private suspend fun initMinBoundary(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        account: Account.CryptoPortfolio?,
        isAccountsMode: Boolean,
    ) {
        minAmountBoundary = getMinimumTransactionAmountSyncUseCase(
            userWalletId = params.userWalletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
        ).getOrNull()?.let {
            EnterAmountBoundary(
                amount = it,
                fiatRate = cryptoCurrencyStatus.value.fiatRate.orZero(),
            )
        }

        appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }

        if (uiState.value is AmountState.Data) {
            _uiState.update(
                AmountBoundaryUpdateTransformer(
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    maxEnterAmount = maxAmountBoundary,
                    appCurrency = appCurrency,
                    isBalanceHidden = params.isBalanceHidingFlow.value,
                ),
            )
        } else {
            initialState(cryptoCurrencyStatus, account, isAccountsMode)
        }
    }

    private fun initialState(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        @Suppress("UnusedParameter") account: Account.CryptoPortfolio?,
        @Suppress("UnusedParameter") isAccountsMode: Boolean,
    ) {
        if (uiState.value is AmountState.Empty && userWallet != null) {
            val isOnlyOneWallet = getWalletsUseCase.invokeSync().size == 1
            val walletTitle = if (isOnlyOneWallet) {
                resourceReference(R.string.send_from_title)
            } else {
                resourceReference(
                    R.string.send_from_wallet_name,
                    WrappedList(listOf(userWallet?.name.orEmpty())), // TODO [REDACTED_TASK_KEY]
                )
            }
            _uiState.update {
                AmountStateConverter(
                    clickIntents = this,
                    appCurrency = appCurrency,
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    maxEnterAmount = maxAmountBoundary,
                    iconStateConverter = CryptoCurrencyToIconStateConverter(),
                    isBalanceHidden = params.isBalanceHidingFlow.value,
                    accountTitleUM = AmountAccountConverter(
                        isAccountsMode = isAccountsMode,
                        walletTitle = walletTitle,
                        prefixText = resourceReference(R.string.common_from),
                    ).convert(account),
                ).convert(
                    AmountParameters(
                        title = walletTitle,
                        value = "",
                    ),
                )
            }
        }
        val predefinedAmount = (params.predefinedValues as? PredefinedValues.Content)?.amount
        if (predefinedAmount != null) {
            onAmountValueChange(predefinedAmount)
        }
    }

    fun updateState(amountUM: AmountState) {
        if (amountUM !is AmountState.Empty) {
            _uiState.value = amountUM
        }
    }

    override fun onCurrencyChangeClick(isFiat: Boolean) {
        _uiState.update(AmountCurrencyTransformer(cryptoCurrencyStatus, isFiat))
    }

    override fun onAmountValueChange(value: String) {
        _uiState.update(
            AmountFieldChangeTransformer(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                maxEnterAmount = maxAmountBoundary,
                minimumTransactionAmount = minAmountBoundary,
                value = value,
            ),
        )
    }

    override fun onMaxValueClick() {
        val decimalCryptoValue = cryptoCurrencyStatus.value.amount
        if (decimalCryptoValue.isNullOrZero()) return

        _uiState.update(
            AmountFieldSetMaxAmountTransformer(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                maxAmount = maxAmountBoundary,
                minAmount = minAmountBoundary,
            ),
        )
        analyticsEventHandler.send(
            CommonSendAmountAnalyticEvents.MaxAmountButtonClicked(
                categoryName = analyticsCategoryName,
                token = params.cryptoCurrency.symbol,
                blockchain = params.cryptoCurrency.network.name,
                source = params.analyticsSendSource,
            ),
        )
    }

    override fun onAmountPasteTriggerDismiss() {
        _uiState.update(AmountPastedTriggerDismissTransformer)
    }

    override fun onAmountNext() {
        (uiState.value as? AmountState.Data)?.amountTextField?.isFiatValue?.let { isFiatSelected ->
            analyticsEventHandler.send(
                CommonSendAmountAnalyticEvents.SelectedCurrency(
                    categoryName = analyticsCategoryName,
                    type = if (isFiatSelected) {
                        SelectedCurrencyType.AppCurrency
                    } else {
                        SelectedCurrencyType.Token
                    },
                ),
            )
        }
        saveResult()
    }

    override fun onConvertToAnotherToken() {
        val amountParams = params as? SendAmountComponentParams.AmountParams ?: return
        if (amountParams.currentRoute.value.isEditMode) {
            sendAmountAlertFactory.showResetSendingAlert {
                params.callback.resetSendNavigation()
                confirmConvertToToken()
            }
        } else {
            confirmConvertToToken()
        }
    }

    private fun confirmConvertToToken() {
        val amountParams = params as? SendAmountComponentParams.AmountParams ?: return
        val amountFieldData = uiState.value as? AmountState.Data
        _uiState.update {
            (it as? AmountState.Data)?.copy(
                isPrimaryButtonEnabled = false,
            ) ?: it
        }

        val isEnterInFiatSelected = amountFieldData?.amountTextField?.isFiatValue == true
        val lastAmount = if (isEnterInFiatSelected) {
            amountFieldData.amountTextField.fiatValue
        } else {
            amountFieldData?.amountTextField?.value
        }

        amountParams.callback.onConvertToAnotherToken(lastAmount.orEmpty(), isEnterInFiatSelected)
    }

    private fun subscribeOnAmountReduceToTriggerUpdates() {
        sendAmountReduceListener.reduceToTriggerFlow
            .onEach { reduceTo ->
                _uiState.update(
                    AmountReduceToTransformer(
                        cryptoCurrencyStatus = cryptoCurrencyStatus,
                        minimumTransactionAmount = minAmountBoundary,
                        value = reduceTo,
                    ),
                )
                feeSelectorReloadTrigger.triggerUpdate()
            }
            .launchIn(modelScope)
    }

    private fun subscribeOnAmountReduceByTriggerUpdates() {
        sendAmountReduceListener.reduceByTriggerFlow
            .onEach { reduceByData ->
                _uiState.update(
                    AmountReduceByTransformer(
                        cryptoCurrencyStatus = cryptoCurrencyStatus,
                        minimumTransactionAmount = minAmountBoundary,
                        value = reduceByData,
                    ),
                )
                feeSelectorReloadTrigger.triggerUpdate()
            }
            .launchIn(modelScope)
    }

    private fun subscribeOnAmountIgnoreReduceTriggerUpdates() {
        sendAmountReduceListener.ignoreReduceTriggerFlow
            .onEach { _uiState.update(AmountIgnoreReduceTransformer::transform) }
            .launchIn(modelScope)
    }

    private fun subscribeOnAmountUpdateTriggerUpdates() {
        sendAmountUpdateListener.updateAmountTriggerFlow
            .onEach { (amount, isEnterInFiat) ->
                isEnterInFiat?.let { onCurrencyChangeClick(isEnterInFiat) }
                onAmountValueChange(amount)
                saveResult()
            }.launchIn(modelScope)
    }

    private fun saveResult() {
        val params = params as? SendAmountComponentParams.AmountParams ?: return
        val predefinedAmount = (params.predefinedValues as? PredefinedValues.Content.QrCode)?.amount
        val enteredAmount = (uiState.value as? AmountState.Data)?.amountTextField?.value
        params.callback.onAmountResult(
            amountUM = uiState.value,
            isResetPredefined = predefinedAmount != enteredAmount,
        )
    }

    private fun configAmountNavigation() {
        val params = params as? SendAmountComponentParams.AmountParams ?: return
        combine(
            flow = uiState,
            flow2 = params.currentRoute.filterIsInstance<CommonSendRoute.Amount>(),
            transform = { state, route -> state to route },
        ).onEach { (state, route) ->
            setSendWithSwapAvailability()
            params.callback.onNavigationResult(
                NavigationUM.Content(
                    title = resourceReference(R.string.send_amount_label),
                    subtitle = null,
                    backIconRes = if (route.isEditMode) {
                        R.drawable.ic_back_24
                    } else {
                        R.drawable.ic_close_24
                    },
                    backIconClick = params.callback::onBackClick,
                    primaryButton = NavigationButton(
                        textReference = if (route.isEditMode) {
                            resourceReference(R.string.common_continue)
                        } else {
                            resourceReference(R.string.common_next)
                        },
                        isEnabled = state.isPrimaryButtonEnabled,
                        onClick = {
                            onAmountNext()
                            params.callback.onNextClick()
                        },
                    ),
                ),
            )
        }.launchIn(modelScope)
    }

    private fun setSendWithSwapAvailability() {
        // Allowed only in multicurrency wallets
        val isMultiCurrency = userWallet?.isMultiCurrency == true

        // Allowed only on networks without tx extras (e.i. memo and destination tag)
        val isExtrasSupported = cryptoCurrencyStatus.currency.network.transactionExtrasType.isTxExtrasSupported()
        isSendWithSwapAvailable.update {
            isAvailableForSwap && isMultiCurrency && !isExtrasSupported
        }
    }
}