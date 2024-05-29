package com.tangem.domain.tokens.model

import java.math.BigDecimal

/**
 * The amount of currency with the possible [maxValue] field
 * Useful for some [com.tangem.blockchain.common.AmountType] that have maximum value
 */
data class CurrencyAmount(
    val value: BigDecimal,
    val maxValue: BigDecimal?,
)