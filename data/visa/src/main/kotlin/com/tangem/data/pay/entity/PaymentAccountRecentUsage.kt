package com.tangem.data.pay.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class PaymentAccountRecentUsage(
    @Json(name = "accountId") val accountId: String,
    @Json(name = "usedAtMillis") val usedAtMillis: Long,
)