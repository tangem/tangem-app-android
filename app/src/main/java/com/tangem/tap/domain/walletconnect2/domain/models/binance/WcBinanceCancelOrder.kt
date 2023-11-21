package com.tangem.tap.domain.walletconnect2.domain.models.binance

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Suppress("LongParameterList")
@JsonClass(generateAdapter = true)
class WcBinanceCancelOrder(
    @Json(name = "account_number")
    accountNumber: String,
    @Json(name = "chain_id")
    chainId: String,
    @Json(name = "data")
    data: String?,
    @Json(name = "memo")
    memo: String?,
    @Json(name = "sequence")
    sequence: String,
    @Json(name = "source")
    source: String,
    @Json(name = "msgs")
    msgs: List<Message>,
) : WcBinanceOrder<WcBinanceCancelOrder.Message>(accountNumber, chainId, data, memo, sequence, source, msgs) {

    data class Message(
        val refid: String,
        val sender: String,
        val symbol: String,
    )
}