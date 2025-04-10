package com.tangem.domain.walletconnect.model.legacy

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Session(
    @Json(name = "topic")
    val topic: String,

    @Json(name = "accounts")
    val accounts: List<Account>,
)