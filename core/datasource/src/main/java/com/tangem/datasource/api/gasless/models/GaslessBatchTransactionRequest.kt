package com.tangem.datasource.api.gasless.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for gasless batch transaction submission.
 * Represents a batch of transactions with fee delegation metadata.
 */
@JsonClass(generateAdapter = true)
data class GaslessBatchTransactionRequest(
    @Json(name = "gaslessBatchTransaction")
    val gaslessBatchTransaction: GaslessBatchTransactionDataDTO,

    @Json(name = "signature")
    val signature: String,

    @Json(name = "userAddress")
    val userAddress: String,

    @Json(name = "chainId")
    val chainId: Int,

    @Json(name = "eip7702auth")
    val eip7702Auth: Eip7702AuthorizationDTO? = null,
)

@JsonClass(generateAdapter = true)
data class GaslessBatchTransactionDataDTO(
    @Json(name = "transactions")
    val transactions: List<TransactionData>,

    @Json(name = "fee")
    val fee: FeeData,

    @Json(name = "nonce")
    val nonce: String,
)