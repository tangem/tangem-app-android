package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName

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