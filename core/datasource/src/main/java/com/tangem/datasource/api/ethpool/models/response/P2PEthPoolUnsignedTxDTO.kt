package com.tangem.datasource.api.ethpool.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Unsigned transaction structure
 *
 * Used in deposit, withdraw responses
 */
@JsonClass(generateAdapter = true)
data class P2PEthPoolUnsignedTxDTO(
    @Json(name = "serializeTx")
    val serializeTx: String,
    @Json(name = "to")
    val to: String,
    @Json(name = "data")
    val data: String,
    @Json(name = "value")
    val value: String,
    @Json(name = "nonce")
    val nonce: Int,
    @Json(name = "chainId")
    val chainId: Int,
    @Json(name = "gasLimit")
    val gasLimit: BigDecimal,
    @Json(name = "maxFeePerGas")
    val maxFeePerGas: BigDecimal,
    @Json(name = "maxPriorityFeePerGas")
    val maxPriorityFeePerGas: BigDecimal,
)