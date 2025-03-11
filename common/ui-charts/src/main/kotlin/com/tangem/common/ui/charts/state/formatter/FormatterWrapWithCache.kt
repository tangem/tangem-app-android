package com.tangem.common.ui.charts.state.formatter

import java.math.BigDecimal

internal class FormatterWrapWithCache(private val formatter: AxisLabelFormatter) : AxisLabelFormatter {
    private val cache = mutableMapOf<BigDecimal, CharSequence>()

    override fun format(value: BigDecimal): CharSequence {
        return cache.getOrPut(value) { formatter.format(value) }
    }

    fun clearCache() {
        cache.clear()
    }
}
