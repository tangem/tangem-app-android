package com.tangem.grow.datasource.polymarket.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** The signed batch envelope shared by `POST /withdraw` and `POST /positions/redeem`. */
@JsonClass(generateAdapter = true)
data class PolymarketRelayedBatchRequest(
    @Json(name = "ownerAddress") val ownerAddress: String,
    @Json(name = "depositWalletAddress") val depositWalletAddress: String,
    @Json(name = "nonce") val nonce: String,
    @Json(name = "deadline") val deadline: String,
    @Json(name = "calls") val calls: List<PolymarketApprovalCallDto>,
    @Json(name = "signature") val signature: String,
)

/** The BFF's own record of a relayed batch, read back by transaction id. */
@JsonClass(generateAdapter = true)
data class PolymarketRelayedBatchResponse(
    @Json(name = "transactionId") val transactionId: String,
    @Json(name = "state") val state: String,
    @Json(name = "transactionHash") val transactionHash: String? = null,
)