package com.tangem.datasource.api.ethpool.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.joda.time.DateTime
import java.math.BigDecimal

/**
 * Unified response for staking transactions (deposit, unstake, withdraw)
 *
 * Response for:
 * - POST /api/v1/staking/pool/{network}/staking/deposit
 * - POST /api/v1/staking/pool/{network}/staking/unstake
 * - POST /api/v1/staking/pool/{network}/staking/withdraw
 */
@JsonClass(generateAdapter = true)
data class P2PEthPoolTransactionResponse(
    @Json(name = "amount")
    val amount: BigDecimal,
    @Json(name = "vaultAddress")
    val vaultAddress: String,
    @Json(name = "delegatorAddress")
    val delegatorAddress: String,
    @Json(name = "unsignedTransaction")
    val unsignedTransaction: P2PEthPoolUnsignedTxDTO,
    @Json(name = "createdAt")
    val createdAt: DateTime,
    @Json(name = "tickets")
    val tickets: List<String>? = null,
)