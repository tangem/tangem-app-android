package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName

data class InsightResponse(
        @SerializedName("jsonrpc")
        var jsonrpc: String = "",

        @SerializedName("id")
        var id: Int? = null,

        @SerializedName("balanceSat")
        var balanceSat: Long = null,

        @SerializedName("unconfirmedBalanceSat")
        var unconfirmedBalanceSat: Long = null,

        @SerializedName("addrStr")
        var addrStr: String = "",

        @SerializedName("error")
        var error: String = ""
)