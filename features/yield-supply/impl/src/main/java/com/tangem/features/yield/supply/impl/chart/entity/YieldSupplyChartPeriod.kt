package com.tangem.features.yield.supply.impl.chart.entity

import androidx.annotation.StringRes
import com.tangem.core.ui.R

/**
 * Periods the incentivised chart offers. The backend caps `days` at 90, so a six-month option is impossible
 * by contract rather than by product choice.
 */
internal enum class YieldSupplyChartPeriod(val days: Int, @StringRes val titleRes: Int) {
    Week(days = 7, titleRes = R.string.yield_module_incentive_chart_period_week),
    ThreeMonths(days = 90, titleRes = R.string.yield_module_incentive_chart_period_three_months),
}