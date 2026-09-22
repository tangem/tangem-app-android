package com.tangem.data.pay.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class BankOfframpRecentUsage(
    @Json(name = "usedAtMillis") val usedAtMillis: Long,
)