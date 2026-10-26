package com.tangem.feature.swap.domain.models.domain

import java.math.BigDecimal

private val MIN_SLIPPAGE_PERCENT = BigDecimal("0.01")
private val MAX_SLIPPAGE_DEFAULT_FACTOR = BigDecimal(2)

/** The provider's own slippage from the providers list; without it slippage can't be set. */
val SwapProvider.defaultSlippage: BigDecimal?
    get() = slippage?.takeIf { it.signum() > 0 }

/**
 * The default slippage for the current pair and amount. Express applies a per-pair slippage in the quote (stable pairs
 * get a much tighter one than the providers list states), so a quote loaded without a custom slippage wins over
 * [defaultSlippage].
 *
 * @param quoteDefaultSlippages slippage of quotes requested without a custom one, keyed by [SwapProvider.providerId]
 */
fun SwapProvider.pairDefaultSlippage(quoteDefaultSlippages: Map<String, BigDecimal>): BigDecimal? {
    return quoteDefaultSlippages[providerId]?.takeIf { it.signum() > 0 } ?: defaultSlippage
}

/**
 * A provider takes a custom slippage between 0.01% and twice its default.
 * The user's custom slippage applies to every provider that accepts it; others use their default.
 */
fun SwapProvider.acceptsSlippage(percent: BigDecimal, defaultPercent: BigDecimal? = defaultSlippage): Boolean {
    if (defaultPercent == null || !isCustomSlippageSupported) return false
    return percent in MIN_SLIPPAGE_PERCENT..defaultPercent.multiply(MAX_SLIPPAGE_DEFAULT_FACTOR)
}

fun SwapProvider.acceptedSlippageOrNull(
    percent: BigDecimal?,
    defaultPercent: BigDecimal? = defaultSlippage,
): BigDecimal? = percent?.takeIf { acceptsSlippage(it, defaultPercent) }