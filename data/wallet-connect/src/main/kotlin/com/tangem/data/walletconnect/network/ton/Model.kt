package com.tangem.data.walletconnect.network.ton

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class WcTonSendMessageRequest(
    @Json(name = "valid_until")
    val validUntil: Long?,

    @Json(name = "from")
    val from: String?,

    @Json(name = "messages")
    val messages: List<TonMessageParam>,
)

@JsonClass(generateAdapter = true)
internal data class TonMessageParam(
    @Json(name = "address")
    val address: String,

    @Json(name = "amount")
    val amount: String,

    @Json(name = "payload")
    val payload: String?,

    @Json(name = "stateInit")
    val stateInit: String?,
)

@JsonClass(generateAdapter = true)
internal data class WcTonSignDataRequest(
    @Json(name = "type")
    val type: String,

    @Json(name = "text")
    val text: String?,

    @Json(name = "bytes")
    val bytes: String?,

    @Json(name = "schema")
    val schema: String?,

    @Json(name = "cell")
    val cell: String?,

    @Json(name = "from")
    val from: String?,
)