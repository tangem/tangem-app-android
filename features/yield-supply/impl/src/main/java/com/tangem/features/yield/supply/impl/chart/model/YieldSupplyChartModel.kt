package com.tangem.features.yield.supply.impl.chart.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartUM
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyMarketChartDataUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Suppress("UnusedPrivateProperty")
@ModelScoped
internal class YieldSupplyChartModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    data class Params(
        val tokenContractAddress: String,
        val onRetry: () -> Unit = {},
    )

    private val params: Params = paramsContainer.require()

    val uiState: StateFlow<YieldSupplyChartUM>
        field = MutableStateFlow<YieldSupplyChartUM>(YieldSupplyChartUM.Loading)

    init {
        // TODO [REDACTED_TASK_KEY]
        val chartData = YieldSupplyMarketChartDataUM.mock()
        uiState.update { YieldSupplyChartUM.Data(chartData = chartData) }
    }
}