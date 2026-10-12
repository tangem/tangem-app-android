package com.tangem.features.yield.supply.impl.common.formatter

import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import java.math.BigDecimal

private const val BPS_SCALE = 4

/**
 * Formats a service fee rate the backend reports in basis points.
 *
 * Trailing zeros are dropped, so the common whole-percent rate reads as `15%` rather than `15.00%`, while a rate
 * between whole percents still keeps its precision.
 */
internal fun Int.formatServiceFeeRate(): String = BigDecimal(this)
    .movePointLeft(BPS_SCALE)
    .format { percent(minFractionDigits = 0) }