package com.tangem.core.ui.format.bigdecimal

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import java.math.BigDecimal

interface BigDecimalFormatScope {
    companion object {
        val Empty = object : BigDecimalFormatScope {}
    }
}

fun interface BigDecimalFormat : (BigDecimal) -> String, BigDecimalFormatScope

fun interface BigDecimalFormatStyled : (BigDecimal) -> TextReference, BigDecimalFormatScope

inline fun BigDecimal.format(block: BigDecimalFormatScope.() -> BigDecimalFormat): String {
    return BigDecimalFormatScope.Empty.block()(this)
}

inline fun BigDecimal.formatStyled(block: BigDecimalFormatScope.() -> BigDecimalFormatStyled): TextReference {
    return BigDecimalFormatScope.Empty.block()(this)
}

inline fun BigDecimal?.format(
    fallbackString: String = BigDecimalFormatConstants.EMPTY_BALANCE_SIGN,
    block: BigDecimalFormatScope.() -> BigDecimalFormat,
): String {
    if (this == null) return fallbackString
    return BigDecimalFormatScope.Empty.block()(this)
}

inline fun BigDecimal?.formatStyled(
    fallbackString: String = BigDecimalFormatConstants.EMPTY_BALANCE_SIGN,
    block: BigDecimalFormatScope.() -> BigDecimalFormatStyled,
): TextReference {
    if (this == null) return stringReference(fallbackString)
    return BigDecimalFormatScope.Empty.block()(this)
}

fun BigDecimal?.format(
    format: BigDecimalFormat,
    fallbackString: String = BigDecimalFormatConstants.EMPTY_BALANCE_SIGN,
): String {
    if (this == null) return fallbackString
    return format(this)
}

fun BigDecimal?.format(
    format: BigDecimalFormatStyled,
    fallbackString: String = BigDecimalFormatConstants.EMPTY_BALANCE_SIGN,
): TextReference {
    if (this == null) return stringReference(fallbackString)
    return format(this)
}