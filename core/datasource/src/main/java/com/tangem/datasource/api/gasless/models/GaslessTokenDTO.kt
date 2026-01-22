package com.tangem.datasource.api.gasless.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GaslessTokenDTO(
    @Json(name = "tokenAddress") val tokenAddress: String,
    @Json(name = "tokenSymbol") val tokenSymbol: String,
    @Json(name = "tokenName") val tokenName: String,
    @Json(name = "decimals") val decimals: Int,
    @Json(name = "chainId") val chainId: Int,
    @Json(name = "chain") val chain: String,
)