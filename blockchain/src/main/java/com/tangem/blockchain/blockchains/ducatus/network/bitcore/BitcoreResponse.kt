package com.tangem.blockchain.blockchains.ducatus.network.bitcore

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BitcoreBalance(
        @Json(name = "confirmed")
        var confirmed: Long? = null,

        @Json(name = "unconfirmed")
        var unconfirmed: Long? = null
)

@JsonClass(generateAdapter = true)
data class BitcoreUtxo(
        @Json(name = "mintTxid")
        var transactionHash: String? = null,

        @Json(name = "mintIndex")
        var index: Int? = null,

        @Json(name = "value")
        var amount: Long? = null,

        @Json(name = "script")
        var script: String? = null
)

@JsonClass(generateAdapter = true)
data class BitcoreSendResponse(
        @Json(name = "txid")
        var txid: String? = null
)