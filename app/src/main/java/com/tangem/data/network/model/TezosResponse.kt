package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName

data class TezosAccountResponse(
        @SerializedName("balance")
        var balance: Long? = null,

        @SerializedName("counter")
        var counter: Long? = null
)

data class TezosHeaderResponse(
        @SerializedName("protocol")
        var protocol: String? = null,

        @SerializedName("hash")
        var hash: String? = null
)