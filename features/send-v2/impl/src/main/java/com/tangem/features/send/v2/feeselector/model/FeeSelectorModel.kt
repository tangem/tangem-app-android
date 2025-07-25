package com.tangem.features.send.v2.feeselector.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.blockchain.common.AmountType
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.features.send.v2.api.callbacks.FeeSelectorModelCallback
import com.tangem.features.send.v2.api.entity.FeeItem
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorCheckReloadListener
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorCheckReloadTrigger
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorReloadListener
import com.tangem.features.send.v2.feeselector.model.transformers.*
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class FeeSelectorModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val urlOpener: UrlOpener,
    private val isFeeApproximateUseCase: IsFeeApproximateUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val feeSelectorReloadListener: FeeSelectorReloadListener,
    private val feeSelectorCheckReloadListener: FeeSelectorCheckReloadListener,
    private val feeSelectorCheckReloadTrigger: FeeSelectorCheckReloadTrigger,
    private val feeSelectorAlertFactory: FeeSelectorAlertFactory,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model(), FeeSelectorIntents, FeeSelectorModelCallback {

    private val params = paramsContainer.require<FeeSelectorParams>()
    private var appCurrency: AppCurrency = AppCurrency.Default

    val feeSelectorBottomSheet = SlotNavigation<Unit>()

    val uiState: StateFlow<FeeSelectorUM>
    field = MutableStateFlow<FeeSelectorUM>(params.state)

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

    fun onReadMoreClicked() {
        val locale = if (Locale.getDefault().language == RU_LOCALE) RU_LOCALE else EN_LOCALE
        val url = buildString {
            append(FEE_READ_MORE_URL_FIRST_PART)
            append(locale)
            append(FEE_READ_MORE_URL_SECOND_PART)
        }
        urlOpener.openUrl(url)
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
                                suggestedFeeState = params.suggestedFeeState,
                                isFeeApproximate = isFeeApproximate(fee.normal.amount.type),
                                feeSelectorIntents = this@FeeSelectorModel,
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
        uiState.update(FeeItemSelectedTransformer(feeItem))
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
        (params as? FeeSelectorParams.FeeSelectorDetailsParams)?.callback?.onFeeResult(uiState.value)
    }

    override fun onFeeResult(feeSelectorUM: FeeSelectorUM) {
        uiState.value = feeSelectorUM
        feeSelectorBottomSheet.dismiss()
    }

    private fun subscribeOnFeeReloadTriggerUpdates() {
        feeSelectorReloadListener.reloadTriggerFlow
            .onEach { data ->
                if (data.removeSuggestedFee) {
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
                ifRight = {
                    feeSelectorAlertFactory.getFeeUpdatedAlert(
                        newFee = it,
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

    private companion object {
        const val RU_LOCALE = "ru"
        const val EN_LOCALE = "en"
        const val FEE_READ_MORE_URL_FIRST_PART = "https://tangem.com/"
        const val FEE_READ_MORE_URL_SECOND_PART = "/blog/post/what-is-a-transaction-fee-and-why-do-we-need-it/"
    }
}