package com.tangem.features.send.v2.feeselector.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.blockchain.common.AmountType
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.features.send.v2.api.entity.FeeItem
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.send.v2.feeselector.model.transformers.FeeItemSelectedTransformer
import com.tangem.features.send.v2.feeselector.model.transformers.FeeSelectorErrorTransformer
import com.tangem.features.send.v2.feeselector.model.transformers.FeeSelectorLoadedTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class FeeSelectorModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val isFeeApproximateUseCase: IsFeeApproximateUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model(), FeeSelectorIntents {

    private val params = paramsContainer.require<FeeSelectorParams>()
    private var appCurrency: AppCurrency = AppCurrency.Default

    val uiState: StateFlow<FeeSelectorUM>
    field = MutableStateFlow<FeeSelectorUM>(params.state)

    init {
        initAppCurrency()
        loadFee()
    }

    fun updateState(feeSelectorUM: FeeSelectorUM) {
        uiState.value = feeSelectorUM
    }

    fun dismiss() {
        router.pop()
    }

    private fun initAppCurrency() {
        modelScope.launch {
            appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }
        }
    }

    private fun loadFee() {
        modelScope.launch {
            params.onLoadFee()
                .fold(
                    ifLeft = { error -> uiState.update(FeeSelectorErrorTransformer(error)) },
                    ifRight = { fee ->
                        uiState.update(
                            FeeSelectorLoadedTransformer(
                                cryptoCurrencyStatus = params.cryptoCurrencyStatus,
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
        val networkId = params.cryptoCurrencyStatus.currency.network.id
        return isFeeApproximateUseCase(networkId = networkId, amountType = amountType)
    }

    override fun onFeeItemSelected(feeItem: FeeItem) {
        uiState.update(FeeItemSelectedTransformer(feeItem))
    }

    override fun onCustomFeeValueChange(index: Int, value: String) {
        // TODO: [REDACTED_JIRA]
    }

    override fun onCustomFeeNextClick() {
        // TODO: [REDACTED_JIRA]
    }

    override fun onDoneClick() {
        params.callback.onFeeResult(uiState.value)
        dismiss()
    }
}