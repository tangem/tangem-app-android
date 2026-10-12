package com.tangem.features.polymarket.impl.placeprediction.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippagePickerUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal
import javax.inject.Inject

@ModelScoped
internal class PlacePredictionSlippageModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model(), PlacePredictionSlippageIntents {

    private val params = paramsContainer.require<PlacePredictionRoute.Slippage>()

    val uiState: StateFlow<SlippagePickerUM>
        field = MutableStateFlow(SlippagePickerUM.of(value = params.value.toPlainString()))

    override fun onSlippageValueChange(value: String) {
        uiState.value = SlippagePickerUM.of(value = value)
    }

    override fun onSlippagePresetClick(percent: BigDecimal) {
        onSlippageValueChange(value = percent.toPlainString())
    }

    override fun onSlippageConfirmClick() {
        val state = uiState.value
        if (!state.isConfirmEnabled) return
        val percent = state.value.parseBigDecimalOrNull() ?: return

        params.onConfirm(percent)
        router.pop()
    }

    override fun onBackClick() {
        router.pop()
    }
}