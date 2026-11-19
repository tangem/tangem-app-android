package com.tangem.features.earn.product.api.rate

import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import java.math.BigDecimal
import java.util.Locale

/** Badge strings already carry the percent sign, so the number is formatted without it. */
fun BigDecimal.formatEarnRate(locale: Locale = Locale.getDefault()): String =
    format { percent(locale = locale, canBeLower = true, withPercentSign = false) }

/** A rate or a fee written out in a sentence or a row: "4.5%", a whole number without trailing zeros: "15%" */
fun BigDecimal.formatEarnPercent(): String = format { percent(minFractionDigits = 0) }