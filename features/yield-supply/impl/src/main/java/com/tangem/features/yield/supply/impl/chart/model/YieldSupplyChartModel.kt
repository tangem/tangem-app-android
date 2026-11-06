package com.tangem.features.yield.supply.impl.chart.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.incentives.YieldAprHistory
import com.tangem.domain.yield.supply.usecase.GetYieldAprHistoryUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetChartUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetTokenStatusUseCase
import com.tangem.features.yield.supply.impl.common.formatter.formatApy
import com.tangem.features.yield.supply.impl.chart.DefaultYieldSupplyChartComponent
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartAxisLabel
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartIncentiveUM
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartPeriod
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartUM
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyMarketChartDataUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
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

    /**
     * Cancellation is cooperative, so it cannot be the only guard: a coroutine that has passed its last suspension
     * point before the cancel lands runs its whole publishing block regardless, and every write to the screen is
     * therefore checked against this as well. That interleaving needs two real threads, which is why only the
     * ordinary period switch is covered by a test. Written from the picker, read from the worker the request runs
     * on, hence atomic rather than plain.
     */
    private val latestRequest = AtomicInteger()

    init {
        load()
    }

    private fun load() {
        // The window is read once here and carried down, so an answer can only ever describe the window it was
        // asked for, whatever the picker has moved on to since.
        val period = selectedPeriod
        val request = latestRequest.incrementAndGet()

        loadJob?.cancel()
        loadJob = modelScope.launch(dispatchers.default) {
            params.callback?.onStartLoading()
            // A sheet asked for the incentivised chart wears its own copy from the first frame. Waiting for the
            // market to say so would render the plain sheet first and swap it out once the campaign is known.
            if (!params.isPlainRate) {
                params.callback?.onIncentiveChanged(incentive(averageApy = null, period = period))
            }

            val market = yieldSupplyGetTokenStatusUseCase(params.cryptoCurrency).getOrNull()

            if (isSuperseded(request)) return@launch

            if (market == null) {
                // A market that could not be read is not a token whose campaign is over. Falling back to the Aave
                // history here would plot the base rate under an incentivised headline; only the plain chart, which
                // plots that history anyway and wanted the market for its headline alone, carries on without it.
                if (params.isPlainRate) loadSupplyChart(request = request) else showError()
                return@launch
            }

            // The plain sheet plots the Aave history, so it must not headline the rate the campaign adds to it.
            val headlineApy = if (params.isPlainRate) market.apy else market.effectiveApy
            params.callback?.onRateResolved(headlineApy.formatApy())

            val campaign = market.campaign?.takeIf { it.hasActiveCampaign }

            if (campaign == null || params.isPlainRate) {
                loadSupplyChart(request = request)
            } else {
                loadIncentiveChart(market = market, period = period, request = request)
            }
        }
    }

    private suspend fun loadSupplyChart(request: Int) {
        yieldSupplyGetChartUseCase(params.cryptoCurrency)
            .onRight { chartData ->
                if (isSuperseded(request)) return@onRight

                if (chartData.y.isEmpty()) {
                    showError()
                } else {
                    uiState.update {
                        YieldSupplyChartUM.Data(
                            chartData = YieldSupplyMarketChartDataUM(
                                y = chartData.y.toImmutableList(),
                                x = chartData.x.toImmutableList(),
                                avr = chartData.avr,
                                scale = YieldSupplyChartAxis.scaleOf(tallestColumn = chartData.y.max()),
                                xLabels = YieldSupplyChartAxis.monthLabels(
                                    columnStarts = chartData.bucketStarts,
                                    locale = Locale.getDefault(),
                                ).toImmutableList(),
                            ),
                        )
                    }
                    params.callback?.onIncentiveChanged(null)
                    params.callback?.onSuccessLoad()
                }
            }
            .onLeft { if (!isSuperseded(request)) showError() }
    }

    private suspend fun loadIncentiveChart(market: YieldMarketToken, period: YieldSupplyChartPeriod, request: Int) {
        getYieldAprHistoryUseCase(
            chainId = market.chainId,
            tokenAddress = market.tokenAddress,
            days = period.days,
        )
            .onRight { history ->
                if (isSuperseded(request)) return@onRight

                if (history.points.isEmpty()) {
                    showError()
                } else {
                    val state = toIncentiveState(history = history, period = period)
                    uiState.update { state }
                    params.callback?.onIncentiveChanged(state.incentive)
                    params.callback?.onSuccessLoad()
                }
            }
            .onLeft { if (!isSuperseded(request)) showError() }
    }

    private fun isSuperseded(request: Int): Boolean = request != latestRequest.get()

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
                scale = YieldSupplyChartAxis.scaleOf(tallestColumn = totals.max()),
                rewardCaps = caps.map(BigDecimal::toDouble).toImmutableList(),
                xLabels = axisLabels(points = history.points, period = period).toImmutableList(),
            ),
            incentive = incentive(averageApy = average, period = period),
        )
    }

    private fun incentive(averageApy: BigDecimal?, period: YieldSupplyChartPeriod) = YieldSupplyChartIncentiveUM(
        period = period,
        averageApy = averageApy.formatApy(),
        onPeriodSelected = ::onPeriodSelected,
    )

    /** A week is short enough to name every bar by its weekday; three months are named by month only. */
    private fun axisLabels(
        points: List<YieldAprHistory.Point>,
        period: YieldSupplyChartPeriod,
    ): List<YieldSupplyChartAxisLabel> {
        val locale = Locale.getDefault()

        return when (period) {
            YieldSupplyChartPeriod.Week -> points.mapIndexed { index, point ->
                YieldSupplyChartAxisLabel(
                    text = point.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                    position = index + COLUMN_CENTER,
                    anchor = YieldSupplyChartAxisLabel.Anchor.Center,
                )
            }
            YieldSupplyChartPeriod.ThreeMonths -> YieldSupplyChartAxis.monthLabels(
                columnStarts = points.map(YieldAprHistory.Point::date),
                locale = locale,
            )
        }
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

    companion object {
        private const val COLUMN_CENTER = 0.5

        /**
         * Guard digits, not display digits. Rounding the mean to the two decimals the header shows would turn a
         * rate like 0.004% into a flat zero, taking the reference line down to the axis with it; the formatter
         * rounds for the screen and says `<0.01%` where the mean is too small to write out.
         */
        private const val AVERAGE_GUARD_SCALE = 8
    }
}