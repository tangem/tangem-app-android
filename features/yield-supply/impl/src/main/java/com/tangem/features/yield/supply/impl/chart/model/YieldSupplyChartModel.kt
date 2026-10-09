package com.tangem.features.yield.supply.impl.chart.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.incentives.YieldAprHistory
import com.tangem.domain.yield.supply.usecase.GetYieldAprHistoryUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetChartUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetTokenStatusUseCase
import com.tangem.features.yield.supply.impl.common.formatter.formatApy
import com.tangem.features.yield.supply.impl.chart.DefaultYieldSupplyChartComponent
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartIncentiveUM
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartPeriod
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartUM
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyMarketChartDataUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Month
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@ModelScoped
internal class YieldSupplyChartModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val yieldSupplyGetChartUseCase: YieldSupplyGetChartUseCase,
    private val yieldSupplyGetTokenStatusUseCase: YieldSupplyGetTokenStatusUseCase,
    private val getYieldAprHistoryUseCase: GetYieldAprHistoryUseCase,
) : Model() {

    private val params: DefaultYieldSupplyChartComponent.Params = paramsContainer.require()

    val uiState: StateFlow<YieldSupplyChartUM>
        field = MutableStateFlow<YieldSupplyChartUM>(YieldSupplyChartUM.Loading)

    private var selectedPeriod = params.initialPeriod

    /** A period switch supersedes the request in flight: a late answer for the old window is no longer an answer. */
    private var loadJob: Job? = null

    init {
        load()
    }

    private fun load() {
        // The window is read once here and carried down, so an answer can only ever describe the window it was
        // asked for, whatever the picker has moved on to since.
        val period = selectedPeriod

        loadJob?.cancel()
        loadJob = modelScope.launch(dispatchers.default) {
            params.callback?.onStartLoading()

            val market = yieldSupplyGetTokenStatusUseCase(params.cryptoCurrency).getOrNull()
            val campaign = market?.campaign?.takeIf { it.hasActiveCampaign }

            if (market != null) {
                // The plain sheet plots the Aave history, so it must not headline the rate the campaign adds to it.
                // Without a market there is nothing better to say than the rate the sheet was opened with.
                val headlineApy = if (params.isPlainRate) market.apy else market.effectiveApy
                params.callback?.onRateResolved(headlineApy.formatApy())
            }

            if (campaign == null || params.isPlainRate) {
                loadSupplyChart()
            } else {
                loadIncentiveChart(market = market, period = period)
            }
        }
    }

    private suspend fun loadSupplyChart() {
        yieldSupplyGetChartUseCase(params.cryptoCurrency)
            .onRight { chartData ->
                if (chartData.y.isEmpty()) {
                    showError()
                } else {
                    uiState.update {
                        YieldSupplyChartUM.Data(
                            chartData = YieldSupplyMarketChartDataUM(
                                y = chartData.y.toImmutableList(),
                                x = chartData.x.toImmutableList(),
                                avr = chartData.avr,
                                percentFormat = getPercentFormatPattern(chartData.y),
                            ),
                            monthLables = lastMonthLabels().toImmutableList(),
                        )
                    }
                    params.callback?.onIncentiveChanged(null)
                    params.callback?.onSuccessLoad()
                }
            }
            .onLeft { showError() }
    }

    private suspend fun loadIncentiveChart(market: YieldMarketToken, period: YieldSupplyChartPeriod) {
        params.callback?.onIncentiveChanged(incentive(averageApy = null, period = period))

        getYieldAprHistoryUseCase(
            chainId = market.chainId,
            tokenAddress = market.tokenAddress,
            days = period.days,
        )
            .onRight { history ->
                if (history.points.isEmpty()) {
                    showError()
                } else {
                    val state = toIncentiveState(history = history, period = period)
                    uiState.update { state }
                    params.callback?.onIncentiveChanged(state.incentive)
                    params.callback?.onSuccessLoad()
                }
            }
            .onLeft { showError() }
    }

    private fun toIncentiveState(history: YieldAprHistory, period: YieldSupplyChartPeriod): YieldSupplyChartUM.Data {
        val totals = history.points.map { it.totalApyEst.toDouble() }
        // The bar is the backend total: a reward cap is only what the total leaves above the base, and the
        // reported rewards APR is not a third opinion to compose the rate from.
        val caps = history.points.map { (it.totalApyEst - it.baseApy).coerceAtLeast(BigDecimal.ZERO) }
        val bases = history.points.mapIndexed { index, point -> (point.totalApyEst - caps[index]).toDouble() }
        val average = history.points
            .map(YieldAprHistory.Point::totalApyEst)
            .reduce(BigDecimal::add)
            .divide(history.points.size.toBigDecimal(), AVERAGE_GUARD_SCALE, RoundingMode.HALF_UP)

        return YieldSupplyChartUM.Data(
            chartData = YieldSupplyMarketChartDataUM(
                y = bases.toImmutableList(),
                x = bases.indices.map(Int::toDouble).toImmutableList(),
                avr = average.toDouble(),
                percentFormat = getPercentFormatPattern(totals),
                rewardCaps = caps.map(BigDecimal::toDouble).toImmutableList(),
                xLabels = axisLabels(points = history.points, period = period).toImmutableList(),
            ),
            monthLables = persistentListOf(),
            incentive = incentive(averageApy = average, period = period),
        )
    }

    private fun incentive(averageApy: BigDecimal?, period: YieldSupplyChartPeriod) = YieldSupplyChartIncentiveUM(
        period = period,
        picker = periodPicker(period),
        averageApy = averageApy.formatApy(),
        onPeriodSelected = ::onPeriodSelected,
    )

    /**
     * A week is short enough to label every bar with its weekday; over three months only the first bar of each
     * month is labelled, the rest stay blank so the axis does not collapse into noise.
     */
    private fun axisLabels(points: List<YieldAprHistory.Point>, period: YieldSupplyChartPeriod): List<String> {
        val locale = Locale.getDefault()

        return when (period) {
            YieldSupplyChartPeriod.Week -> points.map {
                it.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
            }
            YieldSupplyChartPeriod.ThreeMonths -> {
                var previousMonth: Month? = null

                points.map { point ->
                    val month = point.date.month
                    if (month == previousMonth) {
                        ""
                    } else {
                        previousMonth = month
                        month.getDisplayName(TextStyle.SHORT, locale)
                    }
                }
            }
        }
    }

    private fun periodPicker(period: YieldSupplyChartPeriod): TangemSegmentedPickerUM {
        val items = YieldSupplyChartPeriod.entries.map { entry ->
            TangemSegmentUM(id = entry.name, title = resourceReference(entry.titleRes))
        }

        return TangemSegmentedPickerUM(
            items = items.toImmutableList(),
            initialSelectedItem = items.first { it.id == period.name },
            isFixed = true,
        )
    }

    private fun onPeriodSelected(periodName: String) {
        val period = YieldSupplyChartPeriod.entries.firstOrNull { it.name == periodName } ?: return
        if (period == selectedPeriod) return

        selectedPeriod = period
        uiState.update { YieldSupplyChartUM.Loading }
        load()
    }

    private fun showError() {
        uiState.update {
            YieldSupplyChartUM.Error(
                onRetry = {
                    uiState.update { YieldSupplyChartUM.Loading }
                    load()
                },
            )
        }
        params.callback?.onLoadFail()
    }

    /** Narrowest range first: every value under a tenth of a percent is also under a whole one. */
    @Suppress("MagicNumber")
    private fun getPercentFormatPattern(values: List<Double>): String {
        return when {
            values.all { it < 0.1 } -> "%.2f"
            values.all { it < 1.0 } -> "%.1f"
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
        /**
         * Guard digits, not display digits. Rounding the mean to the two decimals the header shows would turn a
         * rate like 0.004% into a flat zero, taking the reference line down to the axis with it; the formatter
         * rounds for the screen and says `<0.01%` where the mean is too small to write out.
         */
        private const val AVERAGE_GUARD_SCALE = 8
    }
}