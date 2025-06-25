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
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.send.v2.feeselector.entity.FeeItem
import com.tangem.features.send.v2.feeselector.entity.FeeSelectorUM
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
) : Model() {

    private val params = paramsContainer.require<FeeSelectorParams.FeeSelectorBlockParams>()
    private var appCurrency: AppCurrency = AppCurrency.Default

    val uiState: StateFlow<FeeSelectorUM>
    field = MutableStateFlow<FeeSelectorUM>(FeeSelectorUM.Loading(onDone = ::onDone))

    init {
        initAppCurrency()
        loadFee()
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
                    ifLeft = { uiState.update(FeeSelectorErrorTransformer(it)) },
                    ifRight = {
                        uiState.update(
                            FeeSelectorLoadedTransformer(
                                cryptoCurrencyStatus = params.cryptoCurrencyStatus,
                                appCurrency = appCurrency,
                                fees = it,
                                suggestedFeeState = params.suggestedFeeState,
                                isFeeApproximate = isFeeApproximate(it.normal.amount.type),
                                onFeeSelected = ::onFeeItemSelected,
                                onCustomFeeValueChange = ::onCustomFeeValueChange,
                                onNextClick = ::onNextClick,
                            ),
                        )
                    },
                )
        }
    }

    private fun isFeeApproximate(amountType: AmountType): Boolean {
        val networkId = params.network.id
        return isFeeApproximateUseCase(networkId = networkId, amountType = amountType)
    }

    private fun onFeeItemSelected(feeItem: FeeItem) {
        uiState.update(FeeItemSelectedTransformer(feeItem))
    }

    @Suppress("UnusedPrivateMember")
    private fun onCustomFeeValueChange(index: Int, value: String) {
// [REDACTED_TODO_COMMENT]
    }

    private fun onNextClick() {
// [REDACTED_TODO_COMMENT]
    }

    private fun onDone() {
// [REDACTED_TODO_COMMENT]
    }
}
