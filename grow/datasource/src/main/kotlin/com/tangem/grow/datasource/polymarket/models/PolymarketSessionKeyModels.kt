package com.tangem.grow.datasource.polymarket.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * @property status `PENDING`, `ACTIVE`, `REVOKING`, `REVOKED`, `EXPIRED` or `FAILED`; unknown values are
 *  handled defensively downstream
 * @property scopes `["CLOB"]`, or empty for a signer the BFF learned of only through its revocation
 * @property validUntil Unix seconds, exactly as signed
 */
@JsonClass(generateAdapter = true)
data class PolymarketSessionKeyDto(
    @Json(name = "address") val address: String,
    @Json(name = "deviceId") val deviceId: String? = null,
    @Json(name = "deviceName") val deviceName: String? = null,
    @Json(name = "enabled") val isEnabled: Boolean,
    @Json(name = "status") val status: String,
    @Json(name = "scopes") val scopes: List<String> = emptyList(),
    @Json(name = "validUntil") val validUntil: Long? = null,
    @Json(name = "renewAfter") val renewAfter: Long? = null,
    @Json(name = "authorizedAt") val authorizedAt: Long? = null,
    @Json(name = "createdAt") val createdAt: Long,
)

/**
 * @property calls exactly one `authorizeSessionSigner(sessionKeyAddress, validUntil)` call to the deposit wallet
 * @property replacesSessionKeyAddress an `ACTIVE` key of the same device that Polymarket no longer has
 */
@JsonClass(generateAdapter = true)
data class PolymarketAuthorizeSessionKeyRequest(
    @Json(name = "ownerAddress") val ownerAddress: String,
    @Json(name = "depositWalletAddress") val depositWalletAddress: String,
    @Json(name = "sessionKeyAddress") val sessionKeyAddress: String,
    @Json(name = "deviceId") val deviceId: String,
    @Json(name = "deviceName") val deviceName: String,
    @Json(name = "validUntil") val validUntil: String,
    @Json(name = "nonce") val nonce: String,
    @Json(name = "deadline") val deadline: String,
    @Json(name = "calls") val calls: List<PolymarketApprovalCallDto>,
    @Json(name = "signature") val signature: String,
    @Json(name = "replacesSessionKeyAddress") val replacesSessionKeyAddress: String?,
)

/** @property calls exactly one `revokeSessionSigner(sessionKeyAddress)` call to the deposit wallet */
@JsonClass(generateAdapter = true)
data class PolymarketRevokeSessionKeyRequest(
    @Json(name = "ownerAddress") val ownerAddress: String,
    @Json(name = "depositWalletAddress") val depositWalletAddress: String,
    @Json(name = "sessionKeyAddress") val sessionKeyAddress: String,
    @Json(name = "nonce") val nonce: String,
    @Json(name = "deadline") val deadline: String,
    @Json(name = "calls") val calls: List<PolymarketApprovalCallDto>,
    @Json(name = "signature") val signature: String,
)

@JsonClass(generateAdapter = true)
data class PolymarketSetSessionKeyEnabledRequest(
    @Json(name = "enabled") val isEnabled: Boolean,
)