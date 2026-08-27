package com.tangem.domain.models.account

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class TangemPayOnrampFee(
    val type: String,
    val name: String,
    val amount: SerializedBigDecimal,
    val currency: String,
)