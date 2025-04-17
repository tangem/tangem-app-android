package com.tangem.features.send.v2.subcomponents.amount.model

import androidx.compose.runtime.Stable
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.amountScreen.converters.*
import com.tangem.common.ui.amountScreen.converters.field.AmountFieldChangeTransformer
import com.tangem.common.ui.amountScreen.converters.field.AmountFieldSetMaxAmountTransformer
import com.tangem.common.ui.amountScreen.models.AmountParameters
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.tokens.GetMinimumTransactionAmountSyncUseCase
import com.tangem.features.send.v2.common.NavigationUM
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.send.SendRoute
import com.tangem.features.send.v2.send.analytics.SendAnalyticEvents
import com.tangem.features.send.v2.send.analytics.SendAnalyticEvents.SendScreenSource
import com.tangem.features.send.v2.send.ui.state.ButtonsUM
import com.tangem.features.send.v2.subcomponents.amount.SendAmountComponentParams
import com.tangem.features.send.v2.subcomponents.amount.SendAmountReduceListener
import com.tangem.features.send.v2.subcomponents.amount.SendAmountUpdateQRListener
import com.tangem.features.send.v2.subcomponents.amount.analytics.SendAmountAnalyticEvents
import com.tangem.features.send.v2.subcomponents.amount.analytics.SendAmountAnalyticEvents.SelectedCurrencyType
import com.tangem.features.send.v2.subcomponents.fee.SendFeeData
import com.tangem.features.send.v2.subcomponents.fee.SendFeeReloadTrigger
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.orZero
import com.tangem.utils.isNullOrZero
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class SendAmountModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val appRouter: AppRouter,
    private val getMinimumTransactionAmountSyncUseCase: GetMinimumTransactionAmountSyncUseCase,
    private val sendAmountReduceListener: SendAmountReduceListener,
    private val feeReloadTrigger: SendFeeReloadTrigger,
    private val sendAmountUpdateQRListener: SendAmountUpdateQRListener,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model(), AmountScreenClickIntents {

    private val params: SendAmountComponentParams = paramsContainer.require()

    private val _uiState = MutableStateFlow(params.state)
    val uiState = _uiState.asStateFlow()

    private val analyticsCategoryName = params.analyticsCategoryName
    private val userWallet = params.userWallet
    private val cryptoCurrencyStatus = params.cryptoCurrencyStatus

    private var minAmountBoundary: EnterAmountBoundary? = null
    private var maxAmountBoundary: EnterAmountBoundary = MaxEnterAmountConverter().convert(cryptoCurrencyStatus)

    init {
        configAmountNavigation()
        initMinBoundary()
        subscribeOnAmountReduceByTriggerUpdates()
        subscribeOnAmountReduceToTriggerUpdates()
        subscribeOnAmountIgnoreReduceTriggerUpdates()
        subscribeOnAmountUpdateQRTriggerUpdates()
    }

    private fun initMinBoundary() {
        modelScope.launch {
            minAmountBoundary = getMinimumTransactionAmountSyncUseCase(
                userWalletId = userWallet.walletId,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
            ).getOrNull()?.let {
                EnterAmountBoundary(
                    amount = it,
                    fiatRate = cryptoCurrencyStatus.value.fiatRate.orZero(),
                )
            }

            initialState()
        }
    }

    private fun initialState() {
        if (uiState.value is AmountState.Empty) {
            _uiState.update {
                AmountStateConverterV2(
                    clickIntents = this,
                    appCurrency = params.appCurrency,
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    maxEnterAmount = maxAmountBoundary,
                    iconStateConverter = CryptoCurrencyToIconStateConverter(),
                ).convert(
                    AmountParameters(
                        title = stringReference(userWallet.name),
                        value = "",
                    ),
                )
            }
        }
        params.predefinedAmountValue?.let(::onAmountValueChange)
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
            SendAmountAnalyticEvents.MaxAmountButtonClicked(categoryName = analyticsCategoryName),
        )
    }

    override fun onAmountPasteTriggerDismiss() {
        _uiState.update(AmountPastedTriggerDismissTransformer)
    }

    override fun onAmountNext() {
        (uiState.value as? AmountState.Data)?.amountTextField?.isFiatValue?.let { isFiatSelected ->
            analyticsEventHandler.send(
                SendAmountAnalyticEvents.SelectedCurrency(
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
        if ((params as? SendAmountComponentParams.AmountParams)?.isEditMode == true) {
            router.pop()
        } else {
            router.push(SendRoute.Confirm)
        }
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
                feeReloadTrigger.triggerUpdate(
                    feeData = SendFeeData(
                        amount = (uiState.value as? AmountState.Data)?.amountTextField?.cryptoAmount?.value,
                    ),
                )
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
                feeReloadTrigger.triggerUpdate(
                    feeData = SendFeeData(
                        amount = (uiState.value as? AmountState.Data)?.amountTextField?.cryptoAmount?.value,
                    ),
                )
            }
            .launchIn(modelScope)
    }

    private fun subscribeOnAmountIgnoreReduceTriggerUpdates() {
        sendAmountReduceListener.ignoreReduceTriggerFlow
            .onEach { _uiState.update(AmountIgnoreReduceTransformer::transform) }
            .launchIn(modelScope)
    }

    private fun subscribeOnAmountUpdateQRTriggerUpdates() {
        sendAmountUpdateQRListener.updateAmountTriggerFlow
            .onEach { amount ->
                onAmountValueChange(amount)
                saveResult()
            }.launchIn(modelScope)
    }

    private fun saveResult() {
        val params = params as? SendAmountComponentParams.AmountParams ?: return
        params.callback.onAmountResult(uiState.value)
    }

    private fun configAmountNavigation() {
        val params = params as? SendAmountComponentParams.AmountParams ?: return
        combine(
            flow = uiState,
            flow2 = params.currentRoute,
            transform = { state, route -> state to route },
        ).onEach { (state, route) ->
            params.callback.onNavigationResult(
                NavigationUM.Content(
                    title = resourceReference(R.string.send_amount_label),
                    subtitle = null,
                    backIconRes = if (route.isEditMode) {
                        R.drawable.ic_back_24
                    } else {
                        R.drawable.ic_close_24
                    },
                    backIconClick = {
                        if (route.isEditMode) {
                            router.pop()
                        } else {
                            analyticsEventHandler.send(
                                SendAnalyticEvents.CloseButtonClicked(
                                    source = SendScreenSource.Amount,
                                    isFromSummary = false,
                                    isValid = state.isPrimaryButtonEnabled,
                                ),
                            )
                            appRouter.pop()
                        }
                    },
                    primaryButton = ButtonsUM.PrimaryButtonUM(
                        text = if (route.isEditMode) {
                            resourceReference(R.string.common_continue)
                        } else {
                            resourceReference(R.string.common_next)
                        },
                        isEnabled = state.isPrimaryButtonEnabled,
                        onClick = ::onAmountNext,
                    ),
                    prevButton = ButtonsUM.PrimaryButtonUM(
                        text = TextReference.EMPTY,
                        iconResId = R.drawable.ic_back_24,
                        isEnabled = true,
                        onClick = {
                            saveResult()
                            router.pop()
                        },
                    ).takeIf { route.isEditMode.not() },
                    secondaryPairButtonsUM = null,
                ),
            )
        }.launchIn(modelScope)
    }
}