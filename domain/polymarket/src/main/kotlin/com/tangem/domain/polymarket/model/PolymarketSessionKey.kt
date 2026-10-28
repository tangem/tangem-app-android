package com.tangem.domain.polymarket.model

import java.time.Instant

/**
 * @property address lowercase key address
 * @property deviceId `null` only for a signer the BFF learned of through its revocation
 * @property isEnabled whether the app signs orders with this key; does not affect the key at Polymarket
 * @property scopes `["CLOB"]`, or empty for a signer the BFF learned of through its revocation
 * @property validUntil when the key stops working at Polymarket, exactly as signed
 * @property renewAfter from when renewal is offered
 * @property authorizedAt when the last authorization was confirmed; `null` until then

 */
data class PolymarketSessionKey(
    val address: String,
    val deviceId: String?,
    val deviceName: String?,
    val isEnabled: Boolean,
    val status: PolymarketSessionKeyStatus,
    val scopes: List<String>,
    val validUntil: Instant?,
    val renewAfter: Instant?,
    val authorizedAt: Instant?,
    val createdAt: Instant,
)

enum class PolymarketSessionKeyStatus {
    PENDING,
    ACTIVE,
    REVOKING,
    REVOKED,
    EXPIRED,
    FAILED,
    UNKNOWN,
    ;

    companion object {

        fun fromRaw(raw: String): PolymarketSessionKeyStatus = entries.firstOrNull { it.name == raw } ?: UNKNOWN
    }
}

/** @property address lowercase signer address */
data class PolymarketSessionSigner(
    val address: String,
    val scopes: List<String>,
    val validUntil: Instant?,
)

/**
 * @property validUntil Unix seconds, as signed into [call]
 * @property call the single `authorizeSessionSigner(sessionKeyAddress, validUntil)` call to the deposit wallet
 * @property replacesSessionKeyAddress an `ACTIVE` key of the same device that Polymarket no longer has
 */
data class PolymarketSessionKeyAuthorization(
    val ownerAddress: String,
    val depositWalletAddress: String,
    val sessionKeyAddress: String,
    val deviceId: String,
    val deviceName: String,
    val validUntil: String,
    val nonce: String,
    val deadline: String,
    val call: PolymarketApprovalCall,
    val signature: String,
    val replacesSessionKeyAddress: String? = null,
)

/** @property call the single `revokeSessionSigner(sessionKeyAddress)` call to the deposit wallet */
data class PolymarketSessionKeyRevocation(
    val ownerAddress: String,
    val depositWalletAddress: String,
    val sessionKeyAddress: String,
    val nonce: String,
    val deadline: String,
    val call: PolymarketApprovalCall,
    val signature: String,
)