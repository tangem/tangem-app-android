package com.tangem.data.pay.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class BankOfframpRecentUsage(
    @SerialName("usedAtMillis") val usedAtMillis: Long,
)