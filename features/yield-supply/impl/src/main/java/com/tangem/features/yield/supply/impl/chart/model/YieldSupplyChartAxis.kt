package com.tangem.features.yield.supply.impl.chart.model

import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartAxisLabel
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartScale
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import java.math.BigDecimal
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.floor
import kotlin.math.log10

internal object YieldSupplyChartAxis {

    private const val GRID_INTERVALS = 3
    private const val MONTH_LABELS_COUNT = 5

    /** A month that starts this close before the next one is too narrow to hold its own name. */
    private const val MIN_LEADING_MONTH_DAYS = 15

    private const val STEP_TOLERANCE = 1e-9
    private val NICE_MULTIPLIERS = listOf(1, 2, 3, 5, 10)

    /**
     * Grid lines sit on round values (`0 / 3 / 6 / 9%`) and the axis tops out at the last of them. Splitting the
     * tallest column into equal thirds instead labels lines like 3.58% as `4%`. Three intervals is all the chart
     * height holds: the axis drops lines whose labels would not fit and spreads the rest over the range again.
     */
    fun scaleOf(tallestColumn: Double): YieldSupplyChartScale {
        if (tallestColumn <= 0.0) {
            return YieldSupplyChartScale(
                max = GRID_INTERVALS.toDouble(),
                step = 1.0,
                decimals = 0,
            )
        }

        val rawStep = tallestColumn / GRID_INTERVALS
        val exponent = floor(log10(rawStep)).toInt()
        val magnitude = BigDecimal.ONE.scaleByPowerOfTen(exponent)
        val step = NICE_MULTIPLIERS
            .map { multiplier -> magnitude.multiply(multiplier.toBigDecimal()) }
            .first { candidate -> candidate.toDouble() >= rawStep * (1 - STEP_TOLERANCE) }
            .stripTrailingZeros()

        return YieldSupplyChartScale(
            max = step.multiply(GRID_INTERVALS.toBigDecimal()).toDouble(),
            step = step.toDouble(),
            decimals = step.scale().coerceAtLeast(0),
        )
    }

    /**
     * Each month is named where its first day falls on the time axis, so the names keep an even pace even when a
     * month spans two and a half columns. Every month the columns reach into counts, the one the last column runs
     * into included, but only the latest twelve: a year window opens and closes in the same month, and one name
     * must not stand at both ends. At most [MONTH_LABELS_COUNT], thinned from the newest month so the current one
     * is always named. A month the first column opens in the middle of is named only when it has room and nothing
     * is thinned, or when it is the only month on the chart.
     *
     * @param columnStarts first day of every column, oldest first
     */
    fun monthLabels(columnStarts: List<LocalDate>, locale: Locale): List<YieldSupplyChartAxisLabel> {
        if (columnStarts.isEmpty()) return emptyList()

        val first = columnStarts.first()
        val end = columnStarts.last().plus(columnDays(column = columnStarts.lastIndex, columnStarts), DateTimeUnit.DAY)
        val months = generateSequence(LocalDate(first.year, first.month, 1)) { it.plus(1, DateTimeUnit.MONTH) }
            .takeWhile { monthStart -> monthStart < end }
            .toList()
            .let { all -> all.filter { monthStart -> monthStart > all.last().minus(1, DateTimeUnit.YEAR) } }
            .let { all ->
                val shouldNameLeading = all.size == 1 ||
                    all.size <= MONTH_LABELS_COUNT &&
                    first.daysUntil(nextMonth(all.first())) >= MIN_LEADING_MONTH_DAYS
                all.filter { monthStart -> monthStart >= first || shouldNameLeading }
            }

        val step = ((months.size + MONTH_LABELS_COUNT - 1) / MONTH_LABELS_COUNT).coerceAtLeast(1)

        return months
            .reversed()
            .filterIndexed { position, _ -> position % step == 0 }
            .reversed()
            .map { monthStart ->
                YieldSupplyChartAxisLabel(
                    text = monthStart.month.getDisplayName(TextStyle.SHORT, locale),
                    position = positionOf(date = monthStart, columnStarts = columnStarts),
                    anchor = YieldSupplyChartAxisLabel.Anchor.Start,
                )
            }
    }

    /** Fractional column index of [date]: the column it falls in plus how far into that column's span it is. */
    private fun positionOf(date: LocalDate, columnStarts: List<LocalDate>): Double {
        val column = columnStarts.indexOfLast { it <= date }
        if (column < 0) return 0.0

        return column + columnStarts[column].daysUntil(date).toDouble() / columnDays(column, columnStarts)
    }

    /** Days up to the next column's start; the last column is taken to be as long as the one before it. */
    private fun columnDays(column: Int, columnStarts: List<LocalDate>): Int = when {
        column + 1 < columnStarts.size -> columnStarts[column].daysUntil(columnStarts[column + 1])
        column > 0 -> columnStarts[column - 1].daysUntil(columnStarts[column])
        else -> 1
    }.coerceAtLeast(1)

    private fun nextMonth(monthStart: LocalDate): LocalDate = monthStart.plus(1, DateTimeUnit.MONTH)
}