package com.tangem.datasource.api.p2p.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.joda.time.DateTime

/**
 * Response for POST /api/v1/staking/pool/{network}/staking/withdraw
 */
@JsonClass(generateAdapter = true)
data class P2PWithdrawResponse(
    @Json(name = "amount")
    val amount: Double,
    @Json(name = "vaultAddress")
    val vaultAddress: String,
    @Json(name = "delegatorAddress")
    val delegatorAddress: String,
    @Json(name = "unsignedTransaction")
    val unsignedTransaction: P2PUnsignedTransactionDTO,
    @Json(name = "createdAt")
    val createdAt: DateTime,
    @Json(name = "tickets")
    val tickets: List<String>,
)