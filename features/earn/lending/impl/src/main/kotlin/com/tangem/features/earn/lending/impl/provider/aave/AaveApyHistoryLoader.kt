package com.tangem.features.earn.lending.impl.provider.aave

import arrow.core.Either
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.yield.supply.models.YieldChartWindow
import com.tangem.domain.yield.supply.models.YieldSupplyMarketChartData
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetChartUseCase
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import com.tangem.features.earn.product.api.product.EarnApyHistory
import com.tangem.features.earn.product.api.product.EarnApyPoint
import com.tangem.features.earn.product.api.product.EarnPeriod
import com.tangem.utils.logging.TangemLogger
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import java.math.BigDecimal
import java.math.MathContext
import javax.inject.Inject

internal class AaveApyHistoryLoader @Inject constructor(
    private val getChart: YieldSupplyGetChartUseCase,
) {

    suspend fun load(asset: LendingAsset, period: EarnPeriod): Either<Throwable, EarnApyHistory> {
        val chartPeriod = AaveChartPeriod.of(period)
        return getChart(
            cryptoCurrency = asset.reserveToken,
            window = chartPeriod.window,
            bucketSizeDays = chartPeriod.bucketSizeDays,
        ).map { chart -> chart.toEarnHistory(chartPeriod) }
    }

    /**
     * The 3M and 6M periods take a year window and keep its last months, so their average is the mean of what is
     * kept; the 1M window is shown whole with the backend average
     */
    private fun YieldSupplyMarketChartData.toEarnHistory(period: AaveChartPeriod): EarnApyHistory {
        // A bar without a date cannot be placed on the time axis
        if (bucketStarts.size != y.size || y.isEmpty()) return EarnApyHistory(points = emptyList(), average = null)

        val cutoff = period.months?.let { months -> bucketStarts.last().minus(DatePeriod(months = months)) }
        val bars = bucketStarts.zip(y).filter { (start, _) -> cutoff == null || start >= cutoff }
        val points = bars.map { (start, apy) ->
            EarnApyPoint(at = start.atStartOfDayIn(TimeZone.UTC), rate = apy.toPercent().percentToApy())
        }
        val average = if (cutoff == null) {
            avr.toPercent().percentToApy().fraction
        } else {
            points.map { it.rate.fraction }
                .reduce(BigDecimal::add)
                .divide(points.size.toBigDecimal(), MathContext.DECIMAL64)
        }
        return EarnApyHistory(points = points, average = average)
    }
}

/**
 * @property labelMonths the period length shown on its chip
 * @property months how many last months of the window are shown; null shows the whole window
 */
private enum class AaveChartPeriod(
    val id: String,
    val labelMonths: Int,
    val window: YieldChartWindow,
    val bucketSizeDays: Int,
    val months: Int?,
) {
    Month(id = "1M", labelMonths = 1, window = YieldChartWindow.LastMonth, bucketSizeDays = 1, months = null),
    ThreeMonths(id = "3M", labelMonths = 3, window = YieldChartWindow.LastYear, bucketSizeDays = 3, months = 3),
    SixMonths(id = "6M", labelMonths = 6, window = YieldChartWindow.LastYear, bucketSizeDays = 6, months = 6),
    ;

    companion object {
        fun of(period: EarnPeriod): AaveChartPeriod = entries.firstOrNull { it.id == period.id } ?: run {
            TangemLogger.e("Aave APY history: unknown period ${period.id}, the month is shown instead")
            Month
        }
    }
}

internal object AaveApyPeriods {
    val All: List<EarnPeriod> = AaveChartPeriod.entries.map { period ->
        EarnPeriod(id = period.id, label = monthsLabel(count = period.labelMonths))
    }

    private fun monthsLabel(count: Int) = pluralReference(R.plurals.earn_calculator_months, count, wrappedList(count))
}

/** The chart sends percents as doubles: `2.0` keeps a trailing zero that `2` from other sources does not */
private fun Double.toPercent(): BigDecimal = toBigDecimal().stripTrailingZeros()