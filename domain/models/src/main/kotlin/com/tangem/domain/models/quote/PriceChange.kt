package com.tangem.domain.models.quote

import com.tangem.domain.models.StatusSource
import java.math.BigDecimal

/**
 * Represents the price change of a cryptocurrency asset over a specific time period.
 *
 * @param value The amount of price change.
 * @param source The source of the price change information.
 *
[REDACTED_AUTHOR]
 */
data class PriceChange(
    val value: BigDecimal,
    val source: StatusSource,
)