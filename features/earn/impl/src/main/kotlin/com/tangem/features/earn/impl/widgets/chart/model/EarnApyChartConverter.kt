package com.tangem.features.earn.impl.widgets.chart.model

import com.tangem.core.ui.ds2.segmentedcontrol.TangemSegmentedControl
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.impl.R
import com.tangem.features.earn.impl.formatEarnRate
import com.tangem.features.earn.impl.widgets.chart.EarnApyChartComponent
import com.tangem.features.earn.impl.widgets.chart.EarnApyChartTexts
import com.tangem.features.earn.impl.widgets.chart.ui.state.EarnApyAverageUM
import com.tangem.features.earn.impl.widgets.chart.ui.state.EarnApyBarUM
import com.tangem.features.earn.impl.widgets.chart.ui.state.EarnApyChartUM
import com.tangem.features.earn.product.api.product.EarnApyHistory
import com.tangem.features.earn.product.api.product.EarnApyPoint
import com.tangem.features.earn.product.api.product.EarnPeriod
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

internal class EarnApyChartConverter @Inject constructor() {

    @Suppress("LongParameterList")
    fun convert(
        params: EarnApyChartComponent.Params,
        period: EarnPeriod,
        history: EarnApyHistory,
        onPeriodClick: (EarnPeriod) -> Unit,
        onBarSelected: (Int?) -> Unit,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        locale: Locale = Locale.getDefault(),
    ): EarnApyChartUM {
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        val pointsOldestFirst = history.points.sortedBy(EarnApyPoint::at)
        val dates = pointsOldestFirst.map { it.at.toLocalDateTime(timeZone).date }
        val bars = pointsOldestFirst.mapIndexed { index, point ->
            val rate = point.rate.fraction.formatEarnRate(locale)
            val date = dates[index].toJavaLocalDate().format(dateFormatter)

            EarnApyBarUM(value = point.rate.fraction.toPercent(), tooltip = stringReference("$rate% · $date"))
        }

        val header = header(
            params = params,
            period = period,
            onPeriodClick = onPeriodClick,
            onBarSelected = onBarSelected,
            locale = locale,
        )

        return header.copy(
            bars = bars.toImmutableList(),
            scale = EarnApyChartAxis.scaleOf(tallestColumn = bars.maxOfOrNull(EarnApyBarUM::value) ?: 0.0),
            average = history.average?.let { average(fraction = it, type = params.currentRate.type, locale = locale) },
            xLabels = EarnApyChartAxis.monthLabels(columnStarts = dates, locale = locale).toImmutableList(),
        )
    }

    fun loading(
        params: EarnApyChartComponent.Params,
        period: EarnPeriod,
        onPeriodClick: (EarnPeriod) -> Unit,
        onBarSelected: (Int?) -> Unit,
        locale: Locale = Locale.getDefault(),
    ): EarnApyChartUM = header(
        params = params,
        period = period,
        onPeriodClick = onPeriodClick,
        onBarSelected = onBarSelected,
        locale = locale,
    ).copy(isLoading = true)

    fun failed(
        params: EarnApyChartComponent.Params,
        period: EarnPeriod,
        onPeriodClick: (EarnPeriod) -> Unit,
        onBarSelected: (Int?) -> Unit,
        locale: Locale = Locale.getDefault(),
    ): EarnApyChartUM = header(
        params = params,
        period = period,
        onPeriodClick = onPeriodClick,
        onBarSelected = onBarSelected,
        locale = locale,
    )

    private fun header(
        params: EarnApyChartComponent.Params,
        period: EarnPeriod,
        onPeriodClick: (EarnPeriod) -> Unit,
        onBarSelected: (Int?) -> Unit,
        locale: Locale,
    ) = EarnApyChartUM(
        periods = params.periods
            .map { item ->
                TangemSegmentedControl.Item(
                    id = item.id,
                    label = item.label,
                    isSelected = item == period,
                    onClick = { onPeriodClick(item) },
                )
            }
            .toImmutableList(),
        rateTitle = rateTitle(params.currentRate.type),
        currentRate = stringReference("${params.currentRate.fraction.formatEarnRate(locale)}%"),
        bars = persistentListOf(),
        scale = EarnApyChartAxis.scaleOf(tallestColumn = 0.0),
        average = null,
        xLabels = persistentListOf(),
        selectedBar = null,
        isLoading = false,
        onBarSelected = onBarSelected,
        onCalculatorClick = params.callback?.let { callback -> { callback.onCalculatorClick() } },
    )

    private fun rateTitle(type: EarnRateType): TextReference = when (type) {
        EarnRateType.APY -> EarnApyChartTexts.currentApy
        EarnRateType.APR -> EarnApyChartTexts.currentApr
    }

    private fun averageTitle(type: EarnRateType): TextReference = when (type) {
        EarnRateType.APY -> EarnApyChartTexts.averageApy
        EarnRateType.APR -> EarnApyChartTexts.averageApr
    }

    private fun average(fraction: BigDecimal, type: EarnRateType, locale: Locale): EarnApyAverageUM {
        val rate = "${fraction.formatEarnRate(locale)}%"

        return EarnApyAverageUM(
            value = fraction.toPercent(),
            text = resourceReference(
                id = R.string.yield_module_rate_info_sheet_chart_average,
                formatArgs = wrappedList(rate),
            ),
            title = averageTitle(type),
            rate = stringReference(rate),
        )
    }

    private fun BigDecimal.toPercent(): Double = multiply(PERCENT).toDouble()

    private companion object {
        val PERCENT = BigDecimal(100)
    }
}