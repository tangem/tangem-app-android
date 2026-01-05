package com.tangem.datasource.api.gasless.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Result of gasless transaction signing.
 * Contains the signed transaction data and gas parameters.
 */
@JsonClass(generateAdapter = true)
data class GaslessSignedTransactionResult(
    @Json(name = "signedTransaction")
    val signedTransaction: String,

    @Json(name = "gasLimit")
    val gasLimit: String,

    @Json(name = "maxFeePerGas")
    val maxFeePerGas: String,

    @Json(name = "maxPriorityFeePerGas")
    val maxPriorityFeePerGas: String,
)