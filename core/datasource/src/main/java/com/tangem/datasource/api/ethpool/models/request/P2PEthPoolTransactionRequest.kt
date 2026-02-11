package com.tangem.datasource.api.ethpool.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Unified request body for creating staking transactions (deposit, unstake, withdraw)
 *
 * Used in:
 * - POST /api/v1/staking/pool/{network}/staking/deposit
 * - POST /api/v1/staking/pool/{network}/staking/unstake
 * - POST /api/v1/staking/pool/{network}/staking/withdraw
 */
@JsonClass(generateAdapter = true)
data class P2PEthPoolTransactionRequest(
    @Json(name = "delegatorAddress")
    val delegatorAddress: String,
    @Json(name = "vaultAddress")
    val vaultAddress: String,
    @Json(name = "amount")
    val amount: BigDecimal,
)