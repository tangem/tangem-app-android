package com.tangem.features.markets.details.impl.ui.state

import androidx.annotation.FloatRange

internal data class PricePerformanceUM(
    val h24: Value,
    val month: Value,
    val all: Value,
) {
    data class Value(
        val low: String,
        val high: String,
        @FloatRange(from = 0.0, to = 1.0) val indicatorFraction: Float,
    )
}