package com.tangem.domain.onramp.model

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class OnrampAmount(
    val symbol: String,
    val value: SerializedBigDecimal,
    val decimals: Int,
)