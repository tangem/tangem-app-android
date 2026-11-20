package com.tangem.spend.datasource.tangempay.models.response

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AmountLimitsResponse(
    @SerialName("min") val min: SerializedBigDecimal?,
    @SerialName("max") val max: SerializedBigDecimal?,
    @SerialName("currency") val currency: String?,
)