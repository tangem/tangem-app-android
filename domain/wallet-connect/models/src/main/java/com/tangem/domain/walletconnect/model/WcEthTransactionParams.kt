package com.tangem.domain.walletconnect.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WcEthTransactionParams(
    @Json(name = "from")
    val from: String,

    @Json(name = "to")
    val to: String?,

    @Json(name = "data")
    val data: String?,

    @Json(name = "gas")
    val gas: String?,

    @Json(name = "gasPrice")
    val gasPrice: String?,

    @Json(name = "value")
    val value: String?,

    @Json(name = "nonce")
    val nonce: String?,
)