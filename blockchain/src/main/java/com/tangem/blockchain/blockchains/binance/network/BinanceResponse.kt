package com.tangem.blockchain.blockchains.binance.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BinanceFee(
        @Json(name = "fixed_fee_params")
        var transactionFee: BinanceFeeData? = null
)

@JsonClass(generateAdapter = true)
data class BinanceFeeData(
        @Json(name = "msg_type")
        var messageType: String? = null,

        @Json(name = "fee")
        var value: Int? = null
)