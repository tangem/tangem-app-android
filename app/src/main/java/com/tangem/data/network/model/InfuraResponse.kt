package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName

data class InfuraResponse(
        @SerializedName("jsonrpc")
        var jsonrpc: String = "",

        @SerializedName("id")
        var id: Int? = null,

        @SerializedName("result")
        var result: String? = null,

        @SerializedName("error")
        var error: Object
)