package com.tangem.features.yield.supply.impl.common.formatter

import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import java.math.BigDecimal

private const val PERCENT_SCALE = 2

/**
 * Formats a rate the backend already reports in percent.
 *
 * Raw `toString()` would leak the payload's scale (`3.0900`), use a dot in locales that expect a comma and fall
 * back to scientific notation on a tiny campaign rate, so every APY on these surfaces goes through here.
 * The point is moved left because the shared formatter takes a share, not a percentage.
 */
internal fun BigDecimal.formatApy(): String = movePointLeft(PERCENT_SCALE).format { percent(canBeLower = true) }

/**
 * The same rate as it reaches a screen over a route — a raw backend string such as `9.1149999999999998`.
 * Left as-is when it is not a number, so an empty APY stays empty rather than becoming `0.00%`.
 */
internal fun String.formatApy(): String = toBigDecimalOrNull()?.formatApy() ?: this

/** [formatApy] without the percent sign, for the strings that carry their own. */
internal fun String.formatApyValue(): String = toBigDecimalOrNull()
    ?.movePointLeft(PERCENT_SCALE)
    ?.format { percent(canBeLower = true, withPercentSign = false) }
    ?: this