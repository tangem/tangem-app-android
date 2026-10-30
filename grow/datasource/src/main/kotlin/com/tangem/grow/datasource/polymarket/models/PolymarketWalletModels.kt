package com.tangem.grow.datasource.polymarket.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.core.remote.moshi.SkipMalformedElements

/**
 * Response of `GET /api/predictions/v1/wallet` (BFF `WalletStatusResponse`).
 *
 * @property depositWalletAddress the stored deposit-wallet address, or `null` until deployed via us.
 * @property status one of the 7 onboarding states (unknown values are handled defensively downstream).
 * @property sessionKeys every session key of the wallet in every status, newest first; empty or `null` on a lookup
 *  by `ownerAddress` and from a backend that predates session keys.
 */
@JsonClass(generateAdapter = true)
data class PolymarketWalletStatusResponse(
    @Json(name = "depositWalletAddress") val depositWalletAddress: String?,
    @Json(name = "status") val status: String,
    @SkipMalformedElements
    @Json(name = "sessionKeys")
    val sessionKeys: List<PolymarketSessionKeyDto>? = emptyList(),
)

/** Body of `POST /api/predictions/v1/wallet/deploy` (BFF `DeployRequest`). */
@JsonClass(generateAdapter = true)
data class PolymarketWalletDeployRequest(
    @Json(name = "ownerAddress") val ownerAddress: String,
    @Json(name = "walletId") val walletId: String,
    @Json(name = "depositWalletAddress") val depositWalletAddress: String,
)

/** Body of `POST /api/predictions/v1/wallet/approvals` (BFF `ApprovalsRequest`) — the fully-signed batch. */
@JsonClass(generateAdapter = true)
data class PolymarketWalletApprovalsRequest(
    @Json(name = "ownerAddress") val ownerAddress: String,
    @Json(name = "depositWalletAddress") val depositWalletAddress: String,
    @Json(name = "nonce") val nonce: String,
    @Json(name = "deadline") val deadline: String,
    @Json(name = "calls") val calls: List<PolymarketApprovalCallDto>,
    @Json(name = "signature") val signature: String,
)

/** BFF `ApprovalCall`. */
@JsonClass(generateAdapter = true)
data class PolymarketApprovalCallDto(
    @Json(name = "target") val target: String,
    @Json(name = "value") val value: String,
    @Json(name = "data") val data: String,
)

/** Success/accepted body of the deploy & approvals endpoints (BFF `WalletOperationResponse`). */
@JsonClass(generateAdapter = true)
data class PolymarketWalletOperationResponse(
    @Json(name = "status") val status: String,
)

/**
 * RFC-7807 ProblemDetail (`application/problem+json`) — the BFF error body for any 4xx/5xx failure.
 * Only the fields the client acts on are declared.
 *
 * @property reason machine-readable cause, e.g. `SESSION_KEY_BUSY`; absent on a `401` and on older endpoints
 */
@JsonClass(generateAdapter = true)
data class ProblemDetailResponse(
    @Json(name = "status") val status: Int?,
    @Json(name = "title") val title: String?,
    @Json(name = "detail") val detail: String?,
    @Json(name = "instance") val instance: String?,
    @Json(name = "reason") val reason: String? = null,
)