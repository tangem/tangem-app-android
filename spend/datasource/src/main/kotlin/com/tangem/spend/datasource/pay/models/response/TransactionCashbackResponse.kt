package com.tangem.spend.datasource.pay.models.response

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionCashbackResponse(
    @SerialName("status") val status: String,
    @SerialName("amount") val amount: SerializedBigDecimal? = null,
    @SerialName("currency") val currency: String? = null,
    @SerialName("cap_trimmed") val isCapTrimmed: Boolean? = null,
    @SerialName("exclusion_reason") val exclusionReason: String? = null,
)