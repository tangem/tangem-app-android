package com.tangem.blockchain.blockchains.tezos.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TezosAddressResponse(
        @Json(name = "balance")
        var balance: Long? = null,

        @Json(name = "counter")
        var counter: Long? = null
)

@JsonClass(generateAdapter = true)
data class TezosHeaderResponse(
        @Json(name = "protocol")
        var protocol: String? = null,

        @Json(name = "hash")
        var hash: String? = null
)