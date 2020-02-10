package com.tangem.blockchain.ethereum.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InfuraResponse(
        @Json(name = "jsonrpc")
        val jsonrpc: String = "",

        @Json(name = "id")
        val id: Int? = null,

        @Json(name = "result")
        val result: String? = null,

        @Json(name = "error")
        val error: InfuraError? = null
)

@JsonClass(generateAdapter = true)
data class InfuraError(
        @Json(name = "code")
        val code: Int? = null,

        @Json(name = "message")
        val message: String? = null
)


