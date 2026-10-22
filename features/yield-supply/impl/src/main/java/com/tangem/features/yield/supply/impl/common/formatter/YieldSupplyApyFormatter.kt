package com.tangem.features.yield.supply.impl.common.formatter

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import java.math.BigDecimal

private const val PERCENT_SCALE = 2

/**
 * Formats a rate the backend already reports in percent.
 *
 * Raw `toString()` would leak the payload's scale (`3.0900`), use a dot in locales that expect a comma and fall
 * back to scientific notation on a tiny campaign rate, so every APY on these surfaces goes through here.
 * The point is moved left because the shared formatter takes a share, not a percentage, and a rate that is absent
 * reads as the dash the shared formatter falls back to.
 */
internal fun BigDecimal?.formatApy(): TextReference = stringReference(formatApyText())

internal fun BigDecimal?.formatApyText(): String =
    this?.movePointLeft(PERCENT_SCALE).format { percent(canBeLower = true) }

/** The same rate as it reaches a screen over a route — a raw backend string such as `9.1149999999999998`. */
internal fun String?.formatApy(): TextReference = this?.toBigDecimalOrNull().formatApy()

/** [formatApy] without the percent sign, for the strings that carry their own. */
internal fun BigDecimal?.formatApyValue(): TextReference = stringReference(
    this?.movePointLeft(PERCENT_SCALE).format { percent(canBeLower = true, withPercentSign = false) },
)

internal fun String?.formatApyValue(): TextReference = this?.toBigDecimalOrNull().formatApyValue()