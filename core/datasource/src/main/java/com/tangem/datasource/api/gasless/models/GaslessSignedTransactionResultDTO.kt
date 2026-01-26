package com.tangem.datasource.api.gasless.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Result of gasless transaction signing.
 * Contains the signed transaction data and gas parameters.
 */
@JsonClass(generateAdapter = true)
data class GaslessSignedTransactionResultDTO(
    @Json(name = "txHash")
    val txHash: String,
)