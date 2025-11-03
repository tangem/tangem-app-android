package com.tangem.domain.models.quote

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * Represents the price change of a cryptocurrency asset over a specific time period.
 *
 * @param value The amount of price change (like `0.00`)
 * @param source The source of the price change information.
 *
[REDACTED_AUTHOR]
 */
@Serializable
data class PriceChange(
    val value: SerializedBigDecimal,
    val source: StatusSource,
)