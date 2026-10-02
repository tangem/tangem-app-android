package com.tangem.data.pay.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PaymentAccountRecentUsage(
    @SerialName("accountId") val accountId: String,
    @SerialName("usedAtMillis") val usedAtMillis: Long,
)