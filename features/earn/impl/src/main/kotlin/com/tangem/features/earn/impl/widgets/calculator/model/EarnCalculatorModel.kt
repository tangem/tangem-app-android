package com.tangem.features.earn.impl.widgets.calculator.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.impl.widgets.calculator.EarnCalculatorComponent
import com.tangem.features.earn.impl.widgets.calculator.ui.state.EarnCalculatorUM
import com.tangem.features.earn.product.api.product.EarnHorizon
import com.tangem.features.earn.product.api.product.EarnProjection
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class EarnCalculatorModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    getSelectedAppCurrency: GetSelectedAppCurrencyUseCase,
    private val converter: EarnCalculatorConverter,
) : Model() {

    private val params: EarnCalculatorComponent.Params = paramsContainer.require()

    private val input = MutableStateFlow(CalculatorInput.Default)

    val uiState: StateFlow<EarnCalculatorUM?>
        field = MutableStateFlow<EarnCalculatorUM?>(null)

    init {
        combine(
            flow = params.projection,
            flow2 = params.incentiveRate,
            flow3 = input,
            flow4 = getSelectedAppCurrency.invokeOrDefault(),
        ) { projection, incentiveRate, current, currency ->
            projection?.let {
                createUiState(projection = it, incentiveRate = incentiveRate, current = current, currency = currency)
            }
        }
            .flowOn(dispatchers.default)
            .onEach { uiState.value = it }
            .launchIn(modelScope)
    }

    fun onInitialStep(direction: Int) {
        input.update { it.stepInitial(direction) }
    }

    fun onTopUpStep(direction: Int) {
        input.update { it.stepMonthlyTopUp(direction) }
    }

    fun onHorizonSelect(horizon: EarnHorizon) {
        input.update { it.copy(horizon = horizon) }
    }

    private fun createUiState(
        projection: EarnProjection,
        incentiveRate: EarnProductRate?,
        current: CalculatorInput,
        currency: AppCurrency,
    ) = converter.convert(
        result = projection.project(current.initialFiat, current.monthlyTopUpFiat, current.horizon),
        input = current,
        appCurrency = currency,
        incentiveRate = incentiveRate,
        onInitialStep = ::onInitialStep,
        onTopUpStep = ::onTopUpStep,
        onHorizonSelect = ::onHorizonSelect,
    )
}