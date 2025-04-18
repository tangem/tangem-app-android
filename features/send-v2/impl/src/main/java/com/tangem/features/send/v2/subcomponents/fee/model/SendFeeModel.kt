package com.tangem.features.send.v2.subcomponents.fee.model

import androidx.compose.runtime.Stable
import com.tangem.blockchain.common.AmountType
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.transaction.usecase.GetTransferFeeUseCase
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.features.send.v2.common.ui.state.NavigationUM
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.send.ui.state.ButtonsUM
import com.tangem.features.send.v2.subcomponents.fee.SendFeeCheckReloadListener
import com.tangem.features.send.v2.subcomponents.fee.SendFeeCheckReloadTrigger
import com.tangem.features.send.v2.subcomponents.fee.SendFeeComponentParams
import com.tangem.features.send.v2.subcomponents.fee.SendFeeReloadListener
import com.tangem.features.send.v2.subcomponents.fee.analytics.SendFeeAnalyticEvents
import com.tangem.features.send.v2.subcomponents.fee.analytics.SendFeeAnalyticEvents.GasPriceInserter
import com.tangem.features.send.v2.subcomponents.fee.model.transformers.*
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeType
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Locale
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class SendFeeModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val isFeeApproximateUseCase: IsFeeApproximateUseCase,
    private val getTransferFeeUseCase: GetTransferFeeUseCase,
    private val urlOpener: UrlOpener,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val sendFeeAlertFactory: SendFeeAlertFactory,
    private val feeReloadListener: SendFeeReloadListener,
    private val feeCheckReloadListener: SendFeeCheckReloadListener,
    private val feeCheckReloadTrigger: SendFeeCheckReloadTrigger,
) : Model(), SendFeeClickIntents {

    private val params: SendFeeComponentParams = paramsContainer.require()

    private val _uiState = MutableStateFlow(params.state)
    val uiState = _uiState.asStateFlow()

    private val analyticsCategoryName = params.analyticsCategoryName
    private val appCurrency = params.appCurrency
    private val cryptoCurrencyStatus = params.cryptoCurrencyStatus
    private val feeCryptoCurrencyStatus = params.feeCryptoCurrencyStatus

    private var feeJobHolder = JobHolder()

    init {
        configFeeNavigation()
        subscribeOnFeeReloadTriggerUpdates()
        subscribeOnFeeCheckReloadTriggerUpdates()
        initialState()
        feeReload()
    }

    fun updateState(state: FeeUM) {
        _uiState.value = state
    }

    override fun feeReload() {
        loadFee(
            amountValue = params.sendAmount,
            destinationAddress = params.destinationAddress,
        )
    }

    override fun onFeeSelectorClick(feeType: FeeType) {
        _uiState.update(
            SendFeeSelectTransformer(
                feeType = feeType,
                clickIntents = this@SendFeeModel,
                appCurrency = appCurrency,
                feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
            ),
        )
        updateFeeNotifications()
        if (feeType == FeeType.Custom) {
            analyticsEventHandler.send(
                SendFeeAnalyticEvents.CustomFeeButtonClicked(categoryName = analyticsCategoryName),
            )
        }
    }

    override fun onCustomFeeValueChange(index: Int, value: String) {
        _uiState.update(
            SendFeeCustomValueChangeTransformer(
                index = index,
                value = value,
                clickIntents = this@SendFeeModel,
                appCurrency = appCurrency,
                feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
            ),
        )
        updateFeeNotifications()
    }

    override fun onReadMoreClick() {
        val locale = if (Locale.getDefault().language == RU_LOCALE) RU_LOCALE else EN_LOCALE
        val url = buildString {
            append(FEE_READ_MORE_URL_FIRST_PART)
            append(locale)
            append(FEE_READ_MORE_URL_SECOND_PART)
        }
        urlOpener.openUrl(url)
    }

    override fun onNextClick() {
        val feeUM = uiState.value as? FeeUM.Content
        val feeSelectorUM = feeUM?.feeSelectorUM as? FeeSelectorUM.Content

        if (feeSelectorUM != null) {
            sendFeeAlertFactory.checkAndShowAlerts(feeSelectorUM) {
                val isCustomFeeEdited =
                    feeSelectorUM.selectedFee?.amount?.value != feeSelectorUM.fees.normal.amount.value
                if (feeSelectorUM.selectedType == FeeType.Custom && isCustomFeeEdited) {
                    analyticsEventHandler.send(GasPriceInserter(categoryName = analyticsCategoryName))
                }
                analyticsEventHandler.send(
                    SendFeeAnalyticEvents.SelectedFee(
                        categoryName = analyticsCategoryName,
                        feeType = feeSelectorUM.selectedType.toAnalyticType(feeSelectorUM),
                    ),
                )

                saveResult()
            }
        } else {
            saveResult()
        }
    }

    private fun initialState() {
        if (uiState.value is FeeUM.Empty) {
            _uiState.update(
                SendFeeInitialStateTransformer(
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                    appCurrency = appCurrency,
                ),
            )
        }
    }

    private fun subscribeOnFeeReloadTriggerUpdates() {
        feeReloadListener.reloadTriggerFlow
            .onEach { (amount, destination) ->
                loadFee(
                    amountValue = amount ?: params.sendAmount,
                    destinationAddress = destination ?: params.destinationAddress,
                )
            }
            .launchIn(modelScope)
    }

    private fun subscribeOnFeeCheckReloadTriggerUpdates() {
        feeCheckReloadListener.checkReloadTriggerFlow
            .onEach { checkLoadFee() }
            .launchIn(modelScope)
    }

    private fun saveResult() {
        _uiState.update(
            SendFeeCustomAutoFixTransformer(
                clickIntents = this@SendFeeModel,
                appCurrency = appCurrency,
                feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
            ),
        )
        val params = params as? SendFeeComponentParams.FeeParams ?: return
        params.callback.onFeeResult(uiState.value)
    }

    private fun loadFee(amountValue: BigDecimal, destinationAddress: String) {
        modelScope.launch {
            val isShowLoading = (uiState.value as? FeeUM.Content)?.feeSelectorUM !is FeeSelectorUM.Content
            if (isShowLoading) {
                _uiState.update(SendFeeLoadingTransformer)
            }
            getTransferFeeUseCase.invoke(
                amount = amountValue,
                destination = destinationAddress,
                userWallet = params.userWallet,
                cryptoCurrency = cryptoCurrencyStatus.currency,
            ).fold(
                ifRight = {
                    _uiState.update(
                        SendFeeLoadedTransformer(
                            fees = it,
                            clickIntents = this@SendFeeModel,
                            appCurrency = appCurrency,
                            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                            isFeeApproximate = isFeeApproximate(it.normal.amount.type),
                        ),
                    )
                    updateFeeNotifications()
                },
                ifLeft = { feeError ->
                    if (isShowLoading) {
                        _uiState.update(SendFeeFailedTransformer(feeError))
                    }
                    updateFeeNotifications()
                },
            )
        }.saveIn(feeJobHolder)
    }

    private fun checkLoadFee() {
        modelScope.launch {
            getTransferFeeUseCase.invoke(
                amount = params.sendAmount,
                destination = params.destinationAddress,
                userWallet = params.userWallet,
                cryptoCurrency = cryptoCurrencyStatus.currency,
            ).fold(
                ifRight = {
                    sendFeeAlertFactory.getFeeUpdatedAlert(
                        newFee = it,
                        feeUM = uiState.value,
                        proceedAction = {
                            modelScope.launch {
                                feeCheckReloadTrigger.callbackCheckResult(true)
                            }
                        },
                        stopAction = {
                            modelScope.launch {
                                feeCheckReloadTrigger.callbackCheckResult(false)
                            }
                        },
                    )
                    _uiState.update(
                        SendFeeLoadedTransformer(
                            fees = it,
                            clickIntents = this@SendFeeModel,
                            appCurrency = appCurrency,
                            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                            isFeeApproximate = isFeeApproximate(it.normal.amount.type),
                        ),
                    )
                    updateFeeNotifications()
                },
                ifLeft = { feeError ->
                    feeCheckReloadTrigger.callbackCheckResult(false)
                    _uiState.update(SendFeeFailedTransformer(feeError))
                    sendFeeAlertFactory.getFeeUnreachableErrorState(::feeReload)
                    updateFeeNotifications()
                },
            )
        }.saveIn(feeJobHolder)
    }

    private fun isFeeApproximate(amountType: AmountType): Boolean {
        val networkId = feeCryptoCurrencyStatus.currency.network.id
        return isFeeApproximateUseCase(
            networkId = networkId,
            amountType = amountType,
        )
    }

    private fun updateFeeNotifications() {
        _uiState.update(
            SendFeeNotificationsTransformer(
                cryptoCurrencyName = cryptoCurrencyStatus.currency.name,
                onFeeReload = ::feeReload,
            ),
        )
    }

    private fun configFeeNavigation() {
        val params = params as? SendFeeComponentParams.FeeParams ?: return
        combine(
            flow = uiState,
            flow2 = params.currentRoute,
            transform = { state, route -> state to route },
        ).onEach { (state, _) ->
            params.callback.onNavigationResult(
                NavigationUM.Content(
                    title = resourceReference(R.string.common_fee_selector_title),
                    subtitle = null,
                    backIconRes = R.drawable.ic_back_24,
                    backIconClick = params.onNextClick,
                    primaryButton = ButtonsUM.PrimaryButtonUM(
                        text = resourceReference(R.string.common_continue),
                        isEnabled = state.isPrimaryButtonEnabled,
                        onClick = {
                            onNextClick()
                            params.onNextClick()
                        },
                    ),
                    prevButton = null,
                    secondaryPairButtonsUM = null,
                ),
            )
        }.launchIn(modelScope)
    }

    private companion object {
        const val RU_LOCALE = "ru"
        const val EN_LOCALE = "en"
        const val FEE_READ_MORE_URL_FIRST_PART = "https://tangem.com/"
        const val FEE_READ_MORE_URL_SECOND_PART = "/blog/post/what-is-a-transaction-fee-and-why-do-we-need-it/"
    }
}