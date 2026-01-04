package com.tangem.features.send.v2.feeselector.model

import arrow.core.getOrElse
import com.tangem.blockchain.common.AmountType
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents.NonceInserted
import com.tangem.features.send.v2.api.entity.FeeItem
import com.tangem.features.send.v2.api.entity.FeeNonce
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorCheckReloadListener
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorCheckReloadTrigger
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorReloadListener
import com.tangem.features.send.v2.api.subcomponents.feeSelector.analytics.CommonSendFeeAnalyticEvents
import com.tangem.features.send.v2.api.subcomponents.feeSelector.analytics.CommonSendFeeAnalyticEvents.GasPriceInserter
import com.tangem.features.send.v2.feeselector.model.transformers.*
import com.tangem.utils.transformer.update
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
internal class FeeSelectorLogic @AssistedInject constructor(
    @Assisted private val params: FeeSelectorParams,
    @Assisted private val modelScope: CoroutineScope,
    private val isFeeApproximateUseCase: IsFeeApproximateUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val feeSelectorReloadListener: FeeSelectorReloadListener,
    private val feeSelectorCheckReloadListener: FeeSelectorCheckReloadListener,
    private val feeSelectorCheckReloadTrigger: FeeSelectorCheckReloadTrigger,
    private val feeSelectorAlertFactory: FeeSelectorAlertFactory,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : FeeSelectorIntents {

    private var appCurrency: AppCurrency = AppCurrency.Default
    val uiState = MutableStateFlow<FeeSelectorUM>(params.state)

    init {
        initAppCurrency()
        subscribeOnFeeReloadTriggerUpdates()
        subscribeOnFeeCheckReloadTriggerUpdates()
        subscribeOnFeeLoadingStateTriggerUpdates()
        loadFee()
    }

    fun updateState(feeSelectorUM: FeeSelectorUM) {
        uiState.value = feeSelectorUM
    }

    private fun initAppCurrency() {
        modelScope.launch {
            appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }
        }
    }

    private fun loadFee() {
        if (uiState.value !is FeeSelectorUM.Content) {
            uiState.update(FeeSelectorLoadingTransformer)
        }
        modelScope.launch {
            params.onLoadFee()
                .fold(
                    ifLeft = { error -> uiState.update(FeeSelectorErrorTransformer(error)) },
                    ifRight = { fee ->
                        uiState.update(
                            FeeSelectorLoadedTransformer(
                                cryptoCurrencyStatus = params.cryptoCurrencyStatus,
                                feeCryptoCurrencyStatus = params.feeCryptoCurrencyStatus,
                                appCurrency = appCurrency,
                                fees = fee,
                                feeStateConfiguration = params.feeStateConfiguration,
                                isFeeApproximate = isFeeApproximate(fee.normal.amount.type),
                                feeSelectorIntents = this@FeeSelectorLogic,
                            ),
                        )
                    },
                )
        }
    }

    private fun isFeeApproximate(amountType: AmountType): Boolean {
        val networkId = params.feeCryptoCurrencyStatus.currency.network.id
        return isFeeApproximateUseCase(networkId = networkId, amountType = amountType)
    }

    override fun onFeeItemSelected(feeItem: FeeItem) {
        if (feeItem is FeeItem.Custom) {
            analyticsEventHandler.send(
                CommonSendFeeAnalyticEvents.CustomFeeButtonClicked(categoryName = params.analyticsCategoryName),
            )
        }
        uiState.update(FeeItemSelectedTransformer(feeItem))
        if (feeItem !is FeeItem.Custom) {
            onDoneClick()
        }
    }

    override fun onCustomFeeValueChange(index: Int, value: String) {
        uiState.update(
            FeeSelectorCustomValueChangedTransformer(
                index = index,
                value = value,
                intents = this,
                appCurrency = appCurrency,
                feeCryptoCurrencyStatus = params.feeCryptoCurrencyStatus,
            ),
        )
    }

    override fun onNonceChange(value: String) {
        uiState.update(FeeSelectorNonceChangeTransformer(value = value))
    }

    override fun onDoneClick() {
        val feeSelectorUM = uiState.value as? FeeSelectorUM.Content ?: return
        analyticsEventHandler.send(
            CommonSendFeeAnalyticEvents.SelectedFee(
                categoryName = params.analyticsCategoryName,
                feeType = feeSelectorUM.toAnalyticType(),
                source = params.analyticsSendSource,
            ),
        )
        val isCustomFeeEdited = feeSelectorUM.selectedFeeItem.fee.amount.value != feeSelectorUM.fees.normal.amount.value
        if (feeSelectorUM.selectedFeeItem is FeeItem.Custom && isCustomFeeEdited) {
            analyticsEventHandler.send(GasPriceInserter(categoryName = params.analyticsCategoryName))
        }
        if (feeSelectorUM.feeNonce is FeeNonce.Nonce) {
            analyticsEventHandler.send(
                NonceInserted(
                    categoryName = params.analyticsCategoryName,
                    token = params.feeCryptoCurrencyStatus.currency.symbol,
                    blockchain = params.feeCryptoCurrencyStatus.currency.network.name,
                ),
            )
        }

        (params as? FeeSelectorParams.FeeSelectorDetailsParams)?.callback?.onFeeResult(uiState.value)
    }

    private fun subscribeOnFeeReloadTriggerUpdates() {
        feeSelectorReloadListener.reloadTriggerFlow
            .onEach { data ->
                if (data.isRemoveSuggestedFee) {
                    uiState.update(FeeSelectorRemoveSuggestedTransformer)
                }
                loadFee()
            }
            .launchIn(modelScope)
    }

    private fun subscribeOnFeeLoadingStateTriggerUpdates() {
        feeSelectorReloadListener.loadingStateTriggerFlow
            .onEach { uiState.update(FeeSelectorLoadingTransformer) }
            .launchIn(modelScope)
    }

    private fun subscribeOnFeeCheckReloadTriggerUpdates() {
        feeSelectorCheckReloadListener.checkReloadTriggerFlow
            .onEach { checkLoadFee() }
            .launchIn(modelScope)
    }

    private fun checkLoadFee() {
        modelScope.launch {
            params.onLoadFee().fold(
                ifRight = { newFee ->
                    feeSelectorAlertFactory.getFeeUpdatedAlert(
                        newTransactionFee = newFee,
                        feeSelectorUM = uiState.value,
                        proceedAction = {
                            modelScope.launch {
                                feeSelectorCheckReloadTrigger.callbackCheckResult(true)
                            }
                        },
                        stopAction = {
                            modelScope.launch {
                                feeSelectorCheckReloadTrigger.callbackCheckResult(false)
                            }
                        },
                    )
                },
                ifLeft = { feeError ->
                    feeSelectorCheckReloadTrigger.callbackCheckResult(false)
                    feeSelectorAlertFactory.getFeeUnreachableErrorState(::loadFee)
                },
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(params: FeeSelectorParams, modelScope: CoroutineScope): FeeSelectorLogic
    }
}