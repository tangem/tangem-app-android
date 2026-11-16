package com.tangem.grow.datasource.gasless.models.solana

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SolanaFeePayerResponse(@Json(name = "feePayer") val feePayer: String)

@JsonClass(generateAdapter = true)
data class SolanaSubmitRequestBody(
    @Json(name = "transaction") val transaction: String,
    @Json(name = "lastValidBlockHeight") val lastValidBlockHeight: Long,
)

@JsonClass(generateAdapter = true)
data class SolanaSubmitResponse(
    @Json(name = "signature") val signature: String,
    @Json(name = "status") val status: String?,
)