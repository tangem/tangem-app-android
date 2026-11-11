package com.tangem.domain.tangempay.model

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class AmountLimits(
    val min: SerializedBigDecimal,
    val max: SerializedBigDecimal?,
    val currency: String,
)