package com.tangem.features.yield.supply.impl.chart.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.yield.supply.models.YieldSupplyMarketChartData
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetChartUseCase
import com.tangem.features.yield.supply.impl.chart.DefaultYieldSupplyChartComponent
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartUM
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyMarketChartDataUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@ModelScoped
internal class YieldSupplyChartModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val yieldSupplyGetChartUseCase: YieldSupplyGetChartUseCase,
) : Model() {

    private val params: DefaultYieldSupplyChartComponent.Params = paramsContainer.require()

    val uiState: StateFlow<YieldSupplyChartUM>
        field = MutableStateFlow<YieldSupplyChartUM>(YieldSupplyChartUM.Loading)

    init {
        loadChart()
    }

    private fun loadChart() {
        modelScope.launch(dispatchers.default) {
            params.callback?.onStartLoading()
            yieldSupplyGetChartUseCase(params.cryptoCurrency).onRight { chartData ->
                if (chartData.y.isEmpty()) {
                    uiState.update {
                        YieldSupplyChartUM.Error({
                            uiState.update { YieldSupplyChartUM.Loading }
                            loadChart()
                        })
                    }
                    params.callback?.onLoadFail()
                } else {
                    uiState.update {
                        YieldSupplyChartUM.Data(
                            chartData = YieldSupplyMarketChartDataUM(
                                y = chartData.y.toImmutableList(),
                                x = chartData.x.toImmutableList(),
                                avr = chartData.avr,
                                percentFormat = getPercentFormatPattern(chartData),
                            ),
                            monthLables = lastMonthLabels().toImmutableList(),
                        )
                    }
                    params.callback?.onSuccessLoad()
                }
            }.onLeft {
                uiState.update {
                    YieldSupplyChartUM.Error({
                        uiState.update { YieldSupplyChartUM.Loading }
                        loadChart()
                    })
                }
                params.callback?.onLoadFail()
            }
        }
    }

    @Suppress("MagicNumber")
    private fun getPercentFormatPattern(chartData: YieldSupplyMarketChartData): String {
        return when {
            chartData.y.all { it < 1.0 } -> "%.1f"
            chartData.y.all { it < 0.1 } -> "%.2f"
            else -> "%.0f"
        }
    }
    private fun lastMonthLabels(n: Int = MONTH_LABELS_COUNT, locale: Locale = Locale.getDefault()): List<String> {
        val calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("MMM", locale)

        return (n downTo 1).map {
            calendar.add(Calendar.MONTH, -1)
            formatter.format(calendar.time)
        }.reversed()
    }

    companion object {
        private const val MONTH_LABELS_COUNT = 5
    }
}