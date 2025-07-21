package com.tangem.features.send.v2.feeselector.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.blockchain.common.AmountType
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.features.send.v2.api.callbacks.FeeSelectorModelCallback
import com.tangem.features.send.v2.api.entity.FeeItem
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.feeselector.FeeSelectorReloadListener
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.send.v2.feeselector.model.transformers.*
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class FeeSelectorModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val feeSelectorReloadListener: FeeSelectorReloadListener,
    private val isFeeApproximateUseCase: IsFeeApproximateUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model(), FeeSelectorIntents, FeeSelectorModelCallback {

    private val params = paramsContainer.require<FeeSelectorParams>()
    private var appCurrency: AppCurrency = AppCurrency.Default

    val feeSelectorBottomSheet = SlotNavigation<Unit>()

    val uiState: StateFlow<FeeSelectorUM>
    field = MutableStateFlow<FeeSelectorUM>(params.state)

    init {
        initAppCurrency()
        loadFee()
        listenReloadTrigger()
    }

    fun updateState(feeSelectorUM: FeeSelectorUM) {
        uiState.value = feeSelectorUM
    }

    private fun listenReloadTrigger() {
        feeSelectorReloadListener.reloadTriggerFlow
            .onEach { data ->
                if (data.removeSuggestedFee) {
                    uiState.update(FeeSelectorRemoveSuggestedTransformer)
                }
                loadFee()
            }
            .launchIn(modelScope)
    }

    private fun initAppCurrency() {
        modelScope.launch {
            appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }
        }
    }

    private fun loadFee() {
        uiState.update(FeeSelectorLoadingTransformers)
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
}