package com.tangem.grow.datasource.polymarket.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PolymarketWithdrawRequest(
    @Json(name = "ownerAddress") val ownerAddress: String,
    @Json(name = "depositWalletAddress") val depositWalletAddress: String,
    @Json(name = "nonce") val nonce: String,
    @Json(name = "deadline") val deadline: String,
    @Json(name = "calls") val calls: List<PolymarketApprovalCallDto>,
    @Json(name = "signature") val signature: String,
)

@JsonClass(generateAdapter = true)
data class PolymarketWithdrawResponse(
    @Json(name = "transactionId") val transactionId: String,
    @Json(name = "state") val state: String,
    @Json(name = "transactionHash") val transactionHash: String? = null,
)