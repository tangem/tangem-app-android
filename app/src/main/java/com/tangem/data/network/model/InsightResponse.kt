package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName

data class InsightResponse(
        @SerializedName("balanceSat")
        var balanceSat: Long? = null,

        @SerializedName("unconfirmedBalanceSat")
        var unconfirmedBalanceSat: Long? = null,

        @SerializedName("addrStr")
        var addrStr: String = "",

        @SerializedName("txid")
        var txid: String = "",

        @SerializedName("satoshis")
        var satoshis: Long? = null,

        @SerializedName("height")
        var height: Int? = null,

        @SerializedName("2")
        var fee2: String = "",

        @SerializedName("3")
        var fee3: String = "",

        @SerializedName("6")
        var fee6: String = "",

        @SerializedName("rawtx")
        var rawtx: String = "",

        @SerializedName("error")
        var error: String = ""
)