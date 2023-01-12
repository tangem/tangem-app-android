package com.tangem.feature.swap.domain.models

import com.tangem.utils.toFormattedString
import java.math.BigDecimal

data class SwapAmount(
    val value: BigDecimal,
    val decimals: Int,
)

fun SwapAmount.formatToUIRepresentation(): String {
    return value.toFormattedString(decimals = decimals)
}

fun createFromAmountWithoutOffset(amountWithoutOffset: String, decimals: Int): SwapAmount {
    return SwapAmount(
        value = requireNotNull(
            amountWithoutOffset.toBigDecimalOrNull(),
        ) { "wrong amount format, use only digits" },
        decimals = decimals,
    )
}

fun createFromAmountWithOffset(amountWithOffset: String, decimals: Int): SwapAmount {
    return SwapAmount(
        value = requireNotNull(
            amountWithOffset.toBigDecimalOrNull()?.movePointLeft(decimals),
        ) { "wrong amount format, use only digits" },
        decimals = decimals,
    )
}

@Throws(IllegalStateException::class)
fun SwapAmount.toStringWithRightOffset(): String {
    return value.movePointRight(decimals).toPlainString()
}

@Throws(IllegalStateException::class)
fun SwapAmount.toStringWithLeftOffset(): String {
    return value.movePointLeft(decimals).toPlainString()
}
