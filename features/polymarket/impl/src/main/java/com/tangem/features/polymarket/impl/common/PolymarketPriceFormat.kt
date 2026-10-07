package com.tangem.features.polymarket.impl.common

import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.simple
import com.tangem.utils.StringsSigns.DASH_SIGN
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Formats a raw share price (0..1) to tenths of a cent, omitting a trailing zero.
 * Unresolved markets stay within 0.1¢..99.9¢. This formatting never changes the price used in an order.
 * [withSymbol] is false for localized descriptions that already contain the cent sign.
 */
internal fun BigDecimal?.formatPolymarketPrice(
    isResolved: Boolean = false,
    withSymbol: Boolean = true,
    locale: Locale = Locale.getDefault(),
): String {
    this ?: return DASH_SIGN
    val roundedCents = movePointRight(2).setScale(1, RoundingMode.HALF_UP)
    val cents = if (isResolved) roundedCents else roundedCents.coerceIn(MIN_UNRESOLVED_CENTS, MAX_UNRESOLVED_CENTS)
    val formatted = cents.format { simple(decimals = 1, locale = locale) }
    return if (withSymbol) "$formatted${PolymarketConst.CENT_SIGN}" else formatted
}

private val MIN_UNRESOLVED_CENTS = BigDecimal("0.1")
private val MAX_UNRESOLVED_CENTS = BigDecimal("99.9")