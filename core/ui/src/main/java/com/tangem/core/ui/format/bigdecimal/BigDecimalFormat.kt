package com.tangem.core.ui.format.bigdecimal

import java.math.BigDecimal

interface BigDecimalFormatScope {
    companion object { val Empty = object : BigDecimalFormatScope {} }
}

fun interface BigDecimalFormat : (BigDecimal) -> String, BigDecimalFormatScope

inline fun BigDecimal.format(block: BigDecimalFormatScope.() -> BigDecimalFormat): String {
    return BigDecimalFormatScope.Empty.block()(this)
}

inline fun BigDecimal?.format(
    fallbackString: String = BigDecimalFormatConstants.EMPTY_BALANCE_SIGN,
    block: BigDecimalFormatScope.() -> BigDecimalFormat,
): String {
    if (this == null) return fallbackString
    return BigDecimalFormatScope.Empty.block()(this)
}

fun BigDecimal?.format(
    format: BigDecimalFormat,
    fallbackString: String = BigDecimalFormatConstants.EMPTY_BALANCE_SIGN,
): String {
    if (this == null) return fallbackString
    return format(this)
}