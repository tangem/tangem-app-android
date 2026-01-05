package com.tangem.datasource.api.gasless.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for gasless transaction submission.
 * Represents complete transaction with fee delegation metadata.
 */
@JsonClass(generateAdapter = true)
data class GaslessTransactionRequest(
    @Json(name = "gaslessTransaction")
    val gaslessTransaction: GaslessTransactionData,

    @Json(name = "signature")
    val signature: String,

    @Json(name = "userAddress")
    val userAddress: String,

    @Json(name = "chainId")
    val chainId: Int,

    @Json(name = "eip7702auth")
    val eip7702Auth: Eip7702Authorization? = null,
)

@JsonClass(generateAdapter = true)
data class GaslessTransactionData(
    @Json(name = "transaction")
    val transaction: TransactionData,

    @Json(name = "fee")
    val fee: FeeData,

    @Json(name = "nonce")
    val nonce: String,
)

@JsonClass(generateAdapter = true)
data class TransactionData(
    @Json(name = "to")
    val to: String,

    @Json(name = "value")
    val value: String,

    @Json(name = "data")
    val data: String,
)

@JsonClass(generateAdapter = true)
data class FeeData(
    @Json(name = "feeToken")
    val feeToken: String,

    @Json(name = "maxTokenFee")
    val maxTokenFee: String,

    @Json(name = "coinPriceInToken")
    val coinPriceInToken: String,

    @Json(name = "feeTransferGasLimit")
    val feeTransferGasLimit: String,

    @Json(name = "baseGas")
    val baseGas: String,
)

/**
 * EIP-7702 authorization for account abstraction.
 * Optional field, used only when EOA delegation is required.
 */
@JsonClass(generateAdapter = true)
data class Eip7702Authorization(
    @Json(name = "chainId")
    val chainId: Int,

    @Json(name = "address")
    val address: String,

    @Json(name = "nonce")
    val nonce: String,

    @Json(name = "yParity")
    val yParity: Int,

    @Json(name = "r")
    val r: String,

    @Json(name = "s")
    val s: String,
)