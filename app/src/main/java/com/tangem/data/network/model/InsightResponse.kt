package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName

data class InsightResponse(
        @SerializedName("jsonrpc")
        var jsonrpc: String = "",

        @SerializedName("id")
        var id: Int? = null,

        @SerializedName("result")
        var result: String = "",

        @SerializedName("error")
        var error: String = ""
)