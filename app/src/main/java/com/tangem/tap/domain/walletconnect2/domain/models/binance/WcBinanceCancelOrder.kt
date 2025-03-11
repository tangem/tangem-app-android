package com.tangem.tap.domain.walletconnect2.domain.models.binance

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.tap.domain.walletconnect2.domain.WcRequestData

@Suppress("LongParameterList")
@JsonClass(generateAdapter = true)
data class WcBinanceCancelOrder(
    @Json(name = "account_number")
    val accountNumber: String,
    @Json(name = "chain_id")
    val chainId: String,
    @Json(name = "data")
    val data: String?,
    @Json(name = "memo")
    val memo: String?,
    @Json(name = "sequence")
    val sequence: String,
    @Json(name = "source")
    val source: String,
    @Json(name = "msgs")
    val msgs: List<Message>,
) : WcRequestData {

    @JsonClass(generateAdapter = true)
    data class Message(
        @Json(name = "refid")
        val refid: String,
        @Json(name = "sender")
        val sender: String,
        @Json(name = "symbol")
        val symbol: String,
    )
}