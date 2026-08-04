package com.tangem.domain.polymarket.model

/**
 * Polymarket CLOB L2 API credentials, derived once during onboarding from an L1 signature.
 *
 * Used to sign CLOB requests with an HMAC header; must be persisted so that a re-derivation
 * (and therefore another card tap) is not required on every app launch.
 *
 * @property apiKey     public identifier of the derived API key
 * @property secret     base64 HMAC secret used to sign CLOB requests
 * @property passphrase passphrase sent alongside the HMAC signature
 */
data class PolymarketApiCredentials(
    val apiKey: String,
    val secret: String,
    val passphrase: String,
) {

    override fun toString(): String = "PolymarketApiCredentials(apiKey=REDACTED, secret=REDACTED, passphrase=REDACTED)"
}