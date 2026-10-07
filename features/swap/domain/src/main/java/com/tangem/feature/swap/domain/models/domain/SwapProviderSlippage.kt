package com.tangem.feature.swap.domain.models.domain

import java.math.BigDecimal

private val MIN_SLIPPAGE_PERCENT = BigDecimal("0.01")
private val MAX_SLIPPAGE_DEFAULT_FACTOR = BigDecimal(2)

/** The provider's own slippage from the providers list; without it slippage can't be set. */
val SwapProvider.defaultSlippage: BigDecimal?
    get() = slippage?.takeIf { it.signum() > 0 }

/**
 * A provider takes a custom slippage between 0.01% and twice its own default.
 * The user's custom slippage applies to every provider that accepts it; others use their default.
 */
fun SwapProvider.acceptsSlippage(percent: BigDecimal): Boolean {
    val defaultPercent = defaultSlippage ?: return false
    if (!isCustomSlippageSupported) return false
    return percent in MIN_SLIPPAGE_PERCENT..defaultPercent.multiply(MAX_SLIPPAGE_DEFAULT_FACTOR)
}

fun SwapProvider.acceptedSlippageOrNull(percent: BigDecimal?): BigDecimal? = percent?.takeIf(::acceptsSlippage)