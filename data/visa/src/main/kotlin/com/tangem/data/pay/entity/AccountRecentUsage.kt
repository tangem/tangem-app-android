package com.tangem.data.pay.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AccountRecentUsage(
    @SerialName("accountId") val accountId: String,
    @SerialName("usedAtMillis") val usedAtMillis: Long,
)