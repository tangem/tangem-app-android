package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName

data class InsightResponse(
        @SerializedName("balanceSat")
        var balanceSat: Long? = null,

        @SerializedName("unconfirmedBalanceSat")
        var unconfirmedBalanceSat: Long? = null,

        @SerializedName("addrStr")
        var addrStr: String = "",

//        @SerializedName("2")
//        var fee2: String = "",
//
//        @SerializedName("3")
//        var fee3: String = "",
//
//        @SerializedName("6")
//        var fee6: String = "",

        @SerializedName("error")
        var error: String = ""
)

data class InsightUtxo(
        @SerializedName("txid")
        var txid: String = "",

        @SerializedName("satoshis")
        var satoshis: Long? = null,

        @SerializedName("vout")
        var vout: Int? = null,

        @SerializedName("scriptPubKey")
        var scriptPubKey: String? = null
)