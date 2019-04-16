package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName
import java.security.MessageDigest

data class BinanceAccount(
        @SerializedName("address")
        var address: String? = null,

        @SerializedName("sequence")
        var sequence: Long? = null,

        @SerializedName("balances")
        var balances: List<BinanceBalance>? = null
)

data class BinanceBalance(
        @SerializedName("symbol")
        var symbol: String? = null,

        @SerializedName("free")
        var free: String? = null
)

data class BinanceFees(
        @SerializedName("fixed_fee_params")
        var fixed_fee_params: BinanceFixedFee? = null
)

data class BinanceFixedFee(
        @SerializedName("msg_type")
        var msg_type: String? = null,

        @SerializedName("fee")
        var fee: Long? = null
)

data class BinanceError(
        @SerializedName("message")
        var message: String? = null
)