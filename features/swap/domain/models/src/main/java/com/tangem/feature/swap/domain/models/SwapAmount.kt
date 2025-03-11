package com.tangem.feature.swap.domain.models

import java.math.BigDecimal
import java.math.RoundingMode

data class SwapAmount(
    val value: BigDecimal,
    val decimals: Int,
) {

    companion object {
        fun zeroSwapAmount(): SwapAmount {
            return SwapAmount(BigDecimal.ZERO, 0)
        }
    }
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
    return value.setScale(decimals, RoundingMode.HALF_DOWN).movePointRight(decimals).toPlainString()
}